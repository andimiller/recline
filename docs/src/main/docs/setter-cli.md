---
layout: docs
title: Setter CLIs 
---

# Setter CLIs

We have the concept of a setter CLI, you may not need to use this on it's own, but it's handy to know how it works.

It's a CLI parser that, in stead of parsing a `T` directly, parses a `T => T`, this allows you to parse a `config.yml` and override parts of it from CLI flags or env variables, which is useful for practical microservice use.

```scala mdoc
import com.monovore.decline._
import net.andimiller.recline.annotations._
import net.andimiller.recline.generic._
```

```scala mdoc:to-string
case class Configuration(name: String, port: Int)

val config = Configuration("abc", 123) // imagine we parsed this from a config file

val setterCli = deriveSetterCli[Configuration]

Command("program", "")(setterCli).parse(List("--name", "real name")).map(f => f(config))
```

You'll see that it transforms the configuration using our CLI flags as overrides.