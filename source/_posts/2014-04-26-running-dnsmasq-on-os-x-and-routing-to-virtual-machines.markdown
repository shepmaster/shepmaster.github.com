---
layout: post
title: "Running dnsmasq on OS X and routing to virtual machines"
date: 2014-04-26 17:28:27 -0400
comments: true
categories: docker networking
---

At work, I needed to run a [Docker][docker] container with a Rails
application that talked to another application running inside a
[VMWare][vmware] virtual machine. Adding to the complexity, I use
[boot2docker][b2d], which runs inside of [VirtualBox][vbox].

If I only needed to access `rails.localdomain.dev` or
`api.localdomain.dev` from my Mac, I could have simply edited
`/etc/hosts` and set both domains to resolve to `127.0.0.1` and been
done with it. Unfortunately, Rails needed to be able to directly
resolve the API server.

<!-- more -->

## Setting up dnsmasq

**NOTE**: It's possible that editing `/etc/hosts` would have been
enough and I didn't need to set up dnsmasq at all. Read the section
about configuring the virtual machine's DNS first.

I followed [this tutorial][tutorial] to install and configure
[dnsmasq][dnsmasq]. You can ignore the parts about nginx and foreman.

Our Rails application must run at a domain like
`rails.localdomain.dev`, and the API server runs at
`api.localdomain.dev`, so I configured dnsmasq to manage the
`.localdomain.dev` domain.

I moved the hard-coded DNS entry for `api.localdomain.dev` from
`/etc/hosts` to `dnsmasq.conf`. I found this IP by logging into the
API VM and looking at the output of `ifconfig`. I'm not certain why
this IP will not change, but that's what I was told.

## Creating a routable "loopback address"

Originally, I had configured `api.localdomain.dev` to resolve to
`127.0.0.1`. This works fine when accessed from the Mac, but when a
virtual machine resolved that domain, `127.0.0.1` would refer to the
virtual machine itself! I needed an IP address that:

1. Would refer to my laptop.
2. Wouldn't change when I changed network configuration.
3. Wouldn't resolve to the VM inside the VM.

We can accomplish this by using an ifconfig `alias`:

```
sudo ifconfig lo0 alias 10.254.254.254
```

I picked `10.254.254.254` because it is a [private network][privnet]
address and it is unlikely to be used on any networks I connect to. If
I ever do have a conflict, there are many other private addresses to
choose from!

I edited `dnsmasq.conf` and replaced `127.0.0.1` with
`10.254.254.254`. Requests for `*.localdomain.dev` will now resolve to
an IP address that will always refer to the Mac, but that the virtual
machines will not think resolves to the virtual machine itself.

Big thanks to [Andre][andre] for helping me find and understand how
aliasing works!

## Configuring virtual machine DNS

Next I configured the virtual machine to route all DNS requests
through the Mac's resolving system. For VirtualBox, configure it
according to this [serverfault answer][answer]. If you are using
Vagrant, you can add a stanza like:

```
config.vm.provider "virtualbox" do |vb|
  # Always go through OS X resolver, allowing us to redirect local domains.
  vb.customize ["modifyvm", :id, "--natdnsproxy1", "on"]
  vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
end
```

I'm not sure, but it's possible that these settings would use entries
configured in my Mac's `/etc/hosts`. This could make it so that the
dnsmasq step isn't required.

Instead of resolving through the host, I could have edited
`/etc/resolv.conf` and used `10.254.254.254` as my DNS server
instead. If you do this, you definitely need to run dnsmasq.

Once the virtual machine could ping `api.localdomain.dev`, I restarted
the Docker daemon to pick up the networking changes. Dropping into a
Docker container, I was able to ping the API server as well. Success!

[andre]: https://twitter.com/bobthebotmaker
[answer]: http://serverfault.com/a/453260/119136
[b2d]: https://github.com/boot2docker/boot2docker
[dnsmasq]: http://www.thekelleys.org.uk/dnsmasq/doc.html
[docker]: https://www.docker.io/
[privnet]: http://en.wikipedia.org/wiki/Private_network
[tutorial]: http://blog.molawson.com/replace-pow-on-mavericks-10-9-with-nginx-dnsmasq-and-foreman
[vbox]: https://www.virtualbox.org/
[vmware]: http://www.vmware.com/
