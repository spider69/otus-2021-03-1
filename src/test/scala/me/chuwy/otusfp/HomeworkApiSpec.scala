package me.chuwy.otusfp

import cats.effect._
import cats.effect.testing.specs2.CatsEffect
import cats.effect.unsafe.implicits._
import io.circe.{Decoder, Encoder, Json}
import org.http4s._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.implicits._
import org.specs2.mutable.Specification

class HomeworkApiSpec extends Specification with CatsEffect {

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[IO, A] = jsonOf[IO, A]
  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[IO, A] = jsonEncoderOf[IO, A]

  "service" should {
    "counter route" should {
      "increment counter on each request" in {
        val response = for {
          counter <- Ref[IO].of(0)
          service = HomeworkApi.homeworkApi(counter).orNotFound
          _ <- service.run(Request(method = Method.GET, uri = uri"/counter" ))
          r <- service.run(Request(method = Method.GET, uri = uri"/counter" ))
        } yield r

        val expectedJson = Json.obj(
          ("counter", Json.fromInt(2))
        )

        val actualResp         = response.unsafeRunSync()
        val statusCheck        = actualResp.status == Status.Ok
        val bodyCheck          = actualResp.as[Json].unsafeRunSync() == expectedJson
        statusCheck && bodyCheck
      }
    }

    "slow route" should {
      "return body with proper size" in {
        val response = for {
          counter <- Ref[IO].of(0)
          service = HomeworkApi.homeworkApi(counter).orNotFound
          r <- service.run(Request(method = Method.GET, uri = uri"/slow/2/5/1" ))
        } yield r

        val actualResp         = response.unsafeRunSync()
        val statusCheck        = actualResp.status == Status.Ok
        val bodyCheck          = actualResp.body.compile.toVector.unsafeRunSync().length == 5
        statusCheck && bodyCheck
      }

      "return error with wrong chunk param" in {
        val response = for {
          counter <- Ref[IO].of(0)
          service = HomeworkApi.homeworkApi(counter).orNotFound
          r <- service.run(Request(method = Method.GET, uri = uri"/slow/-1/5/1" ))
        } yield r

        val actualResp         = response.unsafeRunSync()
        val statusCheck        = actualResp.status == Status.BadRequest
        val bodyCheck          = actualResp.as[String].unsafeRunSync() == "Chunk is not valid"
        statusCheck && bodyCheck
      }

      "return error with wrong total param" in {
        val response = for {
          counter <- Ref[IO].of(0)
          service = HomeworkApi.homeworkApi(counter).orNotFound
          r <- service.run(Request(method = Method.GET, uri = uri"/slow/1/-5/1" ))
        } yield r

        val actualResp         = response.unsafeRunSync()
        val statusCheck        = actualResp.status == Status.BadRequest
        val bodyCheck          = actualResp.as[String].unsafeRunSync() == "Total is not valid"
        statusCheck && bodyCheck
      }

      "return error with wrong time param" in {
        val response = for {
          counter <- Ref[IO].of(0)
          service = HomeworkApi.homeworkApi(counter).orNotFound
          r <- service.run(Request(method = Method.GET, uri = uri"/slow/1/5/abc" ))
        } yield r

        val actualResp         = response.unsafeRunSync()
        val statusCheck        = actualResp.status == Status.BadRequest
        val bodyCheck          = actualResp.as[String].unsafeRunSync() == "Time is not valid"
        statusCheck && bodyCheck
      }
    }
  }
}
