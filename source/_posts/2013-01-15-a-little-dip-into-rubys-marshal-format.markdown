---
layout: post
title: "A little dip into Ruby's Marshal format"
date: 2013-01-15 20:00
comments: true
categories: ruby
---

I recently tried to resolve a JRuby [issue involving Marshal][issue].
I've used [Marshal][marshal] before, but never needed to pay attention
to the actual bytes written to disk. I decided to write up what I
learned in the process.

<!-- more -->

## Version number

{% hex 1:04 2:08 %}

I collected this data using Ruby 1.9.3p327, which has Marshal version
4.8. The version number is encoded with two bytes, one each for the
major and minor version. This version number precedes all dumps and
I'll ignore it for the rest of this post.

## Nil, true, false

`nil`
{% hex 0408 1:30 %}
The typecode `30` is ASCII `0`.

`true`
{% hex 0408 1:54 %}
The typecode `54` is ASCII `T`.

`false`
{% hex 0408 1:46 %}
The typecode `46` is ASCII `F`.

## Integers (easy)

`0`
{% hex 0408 1:69 2:00 %}

The typecode `69` is ASCII `i`. The typecode is followed by the value
of the integer. Zero is represented as `00`.

`1`
{% hex 0408 69 2:06 %}

Here we see that the encoded value for one is `06`, not `01` as we
might expect at first. This allows for more efficient storage of
smaller numbers. -123 <= x <= 122 can be encoded in just one byte.

## Arrays

`[]`
{% hex 0408 1:5b 2:00 %}

The typecode `5b` is ASCII `[`. The typecode is followed by the
number of elements in the array.

`[1]`
{% hex 0408 5b 2:06 3:6906 %}

The number of items in the array is encoded in the same form as
integers. Each value in the array is encoded sequentially after the
size of the array.

## Hashes

`{}`
{% hex 0408 1:7b 2:00 %}
The typecode `7b` is ASCII `{`. The typecode is followed by the number
of (key, value) pairs in the hash.

`{1 => 2}`
{% hex 0408 7b 2:06 3:6906 4:6907 %}

Like arrays, the number of items in the hash is encoded in the same
form as integers. Each pair of (key, value) is encoded sequentially
after the size of the hash.

## Symbols

`:hello`
{% hex 0408 1:3a 2:0a 3:68656c6c6f %}

The typecode `3a` is ASCII `:`. The typecode is followed by the length
of the symbol name and then the symbol name itself, encoded as UTF-8.

## Symlinks

When a symbol is repeated multiple times, the Marshal encoding allows
subsequent instances to reference the first instance to save space in
the stream.

`[:hello, :hello]`
{% hex 0408 5b07 2:3a0a68656c6c6f 1:3b 3:00 %}

The typecode `3b` is ASCII `;`.  The typecode is followed by the
position of the symbol in the cache table. This table is indexed by
the order in which the symbol first appeared.

## The rest

There's a lot more to the Marshal format; I haven't even covered
strings yet! I'll need to create another post to address the rest.

## How to explore on your own

To generate the examples for this post, I hacked up a quick helper in
irb:

```ruby
def dump(x)
  File.open('/tmp/out', 'w') {|f| Marshal.dump(x, f)}
  `xxd /tmp/out`
end
```

[issue]: https://github.com/jruby/jruby/issues/456
[marshal]: http://www.ruby-doc.org/core-1.9.3/Marshal.html
