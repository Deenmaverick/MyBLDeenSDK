package com.deenislam.sdk.service.libs.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.deenislam.sdk.R
import com.deenislam.sdk.service.callback.common.DownloaderCallback
import com.deenislam.sdk.utils.CallBackProvider
import com.deenislam.sdk.utils.generateUniqueNumber
import com.deenislam.sdk.utils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


internal class QuranDownloadService : Service() {

    private val CANCEL_ACTION = "cancel_action"
    private val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    private val EXTRA_FILENAME = "extra_filename"

    private val binder = DownloadBinder()
    private val downloadTasks = mutableListOf<Deferred<Boolean>>()
    private val downloadTasksMap = HashMap<Int, Deferred<Boolean>>()

    companion object{
         val downloadFileList = HashMap<String,Int>()
    }

    private var callback = CallBackProvider.get<DownloaderCallback>()


    inner class DownloadBinder : Binder() {
        fun getService(): QuranDownloadService = this@QuranDownloadService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {

            when (intent.action) {
                CANCEL_ACTION -> {
                    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                    val filenameByUser = intent.getStringExtra(EXTRA_FILENAME)
                    if (notificationId != -1) {
                        cancelDownloadTask(notificationId,filenameByUser)
                    }
                }
                else ->{

                    val filenameByUser = intent.getStringExtra("filename")
                    val fileTitle = intent.getStringExtra("filetitle")
                    val downloadUrl = intent.getStringExtra("downloadUrl")
                    val isZipFile = intent.getBooleanExtra("iszip",false)
                    val destinationFolderPath = intent.getStringExtra("destinationFolder")

                    val isFilenameExist = downloadFileList.containsKey(filenameByUser)


                    if(filenameByUser!=null && downloadUrl!=null)
                    downloadFileList[filenameByUser] = 0

                    if (!downloadUrl.isNullOrBlank() && !destinationFolderPath.isNullOrBlank() && !isFilenameExist) {
                        val destinationFolder = File(destinationFolderPath)

                        // Get a unique notification ID for this download
                        val notificationId = generateNotificationId()

                        // Start the download task
                        val downloadTask = downloadFile(
                            downloadUrl = downloadUrl,
                            destinationFolder = destinationFolder,
                            notificationId = notificationId,
                            filenameByUser = filenameByUser,
                            fileTitle = fileTitle,
                            isZipFile = isZipFile
                        )

                        downloadTasksMap[notificationId] = downloadTask
                        // Add the task to the list
                        downloadTasks.add(downloadTask)

                        // Add a callback for when the download task completes
                        downloadTask.invokeOnCompletion {
                            Log.e("cancelDownloadTask", notificationId.toString()+ callback.toString())
                            callback = CallBackProvider.get<DownloaderCallback>()
                            // Remove the completed task from the list
                            downloadTasks.remove(downloadTask)
                            downloadFileList.remove(filenameByUser)

                            if(this::notificationManager.isInitialized)
                                notificationManager.cancel(notificationId)

                            callback?.updateDownloadProgress(
                                filenameByUser,
                                100,
                                downloadTask.isCompleted,
                                notificationId,
                                downloadTask.isCancelled
                            )

                            // Handle completion, update UI, etc.
                            //stopSelf() // Stop the service when the last download is complete

                            // Stop foreground and remove notification
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                stopForeground(STOP_FOREGROUND_REMOVE)
                            } else {
                                stopForeground(true)
                            }

                        }

                        // Start the download with the unique notification ID
                        //startForeground(notificationId, createNotification(notificationId))

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(
                                notificationId,
                                createNotification(notificationId),
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                            )
                        } else {
                            startForeground(notificationId,  createNotification(notificationId))
                        }

                        CoroutineScope(Dispatchers.Main).launch { updateNotification(
                            0,
                            notificationId,
                            filenameByUser,
                            fileTitle
                        ) }
                }
            }

            }
        }

        return super.onStartCommand(intent, flags, startId)
    }


    private fun cancelDownloadTask(notificationId: Int, filenameByUser: String?) {
        Log.e("cancelDownloadTask", "OK")

        // Find the download task with the matching notification ID
        val downloadTask = downloadTasksMap[notificationId]
        downloadTask?.cancel()
        downloadFileList.remove(filenameByUser)
        if(this::notificationManager.isInitialized)
            notificationManager.cancel(notificationId)

       /*if (downloadTasksMap.isEmpty()) {
            // If there are no more download tasks, stop the service
            stopSelf()
        }*/
    }


    private fun downloadFile(
        downloadUrl: String,
        destinationFolder: File,
        notificationId: Int,
        filenameByUser: String?,
        fileTitle: String?,
        isZipFile: Boolean
    ): Deferred<Boolean> =
        CoroutineScope(Dispatchers.IO).async {
            try {
                if(isZipFile)
                downloadAndExtract(
                    downloadUrl = downloadUrl,
                    destinationFolder = destinationFolder,
                    notificationId = notificationId,
                    filenameByUser = filenameByUser,
                    fileTitle = fileTitle
                )
                else
                    regularFile(
                        downloadUrl = downloadUrl,
                        destinationFolder = destinationFolder,
                        notificationId = notificationId,
                        filenameByUser = filenameByUser,
                        fileTitle = fileTitle
                    )

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("REgularDNFAILED",e.toString())
                false
            }
        }

    private suspend fun regularFile(
        downloadUrl: String,
        destinationFolder: File,
        notificationId: Int,
        filenameByUser: String?,
        fileTitle: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext false
            }

            val fileLength = connection.contentLength

            // Create the destination folder if it doesn't exist
            destinationFolder.mkdirs()

            // Determine the final destination file name
            val outputFileName = filenameByUser ?: "downloaded_file"
            val outputFile = File(destinationFolder, outputFileName)

            // Delete the existing file if it already exists
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // Create a temporary file in the destination folder
            val tempFileName = "$outputFileName.temp"
            val tempFile = File(destinationFolder, tempFileName)

            // Use a FileOutputStream to overwrite existing files
            val output = FileOutputStream(tempFile).buffered()

            val input = BufferedInputStream(url.openStream(), 8192)

            val data = ByteArray(1024)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                if (!isActive) {
                    // Coroutine has been canceled, break out of the loop
                    output.close()
                    input.close()
                    tempFile.delete()  // Clean up partially downloaded file
                    return@withContext false
                }

                total += count.toLong()

                if(total<fileLength){
                    updateNotification(
                        progress = (total * 100 / fileLength).toInt(),
                        notificationId = notificationId,
                        filenameByUser = filenameByUser,
                        fileTitle = fileTitle
                    )
                }

                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            // Delete the existing file before renaming the temporary file
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // Rename the temporary file to the final destination
            tempFile.renameTo(outputFile)

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("REgularDNFAILED",e.toString())
            return@withContext false
        }
    }


    private suspend fun downloadAndExtract(
        downloadUrl: String,
        destinationFolder: File,
        notificationId: Int,
        filenameByUser: String?,
        fileTitle: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext false
            }

            val fileLength = connection.contentLength

            // Modify the destination filename
            val zipFile = File(destinationFolder, "${generateUniqueNumber()}.zip")

            // Create the destination folder if it doesn't exist
            destinationFolder.mkdirs()

            // Use a FileOutputStream to overwrite existing files
            val output = FileOutputStream(zipFile).buffered()

            val input = BufferedInputStream(url.openStream(), 8192)

            val data = ByteArray(1024)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                if (!isActive) {
                    // Coroutine has been canceled, break out of the loop
                    output.close()
                    input.close()
                    zipFile.delete()  // Clean up partially downloaded file
                    return@withContext false
                }

                total += count.toLong()
                // Notify progress here
                if(total<fileLength){
                    updateNotification(
                        progress = (total * 100 / fileLength).toInt(),
                        notificationId = notificationId,
                        filenameByUser = filenameByUser,
                        fileTitle = fileTitle
                    )
                }

                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            extractZipFile(zipFile = zipFile, destinationFolder = destinationFolder)

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }



    private fun extractZipFile(zipFile: File, destinationFolder: File) {
        try {
            val zipInputStream = ZipInputStream(zipFile.inputStream())
            var zipEntry: ZipEntry?

            while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                val entryFile = File(destinationFolder, zipEntry!!.name)

                if (zipEntry!!.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile?.mkdirs()

                    // Check if the file already exists, and overwrite it
                    if (entryFile.exists()) {
                        entryFile.delete()
                    }

                    val output = FileOutputStream(entryFile)
                    val buffer = ByteArray(1024)
                    var count: Int

                    while (zipInputStream.read(buffer).also { count = it } != -1) {
                        output.write(buffer, 0, count)
                    }

                    output.close()
                }

                zipInputStream.closeEntry()
            }

            zipInputStream.close()
            zipFile.delete() // Delete the ZIP file after extraction
            CoroutineScope(Dispatchers.Main).launch {
                this@QuranDownloadService.toast("Quran download completed")
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private val NotificationChannelID = "downloader"

    private lateinit var notificationManager: NotificationManager


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete existing channel if it exists
            //notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
            // Create a new channel
            val channel = NotificationChannel(
                NotificationChannelID,
                "Downloader",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun createNotification(notificationId: Int): Notification {

        val cancelIntent = Intent(this, QuranDownloadService::class.java)
        cancelIntent.action = CANCEL_ACTION
        cancelIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        val cancelPendingIntent = PendingIntent.getService(
            this,
            notificationId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val cancelAction = NotificationCompat.Action(
            R.drawable.baseline_close_24,
            "Cancel",
            cancelPendingIntent
        )

        return NotificationCompat.Builder(this, NotificationChannelID)
            .setContentTitle("Downloading File")
            .setContentText("Download in progress")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setTicker("Downloading File")
            .setSound(null)
            .setOnlyAlertOnce(true)
            .setDefaults(0) // Set to 0 to disable sound and vibration
            .setOngoing(true) // Set to true to make the notification not cancelable
            .setProgress(100, 0, false)
            .addAction(cancelAction)
            .build()
    }

    private fun updateNotification(
        progress: Int,
        notificationId: Int,
        filenameByUser: String?,
        fileTitle: String?
    ) {

        /*val isFilenameExist = downloadFileList.containsKey(filenameByUser)

        if(!isFilenameExist){

            notificationManager.cancel(notificationId)
            return
        }*/

        callback = CallBackProvider.get<DownloaderCallback>()
        callback?.updateDownloadProgress(filenameByUser, progress, false,notificationId)

        filenameByUser?.let {
            downloadFileList[it] = progress
        }

        val cancelIntent = Intent(this, QuranDownloadService::class.java)
        cancelIntent.action = CANCEL_ACTION
        cancelIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        cancelIntent.putExtra(EXTRA_FILENAME, filenameByUser)
        val cancelPendingIntent = PendingIntent.getService(
            this,
            notificationId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val cancelAction = NotificationCompat.Action(
            R.drawable.baseline_close_24,
            "Cancel",
            cancelPendingIntent
        )


        val notificationBuilder = NotificationCompat.Builder(this, NotificationChannelID)
            .setContentTitle(fileTitle?:"Downloading File")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setTicker("Downloading File")
            .setContentText("Download in progress: $progress%")
            .setSound(null)
            .setOnlyAlertOnce(true)
            .setDefaults(0) // Set to 0 to disable sound and vibration
            .addAction(cancelAction)
            .setProgress(100, progress, false)

       /* if (progress == 100 ) {
            // If progress is 100%, set the notification to non-ongoing
            notificationBuilder.setOngoing(false)
            // Cancel the notification
            notificationManager.cancel(notificationId)
        } else {
            // If progress is not 100%, set the notification to ongoing
            notificationBuilder.setOngoing(true)
        }*/

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
    }


    private fun generateNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }


    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        //startForeground(NOTIFICATION_ID, createNotification(notificationId))
    }

    override fun onDestroy() {
        // Cancel all ongoing notifications when the service is destroyed
        notificationManager.cancelAll()
        super.onDestroy()
    }
}


