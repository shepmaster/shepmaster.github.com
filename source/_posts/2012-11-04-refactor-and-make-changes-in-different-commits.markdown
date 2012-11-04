---
layout: post
title: "Refactor and make changes in different commits"
date: 2012-11-04 14:31
comments: true
categories: refactoring
---

If you combine refactoring and making a change to your code into the
same commit, you are going to have a bad time.

<!-- more -->

Just in case you've forgotten, [refactoring][refactor] is

> the process of changing a computer program's source code without
> modifying its external functional behavior

When I review a commit that claims to be refactoring, I shift into to
a very specific mindset. I visualize myself as a world-class goalie,
ready to stop any rogue features that come my way; I'm going to stand
my ground, no matter what.

Contrast refactoring to adding new functionality. In that case, the
author should be adding new objects that fulfill a responsibility,
following the [open/closed principle][OCP]. When I review a commit
that adds new functionality, I pay attention that the tests cover the
new functionality, the minimum amount of code was added, and that the
new code is sufficient.

These are very different things to review for.

### Why's it bad?

When you combine refactoring and feature addition into the same
commit, you double the work required to review it. In addition to
figuring out if each changed line is correct, you also have to figure
out what "correct" even means!

Beyond the doubled work, you have to change your mindset for **every
line of code**. That's an amazing amount of context switching. It's
very hard to thoroughly review each line when it's hard to even
remember what you are reviewing for.

Combining these disparate actions into one commit isn't something that
we do maliciously. In fact, it's likely the opposite: good programmers
have an innate drive to make the code better all the time. Sometimes
we see a little problem that we just *have* to fix up.

The problem is that when we have our programmer hats on, we don't
always think about what this commit will mean to others
downstream. This could mean reviewers, approvers, testers,
documenters, whatever needs to happen after the commit.

### What do I do when my commit is too big?

There are a few main techniques I use when I discover I've done work
that should be in different commits.

If I haven't committed yet, and the changes are separate enough, I use
`git add -pu` to add certain lines of code and not others.

If the changes overlap with each other, I will edit a specific section
of the file until it looks like the intermediate change I really
wanted. I `git add` the file and immediately revert my editor
changes. I then repeat with the next section.

If I have already committed, then I go into an interactive rebase and
`edit` the particular commits that are too big. I often create a
throw-away branch so I can easily compare the original and modified
branches to make sure they end up the same.

All of these techniques create "false history" - I didn't *really*
make that small step. After I'm finished, I run a little script that
checks out each commit and runs my tests.

Sometimes, trying to preserve my changes isn't worth the time, or I
can see into the future a bit and know ahead of time that I am about
to make a big set of changes. In these cases, I try a spike: I make
the changes willy-nilly, writing down each step as I do it. Then I
throw it away and *invert the order of steps*. This allows each step
to happen in the order I would prefer, and I often improve on each
step.

### Isn't too many small commits just as bad?

I've heard something like this before:

> It's so small, it doesn't deserve it's own commit

I've **never** had to review a commit that was too small. I have had
to review a commit that was too large. I'm willing to take the risk of
creating many small commits, especially if all the changes are going
to be made one way or another.

If a commit is small, then I can open it, read the commit message, and
review it within seconds.

### What do I do as a reviewer?

I use a modified version of the [single responsibility principle][SRP]
that applies to commits:

> A commit should have one, and only one, change.

I try to follow steps like these:

1. Read the whole commit message. It should have no **and**s, **or**s,
or **but**s. If it does, I kick it back to the author and request that
the commit be split up into those pieces. Otherwise, I sear the commit
message into my brain.
2. Read the diff of the commit and evaluate each change against the
commit message. If a line doesn't fit with the message, mark the
change as unrelated. If it does, review the line as usual.
3. Sometimes I keep reading the diff once you I find an unrelated
line, other times I stop at the first one; the original author may be
faster at separating the concerns.
4. Make sure to thank the author when they provide a commit with a
single focus - positive reinforcement lets us know that we are on the
right track!

[refactor]: http://en.wikipedia.org/wiki/Refactoring
[OCP]: http://en.wikipedia.org/wiki/Open/closed_principle
[SRP]: http://en.wikipedia.org/wiki/Single_responsibility_principle
