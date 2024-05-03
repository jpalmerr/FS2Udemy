package Econcurrency

import cats.effect._
import fs2._

import scala.concurrent.duration._

object Join extends IOApp.Simple{
  override def run: IO[Unit] = {
    val s1Finite = Stream(1, 2, 3).covary[IO].metered(100.millis)
    val s2Finite = Stream(4, 5, 6).covary[IO].metered(50.millis)

    // same as a merge in terms of behaviour
    val jFinite = Stream(s1Finite, s2Finite).parJoinUnbounded
    jFinite.printlns.compile.drain

    val s3Infinite = Stream.iterate(300000)(_ + 1).covary[IO].metered(50.millis)
    val s4Infinite = Stream.iterate(400000)(_ + 1).covary[IO].metered(50.millis)

    val jAll = Stream(s1Finite, s2Finite, s3Infinite, s4Infinite).parJoinUnbounded
    jAll.printlns.interruptAfter(3.seconds).compile.drain

    val s1Failing = Stream(1, 2, 3).covary[IO].metered(100.millis) ++ Stream.raiseError[IO](new Exception("s1 failed"))
    val jAll2 = Stream(s1Failing, s2Finite, s3Infinite, s4Infinite).parJoinUnbounded
    jAll2.printlns.interruptAfter(3.seconds).compile.drain  // when one fails they all stop

    // bounded
    val jBounded = Stream(s1Finite, s2Finite, s3Infinite).parJoin(2)
    jBounded.printlns.interruptAfter(3.seconds).compile.drain
  }
}
