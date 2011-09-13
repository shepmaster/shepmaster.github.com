---
layout: post
title: "Using named routes in ActionMailer tests with Rails 3"
date: 2011-02-26 12:00
comments: true
categories: rails
---
The version of ActionMailer included with Rails 3.0.4 allows you to
use named routes in the mailer view by default, but writing a
functional test that tests those named routes isn't as out-of-the-box.

<!--more-->

Say you had created a "Notifications" object with a subscribe method,
like such:

``` ruby
class Notifications < ActionMailer::Base
  def subscribe(user)
    mail(:to      => 'test@example.com',
	 :subject => "You are now subscribed")
  end
end
```

The subscribe method creates a simple text email format that includes
a link back to your application:

    Thanks for subscribing. Would you like to unsubscribe?
    <%= unsubscribe_url(:email => 'test@example.com') %>

Here is what a test that checks that the important unsubscribe link
exists might look like:

``` ruby
require 'test_helper'

class NotificationsTest < ActionMailer::TestCase
  test "subscribe" do
    mail = Notifications.subscribe
    assert_match unsubscribe_url(:email => 'test@example.com'), mail.body.encoded
  end
end
```

However, running that test will give you an error like:

    NoMethodError: undefined method `unsubscribe_url' for #<NotificationsTest:0x000001039dbe40>
        test/functional/notifications_test.rb:6:in `block in <class:NotificationsTest>'

To get around this, you need to include the URL helpers (to define
those methods) and point to your default URL options (to define
options such as the default host). Modify the test to look like:

``` ruby
require 'test_helper'

class NotificationsTest < ActionMailer::TestCase
  include Rails.application.routes.url_helpers

  def default_url_options
    Rails.application.config.action_mailer.default_url_options
  end

  test "subscribe" do
    mail = Notifications.subscribe
    assert_match unsubscribe_url(:email => 'test@example.com'), mail.body.encoded
  end
end
```