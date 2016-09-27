package com.elchin.app

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.{ImageFormat, SurfaceTexture}
import android.hardware.camera2._
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Bundle
import android.support.annotation.NonNull
import android.util.{Log, Size}
import android.view._

class LiveCameraActivity extends Activity with TypedFindView with TextureView.SurfaceTextureListener {
  private val TAG: String = "SudokuSolver"
  implicit val context = this

  private var mCamera: CameraDevice = _
  private var mSession: CameraCaptureSession = _
  lazy val mPreviewView = findView(TR.textureView)
  private var mJpegCaptureSurface, mPreviewSurface: Surface = _
  private var mPreviewSize: Size = _
  private var mCharacteristics: CameraCharacteristics = _
  private var mCaptureImageFormat: Int = 0

  override protected def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.live_camera)
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    getWindow.getDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN)
    mPreviewView.setSurfaceTextureListener(this)
  }

  override def onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
    try {
      initCamera(surface)
    }
    catch {
      case e: CameraAccessException => Log.e(TAG, "Failed to open camera", e)
    }
  }

  override def onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = {
    if (mCamera != null) {
      mCamera.close()
      mCamera = null
    }
    mSession = null
    true
  }

  override def onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

  override def onSurfaceTextureUpdated(surface: SurfaceTexture) {}

  @throws[CameraAccessException]
  private def initCamera(surface: SurfaceTexture) {
    val cm: CameraManager = getSystemService(Context.CAMERA_SERVICE).asInstanceOf[CameraManager]
    // get ID of rear-facing camera
    var cc: CameraCharacteristics = null
    val cameraId = cm.getCameraIdList.find { id =>
      cc = cm.getCameraCharacteristics(id)
      cc.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
    }.
      getOrElse(throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Couldn't find suitable camera"))

    mCharacteristics = cc
    val streamConfigs: StreamConfigurationMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    // determine supported output formats..
    var supportsRaw: Boolean = false
    var supportsJpeg: Boolean = false
    for (format <- streamConfigs.getOutputFormats)
      format match {
        case ImageFormat.RAW_SENSOR => supportsRaw = true
        case ImageFormat.JPEG => supportsJpeg = true
        case _ =>
      }

    if (supportsRaw) mCaptureImageFormat = ImageFormat.RAW_SENSOR
    else if (supportsJpeg) mCaptureImageFormat = ImageFormat.JPEG
    else throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Couldn't find supported image format")


    // alternatively, make a way for the user to select a capture size..
    val jpegSize: Size = streamConfigs.getOutputSizes(ImageFormat.JPEG)(0)
    // find the preview size that best matches the aspect ratio of the camera sensor..
    val previewSizes: Array[Size] = streamConfigs.getOutputSizes(classOf[SurfaceTexture])
    mPreviewSize = chooseOptimalSize(previewSizes, mPreviewView.getWidth, mPreviewView.getHeight, jpegSize.getWidth, jpegSize.getHeight, jpegSize)

    if (mPreviewSize == null) return
    // set up capture surfaces and image readers..
    mPreviewSurface = new Surface(surface)

    val jpegReader: ImageReader = ImageReader.newInstance(jpegSize.getWidth, jpegSize.getHeight, ImageFormat.JPEG, 1)
    jpegReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
      def onImageAvailable(reader: ImageReader) {
        //new SaveJpegTask(context, mPhotoDir, reader.acquireLatestImage).execute()
      }
    }, null)
    mJpegCaptureSurface = jpegReader.getSurface
    cm.openCamera(cameraId, new CameraDevice.StateCallback() {
      def onOpened(@NonNull camera: CameraDevice) {
        mCamera = camera
        initPreview()
      }

      def onDisconnected(@NonNull camera: CameraDevice) {}

      def onError(@NonNull camera: CameraDevice, error: Int) {}
    }, null)
  }

  private def initPreview() {
    // scale preview size to fill screen width
    val screenWidth: Int = getResources.getDisplayMetrics.widthPixels
    val previewRatio: Float = mPreviewSize.getWidth / mPreviewSize.getHeight.toFloat
    val previewHeight: Int = getResources.getDisplayMetrics.heightPixels//screenWidth * previewRatio.round
    val params: ViewGroup.LayoutParams = mPreviewView.getLayoutParams
    params.width = screenWidth
    params.height = previewHeight
    val surfaces: java.util.List[Surface] = java.util.Arrays.asList(mPreviewSurface, mJpegCaptureSurface)
    try {
      mCamera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
        def onConfigured(session: CameraCaptureSession) {
          mSession = session
          updatePreview()
        }

        def onConfigureFailed(session: CameraCaptureSession) {
        }
      }, null)
    }
    catch {
      case e: CameraAccessException => Log.d(TAG, "Failed to create camera capture session", e)
    }
  }

  /**
    * Call this whenever some camera control changes (e.g., focus distance, white balance, etc) that should affect the preview
    */
  private def updatePreview() {
    try {
      if (mCamera == null || mSession == null) return
      val builder: CaptureRequest.Builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
      builder.addTarget(mPreviewSurface)
      builder.set[Integer](CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
      //            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, ...)
      //            builder.set(CaptureRequest.SENSOR_SENSITIVITY, ...)
      //            builder.set(CaptureRequest.CONTROL_AWB_MODE, ...)
      //            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, ...)
      //            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ...)
      //            etc...
      mSession.setRepeatingRequest(builder.build, new CameraCaptureSession.CaptureCallback() {
        override def onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
          // if desired, we can get updated auto focus & auto exposure values here from 'result'
        }
      }, null)
    }
    catch {
      case e: CameraAccessException => Log.e(TAG, "Failed to start preview")
    }
  }

  /**
    * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
    * is at least as large as the respective texture view size, and that is at most as large as the
    * respective max size, and whose aspect ratio matches with the specified value. If such size
    * doesn't exist, choose the largest one that is at most as large as the respective max size,
    * and whose aspect ratio matches with the specified value.
    *
    * @param choices           The list of sizes that the camera supports for the intended output
    *                          class
    * @param textureViewWidth  The width of the texture view relative to sensor coordinate
    * @param textureViewHeight The height of the texture view relative to sensor coordinate
    * @param maxWidth          The maximum width that can be chosen
    * @param maxHeight         The maximum height that can be chosen
    * @param aspectRatio       The aspect ratio
    * @return The optimal { @code Size}, or an arbitrary one if none were big enough
    */
  private def chooseOptimalSize(choices: Array[Size], textureViewWidth: Int,
                                textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size): Size = {

    // Collect the supported resolutions that are at least as big as the preview Surface
    var bigEnough = List[Size]()
    // Collect the supported resolutions that are smaller than the preview Surface
    var notBigEnough = List[Size]()
    val w = aspectRatio.getWidth
    val h = aspectRatio.getHeight
    for (option <- choices) {
      if (option.getWidth <= maxWidth && option.getHeight <= maxHeight &&
        option.getHeight == option.getWidth * h / w) {
        if (option.getWidth >= textureViewWidth &&
          option.getHeight >= textureViewHeight) {
          bigEnough +:= option
        } else {
          notBigEnough +:= option
        }
      }
    }

    // Pick the smallest of those big enough. If there is no one big enough, pick the
    // largest of those not big enough.
    if (bigEnough.nonEmpty) {
      bigEnough.min(Ordering.by[Size,Int](x => x.getHeight * x.getWidth))
    } else if (notBigEnough.nonEmpty) {
      notBigEnough.min(Ordering.by[Size,Int](x => x.getHeight * x.getWidth))
    } else {
      Log.e(TAG, "Couldn't find any suitable preview size")
      choices(0)
    }
  }
}