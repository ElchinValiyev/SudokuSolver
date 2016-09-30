package com.elchin.app

/**
  * Created by elchin on 30.09.16.
  */

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.view.View.MeasureSpec

/**
  * A {@link TextureView} that can be adjusted to a specified aspect ratio.
  */
class AutoFitTextureView(val context: Context, val attrs: AttributeSet, val defStyle: Int) extends TextureView(context, attrs, defStyle) {
  private var mRatioWidth: Int = 0
  private var mRatioHeight: Int = 0

  def this(context: Context, attrs: AttributeSet) {
    this(context, attrs, 0)
  }

  def this(context: Context) {
    this(context, null)
  }

  /**
    * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
    * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
    * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
    *
    * @param width  Relative horizontal size
    * @param height Relative vertical size
    */
  def setAspectRatio(width: Int, height: Int) {
    if (width < 0 || height < 0) throw new IllegalArgumentException("Size cannot be negative.")
    mRatioWidth = width
    mRatioHeight = height
    requestLayout()
  }

  override protected def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val width: Int = MeasureSpec.getSize(widthMeasureSpec)
    val height: Int = MeasureSpec.getSize(heightMeasureSpec)
    if (0 == mRatioWidth || 0 == mRatioHeight) setMeasuredDimension(width, height)
    else if (width < height * mRatioWidth / mRatioHeight) setMeasuredDimension(width, width * mRatioHeight / mRatioWidth)
    else setMeasuredDimension(height * mRatioWidth / mRatioHeight, height)
  }
}
