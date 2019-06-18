package net.andimiller.recline

import com.monovore.decline.Opts
import net.andimiller.recline.types.{CliDeriver, SetterCliDeriver}

package object generic {
  implicit def deriveCli[T: CliDeriver]: Opts[T] =
    implicitly[CliDeriver[T]].getOpts.get
  implicit def deriveSetterCli[T: SetterCliDeriver]: Opts[T => T] =
    implicitly[SetterCliDeriver[T]].getSetters.get
}
