package CeffectfulStreams

import cats.effect._
import fs2._

object ErrorHandling extends IOApp.Simple {
  val s = Stream.eval(IO.raiseError(new Exception("boom")))
  val s2 = Stream.raiseError[IO](new Exception("boom 2"))
  val s3 = Stream.repeatEval(IO.println("emitting").as(42)).take(3) ++ Stream.raiseError[IO](new Exception("error after"))
  val s4 = Stream.raiseError[IO](new Exception("error before")) ++ Stream.eval(IO.println("the end!"))

  override def run: IO[Unit] = ???
}
