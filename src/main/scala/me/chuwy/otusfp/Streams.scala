package me.chuwy.otusfp

import java.io.FileReader

import scala.concurrent.duration._

import fs2.Stream
import fs2.text.{lines, utf8Decode}

import cats.effect.{IO, MonadCancel}
import cats.effect.kernel.Resource
import cats.effect.std.Queue

object Streams {
  // As in cats.Id
  type Id[A] = A

  val example = Stream(1,2,3)
  val scanExample = example
    .scan(0) { _ + _}
    .map(i => i + 1)
    .evalMap(IO.println)
    .compile
    .drain

  val foldable = Stream.foldable(List(1,2,3,4))

  // ++

  val unfoldExample = Stream.unfold(0) { s =>
    val next = s + 2
    val output = s.toString
    if (s < 10) Some((output, next + 1)) else None
  }

  val repeatUnfold = unfoldExample.repeat.take(10)

  val evalExample = (Stream.eval(IO.print("hello")) ++ Stream.eval(IO.println(" world"))).repeatN(100)

  def getFile[F[_]]: Resource[F, FileReader] = ???
  def readBytes[F[_]](fileReader: FileReader): Stream[F, Byte] = ???

  type MonadCT[F[_]] = MonadCancel[F, Throwable]
  def flatMappedResource[F[_]: MonadCT] = Stream.resource(getFile[F]).flatMap(readBytes)


  val withSleep = Stream.fixedDelay[IO](1.second).evalMap(_ => IO.println("Hello")).take(4)

  // IO: *>
  val withEvalTap = Stream(1,2,3).evalTap(_ => IO.println("Hello")).take(4)

  def q: Queue[IO, Int] = ???
  def fromQueue = Stream.fromQueueUnterminated(q)

  def pipeExample[F[_]: MonadCT]: Stream[F, String] =
    flatMappedResource[F]
      .through(utf8Decode[F])
      .through(lines)
}
