package Fcommunication

import cats.effect.{IO, IOApp}
import fs2._
import fs2.concurrent.SignallingRef

import scala.concurrent.duration.DurationInt
import scala.util.Random

object Signals extends IOApp.Simple{
  override def run: IO[Unit] = {
    def signaller(signal: SignallingRef[IO, Boolean]): Stream[IO, Nothing] = {
      Stream
        .repeatEval(
        IO(Random.between(1, 1000)).flatTap(i => IO.println(s"Generating $i"))
        )
        .metered(100.millis)
        .evalMap(i => if (i % 5 == 0) signal.set(true) else IO.unit)
        .drain
    }

    def worker(signal: SignallingRef[IO, Boolean]): Stream[IO, Nothing] = {
      Stream
        .repeatEval(IO.println("working..."))
        .metered(50.millis)
        .interruptWhen(signal)
        .drain
    }

    Stream.eval(SignallingRef[IO, Boolean](false)).flatMap { signal =>
      worker(signal).concurrently(signaller(signal))
    }.compile.drain


    // exercise

    type Temperature = Double
    def createTemperatureSensor(alarm: SignallingRef[IO, Temperature], threshold: Temperature): Stream[IO, Nothing] = {
      Stream.repeatEval(
        IO(Random.between(-40.0, 40.0))
      ).evalTap(t => IO.println(s"Current temp: $t"))
        .evalMap(t => if (t > threshold) alarm.set(t) else IO.unit)
        .metered(300.millis)
        .drain
    }

    def createCooler(alarm: SignallingRef[IO, Temperature]): Stream[IO, Nothing] = {
      alarm
        .discrete // Returns a stream of the updates to this signal
        .evalMap(t => IO.println(s"$t: degrees is too hot! Cooling down..."))
        .drain
    }

    val threshold: Temperature = 20.0
    val initialTemp: Temperature = 20.0

    Stream.eval(
      SignallingRef[IO, Temperature](initialTemp)
    ).flatMap { signal =>
      createTemperatureSensor(signal, threshold).concurrently(createCooler(signal))
    }.compile.drain

  }
}
