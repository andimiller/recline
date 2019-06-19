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

  }

}