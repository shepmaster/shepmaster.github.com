---
layout: post
title: "Run your tests in a deterministic random order"
date: 2012-10-18 18:56
comments: true
categories: testing
---

Running your tests in a random order is a good idea to help shake out
implicit dependencies between tests. Running your tests in a
deterministic random order is even better.

<!-- more -->

#### What's an implicit dependency?

It's easy to accidentally create order-dependent tests:

```ruby
it "creates a widget" do
  Widget.create(name: 'Awesome Widget')
  Widget.count.should eql(1)
end

it "deletes a widget" do
  Widget.first.delete # Implicitly requires the first test to have been run
  Widget.count.should eql(0)
end
```

#### Why should I care?

Dependencies between tests are bad for a number of reasons:

1. When a single test fails, you need to run many tests to reproduce
the failure. This makes reproduction slower and more annoying.
2. The test method is no longer complete documentation. The required setup
for the test is located in many different methods.
3. The complexity of the test is hidden. What looks like a two line
test may actually comprise hundreds of lines of code. Complex test
code is often an excellent indicator of complex production code.

Running tests in a random order isn't enough; you need to be able to
reproduce the same random order before you can fix it! [RSpec][rspec]
and [MiniTest][minitest] both offer a way to specify the random seed
on the command line or with environment variables. Unfortunately, the
[Surefire][surefire] plugin for Maven does not offer a way to specify
the seed, even though it allows random ordering.

#### Continuous integration servers

At work, we use [gerrit][gerrit] for code reviews and
[Jenkins][jenkins] as our CI server. Whenever a new or updated commit
is pushed to gerrit, a build is started in Jenkins. There is also a
Jenkins job to build `origin/master` every 15 minutes if it has been
updated.

The Gerrit/Jenkins combination allows you to retrigger a specific
build in case there were environmental issues that have since been
fixed. Unfortunately for us, retriggering was being used as a way to
avoid dealing with test failures due to order dependencies. To
encourage us to stop and address our order dependency problem, we
updated both jobs to use a deterministic seed.

For the Gerrit builds, we used the Gerrit change number, which remains
constant across multiple revisions of the same commit. The Gerrit
plugin makes this value available as a environment variable during
script execution.

```bash
rspec SPEC_OPTS="--seed $GERRIT_CHANGE_NUMBER"
```

For the `origin/master` build, we chose to use the Git hash of the
commit. Since the hash contains letters, we used a shell one-liner to
scrape out something that looks reasonable as a seed.

```bash
SEED=$(git rev-parse HEAD | tr -d 'a-z' | cut -b 1-5)
rspec SPEC_OPTS="--seed $SEED"
```

#### Does it work?

Just a few days after making the above changes, another developer came
to me with a strange problem. His commit was unable to pass the tests
in Gerrit, but the failing test had nothing to do with his changes. We
ran the tests locally using the seed from the Jenkins server and were
able to reproduce the problem. Ultimately, we traced the problem to a
request spec that modified some core configuration settings and didn't
reset them successfully. Success!


[rspec]: https://www.relishapp.com/rspec/rspec-core/v/2-11/docs/command-line/order-new-in-rspec-core-2-8
[minitest]: http://www.bootspring.com/2010/09/22/minitest-rubys-test-framework/
[surefire]: http://maven.apache.org/plugins/maven-surefire-plugin/test-mojo.html
[gerrit]: http://code.google.com/p/gerrit/
[jenkins]: http://jenkins-ci.org/
