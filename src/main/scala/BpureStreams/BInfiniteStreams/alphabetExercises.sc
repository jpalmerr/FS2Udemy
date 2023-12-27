import fs2._

// alphabet: iterate
def lettersIter: Stream[Pure, Char] = Stream.iterate('a')(c => (c + 1).toChar).take(26)
lettersIter.toList

// alphabet: unfold

def lettersUnfold: Stream[Pure, Char] =
  Stream.unfold('a')(c => if(c == 'z' + 1) None else Some((c, (c + 1).toChar)))

lettersUnfold.toList

// alphabet: unfold
def myIterate[A](initial: A)(next: A => A): Stream[Pure, A] = {
  Stream.unfold(initial)(a => Some((a, next(a))))
}

myIterate('a')(c => (c + 1).toChar).take(26).toList