package com.elchin.commons

import android.view.View
import android.view.View.OnClickListener
/**
  * Created by evaliyev on 13.09.16.
  */
object ViewExtender {
  implicit def addOnClickToViews(view : View):ViewExtender =
    new ViewExtender(view)
}

class ViewExtender(view : View) {

  def onClick(action : View => Any) = {
    view.setOnClickListener(new View.OnClickListener() {
      def onClick(v : View) { action(v) }
    })
  }

}
