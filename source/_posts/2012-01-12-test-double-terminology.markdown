---
layout: post
title: "Test Double Terminology"
date: 2012-01-12 07:37
comments: true
categories: mocking craftsmanship test-doubles
---

While listening to the Code Quality panel discussion at
[CodeMash][codemash], I [tweeted][original-comment] about "the state
of mocking":

> The state of "mocking" is that the term "mocking" is misused by
> everyone. You often mean "test double" or "stub". 

This garnered some retweets and some great replies, so I wanted to
have the opportunity to respond in a little longer form. I've written
this post between sessions and in the halls, so give me a little
leeway and feel free to send me a tweet with corrections, arguments,
whatever.

<!-- more -->

## Test frameworks encourage misuse

> [I]t doesn't help that most mocking frameworks are used for stubs

[@F1nglas][framework-misuse] mentions that [moq][moq] does this, and I
know that [Mockito][mockito] does as well. [RSpec][rspec] does better
in this area. From the [rspec-mock][rspec-mock] documentation:

> Use the `double` method to create [a test double.] You can also use
> the `mock` and `stub` methods to create test doubles, however these
> methods are there for backward compatibility only

Remember that most of these tools we use are open source: we can
submit patches to improve them. In the meantime, we can add local
alias methods that better express our intent.

## Saying "mock" is just more fluid
 
> [P]eople find it easier to say "I'll mock that out" than "I'll
> test-double that out".

[@jitterted][easier-to-say]'s point is spot on; it *is* easier to say
"mock". However, I don't usually want to use just a test double. I
always want to do/use something particular: stub, mock, dummy, etc.

I'd love it if we as a community could come up with a better verb to
use to mean the generic "use a test double".

## Doesn't language evolve?

> [A] term takes on the meaning that "everyone" gives it

[@JuliansThoughts][meaning-from-everyone]

> The meaning of a word is defined by common understanding

[@roblally][impossible-to-be-wrong]

> [M]isused by everyone is an evolving definition

[@boulderDanH][misuse-is-definition]

Let me get this out of the way first: I **love** the evolution of
language. Dropping irrelevant words, new words being created, and
expanding the meaning of existing words are all good things.

The root issue here is not that language is evolving. The root issue
is that our understanding of the tools and techniques we are referring
to by that language hasn't gotten to the point where we can substitute
one for the other and know **conclusively** what we really mean.

If I said to you, the programmer: "Use a list to store the names". You
go off and write whatever you need, then come back for code review and
I now say: "Why can't I look up a specific name efficiently?". You
would be outraged; why did I tell you to use a list when I really
meant a hashtable instead? My response? "Oh, you know that language
evolves".

If I'd originally said to use a "collection", a more abstract concept
than either list or hashtable, that would force you to think about
what you need to do and how you were going to do it. I'm not
conflating a specific idea with a general concept.

As community, we have heaps of experience with various types of
collections; we don't have that same strong background with test
doubles yet. When we use "mock" when we should use "dummy" or "stub"
or "test double", we avoid thinking about what we really need to do
and why we need to do it.

By using "mock" inappropriately, we confuse two distinct concepts. We
then complain that "mocks" suck and don't do what we need them to
do. Mocks, stubs, fakes and dummies all have a place and a
purpose. Just as using a list when you should use a hashtable reflects
poorly on the programmer, so does using a mock when you should use a
stub.

Mocks are **not** stubs. Don't believe me. Believe
[Martin Fowler][mocks-not-stubs]. Believe
[Gerard Meszaros][xunitpatterns]. Watch
[Why You Don't Get Mock Objects][dont-get-mocks] by Gregory
Moeck. Whatever you do, make sure you understand what a mock object
truly is before you start conflating terms.

*Note:* Don't take from this post that I always do or say the right
thing. I have other posts on this same blog that incorrectly refer to
test doubles as "mocks". Most of those posts were written before I
learned the difference myself. I should edit those posts to clarify my
real intent. The important thing is that as we go forward we should be
careful and precise about what we mean.

Discuss on [reddit][reddit] or [Hacker News][hn].

[codemash]: http://codemash.org/
[dont-get-mocks]: http://confreaks.net/videos/659-rubyconf2011-why-you-don-t-get-mock-objects
[double-patterns]: http://xunitpatterns.com/Test%20Double.html
[easier-to-say]: https://twitter.com/#!/jitterted/status/157265770403987456
[framework-misuse]: https://twitter.com/#!/F1nglas/status/157421334220120065
[hn]: http://news.ycombinator.com/item?id=3460509
[impossible-to-be-wrong]: https://twitter.com/#!/roblally/status/157361888420827137
[meaning-from-everyone]: https://twitter.com/#!/JuliansThoughts/status/157372211840299009
[misuse-is-definition]: https://twitter.com/#!/boulderDanH/status/157262539372249088
[mockito]: http://code.google.com/p/mockito/
[mocks-not-stubs]: http://martinfowler.com/articles/mocksArentStubs.html#TheDifferenceBetweenMocksAndStubs
[moq]: http://code.google.com/p/moq/
[original-comment]: https://twitter.com/#!/JakeGoulding/status/157261614448521216
[reddit]: http://www.reddit.com/r/programming/comments/off9p/test_double_terminology_the_misuse_of_the_term/
[rspec-mock]: https://www.relishapp.com/rspec/rspec-mocks
[rspec]: https://www.relishapp.com/rspec
[xunitpatterns]: http://xunitpatterns.com/
