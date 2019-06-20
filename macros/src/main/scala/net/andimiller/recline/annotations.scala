package net.andimiller.recline

import scala.annotation.StaticAnnotation

object annotations {
  object cli {
    case class name(name: String)       extends StaticAnnotation
    case class help(help: String)       extends StaticAnnotation
    case class metavar(metavar: String) extends StaticAnnotation
    case class short(name: String)      extends StaticAnnotation
    case class autokebab()              extends StaticAnnotation
    case class separator(sep: Char)     extends StaticAnnotation
  }
}
