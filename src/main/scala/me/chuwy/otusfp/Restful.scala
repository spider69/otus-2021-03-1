package me.chuwy.otusfp

import cats.data.Kleisli

import cats.effect._

import org.http4s.{Request, Http, HttpApp, Response, HttpRoutes}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router


object Restful {

  import scala.concurrent.ExecutionContext.global

  val req: Kleisli[IO, Request[IO], Response[IO]] = ???
  val next: Kleisli[IO, Response[IO], String] = ???

  val a = req.andThen(next)

  // ZIO[Any, Throwable, A] = cats.effect.IO[A] = zio Task

  val serviceOne: HttpRoutes[IO] =
    HttpRoutes.of {
      case req @ GET -> Root / "hello" / name =>
        hello(name)
    }

  def hello(name: String): IO[Response[IO]] =
    Ok(s"hello, $name")

  def serviceTwo: HttpRoutes[IO] =
    HttpRoutes.of {
      case GET -> Root / "health"  =>

        IO.println("Health endpoint") *> Ok("Doing fine")
    }

  val httpApp = Router("/" -> serviceOne, "/api" -> serviceTwo).orNotFound

  val builder = BlazeServerBuilder[IO](global).bindHttp(port = 8080, host = "localhost").withHttpApp(httpApp)
}
