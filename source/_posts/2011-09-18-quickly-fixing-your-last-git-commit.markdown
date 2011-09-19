---
layout: post
title: "Quickly fixing your last Git commit"
date: 2011-09-18 13:53
comments: true
categories: git
---
Git encourages you to create commits early and frequently, but I often
find that my last commit isn't quite as awesome as I'd like it to
be. However, there are three little tricks I use to tweak it a bit.

<!-- more -->

The main use for amending your commit is changing your last commit
message:

    $ git commit --amend

However, if you add files before you amend your commit, those files
will be combined with the commit. This is great if you missed a file,
or if you forgot to save that very last change in your editor.

    $ git add README.txt
    $ git commit --amend

Amending your commit will bring up your editor to change your commit
message; what should you do if your previous message was just fine?
Just reuse the commit message!

    $ git add README.txt
    $ git commit --amend -C HEAD

This is conceptually the same as the "fixup" command when rebasing, so
you could alias it as `git fixup`.

    $ git add README.txt
    $ git fixup

#### Update - 8:30 PM EDT

[Nick Rutherford][nr] reminded me to caution everyone that you should
never amend your commit *after* you have pushed it or made it public
in some fashion. This is for the same reason that you should never
rebase commits that are already public: you are changing history that
other people may have already grabbed a copy of.

[nr]: http://twitter.com/nick_rutherford