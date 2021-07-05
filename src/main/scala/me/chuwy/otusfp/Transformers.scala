package me.chuwy.otusfp

import cats.Applicative
import cats.implicits._

import cats.effect.IO
import cats.data.{StateT, OptionT, ReaderT, State, Reader}

object Transformers {
  def getUsername: IO[Option[String]] = IO.pure(Some("Bob"))
  def getId(name: String): IO[Option[Int]] = IO.pure(Some(42))
  def getPermissions(id: Int): IO[Option[String]] = IO.pure(None)

  def program: OptionT[IO, String] =
    for {
      username <-  OptionT(getUsername)
      id <- OptionT(getId(username))
      permissions <- OptionT(getPermissions(id))
    } yield permissions

  case class WorldState(events: List[String]) {
    def addEvent(event: String): WorldState =
      WorldState(event :: events)
  }
  type WorldChange[A] = State[WorldState, A]

  case class EndOfTheWorld(msg: String)

  type Life[A] = Either[EndOfTheWorld, A]

  def born: WorldChange[Unit] = State.modify(_.addEvent("birth"))
  def goSchool: WorldChange[Unit] = State.modify(_.addEvent("applied to school"))
  def learnCode: WorldChange[Unit] = State.modify(_.addEvent("learn Scala"))

  def something: Life[Unit] = Left(EndOfTheWorld("global warming"))

  type LifeWithNuance[A] = StateT[Life, WorldState, A]

  def liftState[F[_]: Applicative, S, A](s: State[S, A]): StateT[F, S, A] =
    StateT.fromState(s.map(a =>  Applicative[F].pure(a)))

  val programS: LifeWithNuance[WorldState] = for {
    _ <- liftState[Life, WorldState, Unit](born)
    _ <- liftState[Life, WorldState, Unit](goSchool)
    _ <- StateT.liftF(something)
    _ <- liftState[Life, WorldState, Unit](learnCode)
    result <- StateT.get[Life, WorldState]
  } yield result


}
