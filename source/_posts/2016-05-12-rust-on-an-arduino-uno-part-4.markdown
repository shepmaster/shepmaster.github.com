---
layout: post
title: "Rust on an Arduino Uno, Part 4"
date: 2016-05-12 12:04:43 -0500
comments: true
categories: arduino rust
---

When we left off, we were [blinking the LED][part3]. Let's take a
brief detour and document how to get a working Rust compiler. This is
mostly a way for me to document what I've been doing so I can find it
again!

<!-- more -->

We are going to start by getting a local version of LLVM that supports
targeting AVR. After cloning [the repository][avr-llvm], we will need
to set up for a build. Note that the upstream `avr-rust-support`
branch sometimes lags compared to `avr-support`, so you will probably
want to merge the two branches to get any updates.

```
cd avr-llvm
git checkout avr-support
git merge origin/avr-rust-support
mkdir -p debug/build
cd debug/build
```

We will then configure LLVM. This *particular* configuration I have
here is based off the current Rust build and is specific to OS X (see
the `C_FLAGS` and `CXX_FLAGS`). If you are using a different platform,
you'll need to poke at the Rust build process to see the appropriate
flags.

Last updated: **2016-11-06**

```
cmake ../.. \
  -DCMAKE_BUILD_TYPE=Debug \
  -DLLVM_TARGETS_TO_BUILD="X86;AVR" \
  -DLLVM_INCLUDE_EXAMPLES=OFF \
  -DLLVM_INCLUDE_TESTS=OFF \
  -DLLVM_INCLUDE_DOCS=OFF \
  -DLLVM_ENABLE_ZLIB=OFF \
  -DWITH_POLLY=OFF \
  -DLLVM_ENABLE_TERMINFO=OFF \
  -DLLVM_INSTALL_UTILS=ON \
  -DCMAKE_C_FLAGS="-ffunction-sections -fdata-sections -m64 -fPIC -stdlib=libc++" \
  -DCMAKE_CXX_FLAGS="-ffunction-sections -fdata-sections -m64 -fPIC -stdlib=libc++" \
  -DCMAKE_INSTALL_PREFIX=..
```

Then it's just a matter of building and installing. Since it created
normal `Makefile`s for me, I passed an extra make flag to build in
parallel. The LLVM build is pretty fast this way!

```
cmake --build . -- -j7
cmake --build . --target install
```

Then we need to build Rust with this custom LLVM. After cloning
[the repository][avr-rust], set up the structure:

```
cd avr-rust
git checkout avr-support
mkdir -p debug
cd debug/
```

AVR-LLVM is based on a very new version of LLVM, so we need to use the
in-progress Rust build system called "rustbuild". Using in-development
build systems with in-development compilers, what could go wrong?

Note that it's very important to use an absolute path to your LLVM
directory.

```
../configure \
  --enable-rustbuild \
  --enable-debug \
  --disable-docs \
  --enable-debug-assertions \
  --disable-jemalloc \
  --llvm-root=/absolute/path/to/avr-llvm/debug
```

Then we build!

```
make -j7
```

**4 or more hours later**, you will have a fully-built
compiler. However, you can usually get up-and-running earlier by using
the stage 1 compiler, located in `debug/build/*/stage1`. This will be
available pretty quickly, before the entire build is complete.

We then add this build as a [rustup][] toolchain and use it as the
override compiler in a directory:

```
rustup toolchain link avr /path/to/rust/debug/build/*/stage1
rustup override set avr
```

Note that this will only produce a cross-compiler; none of the
libraries that make things actually work. That's still coming!

[avr-llvm]: https://github.com/avr-llvm/llvm
[avr-rust]: https://github.com/avr-rust/rust
[cargo]: https://github.com/rust-lang/cargo#installing-cargo
[rustup]: https://rustup.rs/
[part3]: /blog/2016/01/24/rust-on-an-arduino-uno-part-3/
