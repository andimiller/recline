import cats.data.NonEmptyList
import org.scalatest.{MustMatchers, WordSpec}
import com.monovore.decline._
import net.andimiller.recline.annotations._
import net.andimiller.recline.generic._
import cats.implicits._

class DeriverSpec extends WordSpec with MustMatchers {
  implicit class commandAbleOpt[T](o: Opts[T]) {
    def command: Command[T] = Command("", "", true)(o)
  }

  "CliDeriver" should {
    "derive complete CLIs" in {
      case class Cat(name: String, age: Int)
      val cli = deriveCli[Cat]
      cli.command.parse(List("--name", "bob", "--age", "4")) must equal(Right(Cat("bob", 4)))
      cli.command.parse(List("--name", "bob")).isLeft must equal(true)
    }
    "derive nested CLIs" in {
      case class Cat(name: String, age: Int)
      case class Owner(full_name: String, cat: Cat)
      deriveCli[Owner].command.parse(
        List(
          "--full_name",
          "martin",
          "--cat-name",
          "tom",
          "--cat-age",
          "14"
        )) must equal(
        Right(Owner("martin", Cat("tom", 14)))
      )
    }
    "cope with conflicts" in {
      case class Owner(
          name: String
      )
      case class Cat(
          name: String,
          original_owner: Owner,
          original_owner_name: String
      )
      val parser = deriveCli[Cat].command
      parser.parse(List.empty)
    }
    "let you tag on extra information with annotations" in {
      case class Owner(
          @cli.help("full name") name: String
      )
      case class Cat(
          @cli.name("full-name") @cli.help("cats deserve to have names") @cli.short("n") fullName: String,
          @cli.name("age") @cli.help("cats have an age") @cli.short("a") @cli.metavar("2 digits") age: Int,
          @cli.autokebab @cli.help("eg. lasagne") favouriteFood: String,
          @cli.autokebab originalOwner: Owner
      )
      deriveCli[Cat].command
        .parse(List("--help"))
        .leftMap(_.toString) must equal(
        Left(
          """Usage:  [--full-name <string>] [--age <2 digits>] [--favourite-food <string>] [--original-owner-name <string>]
                |
                |
                |
                |Options and flags:
                |    --help
                |        Display this help text.
                |    --full-name <string>, -n <string>
                |        cats deserve to have names
                |    --age <2 digits>, -a <2 digits>
                |        cats have an age
                |    --favourite-food <string>
                |        eg. lasagne
                |    --original-owner-name <string>
                |        full name
                |
                |Environment Variables:
                |    FULL_NAME=<string>
                |        cats deserve to have names
                |    AGE=<2 digits>
                |        cats have an age
                |    FAVOURITE_FOOD=<string>
                |        eg. lasagne
                |    ORIGINAL_OWNER_NAME=<string>
                |        full name""".stripMargin
        )
      )
    }
    "load from environment variables" in {
      case class A(inner: String)
      case class B(a: A)
      case class C(b: B)
      val parser = deriveCli[C].command
      parser.parse(
        List.empty,
        Map(
          "B_A_INNER" -> "value"
        )
      ) must equal(
        Right(
          C(B(A("value")))
        ))
    }
    "let you use defaults" in {
      case class Cat(name: String, age: Int = 4)
      deriveCli[Cat].command
        .parse(List("--name", "martin")) must equal(
        Right(Cat("martin", 4))
      )
    }
    "let you have 1 or more of one parameter" in {
      case class Cat(@cli.name("name") names: NonEmptyList[String])
      deriveCli[Cat].command
        .parse(List("--name", "bob", "--name", "martin")) must equal(
        Right(
          Cat(NonEmptyList.of("bob", "martin"))
        )
      )
    }
    "let you have multiple of one parameter" in {
      case class Cat(@cli.name("name") names: List[String])
      deriveCli[Cat].command
        .parse(List("--name", "bob", "--name", "martin")) must equal(
        Right(
          Cat(List("bob", "martin"))
        )
      )
    }
    "let you have multiple of one parameter, and define a separator for the env" in {
      case class Cat(@cli.name("name") @cli.separator(' ') names: NonEmptyList[String])
      deriveCli[Cat].command
        .parse(List.empty,
               Map(
                 "NAME" -> "millie mildred"
               )) must equal(
        Right(
          Cat(NonEmptyList.of("millie", "mildred"))
        )
      )
    }
    "accept optional things" in {
      case class Cat(name: Option[String])
      val c = deriveCli[Cat].command
      c.parse(List.empty) must equal(Right(Cat(None)))
      c.parse(List("--name", "bob")) must equal(Right(Cat(Some("bob"))))
    }
    "derive CLIs for nested optional sections" in {
      case class Owner(@cli.autokebab firstName: String, @cli.autokebab secondName: String)
      case class Cat(name: String, owner: Option[Owner])
      val c = deriveCli[Cat].command
      c.parse(List("--name", "marge")) must equal(Right(Cat("marge", None)))
      c.parse(List("--name", "marge", "--owner-first-name", "tom", "--owner-second-name", "smith")) must equal(
        Right(Cat("marge", Some(Owner("tom", "smith"))))
      )
    }
    "derive CLIs for configs with boolean values" in {
      case class Config(foo: Boolean, bar: String)
      val c = deriveCli[Config].command

      c.parse(List("--foo", "--bar", "barvalue")) must equal(Right(Config(true, "barvalue")))
      c.parse(List("--bar", "barvalue")) must equal(Right(Config(false, "barvalue")))
    }
  }

