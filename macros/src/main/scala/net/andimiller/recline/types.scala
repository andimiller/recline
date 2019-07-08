package net.andimiller.recline

import cats.data._
import cats.implicits._
import com.monovore.decline.recline.Folder
import com.monovore.decline.{Argument, Opts}
import magnolia._
import net.andimiller.recline.annotations._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

object types {

  @implicitNotFound("Unable to find implicit Cli for ${T}, make sure you've got all of the Arguments in scope for it's members")
  case class Cli[T](opts: Opts[T])
  object Cli {
    implicit def fromCliDeriver[T](implicit cli: CliDeriver[T]): Cli[T] = Cli(cli.getOpts.get)
  }

  case class SetterCli[T](opts: Opts[T => T])
  object SetterCli {
    implicit def fromSetterCliDeriver[T](implicit setterCli: SetterCliDeriver[T]): SetterCli[T] = SetterCli(setterCli.getSetters.get)
  }

  sealed trait CliDeriver[T] {
    def getOpts: Option[Opts[T]]
  }

  case class ROpts[T](o: Opts[T]) extends CliDeriver[T] {
    override def getOpts: Option[Opts[T]] = Some(o)
  }

  case class RArgs[T, O](a: Argument[T], optional: Boolean = false) extends CliDeriver[O] {
    override def getOpts: Option[Opts[O]] = None
  }

  case class RArgOptional[T, O](a: Argument[T]) extends CliDeriver[O] {
    override def getOpts: Option[Opts[O]] = None
  }

  case class RArg[T](a: Argument[T]) extends CliDeriver[T] {
    override def getOpts: Option[Opts[T]] = None
  }

  case class RFlag[T]() extends CliDeriver[T] /* actually boolean */ {
    override def getOpts: Option[Opts[T]] = None
  }

  object CliDeriver {
    implicit val flag: CliDeriver[Boolean]                                                = RFlag[Boolean]()
    implicit def fromArgMulti[T](implicit a: Argument[T]): CliDeriver[NonEmptyList[T]]    = RArgs(a)
    implicit def fromArgMultiList[T](implicit a: Argument[T]): CliDeriver[List[T]]        = RArgs(a, true)
    implicit def fromArgSingle[T](implicit a: Argument[T]): CliDeriver[T]                 = RArg(a)
    implicit def fromArgOptional[T](implicit a: Argument[T]): CliDeriver[Option[T]]       = RArgOptional(a)
    implicit def fromCliOptional[T <: Product](implicit o: Cli[T]): CliDeriver[Option[T]] = ROpts(o.opts.orNone)

    type Typeclass[T] = CliDeriver[T]

    val booleanEnvironmentArgument: Argument[Boolean] = new Argument[Boolean] {
      override def read(string: String): ValidatedNel[String, Boolean] =
        string.toLowerCase match {
          case "yes" | "on" | "true"  => true.validNel[String]
          case "no" | "off" | "false" => false.validNel[String]
          case _                      => true.validNel[String]
        }
      override def defaultMetavar: String = ""
    }

