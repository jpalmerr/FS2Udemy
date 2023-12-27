import fs2._

// effect time is pure as no side effect, result type Int
val s: Stream[Pure, Int] = Stream(1, 2, 3)
s.toList

val s2: Stream[Pure, Int] = Stream.empty
s2.toList

val s3: Stream[Pure, Int] = Stream.emit(42)
s3.toList

val s4: Stream[Pure, Vector[Int]] = Stream.emit(Vector(1, 2, 3))
s4.toList
s4.toVector

// However, are all stremas finite? No. Streams are lazy so we can supply things as needed


