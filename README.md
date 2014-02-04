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

Smallex only emits an rather limited lexer as of now. I'll see if I can pop up a
formal definition when the implementation is done.

## Usage

This is best shown with the output from the help command.

```
usage: java -jar smallex.jar [-v|--version] [-h|--help] [-H|--langhelp lang]
  [--loglevel level] [--logfile file] [-o|--out outdir] [-i|--inputfile file]
  [--list-languages] [-l|--lang language] <language-specific options> ...

Options

 -v, --version
     Prints Smallex' version name.

 -h, --help
     Prints this help.

 -H, --langhelp
     Prints additional information about the specific language, along with other
     flags, the version number and its use.

 --loglevel level
     Specifies the logging level: NONE, INFO or DEBUG. Is by default INFO.

 --logfile file
     Sets the logfile to the specified file. If none is specified, logging is
     sent to stdout.

 --out, -o outdir
     Sets the output directory, and places generated files in that directory. If
     the directory or any parent directory does not exist, will generate those.
     The output directory must be specified.

 --inputfile, -i file
     Specifies the file to read the lexer from.

 --list-languages
     Lists all languages (not necessarily programming languages) supported by
     this version of smallex, along with their version number.

 --lang, -l language
     Specifies the language to emit for. Must be explicitly set if code is to be
     generated.
```

Here is a quick example of how to use the tool: Say I have the file
`lexer.smlx`, and want to generate a java->clojure lexer which should reside in
the directory `mylexer`. Then, the following command is issued (assuming
smallex.jar is in my current directory):

```bash
java -jar smallex.jar -i lexer.smlx --lang java->clojure --out mylexer \
  --pkgname com.hypirion.mylexer
```

TODO: explain how this goes.

## A Small Taste of Smallex Syntax

Smallex itself has a very "dumb" syntax. It consists of `def`, symbol names,
operators, strings and numbers. Definitions start by saying `(def symbol-name
expression)`. An expression is a lisp-like expression, which can use the
operators `or`, `cat`, `star`.

Here's a small example, attempting to read Clojure code.

```lisp
;; Singletons
(def set-start "#{")
(def map-start "{")
(def map-end "}")
(def vector-start "[")
(def vector-end "]")
(def list-start "(")
(def list-end ")")
;; Aliases
(alias opt-sign (opt (or "+-")))
(alias digits (cat (or "1" "2" "3" "4" "5" "6" "7" "8" "9" "0")
                   (star (or "1" "2" "3" "4" "5" "6" "7" "8" "9" "0"))))
;; compound definitions
(def comment (cat ";" (star (not "\n") "\n"))
(def ratio (cat opt-sign digits "/" digits))
(def float (cat opt-sign digits (opt "." (opt digits))
                (opt (or "e" "E") opt-sign digits)))
```



## License

Copyright © 2014 by [hyPiRion](https://github.com/hyPiRion) and
[contributors](https://github.com/hyPiRion/smallex).

Distributed under the [Eclipse Public License, version 1.0][license]. You can
find a copy in the root of this repository with the name `LICENSE`.

[license]: http://www.eclipse.org/legal/epl-v10.html "Eclipse Public License, version 1.0"
