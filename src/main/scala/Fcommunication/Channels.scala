package Fcommunication


/**
 * Signals are fine where one producer, one subscriber. Channels required if want multiple producers
 */

import cats.effect.{IO, IOApp}
import fs2._
import fs2.concurrent.Channel

import scala.concurrent.duration.DurationInt
object Channels extends IOApp.Simple {
  override def run: IO[Unit] = {
    Stream.eval(Channel.unbounded[IO, Int]).flatMap { channel =>
      val p = Stream.iterate(1)(_ + 1).covary[IO].evalMap(channel.send).drain
      val c = channel.stream.evalMap(i => IO.println(s"Read value $i")).drain

      p.concurrently(c).interruptAfter(3.seconds)
    }.compile.drain
  }
}
