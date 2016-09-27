package com.elchin.app

import java.io.{File, IOException}

import android.app.Activity
import android.content.Intent
import android.graphics.{Bitmap, BitmapFactory}
import android.os.{Bundle, Environment}
import android.provider.MediaStore
import android.view.View
import com.elchin.commons.ViewExtender._

class MainActivity extends Activity with TypedFindView {
  // allows accessing `.value` on TR.resource.constants
  implicit val context = this
  val CAMERA_REQUEST = 1888
  val PICK_IMAGE_REQUEST = 1
  val RESULT_OK = -1


  lazy val text = findView(TR.textView)
  lazy val galleryButton = findView(TR.galleryButton)
  lazy val cameraButton = findView(TR.cameraButton)
  lazy val liveButton = findView(TR.live_cameraButton)
  lazy val imageView = findView(TR.imageView)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)



    galleryButton.onClick { view: View => {
      val intent = new Intent()
      // Show only images, no videos or anything else
      intent.setType("image/*")
      intent.setAction(Intent.ACTION_GET_CONTENT)
      // Always show the chooser (if there are multiple options available)
      startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }
    }


    cameraButton.onClick { view: View =>
      val cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
      startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    liveButton.onClick { view: View => {
      val intent = new Intent(context, classOf[LiveCameraActivity])
      startActivity(intent)
      //      val photoPath:String = Environment.getExternalStorageDirectory.toString
      //      val file = new File (photoPath+"/zedge/wallpaper/sky.jpg")
      //      text.setText(file.toString)
//      try {
//
//        val myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath)
//        imageView.setImageBitmap(myBitmap)
//      }
//      catch {
//        case e: Exception => text.setText(e.getMessage)
//      }
    }
    }

  }


  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData != null) {
      val uri = data.getData
      text.setText(uri.toString)
      try {
        val bitmap = MediaStore.Images.Media.getBitmap(getContentResolver, uri)
        // Log.d(TAG, String.valueOf(bitmap));
        imageView.setImageBitmap(bitmap);
      } catch {
        case e: IOException => text.setText("Exception occured!")
      }
    }

    if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
      val photo = data.getExtras.get("data").asInstanceOf[Bitmap]
      imageView.setImageBitmap(photo)
      val root: String = Environment.getExternalStorageDirectory.toString
      text.setText(root)
      val file = new File(root + "/zedge/mm.jpg")
      //val out = new FileOutputStream(file)
      //photo.compress(Bitmap.CompressFormat.JPEG, 90, out)
      // out.flush()
      // out.close()
    }

  }
}