/*
 * Copyright (c) 2011-2015 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

import java.awt._
import java.io.File
import javax.swing.JFrame

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat


/** Helper methods that simplify use of JavaCV API. */
object JavaCVUtils {

  /** Load an image and show in a CanvasFrame. If image cannot be loaded the application will exit with code 1.
    *
    * @param flags Flags specifying the color type of a loaded image:
    *              <ul>
    *              <li> `>0` Return a 3-channel color image</li>
    *              <li> `=0` Return a gray scale image</li>
    *              <li> `<0` Return the loaded image as is. Note that in the current implementation
    *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
    *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
    *              </ul>
    *              Default is gray scale.
    * @return loaded image
    */
  def loadAndShowOrExit(file: File, flags: Int = IMREAD_COLOR): Mat = {
    // Read input image
    val image = loadOrExit(file, flags)
    show(image, file.getName)
    image
  }

  /** Load an image. If image cannot be loaded the application will exit with code 1.
    *
    * @param flags Flags specifying the color type of a loaded image:
    *              <ul>
    *              <li> `>0` Return a 3-channel color image</li>
    *              <li> `=0` Return a gray scale image</li>
    *              <li> `<0` Return the loaded image as is. Note that in the current implementation
    *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
    *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
    *              </ul>
    *              Default is gray scale.
    * @return loaded image
    */
  def loadOrExit(file: File, flags: Int = IMREAD_COLOR): Mat = {
    // Read input image
    val image = imread(file.getAbsolutePath, flags)
    if (image.empty()) {
      println("Couldn't load image: " + file.getAbsolutePath)
      sys.exit(1)
    }
    image
  }

  /** Show image in a window. Closing the window will exit the application. */
  def show(mat: Mat, title: String) {
    val converter = new ToMat()
    val canvas = new CanvasFrame(title, 1)
    canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    canvas.showImage(converter.convert(mat))
  }

  /** Show image in a window. Closing the window will exit the application. */
  def show(image: Image, title: String) {
    val canvas = new CanvasFrame(title, 1)
    canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    canvas.showImage(image)
  }


  /** Draw a shape on an image.
    *
    * @param image   input image
    * @param overlay shape to draw
    * @param color   color to use
    * @return new image with drawn overlay
    */
  def drawOnImage(image: Mat, overlay: Rect, color: Scalar): Mat = {
    val dest = image.clone()
    rectangle(dest, overlay, color)
    dest
  }

  /** Save the image to the specified file.
    *
    * The image format is chosen based on the filename extension (see `imread()` in OpenCV documentation for the list of extensions).
    * Only 8-bit (or 16-bit in case of PNG, JPEG 2000, and TIFF) single-channel or
    * 3-channel (with ‘BGR’ channel order) images can be saved using this function.
    * If the format, depth or channel order is different, use Mat::convertTo() , and cvtColor() to convert it before saving.
    *
    * @param file  file to save to. File name extension decides output image format.
    * @param image image to save.
    */
  def save(file: File, image: Mat) {
    imwrite(file.getAbsolutePath, image)
  }


}