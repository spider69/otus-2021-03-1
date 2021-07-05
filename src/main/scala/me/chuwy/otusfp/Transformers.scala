package me.chuwy.otusfp

import cats.effect.IO

object Transformers {
  def getUsername: IO[Option[String]] = IO.pure(Some("Bob"))
  def getId(name: String): IO[Option[Int]] = IO.pure(Some(42))
  def getPermissions(id: Int): IO[Option[String]] = IO.pure(None)

  def program: IO[Option[String]] = ???

}
