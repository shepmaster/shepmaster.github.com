---
layout: post
title: "A refactoring example: lots of if-else statements on strings"
date: 2012-09-24 12:00
comments: true
categories: refactoring, example
---

I recently did a bit of work that turned out to be a great exercise
for refactoring a huge sequence of if-else statements based on
strings. There are a few ugly bits left, so I'm still poking at it,
but I am pleased with my progress so far.

<!-- more -->

While the original code was Java, the meat of the problem can be
easily shown in Ruby. Translating it to Ruby also makes it easier to
make sure I don't accidentally share any proprietary information!

The problem involves processing a hunk of XML to create nested
configuration objects. The original implementation used a sequence of
if-else blocks, but the Ruby version would have used a `case`
statement.

```ruby
class ObjectsFromXML
  def process(parent, element)
    new_object = nil

    case element.name
    when 'foo'
      f = Foo.new(element['name'])
      parent.add(f)
      new_object = f
    when 'bar'
      b = Bar.new(element['name'])
      b.weight = element['weight'].to_f
      parent.add(b)
      new_object = b
    # ... about 20 of these cases in total
    else
      raise "Invalid node"
    end

    element.children.each { |c| process(new_object, c) }
  end
end
```

This function has some issues: it is pretty long, and each case has
some similarity to the next but are different enough to be
annoying. The code certainly doesn't try to adhere to the Single
Responsibility Principle!

My first step was to split the blocks into classes with a common interface.

```ruby
class FooHandler
  def process(parent, element)
    Foo.new(element['name']).tap |f|
      parent.add(f)
    end
  end
end

class BarHandler
  def process(parent, element)
    Bar.new(element['name']).tap
      b.weight = element['weight'].to_f
      parent.add(b)
    end
  end
end

class ObjectsFromXML
  def process(parent, element)
    new_object = nil

    case element.name
    when 'foo'
      new_object = FooHandler.new.process(parent, element)
    when 'bar'
      new_object = BarHandler.new.process(parent, element)
    # ... other cases
    else
      raise "Invalid node"
    end

    element.children.each { |c| process(new_object, c) }
  end
end
```

This corrals the item-specific code to an item-specific class. If I
need to change how the Bar class is created, only the BarHandler class
needs to be updated.

```ruby
class FooHandler
  # As above...
end

class BarHandler
  # As above...
end

class ObjectsFromXML
  attr_reader :handlers

  def initialize
    @handlers = {}
    handlers['foo'] = FooHandler.new
    handlers['bar'] = BarHandler.new
    # ... other cases
  end

  def process(parent, element)
    handler = handlers.fetch(element.name) { raise "Invalid node" }
    new_object = handler.process(parent, element)
    element.children.each { |c| process(new_object, c) }
  end
end
```

Now all the handlers are in a hash, keyed by the expected element
name. This allows me to pull out the correct handler and go. The
`process` function now only needs to be concerned with picking the
right handler and dealing with children elements.

```ruby
class FooHandler
  # As above...

  def element_name
    'foo'
  end
end

class BarHandler
  # As above...

  def element_name
    'bar'
  end
end

class Handlers
  def initialize
    @handlers = {}
  end

  def add(handler)
    @handlers[handler.element_name] = handler
  end

  def process(parent, element)
    handler = handlers.fetch(element.name) { raise "Invalid node" }
    handler.process(parent, element)
  end
end

class ObjectsFromXML
  attr_reader :handlers

  def initialize
    @handlers = Handlers.new
    handlers.add(FooHandler.new)
    handlers.add(BarHandler.new)
    # ... other cases
  end

  def process(parent, element)
    new_object = handlers.process(parent, element)
    element.children.each { |c| process(new_object, c) }
  end
end
```

Now we have moved the concept of the expected element name into the
handler itself. This makes sense, as each handler should know what
name it expects, not some other piece of code. I also took the
opportunity to move the code purely related to the handlers to a new
class that is highly focused on that one responsibility.

Some further refinements happened after this last point. The
`ObjectsFromXML` class became another `Handler` class, which made it
the same level of abstraction as the other handlers and removed a
redundant `process` method. The return code was removed because it
wasn't used except in a few tests. Iterating over children was moved
to each class that could contain children.
