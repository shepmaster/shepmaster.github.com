---
layout: post
title: "A final dip into Ruby's Marshal format"
date: 2013-01-20 20:30
comments: true
categories: ruby marshal
---

This is the third and last of my posts about the Marshal format. The
[first part][part-1] introduced the format and some straight-forward
serializations. The [second part][part-2] touched on strings and
object links. This post rounds us off with regexes, classes, modules,
and instances of objects.

<!-- more -->

## Regexes

`/hello/`
{% hex 0408 49 1:2f 2:0a 3:68656c6c6f 4:00 063a064546 %}

Like strings, regexes are surrounded by an IVAR. The typecode `2f` is
ASCII `/` and denotes that this object is a regex. The length of the
string follows, again encoded as an integer. The regex string is
stored as a set of bytes, and must be interpreted with the string
encoding from the IVAR. After the string, the regex options are saved.

`/hello/imx`
{% hex 0408 49 2f 0a 68656c6c6f 4:07 063a064546 %}

The regex option byte is a bitset of the five possible options. In
this example, ignore case, extend, and multiline are set (`0x1`,
`0x2`, and `0x4` respectively)

## Classes

`String`
{% hex 0408 1:63 2:0b 3:537472696e67 %}

The typecode `63` is ASCII `c` and denotes that this object is a
class. The length of the class name followed by the class name are
next.

`Math::DomainError`
{% hex 0408 63 2:16 3:4d6174683a3a446f6d61696e4572726f72 %}

Namespaces are separated by `::`.

## Modules

`Enumerable`
{% hex 0408 1:6d 2:0f 3:456e756d657261626c65 %}

Modules are identical to classes, except the typecode `6d` is ASCII `m`.

## Instances of user objects

Let's define a small class to test with.

```ruby
class DumpTest
  def initialize(a)
    @a = a
  end
end
```

`DumpTest.new(nil)`
{% hex 0408 1:6f 2:3a0d44756d7054657374 3:06 4:3a074061 5:30 %}

The typecode `6f` is ASCII `o`, and denotes that this is an
object. The class name is next, written as a symbol - `:DumpTest`. The
number of instance variables is encoded as an integer, followed by
pairs of name, value. This example has 1 pair of instance variables,
[`:@a`, `nil`].

[part-1]: http://jakegoulding.com/blog/2013/01/15/a-little-dip-into-rubys-marshal-format/
[part-2]: http://jakegoulding.com/blog/2013/01/16/another-dip-into-rubys-marshal-format/
