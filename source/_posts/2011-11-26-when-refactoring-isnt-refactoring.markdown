---
layout: post
title: "When refactoring isn't refactoring"
date: 2011-11-25 12:00
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

If we decide that the code would be better if we moved the `greet`
method from `A` to `Parent`, then that change would be refactoring -
no external behavior has changed.

Instead of changing the code just to make it better, assume that user
feedback suggests that that the `B` class needs a `greet` method. This
means that we:

1. Create a failing test for our new functionality.
2. Move the `greet` method from `A` to `Parent`.

Even though we change the code in the same way in both examples, the
second time is *not refactoring* - we are changing the behavior of the
system from failing to passing.

## In which exceptions are noted

All three of the original examples have one thing in common: they all
show refactoring techniques that change the interface of the
class. Doing so means that collaborating objects must be viewed as the
external code that relies on a certain behavior.

Some refactoring techniques change code only within a single method,
such as [Split Loop][split-loop]. In most cases, these techniques
don't have to worry about changing the external behavior. Only when
there are important side-effects, visible outside of the method, could
intra-method refactoring cause a problem.

Another factor that can come into play is the visibility of the
method(s) being refactored. If they are `private` (or language
equivalent), you can feel more secure when refactoring. This is
because limited visibility restricts what code can be considered
external.

## In which the real world is consulted

It is imperitive to consider the environment in which code exists
before refactoring it; if the environment is ignored, then you cannot
possibly understand the desired behavior of the code.

A main point from the blog post
[Forgotten Refactorings][changing-shit] by Hamlet D'Arcy is that it's
not really refactoring if the code being changed isn't covered by
tests. This is because tests are the single-best way to nail down the
expectations of the behavior of a system, with the benefit that they
are continually verified.

However, the environment in which the code exists can change with
every commit.







Is it useful to make a fine distiction between a refactoring

a change that is a refactoring at one point in time and compared to a purposeful change in behavior 

re any real difference between changing code before or after you
have a test that expects that behavior? Probably not.

However, taking the environment of the code undergoing refactoring is
essential to the success of the refactoring.

## In which related areas are suggested

When considering the temporal nature of refactoring, I am reminded of
how git manages commits.

A git commit hash is not just based on the
changes to the code, although that's often how people think about
it. This hash is based on many things, including the changes to the
code and the tip of the branch that the change is being committed
to. From the viewpoint of the version control system, this parent
commit can be treated as a point in time.

Even if you make the same change to the code, the state of the code
when the change is applied carries great importance:

#### Ordering 1
1. Add code for option `foo`
2. Add option `foo` to configuration file

#### Ordering 2
1. Add code for option `foo`
2. Add option `foo` to configuration file

Even though the diffs for each change are identical, the state is
quite different if you look at it between the first and second commit.

Similarly, whether a specific change is a refactoring or not is based
on both the change and what the external system behavior is at that
time. Something to mull about for another blog post.

[refactoring]: http://martinfowler.com/refactoring/
[extract-method]: http://martinfowler.com/refactoring/catalog/extractMethod.html
[inline-method]: http://martinfowler.com/refactoring/catalog/inlineMethod.html
[pull-up-method]: http://martinfowler.com/refactoring/catalog/pullUpMethod.html
[split-loop]: http://martinfowler.com/refactoring/catalog/splitLoop.html
[changing-shit]: http://hamletdarcy.blogspot.com/2009/06/forgotten-refactorings.html
