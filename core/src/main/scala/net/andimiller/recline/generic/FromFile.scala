package net.andimiller.recline.generic

import java.nio.file.Paths

import cats.data.ValidatedNel
import com.monovore.decline.Argument
import io.circe.Decoder
import cats.implicits._

import scala.util.Try

case class FromFile[T](t: T)
object FromFile {
  implicit def jsonFromFile[T: Decoder]: Argument[FromFile[T]] = new Argument[FromFile[T]] {
    override def read(string: String): ValidatedNel[String, FromFile[T]] = {
      for {
        file   <- Try { Paths.get(string) }.toEither.leftMap(_.getMessage)
        format = string.split('.').toList.lastOption.getOrElse("json")
        contents <- Try { scala.io.Source.fromFile(file.toFile).mkString }.toEither
                     .leftMap(_.getMessage)
        json <- {
          format match {
            case "yml" | "yaml" => io.circe.yaml.parser.parse(contents)
            case _              => io.circe.parser.parse(contents)
          }
        }.leftMap(_.getMessage())
        t <- json.as[T].leftMap(_.getMessage())
      } yield FromFile(t)
    }.toValidatedNel
    override def defaultMetavar: String = "config.[yml|json]"
  }
}
