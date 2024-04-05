package DtransformingStreams

import fs2.{Chunk, Stream}
import cats.effect.{IO, IOApp}
import fs2.io.file._

import scala.reflect.ClassTag

object Chunks extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1 = Stream(1, 2, 3)
    IO.println(s1.chunks.toList) // List(Chunk(1,2,3))

    val s2 = Stream(Stream(1, 2, 3), Stream(4, 5 ,6)).flatten
    IO.println(s2.chunks.toList) // List(Chunk(1,2,3), Chunk(4,5,6))

    val s3 = Stream(1) ++ Stream(2)
    IO.println(s3.chunks.toList)

    val s4 = Stream.repeatEval(IO(42)).take(5)
    s4.chunks.compile.toList.flatMap(IO.println)

    val s5 = Files[IO].readAll(Path("sets.csv"))
    s5.chunks.map(_.size).compile.toList.flatMap(IO.println) // List(65536, 65536, 65536, 65536, 65536, 65536, 65536, 48762)

    val c: Chunk[Int] = Chunk(1, 2, 3)
    IO.println(c)

    val c1: Chunk[Int] = Chunk.array(Array(4, 5, 6))
    IO.println(c1)

    val c2: Chunk[Int] = Chunk.singleton(7)
    IO.println(c2)

    val c5 = c ++ c1 ++ c2

    // fast concat
    // fast indexing
    // avoid copying
    // List-like interface

    val a = new Array[Int](3)
    IO{ println(a.toList); c.copyToArray(a); println(a.toList)}
    IO{println(c5.compact)} // Chunk(1, 2, 3, 4, 5, 6, 7)

    // exercise
    def compact[A: ClassTag](chunk: Chunk[A]): Chunk[A] = {
      val arr = new Array[A](chunk.size)
      chunk.copyToArray(arr) // Copies the elements of this chunk in to the specified array at the specified start index
      Chunk.array(arr)
    }
    IO.println(compact(c5))
  }
}
