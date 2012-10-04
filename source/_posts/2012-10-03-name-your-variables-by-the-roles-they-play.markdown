---
layout: post
title: "Name your variables by the roles they play"
date: 2012-10-03 19:49
comments: true
categories:
---

Have you ever seen a variable with a terrible name? This is of course
a trick question; everyone has. I'd like to look at a particular
variable-naming annoyance: naming the variable based on the class
name.

<!-- more -->

In a statically-typed language without type inference, like Java, you
have likely seen something like this:

```java
FooBarZed fooBarZed = new FooBarZed(true);
```

In a dynamically typed language, like Ruby, it would look more like this:

```ruby
foo_bar_zed = FooBarZed.new(true)
```

This style of code may make sense to you. Maybe your class names are
self-describing, and so duplicating the class name as the variable
name is just "reusing a good thing".

The problem with naming variables in this style is that the class
name, at *best*, describes what is special about an object or how the
object works. When you are using the object, you want to know why you
are using it - you want to know the *role* of the object:

```ruby
# this variable name says nothing new to the reader of the code
url_fetcher = UrlFetcher.new('http://example.com/')
# this variable name explains why we want to do something
conversion_rate_fetcher = UrlFetcher.new('http://example.com/')
```

As a pragmatic argument, think how often you rename a class to better
describe it. Now think how often you've changed a class name AND all
the variables to match the new class.

The role that an object plays changes at vastly different rate than
the name of the class. It's unlikely that the role will change
dramatically, as that new role would probably be better represented by
a brand new object. Similarly, code that uses that object is unlikely
to want an object that fills a different role without a rewrite of how
the calling code works.
