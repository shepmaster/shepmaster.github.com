---
layout: post
title: "Rust on an Arduino Uno, Part 2"
date: 2016-01-17 14:34:54 -0500
comments: true
categories: arduino rust
---

After my [previous attempt][part1], I started to think that the
issues were caused by an inability to completely link the program. If
that were the case, could we try to link in a different way?

Through a bit of trial and error, I was able to generate an object
file:

```
rustc --target avr-atmel-none hello.rs --emit obj
```

<!-- more -->

Checking the disassembly of this file with `objdump -d hello.o` showed
promise:

```
00000000 <main>:
   0:   0e 94 00 00     call    0     ; 0x0 <main>
   4:   08 95           ret

Disassembly of section .text._ZN4main10__rust_abiE:

00000000 <_ZN4main10__rust_abiE>:
   0:   8f ef           ldi r24, 0xFF ; 255
   2:   84 b9           out 0x04, r24 ; 4
   4:   00 c0           rjmp    .+0   ; 0x6 <LBB1_1>

00000006 <LBB1_1>:
   6:   8f ef           ldi r24, 0xFF ; 255
   8:   85 b9           out 0x05, r24 ; 5
   a:   80 e0           ldi r24, 0x00 ; 0
   c:   85 b9           out 0x05, r24 ; 5
   e:   fb cf           rjmp    .-10  ; 0x6 <LBB1_1>
```

I then used an existing installation of GCC with AVR support to finish
linking the code together:

```
avr-gcc -mmcu=atmega328p hello.o -o hello.elf
```

Taking a look at the disassembly of this code shows a lot of things
that were not present in the original object file:

1. The interrupt vector table is established. This occupies about the
   first 25 instructions. Each instruction is a jump to the
   appropriate interrupt handler. Most importantly, table index 0 is
   the reset "interrupt" which controls where the processor should
   jump to when it is initialized.
2. The EEPROM Control Register and GPIOR0 are initialized and external
   interrupts are disabled. Then `main` is called.
3. After `main` returns, interrupts are disabled and the chip goes
   into an infinite loop.

### Getting code on board

Now that we have a compiled binary, we need to get it onto the Arduino
proper. [avrdude][] is an in-system programmer that will allow us to
upload the compiled code, but it prefers input in a different format:
Intel HEX. We can convert using `avr-objcopy`:

```
avr-objcopy -O ihex -R .eeprom hello.elf hello.hex
```

Now we can upload to the Arduino:

```
avrdude -p atmega328p -c arduino -P /dev/cu.usbmodem1411 -U flash:w:hello.hex:i
```

The Arduino Uno has a second Atmel chip (ATmega16U2) that looks like a
USB-to-serial device to the host computer. On my OS X computer, that
device shows up at `/dev/cu.usbmodem1411`. Your location will differ.

### It's alive!

Because I have such a basic level of code, I can't do anything nice
like blink an LED. Instead, I can write a tight loop that just turns
the LED on or off some percentage of the time. This allows it to have
a different relative brightness, which in turn lets me see that the
code changes are actually happening.

Check out the LED marked `L` in the following pictures.

#### LED on 100% of the time

<img src="/images/blog/arduino_led/100.jpg" alt="LED at 100%" />

#### LED on 50% of the time

<img src="/images/blog/arduino_led/050.jpg" alt="LED at 50%" />

#### LED on 1% of the time

<img src="/images/blog/arduino_led/001.jpg" alt="LED at 1%" />

### What's next?

This isn't the most *elegant* of solutions, and there are a lot of
avenues to explore:

1. **Avoid installing `avr-gcc` and `avr-objcopy`**. Right now,
   `avr-gcc` is used when compiling Rust itself (for `compiler-rt`)
   and to finish assembly of the executable. It would be ideal if all
   of this could be handled within an AVR-enabled Rust or LLVM.

2. **Set interrupt handlers**. I think the typical solution is
   to use a linker script, but that's one more moving piece I'd like
   to avoid adding.

3. **Compile `libcore`**! In order to get the most basic of things
   to compile, I had to straight-up copy code from `libcore`. An
   impressive amount of things are included there. Things you might
   want to use, like *addition*, not to mention `Option` or anything
   having to do with iterators. `libstd` is unlikely to ever be
   supported as it relies on memory allocation.

4. **Merge the Rust fork of LLVM with the AVR fork of LLVM**. The more
   frequently these are merged, the easier it will be to eventually
   include the AVR support in Rust proper. I started to do this, but
   had a large number of merge conflicts so I backed off.

5. **Compile AVR-enabled Rust in non-debug mode**. For some reason,
   when I compile Rust without debugging symbols, I get an "exciting"
   assertion failure from deep within LLVM. That is most likely a
   symptom of some problem that should be fixed.

### TL;DR

Check out [my repo][] for an example that worked for me. In short:

```
# Compile object
rustc --target avr-atmel-none -C target-cpu=atmega328p --emit=obj hello.rs -o hello.o
# Link together
avr-gcc -mmcu=atmega328p hello.o -o hello.elf
# Reformat for upload
avr-objcopy -O ihex -R .eeprom hello.elf hello.hex
# Upload to the board
avrdude -p atmega328p -c arduino -P /dev/cu.usbmodem1411 -U flash:w:hello.hex:i
```

If you are on OS X, you can install the things you need (except an
AVR-enabled Rust build) with [homebrew][]:

```
brew tap osx-cross/avr
brew install avr-libc
brew install avrdude
```

[avrdude]: http://www.nongnu.org/avrdude/
[homebrew]: http://brew.sh/
[my repo]: https://github.com/shepmaster/rust-arduino-blink-led-no-core
[part1]: /blog/2016/01/02/rust-on-an-arduino-uno/
