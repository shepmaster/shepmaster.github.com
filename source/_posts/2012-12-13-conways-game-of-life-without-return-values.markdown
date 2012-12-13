---
layout: post
title: "Conway's Game of Life without return values"
date: 2012-12-13 15:14
comments: true
categories:
---

On 2012-12-08, I attended the Pittsburgh
[Global Day of Code Retreat][gdcr] facilitated by [Joe Kramer][joe]
and [Jim Hurne][jim]. As usual, I had a great time, and got to meet
new people from the Pittsburgh tech scene. It's always good for me to
remember that there are non-Ruby developers out there! I even started
the day off by doing the Game of Life in C#.

One of the more contentious constraints of the day was "no return
values". I feel like I was the only one in the room that liked this
constraint at all!  As such, I wanted to finish it up to see what my
[final code][code] and observations would look like.

<!-- more -->

### Goal

As I understand it, the point of this constraint is to explore
"[tell don't ask][tda]", with a secondary exploration of
[mocks vs. stubs][mocks-arent-stubs].

### Constraint modifications

I made some small tweaks to the constraint to deal with how Ruby
works and to avoid work orthogonal to the goal.

* Allow return values from constructors
* Allow return values from standard library classes
* Allow return values from private methods

In Ruby, constructors are methods on a Class instance that return a
new instance of the class. Since everything in Ruby is an object, it
would be impossible to make progress if we didn't allow creating new
objects.

The goal of the constraint isn't to rewrite all of Ruby's standard
library. If we cannot use return values from the standard library, we
couldn't do something as simple as `a = 1 + 1`! Our newly-created
code will not return values, so it is safe to use return values hidden
away inside of our objects.

Allowing private methods to return values isn't strictly necessary,
but it allows us to reduce code duplication. Technically, we could
inline the private methods where they are used, but that would be
ugly. Since these methods are private, they won't add to the surface
area of our objects and shouldn't conflict with the goal of the
exercise.

### Things I liked

I usually start out Conway's with the ability to see if a cell is
alive, followed quickly by the ability to bring a cell to life. This
means the first thing I do is rely on return values. This time, I
began with the concept of a UI that would be told when a cell is
alive. I found this interesting as I usually skip over the display
completely, leaving it as a "trivial" thing to be added later.

The `Board` class came into existence while implementing the
`time_passes` method because I needed to have both the current and
next board state. I like that this concept was reified; the `Game`
class deals with coordinating the rules and a board, but the `Board`
class deals with the particulars of the board state.

I was forced into giving human names to more things than I usually
would, such as `has_two_neighbors`, or `AliveCellRules`. I find that
this is the extended version of creating a well-named temporary
variable.

### Things I didn't like

There are two rule-related classes, one for alive cells and one for
dead cells. The alive cell rules class is almost 100%
duplication. This could be reduced using Ruby's `alias` at the cost of
reduced readability, and still wouldn't help the duplication in the
dead cell rules. It's hard to tell if this would be good or bad in the
absence of future changes, but I don't like it as it stands now.

I wanted to create a `Point` class to abstract the concept of x / y
coordinates and also to have a place to hang the idea of
"neighbors". Unfortunately, it would have solely existed to return
values: a list of points, equality comparisons, etc. I think this
would be an ideal example of a value type.

I love Ruby's `Struct`; I have written too many class initializers
longhand to ever want to go back. As far as I am concerned, `Struct`
reduces the work to make an initializer from *O(n)* to *O(1)*.
Unfortunately, it automatically creates a public `attr_accessor`,
which would be too tempting to use. I also avoided `attr_reader` for
the same reason, even though I could have made the reader
private. Seeing all the bare instance variables makes me
uncomfortable.

### Interesting implementation details

For each public method, I returned `self`. In Ruby, the last executed
statement is implicitly returned. Returning `self` avoids accidentally
relying on a return value. In production code I wouldn't go this
overboard, trusting the caller to not use incidental return values. In
a language like Java, I would declare the method as void.

I've never used `flat_map` before, but I'm going to keep my eyes open
for more places to use it. I'm not at the point where it comes without
thinking, but looking for `ary.map{ ... }.flatten(1)` should be easy
enough. Also, I learned that `flatten` can take an argument that
controls how deep it will go.

I swear that there is an existing method that does the equivalent of
`ary.reject { |x| x == CONSTANT }`, but I couldn't find it. `delete`
will mutate the array in place, which isn't quite the same.

### Tests

As the code progressed, I had to start using RSpec's `as_null_object`
more frequently. This is because closely situated cells began
interacting and would be output to the user interface. I wasn't
interested in these outputs, but they weren't incorrect. After enough
tests needed a null object, I changed the test-wide mock, which may
have been too broad a change.

All of the tests that involve time passing have two duplicated
lines. These lines could have been pulled into the rarely-used `after`
block. I've never seen code that does this, and I'm not sure how I
feel about it.

I don't know what order I prefer for the `should_receive` calls
relative to the rest of the setup. In this case, I chose to put the
message expectations at the top of the test block.

### Final thoughts

Like most exercises during Code Retreat, preventing return values has
benefits and disadvantages. I like how certain concepts were forced to
become reified and that I had to think more about the consumer of my
code. Contrariwise, I missed not being able to use `Struct` and really
wanted a `Point`.

Will I change how I code because of this? Maybe a little bit. It
probably would be good practice to avoid return values at first blush,
but I certainly won't stop using them completely. One thing I might
look further into is Ruby 1.9's [`Enumerator`][enumerator]. This would
allow me to provide a nice function that takes a block or returns an
enumerable for further chaining.

Feel free to read over the [code on GitHub][code] if you are
interested!

[gdcr]: http://globalday.coderetreat.org/
[jim]: https://twitter.com/jthurne
[joe]: https://twitter.com/josephrkramer
[code]: https://github.com/shepmaster/gdcr-no-return-values
[tda]: http://pragprog.com/articles/tell-dont-ask
[mocks-arent-stubs]: http://martinfowler.com/articles/mocksArentStubs.html
[enumerator]: http://www.ruby-doc.org/core-1.9.3/Enumerator.html
