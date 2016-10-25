/**
  * Created by evaliyev on 25.10.16.
  */
object Main extends App {

  val solver = new JavaCVSudokuSolver(new NeuralNetwork, new Sudoku)
  solver.solve("1.jpg")

}