  "SetterCliDeriver" should {
    "derive CLIs which are setters" in {
      case class Cat(name: String, age: Int)
      deriveSetterCli[Cat].command
        .parse(
          List(
            "--name",
            "bob",
            "--age",
            "4"
          ))
        .map { f =>
          f(Cat("terry", 3))
        } must equal(Right(Cat("bob", 4)))
    }
    "derive CLIs which can be partial setters" in {
      case class Cat(name: String, age: Int)
      deriveSetterCli[Cat].command
        .parse(
          List(
            "--name",
            "bob",
          ))
        .map { f =>
          f(Cat("terry", 3))
        } must equal(Right(Cat("bob", 3)))
    }
    "derive CLIs which have nested fields" in {
      case class A(inner: String)
      case class B(a: A)
      case class C(b: B)
      val parser = deriveSetterCli[C].command
      parser
        .parse(
          List(
            "--b-a-inner",
            "hi",
          ))
        .map { f =>
          f(C(B(A("initial"))))
        } must equal(Right(C(B(A("hi")))))
    }
    "derive CLIs which can override with environment variables" in {
      case class A(value: String)
      val parser = deriveSetterCli[A].command
      parser
        .parse(
          List.empty,
          Map(
            "VALUE" -> "v"
          )
        )
        .map { f =>
          f(A("original"))
        } must equal(Right(A("v")))
    }
    "let you have multiple of a parameter" in {
      case class Cat(@cli.name("name") names: NonEmptyList[String], age: Int)
      deriveSetterCli[Cat].command
        .parse(
          List("--name", "bob", "--name", "martin")
        )
        .map { f =>
          f(Cat(NonEmptyList.of("marge"), 4))
        } must equal(
        Right(
          Cat(NonEmptyList.of("bob", "martin"), 4)
        )
      )
    }
    "let you have 1 or more of one parameter" in {
      case class Cat(@cli.name("name") names: NonEmptyList[String])
      deriveSetterCli[Cat].command
        .parse(List("--name", "bob", "--name", "martin"))
        .map { f =>
          f(Cat(NonEmptyList.one("blah")))
        } must equal(
        Right(
          Cat(NonEmptyList.of("bob", "martin"))
        )
      )
    }
    "let you have multiple of one parameter" in {
      case class Cat(@cli.name("name") names: List[String])
      deriveSetterCli[Cat].command
        .parse(List("--name", "bob", "--name", "martin"))
        .map { f =>
          f(Cat(List.empty))
        } must equal(
        Right(
          Cat(List("bob", "martin"))
        )
      )
    }
    "let you have multiple of one parameter, and define a separator for the env" in {
      case class Cat(@cli.name("name") @cli.separator(' ') names: NonEmptyList[String])
      deriveSetterCli[Cat].command
        .parse(List.empty,
               Map(
                 "NAME" -> "millie mildred"
               ))
        .map { f =>
          f(Cat(NonEmptyList.one("marge")))
        } must equal(
        Right(
          Cat(NonEmptyList.of("millie", "mildred"))
        )
      )
    }
    "accept optional things" in {
      case class Cat(name: Option[String])
      val c = deriveSetterCli[Cat].command
      c.parse(List.empty).map(f => f(Cat(Some("bob")))) must equal(Right(Cat(Some("bob"))))
      c.parse(List.empty).map(f => f(Cat(None))) must equal(Right(Cat(None)))
      c.parse(List("--name", "bob")).map(f => f(Cat(Some("terry")))) must equal(Right(Cat(Some("bob"))))
      c.parse(List("--name", "bob")).map(f => f(Cat(None))) must equal(Right(Cat(Some("bob"))))
      c.parse(List("--name", "")).map(f => f(Cat(Some("terry")))) must equal(Right(Cat(None)))
      c.parse(List("--name", "")).map(f => f(Cat(None))) must equal(Right(Cat(None)))
    }
    "derive CLIs for nested optional sections" in {
      case class Owner(@cli.autokebab firstName: String, @cli.autokebab secondName: String)
      case class Cat(name: String, owner: Option[Owner])
      val c = deriveSetterCli[Cat].command
      c.parse(List("--name", "marge")).map(_(Cat("bob", None))) must equal(Right(Cat("marge", None)))
      c.parse(List("--name", "marge")).map(_(Cat("bob", Some(Owner("a", "b"))))) must equal(Right(Cat("marge", Some(Owner("a", "b")))))
      c.parse(List("--name", "marge", "--owner-first-name", "tom", "--owner-second-name", "smith")).map(_(Cat("bob", None))) must equal(
        Right(Cat("marge", Some(Owner("tom", "smith"))))
      )
      c.parse(List("--name", "marge", "--owner-first-name", "tom")).map(_(Cat("bob", None))) must equal(
        Right(Cat("marge", None))
      )
      c.parse(List("--name", "marge", "--owner-first-name", "tom")).map(_(Cat("bob", Some(Owner("tim", "smith"))))) must equal(
        Right(Cat("marge", Some(Owner("tom", "smith"))))
      )
    }
  }
  "deriveMain" should {
    "derive a main method that can parse a config" in {
      import io.circe.generic.auto._
      import net.andimiller.recline.generic._
      case class Config(@cli.help("port to bind to") port: Int, name: String)

      deriveMain[Config]("program", "my program").parse(List("--help")).leftMap(_.toString) must equal(
        Left(
          """Usage:
              |    program [--port <integer>] [--name <string>] <config>
              |    program [--port <integer>] [--name <string>]
              |
              |my program
              |
              |Options and flags:
              |    --help
              |        Display this help text.
              |    --port <integer>
              |        port to bind to
              |    --name <string>
              |
              |
              |Environment Variables:
              |    PORT=<integer>
              |        port to bind to
              |    NAME=<string>
              |    """.stripMargin
        )
      )
    }
    "be useful for deriving multiple commands" in {
      import io.circe.generic.auto._
      import net.andimiller.recline.generic._
      case class FooConfig(i: Int)
      case class BarConfig(s: String)

      val fooCommand = deriveMain[FooConfig]("foo", "do a foo")
      val barCommand = deriveMain[BarConfig]("bar", "do a bar")

      val composed = Command("full program", "")(Opts.subcommand(fooCommand).orElse(Opts.subcommand(barCommand)))

      composed.parse(List("--help")).leftMap(_.toString) must equal(Left(
        """Usage:
    full program foo
    full program bar



Options and flags:
    --help
        Display this help text.

Subcommands:
    foo
        do a foo
    bar
        do a bar"""))
    }
  }

}
