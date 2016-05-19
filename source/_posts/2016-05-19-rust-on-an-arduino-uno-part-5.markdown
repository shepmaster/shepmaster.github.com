---
layout: post
title: "Rust on an Arduino Uno, Part 5"
date: 2016-05-19 13:57:04 -0400
comments: true
categories: arduino rust
---

Previously, we wrote some code that allowed us to
[sleep by waiting for a number of cycles to pass][part3]. However, we
had to peek at the disassembly to know how many cycles we were
spending and adapt our source code to match. While it got us started,
it's not a very elegant solution.

<!-- more -->

The Arduino Uno uses an ATmega328P processor. One of the features of
this processor are 3 built-in timers that can trigger *interrupts* at
certain periods. Interrupts are special bits of code that take over
control of the processor when something important happens. These are
often time-critical things that need to be handled quickly.

What would be ideal is if we could rely on the timer feature to
implement our `sleep` method. To get started, we are going to need the
ability to specify the *interrupt vector*.

The interrupt vector is a table of 26 instructions that must be placed
at a specific section in memory. Each element in the table corresponds
to a specific interrupt, and should consist of one instruction that
jumps to the appropriate interrupt handler.

To do this, we need to write a little bit of assembly:

```
ivr:
        jmp _ivr_reset
        jmp _ivr_irq0
        jmp _ivr_irq1
        ;; continues for all the rest
```

In order to use this, we need to include it when linking all of our
code together. We also have to disable the existing interrupt vector
that would be added. This is done via the `-nostartfiles` flag:

```
avr-gcc -mmcu=atmega328p interrupt_vector.S hello.o -nostartfiles -o hello.elf
```

If you compile right now, you will get a whole bunch of errors of the
form:

```
temp_file_name.o: In function `ivr':
(.text+0x0): undefined reference to `_ivr_reset'
```

Our interrupt vector is trying to jump to a bunch of symbols that we
haven't yet defined. We could do the simple thing and define a bunch
of `_ivr_*` methods in Rust (and I did, to start with), but that's
rather annoying. Instead, we can use *weak linking* to define a kind
of "fallback" symbol. We will have one simple handler that just
returns from the interrupt, and set each handler to use that unless it
is defined:

```
_ivr_undefined:
        reti

.weak _ivr_irq0
.set  _ivr_irq0, _ivr_undefined
;;; And so on
```

The only outlier is `_ivr_reset` which we define to point to our
`main` method, avoiding extraneous indirection. At this point, we
should be compiling again, but not using the interrupts yet. Let's
change that.

Following [this guide][timer-guide], we can see all the details of
setting up the timer. At a high level it's:

1. Register an interrupt handler.
1. Disable interrupts.
1. Set a bunch of values as determined by the datasheet and math.
1. Enable interrupts.

We will copy all of the values and registers from this article to
setup timer 0, but with a 1kHz rate instead of 2kHz. This matches
nicer with our `sleep_ms` method which waits milliseconds.

Let's use a little bit of nice Rust for a change. When we disable
interrupts, we *really* want to make sure we enable them again! In a
language like Rust, we can use a (misleadingly labeled) pattern known
as Resource Acquisition Is Initialization (RAII). We will create a
`struct` that disables interrupts when it is created and enables them
when the struct is dropped. This means we can never forget to
re-enable interrupts as the compiler will ensure things are restored!

```
struct DisableInterrupts(PhantomData<()>);
impl DisableInterrupts {
    fn new() -> DisableInterrupts {
        unsafe { asm!("CLI") }
        DisableInterrupts(PhantomData)
    }
}

impl Drop for DisableInterrupts {
    fn drop(&mut self) {
        unsafe { asm!("SEI") }
    }
}
```

We can bundle this into a nice wrapper:

```
fn without_interrupts<F, T>(f: F) -> T
    where F: FnOnce() -> T
{
    let _disabled = DisableInterrupts::new();
    f()
}
```

And use it like so:

```
without_interrupts(|| {
    volatile_store(TCCR0A, 0);
});
```

To define the interrupt handler, we simply create a method that
matches the expected name from our assembly file. The method simply
increments a global variable each time it is triggered:

```
static mut N_MS_ELAPSED: u8 = 0;

#[no_mangle]
pub unsafe extern fn _ivr_timer0_compare_a() {
    N_MS_ELAPSED += 1;
}
```

And re-implement our `sleep_ms` function to:

```
fn sleep_ms(duration_ms: u8) {
    unsafe {
        volatile_store(&mut N_MS_ELAPSED, 0);
        while volatile_load(&mut N_MS_ELAPSED) < duration_ms {
            // spin
        }
    }
}
```

Compile this and load it onto the board, and we are greeted with the
sight of *nothing blinking*. It's time to dig into more
disassembly. Here's what `_ivr_timer0_compare_a` looks like:

```
lds     r24, 0x0000
inc     r24
sts     0x0000, r24
ret
```

Checking the instruction set manual and the datasheet, we will notice a few problems:

1. We use `ret` (Return from Subroutine) instead of `reti` (Return from Interrupt).
1. We do not save and restore the Status register.
1. We do not save and restore the `r24` register.

Let's modify our handler with a bit more assembly to address all three issues:

```
#[no_mangle]
pub unsafe extern fn _ivr_timer0_compare_a() {
    asm!{
        "PUSH R24
         IN R24, 0x3F
         PUSH R24"
    };

    N_MS_ELAPSED += 1;

    asm!{
        "POP R24
         OUT 0x3F, R24
         POP R24
         RETI"
    };
}
```

That's certainly a bit longer, but it compiles and works again! And it
will continue to work, so long as the compiler always decides to use
`r24` for the incremented value, something we have no control over. As
you might guess, there's a better solution.

[part3]: /blog/2016/01/24/rust-on-an-arduino-uno-part-3/
[timer-guide]: http://www.instructables.com/id/Arduino-Timer-Interrupts/?ALLSTEPS
