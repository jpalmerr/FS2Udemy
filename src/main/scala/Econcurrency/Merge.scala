package Econcurrency

import cats.effect.IOApp
import fs2._
import cats.effect._

import scala.concurrent.duration._
import scala.util.Random

object Merge extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1Inf = Stream.iterate("0")(_ + "1").covary[IO].metered(100.millis)
    val s2Inf = Stream.iterate("z")(_ + "z").covary[IO].metered(200.millis)
    val s3Inf = s1Inf.merge(s2Inf)
    s3Inf.interruptAfter(5.seconds).printlns.compile.drain // run concurrently

    // what if one stream fails?
    val s1Failing = Stream("a", "b", "c").covary[IO].metered(100.millis) ++ Stream.raiseError[IO](new Exception("s1 failed"))
    val s3LeftFailing = s1Inf.merge(s1Failing)
    s3LeftFailing.interruptAfter(5.seconds).printlns.compile.drain

    val s2Failing = Stream("a", "b", "c").covary[IO].metered(100.millis) ++ Stream.raiseError[IO](new Exception("s2 failed"))
    val s3RightFailing = s1Inf.merge(s2Failing)
    s3RightFailing.interruptAfter(5.seconds).printlns.compile.drain

    val s1Finite = Stream(1, 2, 3).covary[IO].metered(100.millis)
    val s2Finite = Stream(4, 5, 6).covary[IO].metered(200.millis)
    val s3Finite = s1Finite.merge(s2Finite)

    s3Finite.interruptAfter(5.seconds).printlns.compile.drain

    val s3Mixed = s1Finite.merge(s2Finite)
    s3Mixed.interruptAfter(5.seconds).printlns.compile.drain // will run infinite

    // what if i want them both to stop if the left one stops
    val s3MixedHaltL = s1Finite.mergeHaltL(s2Finite)
    s3MixedHaltL.interruptAfter(5.seconds).printlns.compile.drain

    val s3MixedHaltR = s1Finite.mergeHaltR(s2Finite)
    s3MixedHaltR.interruptAfter(5.seconds).printlns.compile.drain

    val s3MixedHaltBoth = s1Finite.mergeHaltBoth(s2Finite)
    s3MixedHaltBoth.interruptAfter(5.seconds).printlns.compile.drain

    // exercise: fetching quotes from two sources

    def fetchRandomQuoteFromSource1: IO[String] = IO(Random.nextString(5))
    def fetchRandomQuoteFromSource2: IO[String] = IO(Random.nextString(25))

    // fetch 100 quotes from source 1 and 150 from source 2, runs for 5 seconds, prints quotes to console

    val s1 = Stream.repeatEval(fetchRandomQuoteFromSource1).take(100)
    val s2 = Stream.repeatEval(fetchRandomQuoteFromSource2).take(150)

    s1.merge(s2).interruptAfter(5.seconds).printlns.compile.drain

  }
}
