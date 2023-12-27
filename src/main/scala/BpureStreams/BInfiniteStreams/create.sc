import fs2._

/** iterate */

val s5 = Stream.iterate(1)(_ + 1)

// s5.toList will hang as infinite elements

s5.take(5).toList

/** unfold */

  // initial state, function: state => Option[Tuple[Output, NextState]]
val s6 = Stream.unfold(1){s =>
    if (s == 5) None
    else Some(s.toString, s + 1)}

s6.toList

// range and constant

val s7 = Stream.range(1, 15)
s7.toList

val s8 = Stream.constant(42)
s8.take(5).toList