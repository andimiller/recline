package com.monovore.decline
package recline

object Folder {
  def prefixNames(prefix: String)(names: List[Opts.Name]): List[Opts.Name] =
    names.map {
      case Opts.ShortName(s) => Opts.ShortName(s)
      case Opts.LongName(n)  => Opts.LongName(prefix + "-" + n)
    }

  def prefixNames[T](o: Opts[T])(prefix: String): Opts[T] = {
    def recurse[T2](o: Opts[T2]) = prefixNames(o)(prefix)
    o match {
      case p @ Opts.Pure(_)  => p
      case m @ Opts.Missing  => m
      case Opts.App(f, a)    => Opts.App(recurse(f), recurse(a))
      case Opts.OrElse(a, b) => Opts.OrElse(recurse(a), recurse(b))
      case Opts.Single(a)    => Opts.Single(prefixNamesOpt(a)(prefix))
      case Opts.Repeated(a) =>
        Opts.Repeated(prefixNamesOpt(a)(prefix)).asInstanceOf[Opts[T]]
      case sc @ Opts.Subcommand(_) => sc
      case Opts.Validate(value, validate) =>
        Opts.Validate(recurse(value), validate)
      case h @ Opts.HelpFlag(_) => h
      case Opts.Env(name, help, meta) =>
        Opts.Env(prefix.toUpperCase.replace('-', '_') + "_" + name, help, meta).asInstanceOf[Opts[T]]
    }
  }

  def prefixNamesOpt[T](o: Opt[T])(prefix: String): Opt[T] = {
    o match {
      case Opt.Regular(names, meta, help, vis) =>
        Opt.Regular(prefixNames(prefix)(names), meta, help, vis)
      case Opt.Argument(meta) => Opt.Argument(meta)
      case Opt.Flag(names, help, vis) =>
        Opt.Flag(prefixNames(prefix)(names), help, vis)
    }
  }.asInstanceOf[Opt[T]] // this is actually safe, trust me
}
