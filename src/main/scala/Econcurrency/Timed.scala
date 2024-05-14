package Econcurrency

import cats.effect.IOApp
import fs2._
import cats.effect._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._
import scala.util.Random
object Timed extends IOApp.Simple {
  override def run: IO[Unit] = {
    val formatter = DateTimeFormatter.ofPattern("hh:MM:ss")
    def printNow = IO.println(LocalDateTime.now().format(formatter))
    def randomResult = IO(Random.between(1, 100))
    def process(executionTime: FiniteDuration): IO[Int] = {
      IO.sleep(executionTime) *> randomResult.flatTap(_ => printNow)
    }

    val s = Stream.repeatEval(process(1.second))

    s.take(3).compile.toList.flatMap(IO.println)

    val fixedRateStream = Stream.fixedRate[IO](2.seconds)
    fixedRateStream.take(3).printlns.compile.drain
    // why is it useful? emits every x seconds => common pattern is:

    fixedRateStream.zip(s).take(3).compile.toList.flatMap(IO.println) // List(((),86), ((),46), ((),88))
    fixedRateStream.zipRight(s).take(3).compile.toList.flatMap(IO.println) // List(20, 20, 12)

    // metered
    def metered[A](s: Stream[IO, A], d: FiniteDuration): Stream[IO, A] = {
      Stream.fixedRate[IO](d).zipRight(s)
    }
    metered(s, 2.seconds).take(3).compile.toList.flatMap(IO.println)

    // fixed delay
    val fixedDelayStream = Stream.fixedDelay[IO](2.seconds)
    fixedDelayStream.take(3).printlns.compile.drain

    fixedDelayStream.zipRight(s).take(3).compile.toList.flatMap(IO.println) // every 3 seconds (2 + 1)

    def spaced[A](s: Stream[IO, A], duration: FiniteDuration): Stream[IO, A] = {
      Stream.fixedDelay[IO](duration).zipRight(s)
    }
    spaced(s, 2.seconds).take(3).compile.toList.flatMap(IO.println)

    // awake every: emit something rather than unit

    val awakeEveryStream = Stream.awakeEvery[IO](2.seconds)
    awakeEveryStream.take(3).compile.toList.flatMap(IO.println)
    awakeEveryStream.zipRight(s).take(3).compile.toList.flatMap(IO.println) // 2 seconds between each

    // awake delay

    val awakeDelayStream = Stream.awakeDelay[IO](2.seconds)
    awakeDelayStream.take(3).compile.toList.flatMap(IO.println)
    awakeDelayStream.zipRight(s).take(3).compile.toList.flatMap(IO.println)
  }
}
