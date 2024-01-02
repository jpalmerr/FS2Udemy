package CeffectfulStreams

import fs2._
import cats.effect.{IO, IOApp}
import cats.implicits._

object Create extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s: Stream[IO, Unit] = Stream.eval(IO(println("effectful stream")))
    s // effectful so cant call toList etc => .compile
//    s.compile.toList.flatMap(IO.println)
    s.compile.drain

    // if you only care about printing, lift
    val s2: Stream[IO, Nothing] = Stream.exec(IO.println("2nd effectful stream")) // wont emit any elements
    s2.compile.drain

    val fromPure: Stream[IO, Int] = Stream(1, 2, 3).covary[IO] // lifts to effect type
    fromPure.compile.toList.flatMap(IO.println)

    val natsEval = Stream.iterateEval(1)(a => IO.println(s"Producing ${a + 1}") *> IO(a + 1)) // A => F[A]
    natsEval.take(10).compile.toList.flatMap(IO.println)
    // Producing 2
    //Producing 3
    //...
    //Producing 10
    //List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    val alphabet = Stream.unfoldEval('a') { c =>
      if (c == 'z' + 1) IO.println("Finishing...") *> IO(None)
      else IO.println(s"Producing $c") *> IO(Some(c, (c + 1).toChar))
    }
    alphabet.compile.toList.flatMap(IO.println)

    // exercise
    val data = List.range(1, 10)
    val pageSize = 20

    def fetchPage(pageNumber: Int): IO[List[Int]] = {
      val start = pageNumber * pageSize
      val end = start + pageSize
      IO.println(s"Fetching page $pageNumber").as(data.slice(start, end))
    }

    def fetchAll(): Stream[IO, Int] = {
      Stream.unfoldEval(0) { pageNumber =>
        fetchPage(pageNumber).map { pageElems =>
          if (pageElems.isEmpty) None
          else Some((Stream.emits(pageElems), pageNumber + 1))
        }
      }.flatten
    }

    fetchAll().compile.toList.flatMap(IO.println)
  }
}

