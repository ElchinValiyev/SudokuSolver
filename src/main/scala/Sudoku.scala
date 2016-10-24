/**
  * Created by elchin on 23.09.16.
  */

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Sudoku {

  //board height and width
  val size = 9

  //block width and height
  val unitSize = math.sqrt(size).toInt

  def tupleToInt(tuple: (Int, Int)): Int = tuple._1 * size + tuple._2

  // positions on the board
  val squares = for {row <- 0 until size; column <- 0 until size} yield (row, column)

  // list of cells in the same row or column or unit
  val peers = ArrayBuffer[Set[Int]]()

  // list of possible square value
  val possibleValues = ArrayBuffer[mutable.BitSet]()

  // 9 blocks in sudoku board
  val units = squares.groupBy(cell => (cell._1 / unitSize, cell._2 / unitSize))

  // cell in the same board block (unit)
  val unitPeers = ArrayBuffer[IndexedSeq[Int]]()

  val bitSets = Array.tabulate(10)(x => mutable.BitSet(x))

  for ((i, j) <- squares) {

    //squares on the same row
    val rowPeers = (0 until size).map(i * size + _)

    //squares in the same column
    val columnPeers = (0 until size).map(_ * size + j)

    //squares in the same block
    unitPeers += units((i / unitSize, j / unitSize)).map(this.tupleToInt)

    peers += (rowPeers ++ columnPeers ++ unitPeers(tupleToInt(i, j))).toSet - tupleToInt(i, j) // all neighbours
    possibleValues += mutable.BitSet(1 to size: _*) // initial possible values (1 .. 9)
  }

  /** Assign value and then remove this value from neighbour cells */
  private def assignValue(cell: Int, valueToRemove: Int) {
    possibleValues(cell) = bitSets(valueToRemove)
    eliminate(cell, valueToRemove)

    /** Remove value from neighbours */
    def eliminate(cell: Int, valueToRemove: Int): Unit = {
      //(1) If a square has only one possible value, then eliminate that value from the square's peers.
      for (peer <- peers(cell)) {
        val values = possibleValues(peer)
        if (values contains valueToRemove) {
          values -= valueToRemove
          values.size match {
            case 1 => eliminate(peer, values.head) // one possible value, remove this value from peers
            case 0 => throw new NoSuchElementException("Contradiction: removed last value") // should not happen
            case _ => //more than possible value , nothing we can do
          }
        }
      }

      //(2) If a unit has only one possible place for a value, then put the value there.
      //TODO: implement this thing
    }
  }

  /** Using depth-first search and propagation, try all possible values. */
  private def backTrack(): Boolean = {

    /** return true if none of cell's neighbours is assigned passed value */
    def isSecure(cell: Int, value: Int) = !peers(cell).exists { x =>
      possibleValues(x).size == 1 && possibleValues(x).head == value
    }

    if (isSolved) return true

    var cell = 0
    // find next empty cell
    possibleValues.indices.find(possibleValues(_).size > 1) match {
      case Some(x) => cell = x
      case None => return true
    }

    val oldValues = possibleValues(cell)
    for (value <- oldValues if isSecure(cell, value)) {
      possibleValues(cell) = bitSets(value)
      if (backTrack())
        return true
      possibleValues(cell) = oldValues
    }
    false
  }

  /** Sudoku is solved when all cells have only 1 possible value */
  private def isSolved = possibleValues.forall(_.size == 1)


  def drawGrid(grid: Array[Int]): Unit = {
    val line: String = "-" * 8 + '+' + "-" * 7 + '+' + "-" * 8 + "\n"
    var result = line
    var i = 0
    for (row <- grid.grouped(9)) {
      result += row.grouped(3).map(_.mkString(" ", " ", " ")) mkString("|", "|", "|\n")
      i += 1
      if (i % 3 == 0) result += line
    }
    println(result)
  }

  def solve(grid: Array[Int]) = {
    for (cell <- grid.indices if grid(cell) != 0) // try to solve with constraint propagation
      assignValue(cell, grid(cell))
    //val it = possibleValues.indices.filter(possibleValues(_).size > 1).iterator // cells without assigned value
    backTrack() // try to solve with backtracking
    possibleValues.map(_.head).toArray
  }
}