    def combine[T](ctx: CaseClass[CliDeriver, T]): CliDeriver[T] =
      ROpts(
        ctx.parameters
          .map { p =>
            val sep = p.annotations
              .collect { case cli.separator(c) => c }
              .lastOption
              .getOrElse(',')
            val kebab = p.annotations
              .collect { case cli.autokebab() => true }
              .lastOption
              .getOrElse(false)
            val argument = p.annotations
              .collect { case cli.argument() => true }
              .lastOption
              .getOrElse(false)
            val name = p.annotations
              .collect { case cli.name(n) => n }
              .lastOption
              .getOrElse(
                if (kebab)
                  Utils.kebab(p.label)
                else p.label
              )
            val short = p.annotations
              .collect { case cli.short(n) => n }
              .lastOption
              .getOrElse("")
            val metavar = p.annotations
              .collect { case cli.metavar(n) => n }
              .lastOption
              .getOrElse("")
            val configuredHelp = p.annotations
              .collect { case cli.help(h) => h }
              .lastOption
              .getOrElse("")
            val defaultText = p.default match {
              case Some(v) => s"default ${v.toString}"
              case _       => ""
            }
            val help = List(configuredHelp, defaultText).filterNot(_.isEmpty).mkString(", ")
            p.typeclass match {
              case ROpts(o) =>
                Folder.prefixNames(o)(name)
              case RFlag() =>
                val cli = Opts.flag(name, help, short).orFalse
                val env = Opts.env(name.toUpperCase.replace('-', '_'), help, metavar)(booleanEnvironmentArgument)
                cli orElse env
              case RArgs(a, optional) =>
                val parser =
                  if (argument)
                    Opts.arguments(metavar)(a)
                  else
                    Opts.options(name, help, short, metavar)(a)
                val cli = if (optional) parser.orEmpty else parser
                val env =
                  Opts.env(name.toUpperCase.replace('-', '_'), help, metavar)(
                    if (!optional)
                      Utils.multiEnv[p.PType](sep)(a.asInstanceOf[Argument[p.PType]])
                    else
                      Utils.multiEnvList[p.PType](sep)(a.asInstanceOf[Argument[p.PType]])
                  )
                val opts = cli.orElse(env)
                p.default match {
                  case Some(v) => opts.orElse(Opts(v))
                  case None    => opts
                }
              case RArg(a) =>
                val opts = {
                  if (argument)
                    Opts.argument(metavar)(a)
                  else
                    Opts.option(name, help, short, metavar)(a)
                }.orElse(
                  Opts.env(name.toUpperCase.replace('-', '_'), help, metavar)(a)
                )
                p.default match {
                  case Some(v) => opts.orElse(Opts(v))
                  case None    => opts
                }
              case RArgOptional(a) =>
                val opts = {
                  if (argument)
                    Opts.argument(metavar)(a)
                  else
                    Opts.option(name, help, short, metavar)(a)
                }.orElse(
                    Opts.env(name.toUpperCase.replace('-', '_'), help, metavar)(a)
                  )
                  .orNone
                p.default match {
                  case Some(v) => opts.orElse(Opts(v))
                  case None    => opts
                }
            }
          }
          .toList
          .sequence
          .map(ctx.rawConstruct))

    implicit def gencli[T <: Product]: CliDeriver[T] = macro Magnolia.gen[T]

  }

  sealed trait SetterCliDeriver[T] {
    def getSetters: Option[Opts[T => T]]
  }

  case class Setters[T](f: Opts[T => T]) extends SetterCliDeriver[T] {
    override def getSetters: Option[Opts[T => T]] = Some(f)
  }

  case class Args[T, O](a: Argument[T], optional: Boolean = false) extends SetterCliDeriver[O] {
    override def getSetters: Option[Opts[O => O]] = None
  }

  case class Arg[T](a: Argument[T], optional: Boolean = false) extends SetterCliDeriver[T] {
    override def getSetters: Option[Opts[T => T]] = None
  }

  case class ArgOptional[T, O](a: Argument[T]) extends SetterCliDeriver[O] {
    override def getSetters: Option[Opts[O => O]] = None
  }

  case class FlagSetter[T]() extends SetterCliDeriver[T] /* actually boolean */ {
    override def getSetters: Option[Opts[T => T]] = None
  }

  object SetterCliDeriver {
    implicit def fromArgMulti[T](implicit a: Argument[T]): SetterCliDeriver[NonEmptyList[T]] = Args(a)
    implicit def fromArgMultiList[T](implicit a: Argument[T]): SetterCliDeriver[List[T]]     = Args(a, true)
    implicit def fromArgSingle[T](implicit a: Argument[T]): SetterCliDeriver[T]              = Arg(a)
    implicit def fromArgOptional[T](implicit a: Argument[T]): SetterCliDeriver[Option[T]]    = ArgOptional(a)
    implicit val flagSetter: SetterCliDeriver[Boolean]                                       = FlagSetter[Boolean]()

    /**
      * Note that this uses `Cli` as well as `SetterCli` because we want to let them set the whole `T`, or parts of the `T`
      */
    implicit def fromOptsOptional[T <: Product](implicit c: Cli[T], sc: SetterCli[T]): SetterCliDeriver[Option[T]] =
      Setters(
        // try and parse a complete T
        c.opts.orNone
          .map(t => { old: Option[T] =>
            t.map(_.some).getOrElse(old)
          })
          // otherwise parse parts of it and override them
          .orElse(sc.opts.orNone.map { of =>
            { ot: Option[T] =>
              ot.map(t =>
                of.getOrElse({ a: T =>
                  a
                })(t))
            }
          }))

