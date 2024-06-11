package scrapbook.queuingApi

import fs2._
import cats.effect._
import cats.implicits._
import cats.effect.std.Queue


import scala.concurrent.duration.DurationInt

/**
 * Personal example due to considering a use case at work.
 *
 * Goal is to
 * a) have a stream simulating infinite calls to a fake http client - once a second
 *    put each error status (randomly generated) onto a queue
 *
 * b) process this queue by simulating a 3 second time process on error.
 *    However, if an error is already on the queue and this process is ongoing, ignore.
 *    Then flush the queue so this process can repeat
 */

object QueueingMechanism extends IOApp.Simple {

  override def run: IO[Unit] = {

    def httpTrafficSimulator(queueRef: Ref[IO, Queue[IO, String]]): Stream[IO, Nothing] =
      Stream
        .awakeEvery[IO](1.second) // Emit a unit value every second
        .evalMap(_ => HttpClientSimulator.simulateACall()) // Call the method every second
        .flatMap { // work with response
          case Left(err) =>
            Stream.eval(queueRef.get.flatMap(_.size).flatMap(size => IO(println(s"Traffic Queue size: $size")))) >>
              Stream.eval(queueRef.get.flatMap(_.offer(err))).drain
          case Right(success) => Stream.eval(IO(println(success))).drain
        }

    def processQueue(queueRef: Ref[IO, Queue[IO, String]]): Stream[IO, Unit] =
      Stream.repeatEval {
        for {
          queue <- queueRef.get
          size <- queue.size
          _ <- if (size == 0) {
            IO.println(s"Queue is size $size -> do nothing and continue")
          } else if (size == 1) {
            // Queue has one item, simulate a 3-second process
            IO.println("Queue size is 1. Starting process...") >>
              IO.sleep(3.seconds) >>
              IO.println("Process completed. Flushing queue.") >>
              Queue.unbounded[IO, String].flatMap(newQueue => queueRef.set(newQueue))
          } else {
            IO.println(s"Something in the queue already: size $size - do nothing")
          }
        } yield ()
      }.metered(500.millis)


    val myFullStream: Stream[IO, Unit] = Stream.eval {
      for {
        starterQueue <- Queue.unbounded[IO, String]
        ref <- Ref.of[IO, Queue[IO, String]](starterQueue)
      } yield ref
    }.flatMap { queueRef =>
      val trafficStream = httpTrafficSimulator(queueRef)
      val processStream = processQueue(queueRef)
      Stream(trafficStream, processStream).parJoinUnbounded
    }
    myFullStream.interruptAfter(20.seconds).compile.drain
  }
}

