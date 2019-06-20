---
layout: docs
title: Getting Started
---



# Getting Started 

For basic usage you should have these imports

```scala mdoc
import com.monovore.decline._
import net.andimiller.recline.annotations._
import net.andimiller.recline.generic._
```

If you've already got a case class representing your configuration, you can just derive a command line parser for it.

```scala mdoc
case class Configuration(port: Int, hostname: String)

val cli = deriveCli[Configuration]
```