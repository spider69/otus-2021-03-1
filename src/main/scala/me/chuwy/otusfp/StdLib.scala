package me.chuwy.otusfp

import scala.concurrent.duration._

import cats.implicits._

import cats.effect.kernel.{Resource, Concurrent}
import cats.effect.{Ref, IOApp, IO, Deferred}
import cats.effect.std.Console


object StdLib {
  case class Environment[F[_]](ref: Ref[F, Int], promise: Deferred[F, Unit])

  object Environment {
    def build[F[_]: Concurrent: Console]: Resource[F, Environment[F]] = {
      val deferred = Resource.make(Deferred.apply[F, Unit]) { d =>
        d.tryGet.flatMap {
          case Some(_) => Console[F].println("Releasing Deferred after it has was completed")
          case None =>  Console[F].println("Releasing fresh Deferred ")
        }
      }

      for {
        ref <- Resource.make(Ref.of[F, Int](0)) { _ => Console[F].println("Releasing Ref") }
        promise <- deferred
      } yield Environment(ref, promise)
    }
  }

  def process(env: Environment[IO])(cmd: Command): IO[Boolean] =
    cmd match {
      case Command.Echo =>
        IO.readLine.flatMap(IO.println).as(true)
      case Command.Exit =>
        IO.pure(false)
      case Command.AddNumber(num) =>
        env.ref.update(start => start + num).as(true)
      case Command.ReadNumber =>
        env.ref.get.flatMap(IO.println).as(true)
      case Command.SetDeferred =>
        env.promise.complete(()).as(true)
      case Command.RunFiber(_) =>
        val print = env.ref.updateAndGet(_ + 1).flatMap(IO.println)
        val sleep = env.promise.get.as(true)
        val action = (sleep *> print).start.replicateA(10).void
        action *> IO.pure(true)

    }

  def program(env: Environment[IO]): IO[Unit] =
    IO.readLine.map(Command.parse).flatMap {
      case Right(cmd) => process(env)(cmd).flatMap {
        case true => program(env)
        case false => IO.println("Bye bye")
      }
      case Left(error) => IO.println(error)
    }
}
