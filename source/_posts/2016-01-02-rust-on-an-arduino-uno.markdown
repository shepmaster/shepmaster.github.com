---
layout: post
title: "Rust on an Arduino Uno"
date: 2016-01-02 15:26:54 -0500
comments: true
categories: arduino rust
---

We have an [Arduino Uno][] that's been sitting around gathering
dust for a little while, so I decided to see how [Rust][] worked
on it.

<!-- more -->

A bit of searching led to a
[fork of Rust with AVR support, AVR-Rust][avr-rust]. This is built on
top of a [fork of LLVM with AVR support, AVR-LLVM][avr-llvm]. Both of
these projects are led by [Dylan McKay][].

The current documentation for AVR-Rust is a bit lacking, and it was
forked from a development version of Rust 1.4. The current development
version is Rust 1.7, making the fork about 4.5 months old. However,
the changes to LLVM are in the process of being merged into upstream,
laying the groundwork for merging the changes into Rust as well.

Let's start out by doing the bare minimum and try to get a version of
`rustc` that can target the AVR chip:

```
git clone https://github.com/avr-rust/rust.git
mkdir build && cd build
../rust/configure
make
```

You'll note that there's nothing AVR specific here. Every Rust
compiler is actually a *cross-compiler*, a compiler that executes on
one architecture but produces code for another architecture. Because
this fork of Rust has support files for AVR, it will be able to
produce the correct executable code.

Unfortunately, I [couldn't get a basic file to compile][first-compile]
out of the box.

So I did what any sane person would do - I started changing code
without knowing exactly what the failure was or what the code I was
changing did.

First I tried updating the branch of LLVM that AVR-Rust uses. There
are two branches in the repository - [`avr-support`][] is more
actively updated and [`avr-rust-support`][] lags behind.

Merging `avr-support` into `avr-rust-support` went smoothly, but the
Rust LLVM driver code needed to be updated to handle the newer LLVM
version. I grabbed the diff from the main Rust repository and applied
that. This seemed to work, but then I got a segfault from the stage 1
Rust compiler, deep in the internals of LLVM.

```
make: *** [x86_64-apple-darwin/stage1/lib/rustlib/x86_64-apple-darwin/lib/stamp.term] Segmentation fault: 11
```

So I continued changing more stuff!

I merged current Rust into the AVR fork of Rust and resolved the merge
conflicts as best I could figure out. After fixing a few new errors
and some poor merge conflicts, I was on my way. Until I hit the
segfault again.

That means it's actually time to try to figure out where the segfault
was coming from. I configured another build with some debug information:

```
./configure --enable-debug --disable-docs --enable-llvm-assertions --enable-debug-assertions
```

And built. This takes a long time, as nothing gets optimized. And then
it turns out that doing this also hides the segfault. Ugh.

However, I do get to a new error:

```
ld: unknown option: --as-needed
clang: error: linker command failed with exit code 1 (use -v to see invocation)
```

Fortunately, I know where to tweak that in the source. The downside is
I'll need to wait for another long build cycle...

Continue on to [part 2][].

[Arduino Uno]: https://www.arduino.cc/en/Main/ArduinoBoardUno
[Dylan McKay]: https://github.com/dylanmckay
[Rust]: https://www.rust-lang.org/
[`avr-rust-support`]: https://github.com/avr-llvm/llvm/tree/avr-rust-support
[`avr-support`]: https://github.com/avr-llvm/llvm/tree/avr-support
[avr-llvm]: https://github.com/avr-llvm/llvm/
[avr-rust]: https://github.com/avr-rust/rust/
[first-compile]: https://github.com/avr-rust/rust/issues/13
[part 2]: /blog/2016/01/17/rust-on-an-arduino-uno-part-2/
