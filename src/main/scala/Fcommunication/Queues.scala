package Fcommunication

import cats.effect._
import fs2.concurrent._
import fs2._
import cats.effect.std._

import scala.concurrent.duration._

object Queues extends IOApp.Simple {
  override def run: IO[Unit] = {
    Stream.eval(Queue.unbounded[IO, Int]).flatMap { queue =>
      Stream.eval(Ref.of[IO, Int](0)).flatMap { ref =>
        val producer =
          Stream
            .iterate(0)(_ + 1)
            .covary[IO]
            .evalMap(e => IO.println(s"Offering element $e to the queue") *> queue.offer(e))
            .drain
        val consumer =
          Stream
            .fromQueueUnterminated(queue)
            .evalMap(e => ref.update(_ + e))
            .drain
        producer.merge(consumer).interruptAfter(3.seconds) ++ Stream.eval(ref.get.flatMap(IO.println))
      }
    }.compile.drain


    // now what happens if producer is faster than the consumer? (queue builds up))
    Stream.eval(Queue.unbounded[IO, Int]).flatMap { queue =>
      Stream.eval(Ref.of[IO, Int](0)).flatMap { ref =>
        val producer =
          Stream
            .iterate(0)(_ + 1)
            .covary[IO]
            .evalMap(e => IO.println(s"Offering element $e to the queue") *> queue.offer(e))
            .drain
        val consumer =
          Stream
            .fromQueueUnterminated(queue)
            .evalMap(e => ref.update(_ + e))
            .metered(300.millis)
            .drain
        producer.merge(consumer).interruptAfter(3.seconds) ++ Stream.eval(ref.get.flatMap(IO.println))
      }
    }.compile.drain
    // note that whilst offerings kept happening, the ref stayed low ie the queue was acting as a buffer

    // signalling termination with a none
    Stream.eval(Queue.unbounded[IO, Option[Int]]).flatMap { queue =>
      val p = (Stream.range(0, 10).map(Some.apply) ++ Stream(None)).evalMap(queue.offer)
      val c = Stream.fromQueueNoneTerminated(queue).evalMap(i => IO.println(i))
      c.merge(p)
    }.interruptAfter(5.seconds).compile.drain
    // once it found a None in the queue it stopped
  }
}
