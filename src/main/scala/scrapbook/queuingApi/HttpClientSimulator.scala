package scrapbook.queuingApi

import cats.effect._

import scala.util.Random

object HttpClientSimulator {
  def simulateACall(): IO[Either[String, String]] = {
    val bool = Random.nextBoolean()
    println(bool)
    if (bool) { IO(Left("http error"))} else IO(Right("http success"))
  }
}