---
layout: post
title: 'The "meh" programmer'
date: 2011-01-09 12:00
comments: true
categories: craftsmanship
---
Do you know a "meh" programmer? As an example:

> Alice: This variable name isn't very good...  
> Bob: Meh, it's just a variable name, it's good enough.

Where does this lack of caring come from? If you replace *variable name* with something else, it sounds ridiculous:

> Alice: This algorithm isn't very good...  
> Bob: Meh, it's just an algorithm, it's good enough.

<!--more-->

To me, this kind of behavior is antithetical to the ideas of [Software
Craftsmanship][swc]. An incorrect algorithm makes the program wrong, and
most programmers detest that idea. An incorrect variable name only
affects how well-crafted the program is, but we are usually
all-too-willing to let that slide. There shouldn't be such a huge gulf
between these two ideas.

I recently caught myself saying the following during a code review:

> I would have named this variable *descriptive_name* instead of
> *foo*. Don't worry though, it doesn't really matter since the code
> works.

Fortunately, I know exactly why I said this - I was too afraid to be
truly confrontational. Not everyone buys-in to Software Craftsmanship in
to the extent that I strive to. Even if they did, craftsmanship is a
softer science, providing many ways to be right.

I don't know which I think is worse, **not caring** about the
craftsmanship of the code you write, or being **too scared** to
evangelize good practices to others. I bet master woodworkers don't have
to deal with these kinds of problems.

[swc]: http://manifesto.softwarecraftsmanship.org/
