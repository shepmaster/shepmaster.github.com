---
layout: post
title: "Another dip into Ruby's Marshal format"
date: 2013-01-16 20:00
comments: true
categories: ruby marshal
---

In a [previous post][part-1] I started to describe some details of
Ruby's Marshal format. This post goes further: a larger set of
integers, IVARs, strings, and object links.

<!-- more -->

## Larger integers

What happens once we go beyond integer values that can be represented
in one byte? Marshal simply writes the number of bytes needed to
represent the value, followed by the value, least significant byte
first. Leading zeroes are not encoded.

`123`
{% hex 0408 69 2:01 3:7b %}

`01` indicates that the value takes up one byte, followed by the value
itself.

`256`
{% hex 0408 69 2:02 3:0001 %}

256 requires two bytes.

`2**30 - 1`
{% hex 0408 69 2:04 3:ffffff3f %}

This is the largest value you can serialize as an integer. Above this,
Marshal starts serializing integers as a "bignum".

## Negative integers

`-1`
{% hex 0408 69 2:fa %}

`fa` is -6 in two's complement, which mirrors how `1` is encoded as 6.

`-124`
{% hex 0408 69 2:ff 3:84 %}

Here the first byte is -1 in two's complement. This indicates that one
byte of value follows. The value has had leading `FF` bytes removed,
similar to large positive integers.

`-257`
{% hex 0408 69 2:fe 3:fffe %}

-257 requires two bytes.

`-(2**30)`
{% hex 0408 69 2:fc 3:000000c0 %}

This is the largest negative value you can serialize as an integer
before becoming a bignum.

## IVARs

Hang on to your seats, we're going to jump into strings. First,
however, we need to talk about IVARs. The crucial thing that IVARs
bring to the table is the handling of string encodings.

`'hello'`
{% hex 0408 1:49 220a68656c6c6f 2:06 3:3a0645 4:54 %}

The typecode `49` is ASCII `I` and denotes that this object contains
instance variables. After all the object data, the number of instance
variables is provided. The first instance variable is a special one -
it's the string encoding of the object. In this example the string
encoding is UTF-8, denoted by the symbol `:E` followed by a `true`.

`'hello'.force_encoding('US-ASCII')`
{% hex 0408 49 220a68656c6c6f 06 3:3a0645 4:46 %}

To represent US-ASCII, `:E` `false` is used instead. Both US-ASCII and
UTF-8 are common enough string encodings that special indicators were
created for them.

`'hello'.force_encoding('SHIFT_JIS')`
{% hex 0408 49 220a68656c6c6f 06 3:3a0d656e636f64696e67 4:220e53686966745f4a4953 %}

For any other string encoding, the symbol `:encoding` is used and the
full string encoding is written out as a raw string - `"SHIFT_JIS"`.

`'hello'.tap {|s| s.instance_variable_set(:@test, nil)}`
{% hex 0408 49 220a68656c6c6f 2:07 3a064554 3:3a0a4074657374 4:30 %}

Additional instance variables follow the string encoding. There are
now 2 instance variables. The symbol for the instance variable name
`:@test` comes before the value, `nil`.

## Raw strings

`'hello'`
{% hex 0408 49 1:22 2:0a 3:68656c6c6f 063a064554 %}

Raw strings are safely nestled inside an IVAR, and are comparatively
very simple. The typecode `22` is ASCII `"` and denotes that this
object is a raw string. The length of the string data is next, encoded in
the same form as integers. The string data follows as a set of
bytes. These bytes must be interpreted using the encoding from the
surrounding IVAR.

## Object links

When the same object instance is repeated multiple times, the Marshal
encoding allows subsequent instances to reference the first instance
to save space in the stream.

`a = 'hello'; [a, a]`
{% hex 0408 5b07 2:49220a68656c6c6f063a064554 1:40 3:06 %}

The typecode `40` is ASCII `@`. The typecode is followed by the
position of the object in the cache table. This cache table is
distinct from the symbol cache.

## The rest

There's a more types that Marshal can handle, but not all of them are
interesting. The [next post][part-3] covers regexes, classes, modules,
and instances of objects.

[part-1]: http://jakegoulding.com/blog/2013/01/15/a-little-dip-into-rubys-marshal-format/
[part-3]: http://jakegoulding.com/blog/2013/01/20/a-final-dip-into-rubys-marshal-format/
