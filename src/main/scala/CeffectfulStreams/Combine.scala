package CeffectfulStreams

import cats.effect.{IO, IOApp}
import fs2._

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Combine extends IOApp.Simple {
  override def run: IO[Unit] = {

    val s = Stream.repeatEval(IO.println("Emitting...") *> IO(42))
    s.take(10).compile.toList.flatMap(IO.println) // List(42, 42, 42, 42, 42, 42, 42, 42, 42, 42)

    val s2 =
      for {
        x <- Stream.eval(IO.println("Producing 42") *> IO(42))
        y <- Stream.eval(IO.println("Producing 43") *> IO(x + 1))
      } yield y

    s2.compile.toList.flatMap(IO.println) // List(43)

    val s3 = Stream(1, 2, 3).evalMap(i => IO.println(s"Element: $i").as(i))
    s3.compile.drain
//    s3.compile.toList.flatMap(IO.println)

    // evalTap: want to have an effect before producing the value
    val s4: Stream[IO, Int] = Stream(1, 2, 3).evalTap(IO.println)
    s4.compile.toList.flatMap(IO.println)

    val filterEven =
      Stream
        .range(1, 1000)
        .evalFilter(i => IO(i % 2 == 0))
    filterEven.compile.toList.flatMap(IO.println)

    // concatenation

    //exec over eval here as eval returns unit
    val s5 = Stream.exec(IO.println("Start")) ++ Stream(1, 2, 3) ++ Stream(4, 5, 6) ++ Stream.exec(IO.println("Finish"))
    s5.compile.toList.flatMap(IO.println)

    // blocks 'semantically'
    val delayed = Stream.sleep_[IO](1.second) ++ Stream.eval(IO.println("I am awake!"))
    delayed.compile.drain

    // exercise

    // sleep for d, then run affect, then repeat
    def evalEvery[A](d: FiniteDuration)(fa: IO[A]): Stream[IO, A] = {
      (Stream.sleep_[IO](d) ++ Stream.eval(fa)).repeat
    }
    evalEvery(2.second)(IO.println("Hi").as(42)).take(5).compile.toList.flatMap(IO.println)
  }
}
