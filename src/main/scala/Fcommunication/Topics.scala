package Fcommunication

import cats.effect.{IO, IOApp}
import fs2._
import fs2.concurrent.{Channel, Topic}

import scala.concurrent.duration.DurationInt
object Topics extends IOApp.Simple {
  override def run: IO[Unit] = {
    Stream.eval(Topic.apply[IO, Int]).flatMap { topic =>
      val p = Stream.iterate(1)(_ + 1).covary[IO].through(topic.publish).drain
      val c1 = topic.subscribe(10).evalMap { i => IO.println(s"read $i from c1") }.drain
      val c2 = topic.subscribe(10).evalMap { i => IO.println(s"read $i from c2") }.drain
      Stream(p, c1, c2).parJoinUnbounded
    }.interruptAfter(3.seconds).compile.drain
    // each subscriber will listen to every single element


    // what happens when buffer full? other consumer will block
    Stream.eval(Topic.apply[IO, Int]).flatMap { topic =>
      val p = Stream.iterate(1)(_ + 1).covary[IO].through(topic.publish).drain
      val c1 = topic.subscribe(10).evalMap { i => IO.println(s"read $i from c1") }.drain
      val c2 = topic.subscribe(10).evalMap { i => IO.println(s"read $i from c2") }.metered(200.millis).drain // will buffer as slower
      Stream(p, c1, c2).parJoinUnbounded
    }.interruptAfter(3.seconds).compile.drain
  }
}
