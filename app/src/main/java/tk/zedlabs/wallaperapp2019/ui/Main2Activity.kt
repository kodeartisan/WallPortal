package tk.zedlabs.wallaperapp2019.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_image_details.*
import kotlinx.android.synthetic.main.activity_image_details_develop.*
import kotlinx.android.synthetic.main.activity_original_resolution.*
import kotlinx.android.synthetic.main.fab_image_details.*
import kotlinx.android.synthetic.main.progress_saw.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tk.zedlabs.wallaperapp2019.BuildConfig
import tk.zedlabs.wallaperapp2019.R
import tk.zedlabs.wallaperapp2019.repository.BookmarkDatabase
import tk.zedlabs.wallaperapp2019.repository.BookmarkImage
import tk.zedlabs.wallaperapp2019.viewmodel.ImageDetailViewModel
import java.io.File

class Main2Activity : AppCompatActivity() {

    lateinit var imageDetailViewModel : ImageDetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_details_develop)

        val intent = intent
        val urlFull = intent.getStringExtra("url_large")
        val urlRegular = intent.getStringExtra("url_regular")
        val id = intent.getStringExtra("id")
        val activity = intent.getStringExtra("Activity")
        val uri= FileProvider.getUriForFile(this@Main2Activity, BuildConfig.APPLICATION_ID +".fileprovider",
            File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/WallPortal/$id.jpg"))
        imageDetailViewModel = ImageDetailViewModel(applicationContext)
        setUpInitialImage(urlFull)

        if(activity == "BookmarkActivity"){
            bookmark_button_1.text = getString(R.string.remove_from_bookmarks)
        }

        download_button_1.setOnClickListener {
            Glide.with(this)
                .asBitmap()
                .load(urlFull)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        imageDetailViewModel.downloadImage(resource,id)
                        Toast.makeText(this@Main2Activity,"Download Started", Toast.LENGTH_SHORT).show()
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        Toast.makeText(this@Main2Activity,"Downloaded!", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        saw_button_1.setOnClickListener {
            progressLayout.visibility = View.VISIBLE
            Glide.with(this)
                .asBitmap()
                .load(urlFull)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        progressLayout.visibility = View.GONE
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.IO) {
                                imageDetailViewModel.downloadImage(resource, id)
                            }
                            withContext(Dispatchers.Default){
                                setWallpaper1(uri)
                            }
                        }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {  }
                })
        }

        bookmark_button_1.setOnClickListener {
            val db = Room.databaseBuilder(
                applicationContext,
                BookmarkDatabase::class.java, "bookmark-database"
            ).build()
            var unique = true
            CoroutineScope(Dispatchers.IO).launch {
                val idList = db.bookmarkDao().getId()
                for (id1 in idList) {
                    if (id == id1) {
                        unique = false; var s1 = getString(R.string.image_already_bookmarked)
                        if(activity == "BookmarkActivity"){s1 = getString(R.string.remove_from_bookmarks_qm)}
                        val mySnackbar = Snackbar.make( myCoordinatorLayout,s1, Snackbar.LENGTH_LONG)
                        mySnackbar.setAction(getString(R.string.remove_string),
                            RemoveListener(
                                applicationContext,
                                BookmarkImage(id, urlFull, urlRegular)
                            )
                        )
                        mySnackbar.setActionTextColor(getColor(R.color.snackBarAction))
                        mySnackbar.show()
                        break
                    }
                }
                if(unique){
                    db.bookmarkDao().insert(BookmarkImage(id, urlFull, urlRegular))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Main2Activity, "Added to Bookmarks!", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

        highRes_button_1.setOnClickListener {
            val intent1 = Intent(this, OriginalResolutionActivity::class.java)
            intent1.putExtra("imageUrl", urlFull)
            startActivity(intent1)
        }

    }

    private fun setUpInitialImage(urlRegular : String) {

        val circularProgressDrawable = CircularProgressDrawable(this)
        circularProgressDrawable.strokeWidth = 10f
        circularProgressDrawable.centerRadius = 50f
        circularProgressDrawable.start()

        Glide.with(this)
            .load(urlRegular)
            .transform(FitCenter())
            .placeholder(circularProgressDrawable)
            .into(photo_view_1)
    }

    private fun setWallpaper1(uri : Uri) {
        try {
            Log.d("Main2Activity: ", "Crop and Set: $uri")
            val wallpaperIntent = WallpaperManager.getInstance(this).getCropAndSetWallpaperIntent(uri)
            wallpaperIntent.setDataAndType(uri, "image/*")
            wallpaperIntent.putExtra("mimeType", "image/*")
            startActivityForResult(wallpaperIntent, 13451)
        } catch (e : Exception) {
            e.printStackTrace()
            Log.d("Main2Activity", "Chooser: $uri")
            val wallpaperIntent = Intent(Intent.ACTION_ATTACH_DATA)
            wallpaperIntent.setDataAndType(uri, "image/*")
            wallpaperIntent.putExtra("mimeType", "image/*")
            wallpaperIntent.addCategory(Intent.CATEGORY_DEFAULT)
            wallpaperIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            wallpaperIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            wallpaperIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivity(Intent.createChooser(wallpaperIntent, "Set as wallpaper"))
        }
    }

    class RemoveListener(private val ac : Context, private val bm : BookmarkImage) : View.OnClickListener {

        override fun onClick(v: View) {
            CoroutineScope(Dispatchers.IO).launch {
                val db2 = Room.databaseBuilder(ac,BookmarkDatabase::class.java, "bookmark-database").build()
                db2.bookmarkDao().delete(bm)
                withContext(Dispatchers.Main){
                    Toast.makeText(ac, "Removed from Bookmarks", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}