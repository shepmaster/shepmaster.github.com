---
layout: post
title: "Stubbing the Builder pattern in Mockito"
date: 2012-01-09 12:00
comments: true
categories: java mocking
---

Recently, I was asked to help review new tests for production code
that made use of the [Builder][builder] pattern. The code in question
did not lend itself to nice and easy testing, but leveraging a
lesser-used bit of [Mockito][mockito] functionality helped make the
code better.

<!-- more -->

Originally, the tests created a test double for the builder object and
then stubbed all of the methods on the builder to return the builder
double itself. The code looked a bit like:

```java
@Test
public void builder_test_v1()
{
    Foo f = mock(Foo.class);
    FooBuilder b = mock(FooBuilder.class);
    when(b.enableAlpha()).thenReturn(b);
    when(b.disableBeta()).thenReturn(b);
    when(b.increaseGamma()).thenReturn(b);
    when(b.build()).thenReturn(f);

    productionCode(b);
    verify(f).someMethod();
}

public void productionCode(FooBuilder builder)
{
    // code that uses the builder...
    Foo foo = b.enableAlpha().disableBeta().increaseGamma().build();
}
```

There are a few downsides to this approach. The first thing we noticed
was the amount of work done to set up the builder compared to the rest
of the test. All that line noise distracts us from the meaning of the
test. This can be easily improved by pulling the builder setup into a
separate method:

```java
@Test
public void builder_test_v2()
{
    Foo f = mock(Foo.class);
    FooBuilder b = newBuilderMock();
    when(b.build()).thenReturn(f);

    productionCode(b);
    verify(f).someMethod();
}

private FooBuilder newBuilderMock()
{
    FooBuilder b = mock(FooBuilder.class);
    when(b.enableAlpha()).thenReturn(b);
    when(b.disableBeta()).thenReturn(b);
    when(b.increaseGamma()).thenReturn(b);
    return b;
}
```

While the test is now easier to read and the new method is reusable in
other tests, we still will be in trouble when the methods of the
builder change.

If your builder implements an interface, you should consider creating
an implementation of that interface that you can easily configure for
testing. Something like:

```java
public class TestingFooBuilder implements FooBuilder
{
    private Foo returnValue;

    public TestingFooBuilder(Foo returnValue)
    {
        this.returnValue = returnValue;
    }

    public FooBuilder enableAlpha()
    {
        return this;
    }

    public FooBuilder disableBeta()
    {
        return this;
    }

    public FooBuilder increaseGamma()
    {
        return this;
    }

    public Foo build()
    {
        return returnValue;
    }
}
```

```java
@Test
public void builder_test_v3()
{
    Foo f = mock(Foo.class);
    FooBuilder b = new TestingFooBuilder(f);

    productionCode(b);
    verify(f).someMethod();
}
```

An implementation like this allows you to lean on the compiler when
the interface changes.

If you don't have a interface to implement, you could subclass
the concrete builder class and insert your test-specific logic
there. The downside to this is that newly-added methods will inherit
their implementation from the parent class, which can cause very
strange test failures.

We did not have an interface to adhere to, so we used Mockito's
`Answer` class to provide a middle ground solution. When you create a
new mock, an `Answer` can be used to provide default behavior for
methods. Here's the custom `Answer` we came up with:

{% include_code AnswerWithSelf.java %}

The answer can be used like this:

```java
@Test
public void builder_test_v4()
{
    Foo f = mock(Foo.class);
    FooBuilder b = mock(FooBuilder.class, new AnswerWithSelf(FooBuilder.class));
    when(b.build()).thenReturn(f);

    productionCode(b);
    verify(f).someMethod();
}
```

This `Answer` can be used with any [fluent interface][fluent] that
returns the original object.

[builder]: http://en.wikipedia.org/wiki/Builder_pattern
[mockito]: http://code.google.com/p/mockito/
[fluent]: http://en.wikipedia.org/wiki/Fluent_interface
