package Econcurrency

import cats.effect.IOApp
import fs2._
import cats.effect._

import java.time.LocalDateTime
import scala.concurrent.duration._

object Zip extends IOApp.Simple {
  override def run: IO[Unit] = {

    val s1 = Stream(1,2,3).covary[IO].metered(1.second)
    val s2 = Stream(4,5,6).covary[IO].metered(100.millis)
    val s3 = Stream(4,5,6, 7).covary[IO].metered(100.millis)

    s1.zip(s2).printlns.compile.drain
    s1.zip(s3).printlns.compile.drain // only outputs pairs - once a stream is finished it will stop

    val s2Inf = Stream.iterate(0)(_ + 1).covary[IO].metered(100.millis)
    s1.zip(s2Inf).printlns.compile.drain // still ends as finishes as soon as one stream is exhausted

    val s1Failing = s1 ++ Stream.raiseError[IO](new Exception("oh no"))
    s1Failing.zip(s2).printlns.compile.drain

    val s2Failing = s2 ++ Stream.raiseError[IO](new Exception("oh no"))
    s1.zip(s2Failing).printlns.compile.drain // no error: streams are lazy -> we never reach point where error would occur


    s1.zip(s2).compile.toList.flatMap(IO.println)

    // what if i only care about output of the rhs stream
    val s4 = Stream.repeatEval(IO(LocalDateTime.now())).evalTap(IO.println)
    s4.zipRight(s2Inf).interruptAfter(3.seconds).compile.toList.flatMap(IO.println) // still see the side effects from the println but only return List(0,1,2,3,4...)

    // parZip

    val sL = Stream.repeatEval(IO.println("Pulling left").as("L"))
    val sR = Stream.repeatEval(IO.println("Pulling right").as("R"))

    sL.zip(sR).take(15).compile.toList.flatMap(IO.println)
    sL.parZip(sR).take(15).compile.toList.flatMap(IO.println) // same res but runs in parallel

  }
}
