
/**
  * Created by elchin on 24.10.16.
  */

import java.io.File

import JavaCVUtils._
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._

import scala.annotation.switch
import scala.collection.immutable.IndexedSeq

class JavaCVSudokuSolver(val classifier: NeuralNetwork, val sudoku: Sudoku) {

  val gridSize = 450
  val Black = new Scalar(0, 0, 0, 0)

  def solve(imageFilePath: String): Unit = {
    // Read an image
    val src: Mat = imread(imageFilePath, IMREAD_GRAYSCALE)
    println("Detecting edges ...")
    val edges = detectEdges(src)
    val rect = getRectangleWithGrid(edges)
    println("Extracting grid from image ...")
    val grid = extractGridFromImage(src, rect)


    val cells = extractCellsFromGridImage(grid)

    val recognizedDigits = recognizeDigits(cells)
    println(recognizedDigits.mkString(" "))

    val solution = sudoku.solve(recognizedDigits)
    println("Done")

    sudoku.drawGrid(solution)
    showImageWithSolution(grid, solution)

  }

  private def detectEdges(src: Mat): Mat = {
    val canny = new Mat()
    Canny(src, canny, 50, 200, 3, true)
    canny
  }

  private def getRectangleWithGrid(canny: Mat) = {
    // Sudoku is assumed to be biggest object
    val grid = findLargestContour(canny)
    //findind polygon(rectangle) of sudoku grid
    val approx = new Mat(new Size(2, 4), CV_32F)
    approxPolyDP(grid, approx, 0.01 * arcLength(grid, true), true)
    approx
  }

  private def findLargestContour(image: Mat): Mat = {
    val contours = new MatVector()
    findContours(image, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE)
    var maxContour = contours.get(0)
    if (maxContour != null) {
      var maxArea = contourArea(maxContour)
      var currentArea = maxArea
      for (i <- 1 until contours.size.toInt) {
        currentArea = contourArea(contours.get(i))
        if (currentArea > maxArea) {
          maxContour = contours.get(i)
          maxArea = currentArea
        }
      }
    }
    maxContour
  }

  private def extractGridFromImage(src: Mat, rectangle: Mat) = {
    rectangle.reshape(1).convertTo(rectangle, CV_32F) // convert rectangle to 1 channel Float matrix

    val indexer = rectangle.createIndexer().asInstanceOf[FloatIndexer]
    val cornerPoints = for (i <- 0 until rectangle.rows) yield (indexer.get(rectangle.cols * i), indexer.get(rectangle.cols * i + 1))
    val mapping = getCornersMapping(cornerPoints)

    val gridImage = new Mat()
    val transform = getPerspectiveTransform(rectangle, mapping)
    warpPerspective(src, gridImage, transform, new Size(gridSize, gridSize))
    gridImage
  }

  private def getCornersMapping(corners: IndexedSeq[(Float, Float)]) = {
    def sumOfCoordinates(pointWithIndex: ((Float, Float), Int)) = {
      val (coordinates, _) = pointWithIndex
      coordinates._1 + coordinates._2
    }
    def diffOfCoordinates(pointWithIndex: ((Float, Float), Int)) = {
      val (coordinates, _) = pointWithIndex
      coordinates._1 - coordinates._2
    }

    val indexedCorners = corners.zipWithIndex

    // the top-left point will have the smallest sum of coordinates, whereas
    // the bottom-right point will have the largest sum
    val topLeft = indexedCorners.minBy(sumOfCoordinates)._2
    val bottomRight = indexedCorners.maxBy(sumOfCoordinates)._2

    // top-right point will have the largest difference (x-y),
    // whereas the bottom-left will have the smallest difference
    val topRight = indexedCorners.maxBy(diffOfCoordinates)._2
    val bottomLeft = indexedCorners.minBy(diffOfCoordinates)._2

    val mapping = new Mat(new Size(2, 4), CV_32F)
    val indexer = mapping.createIndexer().asInstanceOf[FloatIndexer]

    for (i <- corners.indices) {
      (i: @switch) match {
        case `topLeft` => indexer.put(i, 0, 0).put(i, 1, 0)
        case `topRight` => indexer.put(i, 0, gridSize).put(i, 1, 0)
        case `bottomLeft` => indexer.put(i, 0, 0).put(i, 1, gridSize)
        case `bottomRight` => indexer.put(i, 0, gridSize).put(i, 1, gridSize)
        case _ => println("Error!")
      }
    }
    mapping
  }

  private def applyTheshold(image: Mat) = {
    val thresholded = new Mat()
    adaptiveThreshold(image, thresholded, 1, ADAPTIVE_THRESH_GAUSSIAN_C, CV_THRESH_BINARY_INV, 15, 7)
    thresholded
  }

  private def extractCellsFromGridImage(grid: Mat): IndexedSeq[Mat] = {

    val img = applyTheshold(grid)

    val radius = 6
    val cellSize = gridSize / 9

    for (i <- 0 until gridSize by cellSize; j <- 0 until gridSize by cellSize)
      yield new Mat(img, new Rect(j + radius / 2, i, cellSize - radius / 2, cellSize))
  }

  /** process images to detect digits with neural network */
  private def recognizeDigits(cells: IndexedSeq[Mat]) = {
    val recognizedGrid = new Array[Int](sudoku.squares.size)

    for ((cell, index) <- cells.zipWithIndex) {

      val number = findLargestContour(cell.clone)
      // if cell is not empty
      if (number != null) {

        var subMatrix = new Mat(cell, boundingRect(number)) // extracting number

        // if cell   contains number but just cell line
        if (subMatrix.rows() > 10 && subMatrix.cols() > 10) {
          resize(subMatrix, subMatrix, new Size(10, 10), 0, 0, INTER_NEAREST) // make image smaller
          subMatrix = subMatrix.reshape(1, 100) // preparing to pass to neural network
          val bytes = new Array[Byte](100)
          subMatrix.ptr().get(bytes) // transform mat to array
          recognizedGrid(index) = classifier.predict(bytes)
        }
      }
    }
    recognizedGrid
  }

  def showImageWithSolution(gridImage: Mat, solution: Array[Int]): Unit = {
    val cellSize = gridSize / 9
    var text = for (i <- 0 until 9; j <- 0 until 9)
      putText(gridImage, solution(i * 9 + j).toString, new Point(j * cellSize, (i + 1) * cellSize - cellSize / 4), FONT_HERSHEY_SIMPLEX, 1, Black, 2, LINE_AA, false)

    show(gridImage, "Solution")
  }

}
