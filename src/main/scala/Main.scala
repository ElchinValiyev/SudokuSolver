/**
  * Created by evaliyev on 25.10.16.
  */
object Main extends App {

  val solver = new JavaCVSudokuSolver(new NeuralNetwork, new Sudoku)
  solver.solve("0.jpg")
  //solver.solve("1.jpg")
  // solver.solve("2.jpg")
  // solver.solve("3.jpg")
}
