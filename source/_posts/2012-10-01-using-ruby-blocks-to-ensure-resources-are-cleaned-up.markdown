---
layout: post
title: "Using Ruby blocks to ensure resources are cleaned up"
date: 2012-10-01 18:05
comments: true
categories:
---

In programming, cleaning up resources you have created is an
easily-overlooked problem. In languages like C, you have to clean up
everything by hand: memory, files, network sockets, etc. Languages
that have a garbage collector take away the need to explicitly free
memory, but you still have to manage the other resources.

<!-- more -->

In Ruby, we can use blocks to help ensure resources are closed. You've
probably seen this idiom when dealing with files. The File class
ensures that the file is closed after the block is finished:

```ruby
File.open('file.txt') do |file|
  # ... work with the file ...
end
```

However, this can only be used when the lifespan of the resource is one
method call. If you need to keep the file around in an instance
variable, then you cannot use this pattern, and must fall back to
explicitly closing the resource:

```ruby
class Foo
  def initialize
    @file = File.open('file.txt')
  end

  def close
    @file.close
  end
end
```

To make this code nicer on yourself and others that have to use it,
you should add a method that handles closing the resource for you,
just like `File` does:

```ruby
class Foo
  def self.open
    foo = Foo.new
    yield foo
  ensure
    foo.close
  end
end
```

Unfortunately, this clean implementation of `open` has a problem if
the constructor of `File` can throw an exception. The exception will
occur before the variable `foo` can be set to anything, so the `close`
message will be sent to `nil` instead, causing another exception!
*This **certainly** didn't happen in any code I was writing...*

To handle this case, we have to give up on using the implicit `begin``
block from the function and create our own scope:

```ruby
class Foo
  def self.open
    foo = Foo.new
    begin
      yield foo
    ensure
      foo.close
    end
  end
end
```

Now we can safely create a `Foo` and close it all in one place, but
what if another object wants to keep an instance of `Foo` around for
longer? It's nice to transparently handle both cases:

```ruby
class Foo
  def self.open
    foo = Foo.new

    return foo unless block_given?

    begin
      yield foo
    ensure
      foo.close
    end
  end
end
```
