package io.geeteshk.sensor.util

import android.content.Context
import android.os.Environment
import android.support.design.widget.Snackbar
import android.view.View
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DataHandler {

    companion object {

        private val sensorDir = File(Environment.getExternalStorageDirectory(), "Sensor")

        // Makes sure the /sdcard/Sensor folder exists
        fun init() {
            if (!sensorDir.exists() || !sensorDir.isDirectory) {
                sensorDir.mkdir()
            }
        }

        // Converts a given DataSet to a sorted map for easy parsing
        fun dataToMap(dataSet: ILineDataSet): Map<Float, Float> {
            val dataMap = HashMap<Float, Float>()
            for (i in 0 until dataSet.entryCount) {
                val entry = dataSet.getEntryForIndex(i)
                dataMap[entry.x] = entry.y
            }

            return dataMap.toSortedMap()
        }

        // Creates a filename with some params for use
        private fun getFileName(sensorNumber: Int): String {
            val calendar = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault())
            val currentDateTime = dateFormat.format(calendar)
            return "S" + sensorNumber + "_" + currentDateTime + ".csv"
        }

        // Generates the CSV data contents to put into a file
        private fun getFileContents(dataMap: Map<Float, Float>): String {
            val builder = StringBuilder()
            dataMap.forEach {
                builder.append(it.key)
                builder.append(",")
                builder.append(it.value)
                builder.append("\n")
            }

            return builder.toString()
        }

        // Writes the specific data to a CSV file in storage
        private fun writeToStorage(sensorNumber: Int, dataMap: Map<Float, Float>) {
            val fileName = getFileName(sensorNumber)
            val csvFile = File(sensorDir, fileName)
            val fileStream = FileOutputStream(csvFile)

            fileStream.use { stream ->
                stream.write(getFileContents(dataMap).toByteArray())
            }
        }

        // Writes the specific data to a CSV file in the cloud
        private fun writeToCloud(context: Context, view: View, sensorNumber: Int, dataMap: Map<Float, Float>) {
            val storageRef = FirebaseStorage.getInstance().reference
            val fileName = getFileName(sensorNumber)
            val prefs = context.getSharedPreferences("io.geeteshk.sensor", 0)
            val folderName = prefs.getString("folder_name", FirebaseAuth.getInstance().currentUser!!.email)
            val fileRef = storageRef.child("files")
                    .child(folderName)
                    .child(fileName)
            val fileContents = getFileContents(dataMap)

            val uploadTask = fileRef.putBytes(fileContents.toByteArray())
            uploadTask.addOnFailureListener {
                Timber.e(it)
            }.addOnSuccessListener { task ->
                val name = task.metadata!!.name!!
                Snackbar.make(view, "Successfully uploaded sensor data $fileName", Snackbar.LENGTH_SHORT).show()
                fileRef.downloadUrl.addOnSuccessListener {
                    val url = it.toString()
                    writeMetaData(name, url, folderName)
                }
            }
        }

        /* Helper functions*/
        data class UploadInfo(val name: String, val url: String, val folderName: String)

        private fun writeMetaData(name: String, url: String, folderName: String) {
            val info = UploadInfo(name, url, folderName)
            val ref = FirebaseDatabase.getInstance()
                    .getReference("files")
                    .child(folderName)
            val key = ref.push().key
            ref.child(key!!).setValue(info)
        }

        fun storeData(context: Context, view: View, sensorNumber: Int, dataSet: ILineDataSet) {
            val dataMap = dataToMap(dataSet)
            writeToStorage(sensorNumber, dataMap)
            writeToCloud(context, view, sensorNumber, dataMap)
        }

        fun checkIfUsernameSet(context: Context): Boolean {
            val prefs = context.getSharedPreferences("io.geeteshk.sensor", 0)
            return prefs.contains("folder_name")
        }

        fun setUsername(context: Context, name: String) {
            val prefs = context.getSharedPreferences("io.geeteshk.sensor", 0)
            prefs.edit()
                    .putString("folder_name", name)
                    .apply()

            FirebaseDatabase.getInstance()
                    .getReference("folders")
                    .push().setValue(name)
        }
    }
}