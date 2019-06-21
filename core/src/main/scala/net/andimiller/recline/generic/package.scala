package net.andimiller.recline

import com.monovore.decline.{Command, Opts}
import io.circe._
import net.andimiller.recline.types.{Cli, SetterCli}
import cats.implicits._

package object generic {
  implicit def deriveCli[T](implicit cli: Cli[T]): Opts[T]                        = cli.opts
  implicit def deriveSetterCli[T](implicit setterCli: SetterCli[T]): Opts[T => T] = setterCli.opts

  implicit def deriveInternalCli[T](implicit cli: Cli[T]): Cli[T]                         = cli
  implicit def deriveInternalSetterCli[T](implicit setterCli: SetterCli[T]): SetterCli[T] = setterCli

  def deriveMain[T](name: String, header: String, helpFlag: Boolean = true)(implicit dec: Decoder[T],
                                                                            cli: Cli[T],
                                                                            setterCli: SetterCli[T]): Command[T] =
    Command(name, header, helpFlag)(
      (Opts.argument[FromFile[T]]("config"), setterCli.opts)
        .mapN {
          case (FromFile(t), setter) => setter(t)
        }
        .orElse(cli.opts)
    )

}
