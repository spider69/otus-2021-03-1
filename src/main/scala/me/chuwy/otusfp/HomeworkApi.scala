package me.chuwy.otusfp

import cats.effect._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import scala.util.Random

object HomeworkApi extends IOApp {

  case class Counter(counter: Int)
  implicit val counterEncoder: Encoder[Counter] = deriveEncoder[Counter]
  implicit def userEntityEncoder[F[_]: Concurrent]: EntityEncoder[F, Counter] = jsonEncoderOf[F, Counter]
  implicit def longEntityEncoder[F[_]: Concurrent]: EntityEncoder[F, Long] = jsonEncoderOf[F, Long]
  implicit def stringEntityEncoder[F[_]: Concurrent]: EntityEncoder[F, String] = jsonEncoderOf[F, String]

  def kestrel[A](x: A)(f: A => Unit): A = { f(x); x }

  def createStream(chunkSize: Int, qty: Int, time: Int) =
    fs2.Stream
      .awakeEvery[IO](Duration(time, TimeUnit.SECONDS))
      .map(_ => kestrel(Array.fill[Byte](chunkSize)(0))(Random.nextBytes))
      .take(qty)

  def homeworkApi(counter: Ref[IO, Int]): HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "counter" =>
      counter.modify(c => (c + 1, c + 1)).flatMap { counter =>
        Ok(Counter(counter))
      }

    case GET -> Root / "slow" / chunk / total / time =>
      val response = for {
        chunkSize <- IO.fromOption(chunk.toIntOption.filter(_ > 0))(new Exception("Chunk is not valid"))
        totalSize <- IO.fromOption(total.toIntOption.filter(_ > 0))(new Exception("Total is not valid"))
        delay <- IO.fromOption(time.toIntOption.filter(_ > 0))(new Exception("Time is not valid"))
        result <-
          Ok(
            createStream(chunkSize, totalSize / chunkSize, delay)
              .append(createStream(totalSize % chunkSize, 1, delay))
          )
      } yield result

      response.attempt.flatMap {
        case Left(error) => BadRequest(error.getMessage)
        case Right(value) => IO.pure(value)
      }
  }

  def httpApp(counter: Ref[IO, Int]): HttpApp[IO] = Router(
    "/" -> homeworkApi(counter)
  ).orNotFound

  def buildServer(counter: Ref[IO, Int]) =
    BlazeServerBuilder[IO](global)
      .bindHttp(port = 8080, host = "localhost")
      .withHttpApp(httpApp(counter))

  def run(args: List[String]): IO[ExitCode] =
    Ref[IO].of(0).flatMap { counter =>
      buildServer(counter)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
}
