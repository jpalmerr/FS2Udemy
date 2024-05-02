package DtransformingStreams

import fs2._
import fs2.Stream._
import cats.effect._

object Sum extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s = Stream(1, 2) ++ Stream(3) ++ Stream(4, 5)

    // scanningChunks: f applied to each chunk. Result can be used in next invocation of f


    // another way to create a pipe
    def runningSum: Pipe[Pure, Int, Int] = s => {
      s.scanChunksOpt(0) { acc =>
        Some{ chunk =>
          val newSum = chunk.foldLeft(0)(_ + _) + acc
          (newSum, Chunk.singleton(newSum))
        }
      }
    }
    IO.println(s.through(runningSum).toList) // List(3, 6, 15)

    // runningMax: find max of each chunk
    def runningMax: Pipe[Pure, Int, Int] = s => {
      s.scanChunksOpt(Int.MinValue){ acc =>
        Some { chunk =>
          val newState = chunk.foldLeft(Int.MinValue)(_ max _) max acc
          (newState, Chunk.singleton(newState))
        }
      }
    }
    IO.println(s.through(runningMax).toList) // List(2, 3, 5)
    val s2 = Stream(-1, 3, -2) ++ Stream(10, 4) ++ Stream(8)
    IO.println(s2.through(runningMax).toList) // List(3, 10, 10)
  }
}
