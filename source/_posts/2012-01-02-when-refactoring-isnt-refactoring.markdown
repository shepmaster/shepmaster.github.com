---
layout: post
title: "When refactoring isn't refactoring"
date: 2012-01-02 12:00
comments: true
categories: refactoring
---

What is refactoring? Martin Fowler [provides][refactoring] a solid
definition:

> Refactoring is a disciplined technique for restructuring an
> existing body of code, altering its internal structure without
> changing its external behavior.

That description seems fairly reasonable; let's look at some potential
refactoring techniques.

<!-- more -->

## Example 1
#### Before
```ruby
class A
  def foobar
    'foo' + 'bar'
  end
end
```

#### After
```ruby
class A
  def foobar
    'foo' + bar
  end

  def bar
    'bar'
  end
end
```

This one is pretty clear-cut - it's a refactoring technique that any
programmer would recognize, even if she had never heard of the term
"refactoring". It goes by the name [Extract Method][extract-method].

## Example 2
#### Before
```ruby
class A
  def alphabet
    'alpha' + bet
  end

  def bet
    'bet'
  end
end
```

#### After
```ruby
class A
  def alphabet
    'alpha' + 'bet'
  end
end
```

This is the inverse of the first example, but is it a refactoring
technique?  There is a technique called
[Inline Method][inline-method], so it certainly seems plausible.

## Example 3
#### Before
```ruby
class Parent
end

class A < Parent
  def greet
    'hello'
  end
end

class B < Parent
end
```

#### After
```ruby
class Parent
  def greet
    'hello'
  end
end

class A < Parent
end

class B < Parent
end
```

Again, there is a related refactoring technique,
[Pull Up Method][pull-up-method]. This example differs from the linked
one, as only one of the subclasses has the method definition to start
with. This difference makes us less ready to call our changes
refactoring.

Here's the trick: from just the code above, **you can't tell if these
changes constitute refactoring or not**.

## In which details are examined

The distinction comes from two important words in the definition of
refactoring: *internal* and *external*. These three examples only show
the code being changed (the internal structure), not how other objects
interact with this code (the external behavior).

Take a look at the second example - if any object in the entire system
ever calls the `bet` method, then removing the method will cause the
behavior of the system as a whole to change.

The first and third examples are actually the same from this point of
view - they both add a new method. You might think that adding a
method is safe, but in truth it depends on the circumstances.

In a statically-typed language, adding a method is safe because no
other code could have tried to call the method without getting a
syntax error. In contrast, a dynamically-typed language like Ruby can
make runtime decisions based on what methods an object
implements. This means that adding a method might trigger completely
new behavior elsewhere.

## In which time is considered

Let's focus on the third example and assume that the above caveat
about adding methods in dynamically-typed languages doesn't apply.

First, we decide that the code would be better if we moved the `greet`
method from `A` to `Parent`. This change would be refactoring - no
external behavior has changed.

Second, instead of just making the code better, assume that user
feedback suggests that that the `B` class needs a `greet` method. This
means that we:

1. Create a failing test for our new functionality.
2. Move the `greet` method from `A` to `Parent`.

Even though the code is modified in the same way for both examples,
the second time is *not refactoring* - we are changing the behavior of
the system from failing to passing. The only difference is that time
has passed and the requirements of the system are different.

## In which exceptions are noted

All three of the original examples have one thing in common: they all
show refactoring techniques that change the interface of the
class. Doing so means that collaborating objects must be viewed as the
external code that relies on certain behavior.

Some refactoring techniques change code only within a single method,
such as [Split Loop][split-loop]. In most cases, these techniques
don't have to worry about changing the external behavior. Intra-method
refactoring can only cause a problem when the method has side-effects
that are visible outside of the method.

Another factor that can come into play is the visibility of the
method(s) being refactored. If they are `private` (or whatever
language equivalent), you can feel more secure when refactoring. This
is because limited visibility restricts what code can be considered
external.

## In which the real world is consulted

It is imperative to consider the environment in which code exists
before refactoring it; if the environment is ignored, then you cannot
possibly understand the desired behavior of the code.

A main point from the blog post
[Forgotten Refactorings][changing-shit] by Hamlet D'Arcy is that it's
not really refactoring if the code being changed isn't covered by
tests. This is because tests are the single-best way to nail down the
expectations of the behavior of a system, with the benefit that they
are continually verified.

It is also interesting to note that the order in which code is
modified can alter if you are performing a refactoring or not. While
there probably isn't a practical difference between the two paths, I
cannot help but wonder if choosing one or the other will have some
practical benefit.

Discuss this post on [reddit][reddit] or [Hacker News][hn].

[refactoring]: http://martinfowler.com/refactoring/
[extract-method]: http://martinfowler.com/refactoring/catalog/extractMethod.html
[inline-method]: http://martinfowler.com/refactoring/catalog/inlineMethod.html
[pull-up-method]: http://martinfowler.com/refactoring/catalog/pullUpMethod.html
[split-loop]: http://martinfowler.com/refactoring/catalog/splitLoop.html
[changing-shit]: http://hamletdarcy.blogspot.com/2009/06/forgotten-refactorings.html
[reddit]: http://www.reddit.com/r/programming/comments/nzzfu/when_refactoring_isnt_refactoring/
[hn]: http://news.ycombinator.com/item?id=3416749