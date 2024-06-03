package Fcommunication

import cats.effect._
import fs2.concurrent._
import fs2.{Pure, Stream}

import scala.concurrent.duration._
import scala.util.Random

object CarPositionTracker extends IOApp.Simple {
  override def run: IO[Unit] = {
    case class CarPosition(carId: Long, lat: Long, lng: Long)
    def createCar(carId: Long, topic: Topic[IO, CarPosition]): Stream[IO, Nothing] = {
      Stream.repeatEval(IO(CarPosition(carId, Random.between(-90.0, 90.0).toLong, Random.between(-180.0, 180.0).toLong)))
        .metered(1.second)
        .through(topic.publish)
        .drain
    }

    def createGoogleMapUpdater(topic: Topic[IO, CarPosition]): Stream[IO, Nothing] = {
      topic
        .subscribe(10)
        .evalMap(pos => IO.println(s"Drawing position ${pos.lat}, ${pos.lng} for car ${pos.carId} in map..."))
        .drain
    }

    // use params to listen for changes, notify when approriate. drain stream as dont care about anything but side effects
    def createDriverNotifier(topic: Topic[IO, CarPosition], shouldNotify: CarPosition => Boolean, notify: CarPosition => IO[Unit]): Stream[IO, Nothing] = {
      topic.subscribe(10)
        .evalMap{ position =>
          if (shouldNotify(position)) notify(position) else IO.unit
        }.interruptAfter(5.seconds).drain
    }

    Stream.eval(Topic[IO, CarPosition]).flatMap { topic =>
      val cars: Stream[Pure, Stream[IO, Nothing]] = Stream.range(1, 10).map(carId => createCar(carId, topic))
      val updated: Stream[IO, Nothing] = createGoogleMapUpdater(topic)
      val notifier: Stream[IO, Nothing] = createDriverNotifier(
        topic = topic,
        shouldNotify = pos => pos.lat > 0.0,
        notify = pos => IO.println(s"Car ${pos.carId}: You are above the equator! (${pos.lat}, ${pos.lng})")
      )
      (cars ++ Stream(updated, notifier)).parJoinUnbounded
    }.interruptAfter(5.seconds).compile.drain
  }
}
