---
layout: post
title: "Watch out for lost updates when using Capybara with Selenium"
date: 2012-10-10 19:41
comments: true
categories: testing debugging
---

At work, I am still working on finding and squashing fun test
failures. In this case, "fun" means tests that have an intermittent
failure rate of 5% (or less!). The test issue I worked on today had to
do with the "lost update" problem.

<!-- more -->

### The lost update problem

[Growing Object-Oriented Software, Guided by Tests][goos] has a great
description and diagram of the problem:

<img src="/images/blog/lost-update.png" alt="The lost update problem" />

The short version is that when you poll a system for its state, it's
entirely possible to miss the state you are looking for. In the
diagram, the color changes to red and then to blue before the test
ever has a chance to see that it was red. Since this system will never
go back to red, the test will incorrectly fail.

### The lost update problem in Capybara

Like many other sites, we use the [DataTables][datatables] jQuery
plugin to show tabular data. A test that ensured that the filtering
worked looked something like this:

```ruby
def wait_for_table_loading
  dialog = page.find('.loading_dialog')
  wait_until { dialog.visible? }
end

def wait_for_table_ready
  dialog = page.find('.loading_dialog')
  wait_until { ! dialog.visible? }
end

it 'filters the list' do
  visit list_path
  click_on 'Filter by active'
  wait_for_table_loading
  wait_for_table_ready
  page.all('.data-item').should have(3).items
end
```

Enabling filtering triggers some slow backend activity, which brings
up the loading dialog. The test waits for that dialog to appear and
disappear before continuing on. Now the entire table is populated and
we can safely see how many elements are in the table.

However, the test will fail if the backend is *too fast*. The loading
dialog will appear and disappear almost immediately. The test will
time out waiting for the loading dialog that will never appear
again. This behavior can be reliably replicated by adding a sleep to
the test between lines 13 and 14.

### A Capybara solution

In order to make the test more robust, I rewrote it as:

```
it 'filters the list' do
  visit list_path
  click_on 'Filter by active'
  page.should have_css('.data-item', :count => 3)
end
```

The test now ignores the loading dialogs completely, instead asking
Capybara to find a particular number of elements. Asking Capybara to
find things in this manner will let the test leverage the built-in
waiting facilities of Capybara.

In this test, the number of data items won't change once the table is
loaded, so it is a safe state to poll. As an additional benefit, the
test now has fewer lines of code and is clearer.

As a downside, when the test fails, the Capybara error message doesn't
include how many items were found, which isn't as informative as the
equivalent message from the RSpec matcher.

Also, this test still ultimately relies on polling the DOM, so it's
possible for similar bugs to pop up in the future.

### The GOOS solution to the lost update problem

GOOS provides a solution to the lost update problem that can avoid the
problems with polling completely:

<img src="/images/blog/lost-update-fixed.png" alt="The solution to the lost update problem" />

The system under test must be modified to provide notifications when
something interesting happens. This system now has a listener that is
notified when the color changes and what the color is changed to. The
test supplies a simple listener that accumulates the changes and
offers a nice API suited for the tests.

### A hypothetical Capybara solution without polling

I can imagine a Capybara test that looks something like this:

```ruby
it 'filters the list' do
  visit list_path
  wait_for_js_event('table.loaded') do
    click_on 'Filter by active'
  end
  page.all('.data-item').should have(3).items
end
```

Under the hood, there's some extra JavaScript going on. The
`wait_for_js_event` method would inject some JavaScript into the
running Selenium session that creates an event listener and binds it
to the given event. This listener just collects all the events it
receives. After yielding the block, the test code then polls the event
listener, waiting for the event to be captured.

It's entirely possible that code that does this already exists, but I
don't know of it. It wouldn't be a large amount of code to write, but
it would straddle the borders of Capybara, Selenium and JavaScript.

This might be a useful thing for [Jasmine][jasmine] tests, so it might
already exist in that ecosystem.

[goos]: http://www.amazon.com/gp/product/0321503627/ref=as_li_ss_tl?ie=UTF8&tag=jakgousblo-20&linkCode=as2&camp=217145&creative=399369&creativeASIN=0321503627
[datatables]: http://datatables.net/
[jasmine]: http://pivotal.github.com/jasmine/
