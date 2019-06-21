package net.andimiller.recline

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import com.monovore.decline.Argument
import cats.implicits._

object Utils {

  def kebab(s: String): String = {
    val sb = new StringBuilder()
    s.foreach {
      case c if c.isUpper =>
        sb.append('-')
        sb.append(c.toLower)
      case c =>
        sb.append(c)
    }
    sb.mkString
  }

  def multiEnv[T](sep: Char)(a: Argument[T]): Argument[NonEmptyList[T]] = new Argument[NonEmptyList[T]] {
    override def read(string: String): ValidatedNel[String, NonEmptyList[T]] =
      NonEmptyList.fromList(string.split(sep).toList.filterNot(_.isEmpty)) match {
        case Some(value) => value.traverse(s => a.read(s))
        case None        => Validated.Invalid(NonEmptyList.of("please provide at least one value"))
      }
    override def defaultMetavar: String = a.defaultMetavar + sep + "..."
  }

  def multiEnvList[T](sep: Char)(a: Argument[T]): Argument[List[T]] = new Argument[List[T]] {
    override def read(string: String): ValidatedNel[String, List[T]] =
      string.split(sep).toList.filterNot(_.isEmpty).traverse(s => a.read(s))
    override def defaultMetavar: String = a.defaultMetavar + sep + "..."
  }

}
