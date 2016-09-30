package com.elchin.app

import android.content.Context
import android.graphics._
import android.os.Handler
import android.util.{AttributeSet, Log}
import android.view.{MotionEvent, SurfaceHolder, SurfaceView}

/**
  * Created by elchin on 29.09.16.
  */
class ProcessedView(context: Context) extends SurfaceView(context) {

  private var paint: Paint = _
  private var mHolder: SurfaceHolder = _

  mHolder = getHolder
  mHolder.setFormat(PixelFormat.TRANSPARENT)
  paint = new Paint(Paint.ANTI_ALIAS_FLAG)
  paint.setColor(Color.WHITE)
  paint.setStyle(Paint.Style.STROKE)

  @Override
  override def onDraw(canvas: Canvas): Unit = super.onDraw(canvas)

  @Override
  override def onTouchEvent(event: MotionEvent): Boolean = {
    if (event.getAction == MotionEvent.ACTION_DOWN) {
      invalidate()
      if (mHolder.getSurface.isValid) {
        val canvas: Canvas = mHolder.lockCanvas()
        Log.d("touch", "touchRecieved by camera")
        if (canvas != null) {
          Log.d("touch", "touchRecieved CANVAS STILL Not Null")
          canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
          canvas.drawColor(Color.TRANSPARENT)
          canvas.drawCircle(event.getX(), event.getY(), 100, paint)
          mHolder.unlockCanvasAndPost(canvas)
          new Handler().postDelayed(new Runnable {
            override def run(): Unit = {
              val canvas1: Canvas = mHolder.lockCanvas()
              if (canvas1 != null) {
                canvas1.drawColor(0, PorterDuff.Mode.CLEAR)
                mHolder.unlockCanvasAndPost(canvas1)
              }
            }
          }, 1000L)
        }
      }
    }
    false
  }
}
