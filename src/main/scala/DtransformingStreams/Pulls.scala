package DtransformingStreams

import fs2._
import fs2.Stream._
import cats.effect._

object Pulls extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s = Stream(1, 2) ++ Stream(3) ++ Stream(4, 5)

    /**
     * Creates a pull that emits the elements of the given chunk.
     * The new pull performs no effects and terminates successfully with a unit result
     */

    val outputPull: Pull[Pure, Int, Unit] = Pull.output1(1)
    // effect type, output type, result type. Pull is a monad
    IO.println(outputPull.stream.toList)

    val outputChunk: Pull[Pure, Int, Unit] = Pull.output(Chunk(1,2,3))
    IO.println(outputChunk.stream.toList)

    val donePull: Pull[Pure, Nothing, Unit] = Pull.done // usually used when you're finished (ie a close)

    val purePull: Pull[Pure, Nothing, Int] = Pull.pure(5)

    val combined = for {
      _ <- Pull.output1(1)
      _ <- Pull.output(Chunk(1, 2, 3))
    } yield ()
    IO.println(combined.stream.toList) // List(1, 1, 2, 3)
    IO.println(combined.stream.chunks.toList) // List(Chunk(1), Chunk(1, 2, 3))

    /**
     * Turning streams into pulls
     */

    val toPull: ToPull[Pure, Int] = s.pull
    val echoPull: Pull[Pure, Int, Unit] = s.pull.echo // Writes all inputs to the output of the returned `Pull`

    val takePull: Pull[Pure, Int, Option[Stream[Pure, Int]]] = s.pull.take(2) // rhs type is trying to represent the rest of the stream..
    // ie we MAY have more ints to process, but we MAY have no more elements

    val dropPull: Pull[Pure, INothing, Option[Stream[Pure, Int]]] = s.pull.drop(3)

    // ex: using pulls - skip num of elements, take num of elements
    def skipLimit[A](skip: Int, limit: Int)(s: Stream[IO, A]): Stream[IO, A] = {
      val pull = for {
        maybeTail <- s.pull.drop(skip)
        _         <- maybeTail match {
          case Some(remaining) => remaining.pull.take(limit)
          case None => Pull.done
        }
      } yield ()
      pull.stream
    }

    // List(11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    skipLimit(10, 10)(Stream.range(1, 100)).compile.toList.flatMap(IO.println)

    /**
     * uncons:
     * Waits for a chunk of elements to be available in the source stream.
     * The non-empty chunk of elements along with a new stream are provided as the resource of the returned pull.
     * The new stream can be used for subsequent operations, like awaiting again.
     * A None is returned as the resource of the pull upon reaching the end of the stream
     */
    val unconsedRange: Pull[Pure, Nothing, Option[(Chunk[Int], Stream[Pure, Int])]] = s.pull.uncons
    def firstChunk[A](s: Stream[Pure, A]): Stream[Pure, A] = {
      s.pull.uncons.flatMap {
        case Some((chunk, restOfStream)) => Pull.output(chunk)
        case None => Pull.done
      }.stream
    }
    IO.println(firstChunk(s).toList) // List(1, 2)

    def drop[A](n: Int): Pipe[Pure, A, A] = s => {
      def go(s: Stream[Pure, A], n: Int): Pull[Pure, A, Unit] = {
        s.pull.uncons.flatMap {
          case Some((chunk, ros)) => {
            if (chunk.size < n) go(ros, n - chunk.size)
            else Pull.output(chunk.drop(n)) >> ros.pull.echo
          }
          case None => Pull.done
        }
      }
      go(s, n).stream
    }
    // through: Transforms this stream using the given Pipe
    IO.println(s.through(drop(3)).toList)

    // ex: filter
    def filter[A](p: A => Boolean): Pipe[Pure, A, A] = s => {
      def go(s: Stream[Pure, A]): Pull[Pure, A, Unit] = s.pull.uncons.flatMap {
        case Some((chunk, ros)) =>
          Pull.output(chunk.filter(p)) >> go(ros)
        case None => Pull.done
      }

      go(s).stream
    }
    IO.println(s.through(filter(_ % 2 != 0)).toList)
  }
}
