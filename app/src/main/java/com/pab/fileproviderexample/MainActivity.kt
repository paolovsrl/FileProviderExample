package com.pab.fileproviderexample

import android.content.Intent
import android.content.UriPermission
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.applyCanvas
import com.pab.fileproviderexample.ui.theme.FileProviderExampleTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    val TAG = "MainActivity"

    //get a writeable path (tree)
    var documentTreeSelectorLauncher: ActivityResultLauncher<Uri?>? = null
    //open a file (document) and get the uri
    var documentSelectorLauncher: ActivityResultLauncher<Array<String>>? = null
    var treeUri: Uri? = null
    var fileUri:Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerForWritingPermission(this)

        setContent {
            FileProviderExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(modifier = Modifier.fillMaxSize()){
                        Column (modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.33333f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally){
                            Button(onClick = {
                                documentSelectorLauncher?.launch(arrayOf("*/*"))
                            }) {
                                Text(text = "Select a file")
                            }
                        }

                        Column (modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.33333f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally){
                            Button(onClick = {
                                writeTextFile( this@MainActivity)
                            }) {
                                Text(text = "Write text")
                            }
                        }

                        val view = LocalView.current

                        Column (modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.33333f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally){
                            Button(onClick = {

                                val bmp = Bitmap.createBitmap(view.width, view.height,
                                    Bitmap.Config.ARGB_8888).applyCanvas {
                                    view.draw(this)
                                }


                                writeImageFile(bmp, this@MainActivity)
                            }) {
                                Text(text = "Write image")
                            }
                        }
                    }
                }
            }
        }
    }





    fun registerForWritingPermission(activity: ComponentActivity){
        //Register for managing files. Put in onCreate because LifecycleOwners must call register before they are STARTED.
        documentTreeSelectorLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
            object : ActivityResultCallback<Uri?> {
                override fun onActivityResult(uri: Uri?) {
                    treeUri = uri
                    if (uri != null) {
                        activity.getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        Log.d(TAG, "Permission to write granted.")
                    }
                    //Do something, if needed.
                }


            })


        documentSelectorLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
            object : ActivityResultCallback<Uri?> {
                override fun onActivityResult(uri: Uri?) {
                    // Handle the returned Uri
                    fileUri = uri
                    Log.d(TAG, "File selected: ${fileUri?.path}")
                    activity.runOnUiThread(Runnable {
                        Toast.makeText(activity, "Path read successfully!", Toast.LENGTH_SHORT).show()
                    })
                }
            })
    }



    fun writeTextFile(activity : ComponentActivity) {

        for (p in activity.getContentResolver().getPersistedUriPermissions()) {
            Log.d("URI-FOUND", p.uri.toString())
        }

        val permissions: List<UriPermission> = activity.getContentResolver().getPersistedUriPermissions()
        if (permissions.size == 1) {
            treeUri = permissions[0].uri
        } else {
            //Permission not given, ask to select the path
            activity.getContentResolver().getPersistedUriPermissions().clear()
            documentTreeSelectorLauncher?.launch(null)
        }

        //Keep the main UI thread light
        CoroutineScope(Dispatchers.Default).launch {
            try {
                //format and put data in a string
                var data = "Ciao Mamma!"
                writeTextToMemory(data, activity)

            } catch (e:Exception){
                Log.e(TAG, "Exception: $e")
            }
        }
    }


    private fun writeTextToMemory(data:String, activity:ComponentActivity){
        if(treeUri!=null){
            //Write
            val folderUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            //CREATE A FILE:
            val fileUri = DocumentsContract.createDocument(
                activity.getContentResolver(),
                folderUri,
                "text/plain",
                "errors.txt"
            )

            Log.d(TAG, "Uri: " + fileUri!!.path.toString())
            val oS: OutputStream? = activity.getContentResolver().openOutputStream(fileUri)
            oS?.write(data.toByteArray(StandardCharsets.UTF_8))
            oS?.close()
            activity.runOnUiThread(Runnable {
                Toast.makeText(activity, "Exported successfully!", Toast.LENGTH_SHORT).show()
            })
        } else{
            Log.e(TAG, "URI is null!")
        }
    }



    fun writeImageFile(data:Bitmap, activity : ComponentActivity) {

        for (p in activity.getContentResolver().getPersistedUriPermissions()) {
            Log.d("URI-FOUND", p.uri.toString())
        }

        val permissions: List<UriPermission> = activity.getContentResolver().getPersistedUriPermissions()
        if (permissions.size == 1) {
            treeUri = permissions[0].uri
        } else {
            //Permission not given, ask to select the path
            activity.getContentResolver().getPersistedUriPermissions().clear()
            documentTreeSelectorLauncher?.launch(null)
        }

        //Keep the main UI thread light
        CoroutineScope(Dispatchers.Default).launch {
            try {
                //format and put data in a string
                writeImageToMemory(data, activity)

            } catch (e:Exception){
                Log.e(TAG, "Exception: $e")
            }
        }
    }

    private fun writeImageToMemory(data:Bitmap, activity:ComponentActivity){
        if(treeUri!=null){
            //Write
            val folderUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )

            //Eventually:
            /*CREATE A FOLDER:
            val newFolderUri = DocumentsContract.createDocument(
                contentResolver, folderUri, DocumentsContract.Document.MIME_TYPE_DIR, "test2"
            )
             val newFolderUri2 = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri) + "/test2"
            )
            */


            //CREATE A FILE:
            val fileUri = DocumentsContract.createDocument(
                activity.getContentResolver(),
                folderUri,
                "image/png",
                "img.png"
            )

            Log.d(TAG, "Uri: " + fileUri!!.path.toString())

            val oS: OutputStream? = activity.getContentResolver().openOutputStream(fileUri)
            oS?.let{
                data.compress(Bitmap.CompressFormat.PNG, 90, oS)
            }
            oS?.close()
            activity.runOnUiThread(Runnable {
                Toast.makeText(activity, "Exported successfully!", Toast.LENGTH_SHORT).show()
            })
        } else{
            Log.e(TAG, "URI is null!")
        }
    }


}
