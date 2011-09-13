---
layout: post
title: "Uninitialized variable warnings and compiler optimizations"
date: 2011-01-27 12:00
comments: true
categories: [C, GCC, clang]
redirects:
- /uninitialized-variable-warnings-and-compiler-0
---

A coworker recently got the `may be used uninitialized in this
function` warning from GCC and asked me to take a look at it to see if
the warning was spurious or if he was missing something. While trying
to boil his code down to a concise example, we found an example where
the warning got flipped - we no longer got a warning even though we
both expected one.

<!--more-->

Here is the example code we ended up with. `foo` is a function that
should set `value` when the parameter is true. If the parameter is not
set, then `value` is undefined. The main simply calls `foo`, using the
command-line argument count to control what is passed to the function.

{% include_code uninit-warning.c %}

We compiled it with GCC, expecting to see a warning.

    $ gcc --version
    gcc (Gentoo 4.4.4-r2 p1.2, pie-0.4.5) 4.4.4
    $ gcc -o uninit-gcc uninit.c -O3 -Wall -pedantic

Strange, no warning. What does Clang say?

    $ clang --version
    clang version 2.8 (branches/release_28)
    $ clang -o uninit-clang uninit.c -O3 -Wall -pedantic

Neither compilation produces warnings! We tried compiling with -O0 and
-Os, in case some optimization was being applied, but the results were
the same. Curiosity piqued, we dropped down to the next most logical
level - assembly.

## GCC disassembly
 
    0000000000400570 <foo>:
      400570:       be 9c 06 40 00          mov    $0x40069c,%esi
      400575:       bf 01 00 00 00          mov    $0x1,%edi
      40057a:       31 c0                   xor    %eax,%eax
      40057c:       e9 d7 fe ff ff          jmpq   400458 <__printf_chk@plt>
      400581:       66 66 66 66 66 66 2e    nopw   %cs:0x0(%rax,%rax,1)
      400588:       0f 1f 84 00 00 00 00 
      40058f:       00 

If you want to follow along, I used the [AMD64 Architecture Programmer's Manual][1] as an assembly reference.

GCC starts out by loading the address of the string into `%esi`, then
loads 1 into `%edi` and `%eax` is then XORed against itself, which
sets `%eax` to 0. According to [the X86_64 calling convention][3],
`%edi` and `%esi` are the first and second integral parameters to the
function about to be called and `%eax` is used as the return value
from the current function. Since we only have one parameter to printf,
and `foo` is a void function, I think that `%edi` and `%eax` must have
something to do with calling a variadic function. *The code
**unconditionally** jumps to printf.* The function finishes off with
nopw, a [fancy way of using a NOP][2] to pad out to a nice even
boundary.

## Clang disassembly

    0000000000400540 <foo>:
      400540:       55                      push   %rbp
      400541:       48 89 e5                mov    %rsp,%rbp
      400544:       bf 5c 06 40 00          mov    $0x40065c,%edi
      400549:       5d                      pop    %rbp
      40054a:       e9 e1 fe ff ff          jmpq   400430 <puts@plt>
      40054f:       90                      nop

Clang produces pretty much the same code, but does some different
gyrations. The contents of `%rpb` are pushed onto the stack, then
`%rsp` is moved to `%rpb`. The address of the string is loaded into
`%edi` and `%rbp` is restored - never having been used. Clang has
optimized the call to `printf` to instead use the simpler `puts`
function, so we don't see the same mucking about with `%edi` and
`%eax`. However, it is still an **unconditional** jump!

## Clang LLVM output

Clang has an interesting feature that lets you dump the abstract
syntax tree just like assembly.

    define void @foo(i32 %do_it) nounwind {
      %puts = tail call i32 @puts(i8* getelementptr inbounds ([8 x i8]* @str, i64 0, i64 0))
      ret void
    }

We can see that it ignores the parameter `do_it`.

## Visual Studio 2010

Reddit user DeliciouslyMoist [shows][4] that Visual Studio 2010 does
have a warning.

    cl -c -Ox -W4 test.cpp
    test.cpp
    test.cpp(18) : warning C4100: 'argv' : unreferenced formal parameter
    test.cpp(12) : warning C4701: potentially uninitialized local variable 'value' used

Along with the generated assembly. Note this is Windows assembly, so
the Linux calling conventions above don't apply.

    ?foo@@YAXH@Z (void __cdecl foo(int)):
      00000000: 83 7C 24 04 00     cmp         dword ptr [esp+4],0
      00000005: 75 07              jne         0000000E
      00000007: 83 7C 24 04 07     cmp         dword ptr [esp+4],7
      0000000C: 75 0D              jne         0000001B
      0000000E: C7 44 24 04 00 00  mov         dword ptr [esp+4],offset $SG4825
      		00 00
      00000016: E9 00 00 00 00     jmp         _printf

## Clang static analyzer

As Reddit user masklinn [points out][5], the clang static analyzer
_does_ output a warning, even though the clang compiler does not.

    $ clang --analyze uninit.c 
    uninit.c:12:9: warning: The right operand of '==' is a garbage value
      if (7 == value) {
          ^  ~~~~~
    1 warning generated.

## Lessons

The behavior and generated code of the compilers is interesting, but I
think they are allowed to do whatever they want when optimizing
undefined code, so I can't fault them for that.

What both my coworker and I were surprised was the lack of warning. In
general, we have come to rely on GCC to be overly zealous about
providing warnings. I'd much rather have an extraneous warning that I
can silence instead of no warning at all.

**Edit 2010-01-28** - added Visual Studio and clang analyzer output from reddit.

[1]: http://support.amd.com/us/Processor_TechDocs/24592.pdf
[2]: http://john.freml.in/amd64-nopl
[3]: http://en.wikipedia.org/wiki/X86_calling_conventions#System_V_AMD64_ABI_convention
[4]:http://www.reddit.com/r/programming/comments/fag84/uninitialized_variable_warnings_and_compiler/c1eht39
[5]:http://www.reddit.com/r/programming/comments/fag84/uninitialized_variable_warnings_and_compiler/c1eixa9