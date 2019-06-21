package net.andimiller.recline

import cats.data._
import cats.implicits._
import com.monovore.decline.recline.Folder
import com.monovore.decline.{Argument, Opts}
import magnolia._
import net.andimiller.recline.annotations._

import scala.language.experimental.macros

object types {

  sealed trait CliDeriver[T] {
    def getOpts: Option[Opts[T]]
  }

  case class ROpts[T](o: Opts[T]) extends CliDeriver[T] {
    override def getOpts: Option[Opts[T]] = Some(o)
  }

  case class RArgs[T, O](a: Argument[T], optional: Boolean = false) extends CliDeriver[O] {
    override def getOpts: Option[Opts[O]] = None
  }

  case class RArg[T](a: Argument[T]) extends CliDeriver[T] {
    override def getOpts: Option[Opts[T]] = None
  }

  object CliDeriver {
    implicit def fromArgMulti[T](implicit a: Argument[T]): CliDeriver[NonEmptyList[T]] = RArgs(a)
    implicit def fromArgMultiList[T](implicit a: Argument[T]): CliDeriver[List[T]]     = RArgs(a, true)
    implicit def fromArgSingle[T](implicit a: Argument[T]): CliDeriver[T]              = RArg(a)

    type Typeclass[T] = CliDeriver[T]

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
              case ROpts(o) => Folder.prefixNames(o)(name)
              case RArgs(a, optional) =>
                val cli =
                  if (!optional)
                    Opts.options(name, help, short, metavar)(a)
                  else
                    Opts.options(name, help, short, metavar)(a).orEmpty
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
                val opts =
                  Opts
                    .option(name, help, short, metavar)(a)
                    .orElse(
                      Opts.env(name.toUpperCase.replace('-', '_'), help, metavar)(a)
                    )
                p.default match {
                  case Some(v) => opts.orElse(Opts(v))
                  case None    => opts
                }
            }
          }
          .toList
          .sequence
          .map(ctx.rawConstruct))

    implicit def gencli[T]: CliDeriver[T] = macro Magnolia.gen[T]

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

  case class Arg[T](a: Argument[T]) extends SetterCliDeriver[T] {
    override def getSetters: Option[Opts[T => T]] = None
  }

  object SetterCliDeriver {
    implicit def fromArgMulti[T](implicit a: Argument[T]): SetterCliDeriver[NonEmptyList[T]] = Args(a)
    implicit def fromArgMultiList[T](implicit a: Argument[T]): SetterCliDeriver[List[T]]     = Args(a, true)
    implicit def fromArgSingle[T](implicit a: Argument[T]): SetterCliDeriver[T]              = Arg(a)

    type Typeclass[T] = SetterCliDeriver[T]

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
              case Args(a, optional) =>
                val cli =
                  if (!optional)
                    Opts.options(name, help, short, metavar)(a)
                  else
                    Opts.options(name, help, short, metavar)(a).orEmpty
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

              case Arg(a) => {
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
