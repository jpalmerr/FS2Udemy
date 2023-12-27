package AwhyStreams

import cats.effect._
import fs2.io.file.{Files, Path}
import fs2.text

import java.io.{BufferedReader, FileReader}
import java.nio.file.{Paths, Files => JFFiles}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.util.{Try, Using}
import scala.jdk.CollectionConverters._

object CSVProcessing extends IOApp .Simple {

  case class LegoSet(id: String, name: String, year: Int, themeId: Int, numParts: Int)

  def parseLegoSet(line: String): Option[LegoSet] = {
    val splitted = line.split(",")
    Try {LegoSet(
      id = splitted(0), name = splitted(1), year = splitted(2).toInt, themeId = splitted(3).toInt, numParts = splitted(4).toInt
    ) }.toOption
  }

  // def imperative

  // hard to read, requires resource handling
  def readLegoSetImperative(filename: String, p: LegoSet => Boolean, limit: Int): List[LegoSet] = {
    var reader: BufferedReader = null
    val legoSets: ListBuffer[LegoSet] = ListBuffer.empty
    var counter = 0

    try {
      reader = new BufferedReader(new FileReader(filename))
      var line: String = reader.readLine()
      while (line != null && counter < limit) {
        val legoSet = parseLegoSet(line)
        legoSet.filter(p).foreach { ls =>
          legoSets.append(ls)
          counter += 1
        }
        line = reader.readLine()
      }
    } finally {
      reader.close()
    }

    legoSets.toList
  }

  // def list

  /// much more readable - however notice we are reading ALL lines into memory
  def readLegoSetList(filename: String, p: LegoSet => Boolean, limit: Int): List[LegoSet] = {
    JFFiles.
      readAllLines(Paths.get(filename))
      .asScala
      .flatMap(parseLegoSet)
      .filter(p)
      .take(limit)
      .toList
  }

  // def iterator

  // good but not referentially transparent.. we create a side effect (fromFile) but don't represent it
  def readLegoSetIterator(filename: String, p: LegoSet => Boolean, limit: Int): List[LegoSet] = {
    Using(Source.fromFile(filename)) { source =>
      source
        .getLines()
        .flatMap(parseLegoSet)
        .filter(p)
        .take(limit)
        .toList
    }.get // not error handling atm
  }

  // def stream

  // sequentially
  def readLegoSetStreams(filename: String, p: LegoSet => Boolean, limit: Int): IO[List[LegoSet]] = {
    Files[IO].readAll(Path(filename))
      .through(text.utf8.decode)
      .through(text.lines)
      .map(parseLegoSet)
      .unNone // dropsNones and removes option from type
      .filter(p)
      .take(limit)
      .compile.toList
  }

  // in parallel
  def parallelReadLegoSetStreams(filename: String, p: LegoSet => Boolean, limit: Int): IO[List[LegoSet]] = {
    Files[IO].readAll(Path(filename))
      .through(text.utf8.decode)
      .through(text.lines)
      .parEvalMapUnbounded(s => IO(parseLegoSet(s)))
      .unNone // dropsNones and removes option from type
      .filter(p)
      .take(limit)
      .compile.toList
  }

  // make me work slower
  def slowerReadLegoSetStreams(filename: String, p: LegoSet => Boolean, limit: Int): IO[List[LegoSet]] = {
    Files[IO].readAll(Path(filename))
      .through(text.utf8.decode)
      .through(text.lines)
      .map(parseLegoSet)
      .evalTap(IO.println)
      .metered(1.second)
      .unNone // dropsNones and removes option from type
      .filter(p)
      .take(limit)
      .compile.toList
  }

  override def run: IO[Unit] = {
    val filename = "sets.csv"
//    IO(readLegoSetImperative(filename, _.year >= 1970, 5)).flatMap(IO.println)
//    IO(readLegoSetList(filename, _.year >= 1970, 5)).flatMap(IO.println)
//    IO(readLegoSetIterator(filename, _.year >= 1970, 5)).flatMap(IO.println)
//    readLegoSetStreams(filename, _.year >= 1970, 5).flatMap(IO.println)
//    parallelReadLegoSetStreams(filename, _.year >= 1970, 5).flatMap(IO.println)
    slowerReadLegoSetStreams(filename, _.year >= 1970, 5).flatMap(IO.println)
  }
}
