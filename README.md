# mill cannot resolve [kebab-case](http://wiki.c2.com/?KebabCase) module name

kebab style module name architecture is used in many scala open source projects like [cats](https://github.com/typelevel/cats), [quill](https://github.com/getquill/quill), [monix](https://github.com/monix/monix)...
## Environment
* OS : macOS High Sierra
* JVM : java version "1.8.0_112"

## Mill version(installed via brew) 
```sh
$ brew list --versions | grep mill
mill 0.1.6
```

## build.sc
```scala
import mill._
import mill.scalalib._

// mill cannot resolve foo-bar
object `foo-bar` extends ScalaModule {
    def scalaVersion = "2.12.4"
}

// it works!
object fooBar extends ScalaModule {
    def scalaVersion = "2.12.4"
}
```

## `foo-bar` module running results
```bash
# foo-bar doesn't working
$ mill foo-bar.run
Compiling (synthetic)/ammonite/predef/interpBridge.sc
Compiling (synthetic)/ammonite/predef/DefaultPredef.sc
Compiling /Users/liam/IdeaProjects/mill-module-name-bug/build.sc
Cannot resolve foo. Try `mill resolve _` to see what's available.

# I've tried so other expressions
$ mill `foo-bar`.run
Parsing exception CharsWhileIn("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"):1:1 ...".run"
$ mill 'foo-bar'.run
Cannot resolve foo. Try `mill resolve _` to see what's available.
```

## `fooBar` module running results
It works well.
```sh
$ mill fooBar.run
[35/35] fooBar.run
Hello world
```
