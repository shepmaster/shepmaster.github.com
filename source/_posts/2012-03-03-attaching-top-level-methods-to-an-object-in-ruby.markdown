---
layout: post
title: "Attaching top-level methods to an object in Ruby"
date: 2012-03-03 10:48
comments: true
categories: ruby metaprogramming
---

Sometimes when you are doing rapid development, you need to create a
new method *right now*, but you don't put the method in the right
spot. Maybe you aren't sure what object should ultimately have the
functionality. Maybe you are simply too lazy to open the correct
file. Whatever the reason, you decide to define the method right there
at the top-level, pretending you are a procedural programmer.

<!-- more -->

Time passes, and you finally decide it is time to move that method to
where it belongs. The problem is that someone (certainly not you...)
has been calling that method from *everywhere*. Fixing all the calls
to the method at once will be a huge change; you prefer to make
smaller, easier to review changes. Or maybe you don't have control
over all code that calls the method, and so you *can't* change all the
the calls.

Here's an example of the problem

```ruby
def add_value(object, param)
  object.value + param
end

class MyObject
  def value
    10
  end
end

obj = MyObject.new
add_value(obj, 5)
```

You'd really like `MyObject` to have an `add_value` method, instead of
it just floating around at the top-level. However, you don't want to
just copy code around - what if one of the implementations changes?
Your first instinct might be to do something like this:

```ruby
class MyObject
  def add_value(param)
    add_value(self, param)
  end
end
```

The problem with this is that once you are inside the class
definition, all calls to `add_value` will be handled by the instance,
not the top-level method.

You could avoid this by making the top-level method and the instance
method have unique names. If you like the existing name for both
methods, you could alias the original name to a more unique one to
avoid shadowing it:

```ruby
alias :i_am_secret_method_add_value :add_value

class MyObject
  def add_value(param)
    i_am_secret_method_add_value(self, param)
  end
end
```

Another solution is to qualify access to the top-level method. This
can be done by maintaining a reference to the object that defines the
method.

```ruby
reference_to_main = self                            # 1
MyObject.class_eval do                              # 2
  define_method :add_value do |param|
    reference_to_main.send(:add_value, self, param) # 3
  end
end
```

1. We start by stashing a reference to the object that defines the
top-level method. This object is named "main".
2. If we reopened our class the traditional way, using `class MyObject`,
the reference to main would fall out of scope. Instead, we use
`class_eval`.
3. The definition of the `add_value` instance method must use `send` as
the top-level method is private.

If you don't want to modify the object directly, you can apply the
same techniques to a module:

```ruby
module MathModule
end

reference_to_main = self
MathModule.module_eval do
  define_method :add_value do |param|
    reference_to_main.send(:add_value, self, param)
  end
end

obj = MyObject.new
obj.extend(MathModule)
```

Using modules leads to another solution. You can move the top-level
method into a module, and use the module with both the class and the
main object.

```ruby
module MathModule
  def add_value(object, param)
    object.value + param
  end
end

extend MathModule

class MyObject
  include MathModule

  def add_value(param)
    super(self, param)
  end
end
```

The downside to this solution is that the object now has two
`add_value` methods. One of the methods takes any instance of the
class, which would be confusing to anyone trying to figure out how to
use the object.

The easiest and clearest solution to this problem is not to use clever
metaprogramming, but just flip the way you think about the
problem. Move the entire method into the object and leave a stub
method that redirects to the object's implementation:

```ruby
def add_value(object, param)
  object.add_value(param)
end

class MyObject
  def add_value(param)
    value + param
  end
end
```
