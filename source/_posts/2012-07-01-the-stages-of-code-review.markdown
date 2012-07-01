---
layout: post
title: "The stages of code review"
date: 2012-07-01 13:37
comments: true
categories:
---

We recently started using [gerrit][gerrit] to perform code reviews for
a legacy C codebase that I work on. I also help out on a couple of
newer Java and Ruby projects that have had the benefit of having code
reviews and testing infrastructure from day one.

Starting to use gerrit on our C code has led me to think about how we
approach code reviews, and I've identified some stages that we have
gone through. It's loosely sorted by the order in which we adopted
each check. While not every commit needs each point, this is a general
idea of what might be required.

1. Functionality

   This was what started us on the code review path - sometimes we
   would commit something that just didn't work right. Occasionally,
   the code wouldn't even compile! To try and address these problems,
   we would have a coworker give the code a once-over before pushing
   it. We actually started doing this long before gerrit by walking
   over to another desk.

2. Function names

   One of our explicit coding conventions is that non-static functions
   must be prefixed with the module name they belong to. This keeps us
   sane and helps prevent name collisions. We also have a few
   conventions for constructors, destructors and other common
   patterns. These are all easy to check for and was something we
   started doing almost immediately.

3. Resource leaks

   After a few annoying memory leaks got committed, we started looking
   at the code with a critical eye for all kinds of leaks. Resource
   leaks are easy enough to add and subsequently miss, especially in
   C. Leaks are really just a special type of non-functioning code,
   but one that bites you days/weeks/years later instead of
   immediately.

   Sometimes we use a tool such as [valgrind][valgrind] to test for
   leaks, but in many cases we just inspect the code visually. We
   check to see if resources are handled consistently and pay special
   attention to various error conditions.

4. Efficiency

   For better or worse, we almost always worry about how optimized our
   code is. Sometimes this is can be a valuable exercise, but in most
   situations it was probably overkill. There's just a warm fuzzy
   feeling when you catch an O(n<sup>2</sup>) algorithm that could be
   O(n log n), even if you spend more time coming up with the faster
   algorithm than will ever be saved in runtime. Since this focus on
   optimization is part of our culture, it has found it's way into our
   reviews.

5. Tests

   Tests fall lower on this list than I would prefer. Unit testing C
   code is, at best, hard and/or annoying. Add the fact that trying to
   test code that was never designed to be tested is painful, and you
   can easily see why we tend to turn a blind eye when a commit
   doesn't add any new tests.

6. Documentation

   Code written in C should probably have more documentation than most
   other languages, simply due to all the ways you can shoot yourself
   in the foot. For example, you can't tell if any given function will
   take ownership of the pointer you pass it, that information has to
   be documented somewhere. When we prefix our function names with the
   module name, the descriptive part of the name is often shortened to
   prevent extremely long function names. This means the function
   documentation is vital to understand what the function does.

   Reviewing documentation often comes down to checking that it makes
   sense and is proper English. It's impressive how mangled a sentence
   can get when you refactor the code it is trying to describe.

7. Coding style

   Many different aspects of code fall into "style": the contents,
   formatting and spelling of comments; the names of variables, static
   functions, and structures; the size and complexity of functions;
   the contents of structures.

   These stylistic issues can be difficult to talk about in a code
   review, since sometimes it comes down to personal preference. The
   best you can do is express your preference and try to sway the
   other developer to your line of thinking. It helps if both people
   realize that the code has to be read and maintained by the entire
   development group.

8. Test style

   Test code style is an entirely different kettle of fish from
   production code style. A test needs to focus on how a user would
   want to use the code. It should minimize the clutter required to
   perform the test so as to make the test as readable as possible,
   while still highlighting the interesting part under test.

   Similar to code style, the difficulty reviewing tests comes from
   differences in personal preference. This is compounded by the fact
   that we are not as experienced with writing great tests yet.

9. Commit message

   Right now, this is my holy grail of code reviews. I once spent 15
   minutes skimming through the git log to determine if we had a
   preferred *verb tense* in our commit messages (we did). More
   reasonably, this can involve ensuring that commit messages are
   capitalized consistently and that they describe why a change is
   being made, not just what or how.

#### Where we are now

The C project is currently somewhere around the "Tests" or
"Documentation" stages. The Java and Ruby projects expect tests and
documentation, so they hover around the "Code style" and "Test style"
stages; I've only had one or two opportunities to correct someones
verb tense :-).

[gerrit]: http://code.google.com/p/gerrit/
[valgrind]: http://valgrind.org/