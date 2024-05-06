package Econcurrency

import cats.effect.{IO, IOApp}
import fs2._

import java.lang
import scala.concurrent.duration._

object Concurrently extends IOApp.Simple{
  override def run: IO[Unit] = {
    val s1 = Stream(1, 2, 3).covary[IO].printlns
    val s2 = Stream(4, 5, 6).covary[IO].printlns
    s1.concurrently(s2).compile.drain

    val s1Inf = Stream.iterate(100)(_ + 1).covary[IO].printlns
    s1Inf.concurrently(s2).interruptAfter(2.seconds).compile.drain

    // looks like a merge so far ^

    val s2Inf = Stream.iterate(2000)(_ + 1).covary[IO].printlns
    s1.concurrently(s2Inf).compile.drain

    // concurrently is left bias

    val sFailing = Stream.repeatEval(IO(42)).take(500).printlns ++ Stream.raiseError[IO](new Exception("s1 failed"))
    s1Inf.concurrently(sFailing).compile.drain // rhs failing

    sFailing.concurrently(s1Inf).compile.drain // lhs failing
    // will stop if either fails

    val s3 = Stream.iterate(3000)(_ + 1).covary[IO]
    val s4 = Stream.iterate(4000)(_ + 1).covary[IO]
    s3.concurrently(s4).take(100).compile.toList.flatMap(IO.println) // List(3000,...,3099) only seeing elements from the first stream
    // so on output we have LHS bias
    Stream(s3, s4).parJoinUnbounded.take(100).compile.toList.flatMap(IO.println)
  }
}

