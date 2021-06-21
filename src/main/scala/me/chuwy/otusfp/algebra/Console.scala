package me.chuwy.otusfp.algebra

import scala.io.StdIn

import cats.data.State

import cats.Monad
import cats.implicits._

import cats.effect.IO

trait Console[F[_]] {
  def putStrLn(str: String): F[Unit]
  def readLine: F[String]
}

object Console {

  def apply[F[_]](implicit ev: Console[F]): Console[F] = ev

  implicit val interpreter: Console[IO] =
    new Console[IO] {
      def putStrLn(str: String): IO[Unit] =
        IO.delay(println(str))

      def readLine: IO[String] =
        IO.delay(StdIn.readLine())
    }

  case class RealWorld(events: List[String])

  val initial = RealWorld(List.empty)

  type WorldState[A] = State[RealWorld, A]

  implicit val testInterpreter: Console[WorldState] =
    new Console[WorldState] {
      def putStrLn(str: String): WorldState[Unit] = {
        State.modify(world => world.copy(str :: world.events))
      }

      def readLine: WorldState[String] =
        State { world =>
          if (world.events.filter(_ == "readLine").size == 0) {
            (world.copy("readLine" :: world.events), "Bob")
          } else {
            (world.copy("readLine" :: world.events), "Alice")
          }
        }
    }


  def greetSomeone[F[_]: Console: Monad]: F[Unit] =
    for {
      name <- Console[F].readLine
      _    <- Console[F].putStrLn(s"Hello, ${name}!")
    } yield ()

}