    type Typeclass[T] = SetterCliDeriver[T]

    def makeEmptyStringNone[A](a: Argument[A]): Argument[Option[A]] = new Argument[Option[A]] {
      override def read(string: String): ValidatedNel[String, Option[A]] =
        if (string.trim.isEmpty)
          Option.empty[A].validNel[String]
        else a.read(string).map(_.some)
      override def defaultMetavar: String = a.defaultMetavar
    }

    def combine[T](ctx: CaseClass[SetterCliDeriver, T]): SetterCliDeriver[T] = {
      Setters(
        ctx.parameters
          .map { p =>
            val sep = p.annotations
              .collect { case cli.separator(c) => c }
              .lastOption
              .getOrElse(',')
            val kebab = p.annotations
              .collect { case cli.autokebab() => true }
              .lastOption
              .getOrElse(false)
            val argument = p.annotations
              .collect { case cli.argument() => true }
              .lastOption
              .getOrElse(false)
            val name = p.annotations
              .collect { case cli.name(n) => n }
              .lastOption
              .getOrElse(
                if (kebab)
                  Utils.kebab(p.label)
                else p.label
              )
            val short = p.annotations
              .collect { case cli.short(n) => n }
              .lastOption
              .getOrElse("")
            val metavar = p.annotations
              .collect { case cli.metavar(n) => n }
              .lastOption
              .getOrElse("")
            val help = p.annotations
              .collect { case cli.help(h) => h }
              .lastOption
              .getOrElse("")
            p.typeclass match {
              case Setters(o) =>
                Folder
                  .prefixNames(o)(name)
                  .map(f => f.asInstanceOf[Any => Any])
              case FlagSetter() =>
                val cli = Opts.flag(name, help, short).map(_ => true).orNone
                val env = Opts.env(name.toUpperCase.replace('-', '_'), help, metavar)(CliDeriver.booleanEnvironmentArgument).orNone
                cli.orElse(env).map { o =>
                  { p2: p.PType =>
                    o match {
                      case Some(v) => v
                      case _       => p2
                    }
                  }.asInstanceOf[Any => Any]
                }
              case Args(a, optional) =>
                val parser =
                  if (argument)
                    Opts.arguments(metavar)(a)
                  else
                    Opts.options(name, help, short, metavar)(a)
                val cli =
                  if (optional)
                    parser.orEmpty
                  else parser
                val env = Opts.env(name.toUpperCase.replace('-', '_'), help, metavar)(
                  if (!optional)
                    Utils.multiEnv[p.PType](sep)(a.asInstanceOf[Argument[p.PType]])
                  else
                    Utils.multiEnvList[p.PType](sep)(a.asInstanceOf[Argument[p.PType]])
                )
                cli
                  .orElse(env)
                  .orNone
                  .map { o =>
                    { p2: p.PType =>
                      o match {
                        case Some(v) => v
                        case None    => p2
                      }
                    }.asInstanceOf[Any => Any]
                  }

              case Arg(a, _) => {
                if (argument)
                  Opts.argument(metavar)(a)
                else
                  Opts.option(name, help, short, metavar)(a)
              }.orElse(
                  Opts.env(name.toUpperCase.replace('-', '_'), help, metavar)(a)
                )
                .orNone
                .map { o =>
                  { p2: p.PType =>
                    o match {
                      case Some(v) => v
                      case None    => p2
                    }
                  }.asInstanceOf[Any => Any]
                }
              case ArgOptional(a) => {
                Opts.option(name, help, short, metavar)(makeEmptyStringNone(a.asInstanceOf[Argument[Any]]))
              }.orElse(
                  Opts.env(name.toUpperCase.replace('-', '_'), help, metavar)(a)
                )
                .orNone
                .map { o =>
                  { p2: p.PType =>
                    o match {
                      case Some(v) => v
                      case _       => p2
                    }
                  }.asInstanceOf[Any => Any]
                }
            }
          }
          .toList
          .sequence
          .map { params =>
            { t: T =>
              ctx.rawConstruct(
                params
                  .zip(t.asInstanceOf[Product].productIterator.toList)
                  .map {
                    case (f, old) =>
                      f(old)
                  }
              )
            }
          })
    }

    implicit def gen[T]: SetterCliDeriver[T] = macro Magnolia.gen[T]
  }

}
