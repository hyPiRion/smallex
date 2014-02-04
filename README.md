# smallex

Smallex is a small application/library used for emitting small, hopefully
somewhat readable lexers. The lexers emitted are intended to be designed for
customisability and readability.

You often want to have control of your lexer, but let's face it: You very seldom
want to create the control flow graph by hand. Whenever I create
lexers/scanners, they end up just having a "backtrack" function which puts
values on top of the stack. This actually turned out to be a performance issue
for me, and although it seldom is, I don't want to end up with the same problem
twice.

To state the somewhat obvious here: This is a *lexer* library. If you wanted to
do easy *parsing*, then I would recommend the amazing
[instaparse](https://github.com/Engelberg/instaparse) library. If you want a
general lexer library for Clojure/JVM, I'd recommend
[ANTLR](http://www.antlr.org/). Hopefully I can someday recommend this lexer
generator instead.

## (Subsections go here)

TODO:
* What does the library do?
* Why did you write it (or didn't use an existing solution)?
* Who should be using it, and for what reasons?
* How should it be used (how to build it, how to configure it, how to run it and get started)?
* Where can additional information be found? (typically doc/tutorial.md)
From [Don't be a jerk: Write documentation](http://ferd.ca/don-t-be-a-jerk-write-documentation.html)
by Frederic Trottier-Hebert.

## License

Copyright © 2014 by [hyPiRion](https://github.com/hyPiRion) and
[contributors](https://github.com/hyPiRion/smallex).

Distributed under the [Eclipse Public License, version 1.0][license]. You can
find a copy in the root of this repository with the name `LICENSE`.

[license]: http://www.eclipse.org/legal/epl-v10.html "Eclipse Public License, version 1.0"
