---
layout: docs
title: Getting Started
---

# Getting Started

You can depend on the project from maven central in SBT like so:

```scala
libraryDependencies += "net.andimiller" %% "recline" % "@VERSION@"
```

For basic usage you should have these imports

```scala mdoc
import com.monovore.decline._
import net.andimiller.recline.annotations._
import net.andimiller.recline.generic._
```


## A standard CLI

If you've already got a case class representing your configuration, you can just derive a command line parser for it.

```scala mdoc:to-string
case class Configuration(port: Int, hostname: String)

val command = Command("server", "")(deriveCli[Configuration])

command.parse(List("--help"))

command.parse(List("--port", "1234", "--hostname", "myserver"))

command.parse(List.empty, env=Map(
  "PORT" -> "8080",
  "HOSTNAME" -> "somehost"
))
```

## Nested CLIs with some fancy types

```scala mdoc:reset:invisible
import com.monovore.decline._
import net.andimiller.recline.annotations._
import net.andimiller.recline.generic._
```

```scala mdoc:to-string
case class GraphiteConfig(hostname: String, port: Int)
case class Configuration(name: String, graphite: Option[GraphiteConfig])

val command = Command("server", "")(deriveCli[Configuration])
```

This time it's nested, so let's see how the command line works

```scala mdoc:to-string
command.parse(List("--help"))
```

Alright, so we've automatically got a `graphite` prefix on our CLI flags, and `GRAPHITE` on the environment variables, let's use those:

```scala mdoc:to-string
command.parse(List("--name", "myprogram", "--graphite-hostname", "localhost", "--graphite-port", "2003"))
```

And what if I forgot the graphite port?

```scala mdoc:to-string
command.parse(List("--name", "myprogram", "--graphite-hostname", "localhost"))
```

And if there was a default value for it?

```scala mdoc:reset:invisible
import com.monovore.decline._
import net.andimiller.recline.annotations._
import net.andimiller.recline.generic._
```

```scala mdoc:to-string
case class GraphiteConfig(hostname: String, port: Int = 2003)
case class Configuration(name: String, graphite: Option[GraphiteConfig])

val command = Command("server", "")(deriveCli[Configuration])
command.parse(List("--name", "myprogram", "--graphite-hostname", "localhost"))
```

## Okay cool but what if I want to parse into an unusual type?
```scala mdoc:reset:invisible
import com.monovore.decline._
import net.andimiller.recline.annotations._
import net.andimiller.recline.generic._
```

```scala mdoc:to-string
import java.time.Instant
case class Configuration(name: String, timestamp: Instant)
```

```scala mdoc:to-string:fail
val cli = deriveCli[Configuration]
```

So this means we're probably missing one of the types we need, let's try providing one:

```scala mdoc:to-string
import cats.implicits._, cats.data._
import scala.util.Try
implicit val instantArgument: Argument[Instant] = new Argument[Instant] {
  override def read(string: String): ValidatedNel[String, Instant] =
    Try { Instant.parse(string) }.toEither.leftMap(_.getLocalizedMessage).toValidatedNel
  override def defaultMetavar = "timestamp"
}
val cli = deriveCli[Configuration]

Command("my program", "")(cli).parse(List("--name", "foo", "--timestamp", "2019-07-02T12:23:58.006Z"))
```
