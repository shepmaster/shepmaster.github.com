---
layout: post
title: "Game of Life in Clojure"
date: 2011-09-18 11:22
comments: true
categories: clojure
---
I've been trying to learn [Clojure][clj] recently, so I wrote
[Conway's Game of Life][gol]. I'm sure that the code is highly
non-idiomatic, but I'd rather get it out into the wild, instead of
sitting on my disk.

<!-- more -->

Note that I have inline tests (the simple `println` statements) mostly
because I wasn't trying to figure out how to properly test in Clojure
(yet).

{% include_code life.clj %}

[clj]: http://clojure.org
[gol]: http://en.wikipedia.org/wiki/Conway's_Game_of_Life
