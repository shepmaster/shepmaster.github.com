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

Nothing above is particularly new; every programmer *knows* that if
you remove a method that is being used, then your code will
break.

## In which time is considered

Let us assume that the `greet` method is never called on an instance
of the `B` class in the third example. Moving the method up the
hierarchy is refactoring - no external behavior has changed.

Now assume, thanks to user feedback, that we've decided that the `B`
class needs a `greet` method. Following TDD, we create a test for
our new functionality, which fails. If we now make the exact same
change to our classes that we made earlier, the change is *not a
refactoring* - we are changing the behavior of the system from failing
to passing.

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
"external".

## In which the real-world is consulted

Considering the environment of code undergoing refactoring is
essential to the success of the refactoring.

Is there really a benefit to thinking of

## In which related areas are suggested

When considering the temporal nature of refactoring, I am reminded of
how git manages commits. A commit hash is not just based on the
changes to the code, although that's often how people think about
it. The hash is based on many things, including the changes to the
code and the commit that the change is being made to. This parent
commit can be thought of as point in time from the viewpoint of the
version control system.

Even if you have two identical changes to the code, the state in time
which they are applied carries great importance. For example, if the
change adds a new option to a configuration file, but the change is
applied before the corresponding option is added to the code, then the
option will be ineffective, or worse.

Similarly, whether changes to code is "refactoring" or not is based on
the change and what the external system behavior is at that
time. Something to mull about for a future time.

[refactoring]: http://martinfowler.com/refactoring/
[extract-method]: http://martinfowler.com/refactoring/catalog/extractMethod.html
[inline-method]: http://martinfowler.com/refactoring/catalog/inlineMethod.html
[pull-up-method]: http://martinfowler.com/refactoring/catalog/pullUpMethod.html
[add-parameter]: http://martinfowler.com/refactoring/catalog/addParameter.html
[remove-parameter]: http://martinfowler.com/refactoring/catalog/removeParameter.html
[split-loop]: http://martinfowler.com/refactoring/catalog/splitLoop.html
