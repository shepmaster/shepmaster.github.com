---
layout: post
title: "Be careful when using JUnit's expected exceptions"
date: 2012-09-26 18:52
comments: true
categories: testing
---

For many people, JUnit is the grand-daddy of testing frameworks. Even
though other testing frameworks came first, a lot of people got their
start with JUnit.

People often start out testing with simple Boolean assertions, then
move on substring matching, then maybe on to mocks and stubs. At some
point, however, most people want to assert that their code throws a
particular exception, and that's where our story starts.

<!-- more -->

When JUnit 3 was the latest and greatest, you were supposed to catch
the exception yourself and assert if no such exception was
thrown. Here's an example I tweaked from [Lasse's blog][lasse] and the
JUnit documentation for [`@Test`][test-doc].

```java
@Test
public void test_for_npe_with_try_catch() {
    try {
        throw new NullPointerException();
        fail("should've thrown an exception!");
    } catch (NullPointerException expected) {
        // go team!
    }
}
```

With the newest versions of JUnit 4 (4.11 at the time of writing),
there are two more options available to you: the [`@Test`][test-doc]
annotation and the [`ExpectedException`][expected-doc] rule.

```java
@Test(expected = NullPointerException.class)
public void test_for_npe_with_annotation() {
    throw new NullPointerException();
}
```

```java
@Rule
public ExpectedException thrown = ExpectedException.none();

@Test
public void test_for_npe_with_rule() {
    thrown.expect(NullPointerException.class);
    throw new NullPointerException();
}
```

Both of these forms offer a lot in the way of conciseness and
readability, and I prefer to use them when I need to test this kind of
thing. However, both forms can cause a test to pass when it shouldn't
when the code can throw the exception in multiple ways:

```java
@Test(expected = NullPointerException.class)
public void test_for_npe_but_which_one() {
    CoolObject obj = new CoolObject(null);
    obj.doSomeSetupWork(42);  // What actually throws the exception
    obj.calculateTheAnswer(); // What we want to throw the exception
}
```

In languages that have lambdas or equivalents, this problem is easily
avoided. For example, you can use [`expect`][rspec] and [`raise_error`][rspec] in RSpec:

```ruby
it 'throws_a_npe' do
  obj = CoolObject.new(nil)
  obj.do_some_setup_work(42)
  expect { obj.calculate_the_answer }.to_raise(NoMethodError)
end
```

#### Alternate solutions

Until a version of Java is released with lambdas, I see no better
solution than using try-catch blocks, the old JUnit 3 way. You could
define an interface and then create anonymous classes in the test to
have the desired level of granularity. This is a pretty bulky syntax,
any variables you use in the object would need to be declared final,
and then you have to explictly run the code!

```java
@Test
public void test_for_one_of_two_npe_bulky_syntax() {
    final CoolObject obj = new CoolObject(null);
    obj.doSomeSetupWork(42);

    new GonnaThrowException(NullPointerException.class) {
        public void test() {
            obj.calculateTheAnswer();
        }
    }.run();
}
```

If you can rephrase the problem slightly, you might be able to use the
fact that `ExpectedException` can assert on the exception message to
restrict your test. If you know that **only** your error can include a
certain string, then checking for that string could prevent tests from
passing when they shouldn't.

Another solution would be to modify your code or tests so that you
don't have to deal with the problem in the first place. If you can
move the setup code into a `@Before` block, then the exception
wouldn't be caught by the test. If you can change your code so it
cannot throw the exception multiple ways, or if it throws different
exceptions, then that would also allow you to sidestep the problem.

[lasse]: http://radio.javaranch.com/lasse/2007/05/17/1179405760728.html
[test-doc]: http://kentbeck.github.com/junit/javadoc/latest/org/junit/Test.html
[expected-doc]: http://kentbeck.github.com/junit/javadoc/latest/org/junit/rules/ExpectedException.html
[rspec]: https://www.relishapp.com/rspec/rspec-expectations/v/2-11/docs/built-in-matchers/raise-error-matcher
