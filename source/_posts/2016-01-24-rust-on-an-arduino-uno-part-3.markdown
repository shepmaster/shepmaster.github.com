---
layout: post
title: "Rust on an Arduino Uno, Part 3"
date: 2016-01-24 13:11:12 -0500
comments: true
categories: arduino rust
---

Now that we can [turn an LED on][part2], let's see if we can do
something more exciting: make the LED blink. Surprisingly, this is
more difficult than you might expect!

<!-- more -->

Blinking boils down to "turn the light on, wait a while, turn the
light off, wait a while" and repeat forever. We already know how to
turn the light on and off, as well as repeating forever. The trick
lies in "wait a while".

In a conventional Rust application, we'd probably call something like
[`std::thread::sleep`][], but we don't have access to `libstd` on an
Arduino as that library is too high-level. We will have to implement
it ourselves!

It's easy enough, all we have to do is loop a bunch of times. If the
Arduino processor runs at 16MHz, we can waste 16000 cycles to take one
millisecond. We will execute a `nop` instruction to waste the time:

```
fn sleep_ms(duration_ms: u16) {
    const FREQUENCY_HZ: u32 = 16_000_000;
    const CYCLES_PER_MS: u16 = (FREQUENCY_HZ / 1000) as u16;

    for _ in 0..duration_ms {
        for _ in 0..CYCLES_PER_MS {
            unsafe { asm!("nop"); }
        }
    }
}
```

Just compile this and away we go! Or not...

```
error: failed to resolve. Maybe a missing `extern crate iter`? [E0433]
     for _ in 0..duration_ms {
     ^~~~

error: unresolved name `iter::IntoIterator::into_iter` [E0425]
```

Right, we haven't actually defined any of the `Iterator` logic; that's
in `libcore` which we don't have yet. Let's skip that and do something
a little more C-like. We can just loop and increment integers and
compare them:

```
fn sleep_ms(duration_ms: u16) {
    const FREQUENCY_HZ: u32 = 16_000_000;
    const CYCLES_PER_MS: u16 = 16_000;

    let mut outer = 0;
    while outer < duration_ms {
        let mut inner = 0;
        while inner < CYCLES_PER_MS {
            unsafe { asm!("nop"); }
            inner += 1;
        }
        outer += 1;
    }
}
```

And... that fails too:

```
error: binary operation `/` cannot be applied to type `u32` [E0369]
     const CYCLES_PER_MS: u16 = (FREQUENCY_HZ / 1000) as u16;
                                 ^~~~~~~~~~~~
```

Ok, no division, even if it is just a constant and should be computed
at compile time. Well, we can hard code it for the moment...

```
error: binary operation `<` cannot be applied to type `_` [E0369]
     while outer < duration_ms {
           ^~~~~

error: binary assignment operation `+=` cannot be applied to type `u16` [E0368]
         outer += 1;
         ^~~~~
```

OK, wow, no addition or comparison either. There's no way around
this - we really need `libcore` or else we are stuck with a pretty
primitive environment. Since we know we have issues compiling all of
libcore, let's try a smaller part, just enough to compile this
example.

Previously, we had copied in some small snippets from libcore, but
let's replace those excerpts with the complete files and drag in a few
more. After some trial-and-error, this small set compiles:

* `clone`
* `cmp`
* `intrinsics`
* `marker`
* `ops`
* `option`

With it compiling, let's actually call `sleep_ms` in our `main` and
load the program onto the board:

```
loop {
    sleep_ms(500);
    volatile_store(PORTB, 0xFF); // Everything is on
    sleep_ms(500);
    volatile_store(PORTB, 0x00); // Everything is off
}
```

<video src="/images/blog/arduino_led/blink.mp4" controls>
<a href="/images/blog/arduino_led/blink.mp4">
A video of the blinking LED.
</a>
</video>

Look at that nice, steady blinking. Blinking at a rate that is
*nothing* like 500 milliseconds. Let's take a look at the disassembly
for the inner loop to understand why:

```
adiw ;; Add word (16-bit)            ;; 2 cycles
cp   ;; Compare registers            ;; 1 cycle
cpc  ;; Compare registers with carry ;; 1 cycle
brcs ;; Branch if carry set          ;; 1 cycle (false) / 2 cycles (true)
```

We increment our counter and check to see if we've exceeded our
limit. In all cases except the last iteration we will branch back to
the beginning of the loop, bringing the total cycle count of the loop
to six. Compare that to the naive calculation that the `nop` would
take one cycle and the rest of the loop would be free. Dividing the
inner loop constant by six gets us much closer to the appropriate
duration.

The outer loop and the function call itself also have some overhead,
but these only add up to a few cycles per inner loop. Since the inner
loop corresponds to many thousands of cycles, a few cycles is a small
error and I think can be safely ignored.

An interesting aside is that I have no idea why the `nop` does not
occur inside the loop. The compiler has reordered the code such that
the `nop` occurs in the variable initialization of the function. You
can change the code to just `asm!("")` and accomplish the same goal of
preventing the loop from being optimized away.

Next time, we will see if we can do something a little more structured
than counting cycles to sleep. As before, check out
[the repository][repo] for the code up to this point.

[part2]: /blog/2016/01/17/rust-on-an-arduino-uno-part-2/
[`std::thread::sleep`]: http://doc.rust-lang.org/std/thread/fn.sleep.html
[repo]: https://github.com/shepmaster/rust-arduino-blink-led-no-core/tree/part3
