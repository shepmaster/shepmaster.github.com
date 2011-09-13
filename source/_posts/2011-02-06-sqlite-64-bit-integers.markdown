---
layout: post
title: "SQLite, 64-bit integers, and the impossible number"
date: 2011-02-06 12:00
comments: true
categories: [SQLite, debugging]
redirects:
- /sqlite-64-bit-integers-and-the-impossible-num
---
We recently tackled an issue that seemed rather impossible - an
unsigned 64-bit value was _greater than_ the maximum value that a
64-bit value can hold. What unfolded was a dark, gritty look at the
underbelly of everything we hold dear (or a normal debugging session,
as we like to call them).

<!--more-->

## SQLite3 and 64-bit integers

First off, lets create a table with some big numbers:

``` sql
CREATE TABLE big_numbers (i INTEGER, r REAL, t TEXT, b BLOB);
INSERT INTO big_numbers VALUES (9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807); -- 2^63 - 1
INSERT INTO big_numbers VALUES (9223372036854775808, 9223372036854775808, 9223372036854775808, 9223372036854775808); -- 2^63
```

Let's sanity check our data to make sure it looks like what we would
expect:

``` sql
> SELECT * FROM big_numbers;
i                     r                     t                     b                   
--------------------  --------------------  --------------------  --------------------
9223372036854775807   9.22337203685478e+18  9223372036854775807   9223372036854775807 
9.22337203685478e+18  9.22337203685478e+18  9.22337203685478e+18  9.22337203685478e+18
```

Huh. We definitely were not expecting most of those floating point
numbers, so let's see what types are being returned:

``` sql
> SELECT typeof(i),typeof(r),typeof(t),typeof(b) FROM big_numbers;
typeof(i)   typeof(r)   typeof(t)   typeof(b)
----------  ----------  ----------  ----------
integer     real        text        integer
real        real        text        real
```

Sure enough, the numeric types in the second row are all reals. Let's
do a nice simple addition operation on our data:

``` sql
> SELECT i+1,r+1,t+1,b+1 FROM big_numbers;
i+1                   r+1                   t+1                   b+1
--------------------  --------------------  --------------------  --------------------
-9223372036854775808  9.22337203685478e+18  -9223372036854775808  -9223372036854775808
9.22337203685478e+18  9.22337203685478e+18  9.22337203685478e+18  9.22337203685478e+18
```

Woah, what happened here? Those familiar with signed and unsigned
integers are already nodding and going "Mmm-hmm". For everyone else, I
suggest brushing up on [two's compliment][tc] notation. Suffice it to
say that integers are usually represented by a fixed number of bits,
and once you run out of bits you roll over back to the beginning, in
this case a large negative number.

As it turns out, SQLite is pretty straight-forward about this. From
the [datatype reference][sqldt] in SQLite (emphasis mine):

> INTEGER. The value is a **signed** integer, stored in 1, 2, 3, 4, 6,
> or 8 bytes depending on the magnitude of the value.

That is, you can only store values from -2\*\*63 to (2\*\*63-1). What
does SQLite do for a value outside of this range? As we saw earlier,
it switches over into floating point. Again, quoting from the SQLite
reference:

> REAL. The value is a floating point value, stored as an 8-byte IEEE
> floating point number.

Many programmers are familiar with this type under the name
[double][double].

## Are you smarter than a SQLite engineer?

In our case, we are trying to store an unsigned 64-bit integer. For
reasons now lost to the past, we marshall native C types to strings
ourselves before handing them off to SQLite, rather than using the
appropriate [SQLite API][sqlapi] functions.

An unrelated issue elsewhere caused this value to go negative. Similar
to the issue above when we overflow a integer type, when you underflow
you get a very large positive number. Since we have an _unsigned_
number, the resultant number is far over the maximum of a _signed_
64-bit integer. This causes SQLite to switch into floating-point mode
when the value is saved to the database.

Of course, our particular tale of woe doesn't end there. Indeed, we
need to report this value in a status response. As an example of what
I can only assume is premature optimization, we simply grab the string
value from SQLite and report that. This causes another part of the
application to choke when trying to parse the value, as it can no
longer fit within a 64-bit type.

## Wait, what?

How did we go from simply over- or under-flowing a 64-bit value to
exceeding the range of the datatype? It shouldn't even be possible to
have a 64-bit value that is larger than a 64-bit value, by definition!
As a coworker said: "It would violate the logic of the reality of the
number". The answer stems from how the value is converted into a
double by SQLite.

Here is a simple example that shows what happens when you mess around
with 64-bit integer types and doubles the wrong way:

    #!C
{% include_code 64bit-integer-float.c %}

On Windows (Microsoft (R) C/C++ Optimizing Compiler Version
14.00.50727.762 for x64), we see:

    18446744073709551615
    18446744073709552000
    18446744073709551615

On Linux (gcc (Gentoo 4.4.4-r2 p1.2, pie-0.4.5) 4.4.4), we get
different but equally surprising output:

    18446744073709551615
    18446744073709551616
    0

In both cases, the intermediate double representation is greater than
the original integral representation. The double value is much greater
than the original on Windows, but the cast back to an integer has the
same value as the original. On Linux, the double value is just a
little bit greater, but casting back rolls all the way back to
zero. Floating point numbers are **hard**, and we programmers far too
often forget exactly how hard they are.

When we grabbed the string value directly from the database, there was
no cast back to a 64-bit type to truncate the value. This causes
[the rest of the dominoes to fall like a house of cards][zapp].

## Lessons learned

**Be careful when you need to store large numbers in SQLite.** If you
really need to support unsigned 64-bit numbers, you could always shift
your numbers down by 2\*\*63. Of course, this means that you will
need to massage any access to that column to correct for the
offset. If you don't need to perform any calculations or sorting on
that column inside of SQLite, it's possible that you could give the
column a text affinity, preventing SQLite from messing with the
data.

**Don't write your own marshaling code unless you really need it.** If
we had used the actual SQLite C API for storing and retrieving our
values, I assume (but have not verified) that this wouldn't have
happened, not to mention we would have likely realized that SQLite
only natively supports **signed** 64-bit integers. This would have led
us to test the edge cases that were more obvious.

**Test maximal and minimal values.** This is such a basic testing
strategy that it is surprising that we forget it so
much. I'mdefinitely in the camp of people that don't do this kind of
testing as much as I should.

[tc]: http://en.wikipedia.org/wiki/Two's_complement
[sqldt]: http://www.sqlite.org/datatype3.html
[double]: http://en.wikipedia.org/wiki/Double_precision_floating-point_format
[sqlapi]: http://www.sqlite.org/capi3ref.html
[zapp]: http://www.youtube.com/watch?v=KFLq7cyHKMg
