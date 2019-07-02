---
layout: docs
title: Main Methods 
---

# Main Methods

We can now combine the previous derivations together to make a main method.

This main method has 2 modes

1. fully configured from the command line and environment variables
2. configured from a config file plus overrides

The config file may be `JSON` or `YAML` and will be parsed by deriving the associated circe `Decoder` instances, you can bring your own ones of these.

```scala mdoc
import com.monovore.decline._
import net.andimiller.recline.annotations._
import net.andimiller.recline.generic._
import io.circe.generic.auto._
```

```scala mdoc:to-string
case class GraphiteConfig(
  hostname: String,
  port: Int
)
case class Configuration(
  port: Int, 
  @cli.autokebab
  adminPort: Int,
  graphite: Option[GraphiteConfig]
)
val main = deriveMain[Configuration]("my program", "an example program")

main.parse(List("--help"))
```

So we've derived our main method, we can run it with a config file:

```scala mdoc:to-string
main.parse(List("./docs/src/main/resources/example.yml"))
```

We can run it with a config file and override parts of it from the CLI or env variables:
```scala mdoc:to-string
main.parse(List("./docs/src/main/resources/example.yml", "--port", "1234"), env = Map("ADMIN_PORT" -> "9876"))
```

We could configure the whole thing via flags:
```scala mdoc:to-string
main.parse(List(
  "--port", "123",
  "--admin-port", "456"
))
```
