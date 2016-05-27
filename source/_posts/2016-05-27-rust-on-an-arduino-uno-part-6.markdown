---
layout: post
title: "Rust on an Arduino Uno, Part 6"
date: 2016-05-27 11:11:11 -0400
comments: true
categories: arduino rust
---

We can't yet compile the stock version of libcore, so in the meantime
we have our own version with the essentials. Because we've directly
added this code to our project, each recompile takes a while. It'd be
really nice if we could use Cargo like a Real Rust Project would,
allowing us to compile our modified libcore once and reuse it.

<!-- more -->

Create a new library crate (`cargo new avr-core`) and move all of the
hacked-up core files that we created before into the `src` directory:

- clone.rs
- cmp.rs
- intrinsics.rs
- marker.rs
- ops.rs
- option.rs

Additionally, create a `lib.rs` with the top-level core items:

- all the `feature` flags
- the `prelude`
- module references
- the `eh_personality` and `panic` handlers.

Now we can create a binary crate that will use our AVR-compatible
libcore. After `cargo new --bin blink`, add a path to the core library:

```
[dependencies.avr-core]
path = "../avr-core"
```

We can remove a bunch of junk from our `main.rs` and just import the
interesting core items:

```
extern crate avr_core;

use avr_core::prelude::*;
use avr_core::intrinsics::{volatile_load, volatile_store};
use avr_core::marker::PhantomData;
```

Now when we compile, we only need to rebuild our own code, not all of
libcore! Much better.

----

Let's continue improving our code. The last thing we did was to hook
up an interrupt handler for the timers, but we had to add a bunch of
assembly to make the handler behave in the proper way. As suggested in
the previous post, there's a much better way to do it.

Rust allows us to declare `extern` functions with a *calling
convention*. A calling convention describes where the arguments are
located, where the return value should be placed, and what registers a
function is allowed to change.

There are two special calling conventions for AVR code:
`avr-interrupt` and `avr-non-blocking-interrupt`. They are basically
the same, except that the latter immediately re-enables interrupt
handling when it starts. With the former, you don't have to worry
about one interrupt happening while you are handling another.

That means we can rewrite our interrupt handler much easier:

```
#[no_mangle]
pub unsafe extern "avr-interrupt" fn _ivr_timer1_compare_a() {
    let prev_value = volatile_load(PORTB);
    volatile_store(PORTB, prev_value ^ PINB5);
}
```

----

Now that we are using Cargo, it would be nice if we didn't have to
directly call `avr-gcc` ourselves. We can accomplish this with a
*target file*. This is JSON configuration that can enhance the Rust
compiler's knowledge about how to compile a piece of code.

There are many fields that are required (check the [repository][repo]
for the full reference), but the important one is that we can tell the
compiler to use `avr-gcc` as our linker:

```
  "linker": "avr-gcc",
  "pre-link-args": ["-mmcu=atmega328p", "-nostartfiles", "../interrupt_vector.S"],
  "exe-suffix": ".elf",
  "post-link-args": ["-Wl,--no-gc-sections"],
```

And we can use this JSON target file when compiling:

```
cargo build --release --target=./arduino.json
```

This will create our ELF file, automatically linking to our interrupt
vector definition, and ready to be processed with `avr-objcopy` and
uploaded to the board. We are getting closer and closer to an
enjoyable development experience!

As before, the [complete source][repo] is available on Github.

[repo]: https://github.com/shepmaster/rust-arduino-blink-led-no-core-with-cargo
