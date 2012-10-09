---
layout: post
title: "Finding a race condition in Capybara with Selenium"
date: 2012-10-08 20:40
comments: true
categories: debugging
---

At work, we've been using [Capybara][capybara] and
[Selenium][selenium] to test our newest web application. Many of us
have used this combination before for our own projects, but it's new
territory for a work project.

Every so often, we would get this error from a specific test:

```
Selenium::WebDriver::Error::StaleElementReferenceError:
  Element not found in the cache - perhaps the page has changed since it was looked up
```

The error was intermittent, so we fell into the seductive but
dangerous trap of simply rerunning our tests whenever it
failed. Recently, I had a bit of time and decided to dig into it and
fix it once and for all.

<!-- more -->

My first task was to see if I could reproduce the error locally. We
often saw the error when running the tests on our [Jenkins][jenkins]
continuous integration server, so there was the possibility that the
problem was environmental. However, we also knew that the failure was
intermittent, so we couldn't be sure it was environmental even if the
test passed locally a few times.

I rigged up a small shell script to simply run the test over and over
again while I wandered away from my computer. The script looked
something like:

{% include_code test-script-runner.sh %}

I'm sure there's a proper statistical manner to determine how many
times the test would have to be run without failing to be reasonably
certain that the test won't fail, but I didn't have to worry about
that - the test failed somewhere within the first ten or so runs.

Now that I knew the test could fail on my local setup, it was time to
dig into what the test was doing. The test was fairly concise and
readable (which I highly appreciated) and looked something like:

```ruby
it 'deletes the element', :js => true  do
  visit path_to_the_page
  click_on 'Remove item'
  page.should_not have_css(".item", text: "Old text")
end
```

The exception was coming from line #4 - when the test made the first
assertion about the elements. Unfortunately, the stacktrace isn't very
useful, as it mostly contains references to the JavaScript running
inside of Firefox. The exception text indicates that the test has a
reference to an element, but it isn't available in the cache
anymore. Considering that the test just deleted the element, this is
certainly suspicious.

At this point, I cloned the capybara repository and started poking
around. A `git grep` quickly found where `has_no_css?` was
[defined][has_no_css]. Following the thread of code led to
`has_no_selector?`, which calls the `all` [method][all]. This method
had a pretty clear split between the "finding" part of the code and
the "filtering" part. There was no magic I used here to see this, just
previous experience debugging race conditions.

I opened up the installed gem and inserted a sleep directly into the
code between the "finding" and "filtering" sections. It's ugly doing
this, but it's good to try to not change too many things at once when
debugging. I played with the sleep value a bit and eventually found a
value that reliably reproduced the failure. Success!

Well, maybe not *complete* success, but at least a step in the right
direction. Even though I could reproduce the problem, I had only
reproduced it in our production application, and I had modified my
installed gem directly. It was time to make a nice test case.

I created a new Rails app and added the requisite RSpec gems. Since we
only need a simple HTML page with a bit of JavaScript to remove the
element, I modified the index.html that ships with Rails to have the
JavaScript inline and created an element and link to wire the action
to.

Since I knew that I would want to make changes to Capybara, I used the
`:path` parameter in the Gemfile to point to my local checkout of
Capybara. This is an awesome feature of [Bundler][bundler] that you might not
know about. It also means I'm not messing with my generally-available
copy of Capybara, which is good for my sanity.

I then created a stripped-down version of the test, the same as the
example above. After getting everything hooked up, I ran the test but
it didn't fail. This was bad news - I had done a few big steps between
the production app and the smaller test case - which one of them could
have changed the behavior?

This is where my knowledge of our production system came in useful. In
that application, we aren't just removing something from the page, we
are persisting that deletion to disk. Doing that can add some time
before the JavaScript fires to remove the item. I changed the test
JavaScript to have a delay less than the delay in Capybara and ran the
test again. It failed, just like we wanted it to. To be sure, I ran
the test case a bunch of times to make sure it always failed and for
the expected reason. Success!

Well, almost. Even though I had a test case, I still needed to show
that code to someone who could do something about it. Checking back at
the [Capybara website][capybara], I looked for how to submit a
ticket. Right at the top is a nice, clear comment:

> Need help? Ask on the mailing list (please do not open an issue on GitHub)

So, I pushed my changes to Capybara to [my fork][my-capybara] and
updated my test app to use a remote git version of the gem (another
cool Bundler feature). I then pushed [my test case][test-app] to
GitHub as well. I took a bit of time to create a short README so that
anyone stumbling on the test app would have a clue as to what it was.

After that, it was just a small matter to write up a clear email to
the Capybara list. I still find emailing new lists scary. Who knows
how the list will respond? This time I got a [nice surprise][list-post]:

> That's a very nice bug report, Jake.
>
> It appears to be a bug indeed. I've been able to reproduce it on master as well.

A GitHub issue [has been opened][issue], and the bug is well on the
way to being fixed. Yay for Open Source!

[capybara]: https://github.com/jnicklas/capybara
[selenium]: https://code.google.com/p/selenium/
[jenkins]: http://jenkins-ci.org/
[has_no_css]: https://github.com/jnicklas/capybara/blob/1.1_stable/lib/capybara/node/matchers.rb#L171
[all]: https://github.com/jnicklas/capybara/blob/1.1_stable/lib/capybara/node/finders.rb#L109
[bundler]: http://gembundler.com/
[my-capybara]: https://github.com/shepmaster/capybara
[test-app]: https://github.com/shepmaster/capybara-race
[list-post]: https://groups.google.com/forum/?fromgroups=#!topic/ruby-capybara/O3Ib6INOP58
[issue]: https://github.com/jnicklas/capybara/issues/843
