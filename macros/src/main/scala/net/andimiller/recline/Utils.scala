package net.andimiller.recline

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
}
