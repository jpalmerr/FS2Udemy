package GtinyServer

import cats.effect.std.Queue
import cats.effect.{IO, IOApp}
import fs2.Stream

import scala.util.Random
import java.time.LocalDateTime
import scala.concurrent.duration.DurationInt

object Server extends IOApp.Simple {
  override def run: IO[Unit] = {

    trait Controller {
      def postAccount(customerId: Long, accountType: String, creationDate: LocalDateTime): IO[Unit]
    }

    class Server(controller: Controller) {
      def start(): IO[Nothing] = { // since will run forever
        val prog = for {
          randomWait <- IO(math.abs(Random.nextInt() % 500))
          _          <- IO.sleep(randomWait.millis)
          _          <- controller.postAccount(
            customerId = Random.between(1L, 200L),
            accountType = if (Random.nextBoolean()) "ira" else "brokerage",
            creationDate = LocalDateTime.now()
          )
        } yield ()
        prog.foreverM
      }
    }

    object PrintController extends Controller {
      override def postAccount(customerId: Long, accountType: String, creationDate: LocalDateTime): IO[Unit] = {
        IO.println(s"Initiaiting account creation. Customer: $customerId Account type: $accountType Created: $creationDate")
      }
    }
    case class CreateAccountData(customerId: Long, accountType: String, creationDate: LocalDateTime)
    class QueueController(queue: Queue[IO, CreateAccountData]) extends Controller {
      override def postAccount(customerId: Long, accountType: String, creationDate: LocalDateTime): IO[Unit] =
        queue.offer(CreateAccountData(customerId, accountType, creationDate))
    }
    // create a stream that emits the queue
    // create a stream for the server (started)
    // create a consumer which reads from the queue and prints the message
    // run everything concurrently

    Stream.eval(Queue.unbounded[IO, CreateAccountData]).flatMap { queue =>
      val serverStream: Stream[IO, Nothing] = Stream.eval(new Server (new QueueController(queue)).start())
      val consumerStream = Stream.fromQueueUnterminated(queue).printlns
      consumerStream.merge(serverStream).interruptAfter(3.seconds)
    }.interruptAfter(3.seconds).compile.drain
  }
}
