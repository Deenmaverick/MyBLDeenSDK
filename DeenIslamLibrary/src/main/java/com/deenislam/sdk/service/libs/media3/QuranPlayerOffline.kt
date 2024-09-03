package com.deenislam.sdk.service.libs.media3

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.deenislam.sdk.R
import com.deenislam.sdk.service.callback.AlQuranAyatCallback
import com.deenislam.sdk.service.callback.quran.QuranPlayerCallback
import com.deenislam.sdk.service.network.response.quran.qurangm.surahlist.Data
import com.deenislam.sdk.utils.getLocalContext
import com.deenislam.sdk.utils.numberLocale
import com.deenislam.sdk.views.main.MainActivityDeenSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit


internal class QuranPlayerOffline: Service(){

    private val packageNameHash = "com.deenislam.sdk.mybl".hashCode()

    private val binder = LocalBinder()
    private var surahData: Data? = null
    private var surahID:Int = 0
    private var currentlyPlayingPos:Int = -1
    private var mMediaPlayer: MediaPlayer? = null
    private var retryPlayAudio = 0
    private var curProgress = 0
    private var formattedTime = "0:00".numberLocale()
    private lateinit var mediaSession: MediaSessionCompat

    private var alQuranAyatCallback: AlQuranAyatCallback? = null
    private var apAdapterCallback:APAdapterCallback ? = null
    private var quranPlayerCallback: QuranPlayerCallback? = null
    private var countDownTimer: CountDownTimer?=null
    private var isQuranPlayComplete = false

    private val localReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action != null) {
                handleIntentAction(intent)
            }
        }
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private var notificationUpdateState = 0
    private var isPlayerPlaying = false
    private var currentPlayingTime: Long = 0L // Variable to store elapsed time


    inner class LocalBinder : Binder() {
        fun getService(): QuranPlayerOffline {
            return this@QuranPlayerOffline
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            handleIntentAction(intent)
        }

        isServiceRunning = true

        // Create a MediaSession
        mediaSession = MediaSessionCompat(this, "Quran")

// Set the media session's playback state (adjust as needed)
        val playbackState = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)

        /*// Set callbacks for media buttons and transport controls
                mediaSession.setCallback(object : MediaSessionCompat.Callback() {
                    // Implement relevant callback methods for your media playback
                })*/


        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Initialize the notification builder
        notificationBuilder = createNotification()


        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREV)
            addAction(ACTION_STOP)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, filter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                packageNameHash,
                notificationBuilder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(packageNameHash, notificationBuilder.build())
        }

        return START_STICKY
    }



    fun setGlobalMiniPlayerCallback(callback: QuranPlayerCallback)
    {
        quranPlayerCallback = callback
    }

    fun initGlobalMiniPlayer(){
        surahData?.let { quranPlayerCallback?.updateSurahDetails(it) }

        if(isPlayerPlaying)
            quranPlayerCallback?.isQuranPlaying(currentlyPlayingPos, mMediaPlayer?.duration?.toLong(),surahData?.TotalAyat?.toInt()?:0)

    }

    private fun createNotification(): NotificationCompat.Builder {

        // custom notification view
        val miniCustomLayout = RemoteViews(packageName, R.layout.item_quran_player_notification)
        val largeCustomLayout = RemoteViews(packageName, R.layout.item_quran_player_large_notification)

        val notificationIntent = Intent(this, MainActivityDeenSDK::class.java)
        notificationIntent.putExtra("destination", R.id.action_global_dashboardFakeFragment)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if(mMediaPlayer?.isPlaying == true)
            isPlayerPlaying = true


        val currentSurahDetails = surahData

        // mini player

        if (isPlayerPlaying)
            miniCustomLayout.setImageViewResource(R.id.ic_play_pause, R.drawable.ic_pause_fill)
        else
            miniCustomLayout.setImageViewResource(R.id.ic_play_pause, R.drawable.ic_play_fill)

        miniCustomLayout.setTextViewText(R.id.surahTitile,currentSurahDetails?.SurahName)
        miniCustomLayout.setOnClickPendingIntent(R.id.icPrev, createActionIntent(ACTION_PREV))
        miniCustomLayout.setOnClickPendingIntent(
            R.id.ic_play_pause, if(isPlayerPlaying)createActionIntent(
            ACTION_PAUSE) else createActionIntent(ACTION_PLAY))
        miniCustomLayout.setOnClickPendingIntent(R.id.icNext, createActionIntent(ACTION_NEXT))
        miniCustomLayout.setViewVisibility(R.id.icPrev, View.GONE)
        miniCustomLayout.setViewVisibility(R.id.icNext, View.GONE)

        // large player

        if (isPlayerPlaying)
            largeCustomLayout.setImageViewResource(R.id.ic_play_pause, R.drawable.ic_pause_fill)
        else
            largeCustomLayout.setImageViewResource(R.id.ic_play_pause, R.drawable.ic_play_fill)

        largeCustomLayout.setTextViewText(R.id.surahTitile,currentSurahDetails?.SurahName)
        largeCustomLayout.setOnClickPendingIntent(R.id.icPrev, createActionIntent(ACTION_PREV))
        largeCustomLayout.setOnClickPendingIntent(
            R.id.ic_play_pause, if(isPlayerPlaying)createActionIntent(
            ACTION_PAUSE) else createActionIntent(ACTION_PLAY))
        largeCustomLayout.setOnClickPendingIntent(R.id.icNext, createActionIntent(ACTION_NEXT))
        largeCustomLayout.setOnClickPendingIntent(R.id.icStop, createActionIntent(ACTION_STOP))
        largeCustomLayout.setTextViewText(R.id.currentTime,"0:00".numberLocale())
        largeCustomLayout.setViewVisibility(R.id.icPrev, View.GONE)
        largeCustomLayout.setViewVisibility(R.id.icNext, View.GONE)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, "Quran")
        } else {
            NotificationCompat.Builder(this)
        }
            .setContentTitle("Deen")
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSmallIcon(android.R.color.transparent)
            .setCustomContentView(miniCustomLayout)
            .setCustomBigContentView(largeCustomLayout)  // For expanded view
            .setCustomHeadsUpContentView(miniCustomLayout)  // For heads-up notification

    }


    fun updateNotification() {

        if(!isServiceRunning)
            return

        // custom notification view
        val miniCustomLayout = RemoteViews(packageName, R.layout.item_quran_player_notification)
        val largeCustomLayout = RemoteViews(packageName, R.layout.item_quran_player_large_notification)

        val notificationIntent = Intent(this, MainActivityDeenSDK::class.java)
        notificationIntent.putExtra("destination", R.id.action_global_dashboardFakeFragment)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if(mMediaPlayer?.isPlaying == true)
            isPlayerPlaying = true

        val currentSurahDetails = surahData


        // mini player

        if (isPlayerPlaying)
            miniCustomLayout.setImageViewResource(R.id.ic_play_pause, R.drawable.ic_pause_fill)
        else
            miniCustomLayout.setImageViewResource(R.id.ic_play_pause, R.drawable.ic_play_fill)

        miniCustomLayout.setTextViewText(R.id.surahTitile,currentSurahDetails?.SurahName)
        miniCustomLayout.setOnClickPendingIntent(R.id.icPrev, createActionIntent(ACTION_PREV))
        miniCustomLayout.setOnClickPendingIntent(
            R.id.ic_play_pause, if(isPlayerPlaying)createActionIntent(
            ACTION_PAUSE) else createActionIntent(ACTION_PLAY))
        miniCustomLayout.setOnClickPendingIntent(R.id.icNext, createActionIntent(ACTION_NEXT))
        miniCustomLayout.setViewVisibility(R.id.icPrev, View.GONE)
        miniCustomLayout.setViewVisibility(R.id.icNext, View.GONE)

        // large player

        if (isPlayerPlaying)
            largeCustomLayout.setImageViewResource(R.id.ic_play_pause, R.drawable.ic_pause_fill)
        else
            largeCustomLayout.setImageViewResource(R.id.ic_play_pause, R.drawable.ic_play_fill)

        largeCustomLayout.setTextViewText(R.id.surahTitile,currentSurahDetails?.SurahName)
        largeCustomLayout.setOnClickPendingIntent(R.id.icPrev, createActionIntent(ACTION_PREV))
        largeCustomLayout.setOnClickPendingIntent(
            R.id.ic_play_pause, if(isPlayerPlaying)createActionIntent(
            ACTION_PAUSE) else createActionIntent(ACTION_PLAY))
        largeCustomLayout.setOnClickPendingIntent(R.id.icNext, createActionIntent(ACTION_NEXT))
        largeCustomLayout.setOnClickPendingIntent(R.id.icStop, createActionIntent(ACTION_STOP))
        largeCustomLayout.setInt(R.id.playerProgress, "setProgress", curProgress);
        largeCustomLayout.setTextViewText(
            R.id.totalAyat,this.getLocalContext().resources.getString(
                R.string.quran_popular_surah_ayat,currentSurahDetails?.TotalAyat?.numberLocale(),""))
        largeCustomLayout.setTextViewText(R.id.currentTime,formattedTime)
        largeCustomLayout.setViewVisibility(R.id.icPrev, View.GONE)
        largeCustomLayout.setViewVisibility(R.id.icNext, View.GONE)

        // Create a new instance of the NotificationCompat.Builder with updated actions
        val updatedNotificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, "Quran")
        } else {
            NotificationCompat.Builder(this)
        }
            //.setStyle(mediaStyle)
            .setContentTitle("Al Quran")
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSmallIcon(android.R.color.transparent)
            //.setContentText(getCurrentSurahDetails())
            .setCustomContentView(miniCustomLayout)
            .setCustomBigContentView(largeCustomLayout)  // For expanded view
            .setCustomHeadsUpContentView(miniCustomLayout)  // For heads-up notification

        // Notify the notification manager with the updated notification
        notificationManager.notify(packageNameHash, updatedNotificationBuilder.build())
    }

    fun formatNumber(number: Int): String {
        // Format the number with leading zeros
        return String.format(Locale.ENGLISH,"%03d", number)
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, QuranPlayerBroadcast::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }


    // Handle media actions based on the received intent
    private fun handleIntentAction(intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY -> {
                resumeQuran()
            }
            ACTION_PAUSE -> {
                pauseQuran(true)
            }
            ACTION_NEXT -> {

                //playNextSurah()
            }
            ACTION_PREV -> {
                //playPrevSurah()
            }

            ACTION_STOP -> {

                stopQuranPlayer()
            }
        }
    }


    fun pauseQuran(byService: Boolean=false)
    {
        countDownTimer?.cancel()
        mMediaPlayer?.pause()
        isPlayerPlaying = false
        apAdapterCallback?.isPause(currentlyPlayingPos,byService)
        quranPlayerCallback?.isQuranPause()
        updateNotification()
    }


    fun resumeQuran(){
        countDownTimer?.start()
        mMediaPlayer?.start()
        apAdapterCallback?.isPlaying(currentlyPlayingPos,mMediaPlayer?.duration?.toLong(),surahID)
        quranPlayerCallback?.isQuranPlaying(currentlyPlayingPos,mMediaPlayer?.duration?.toLong(),surahData?.TotalAyat?.toInt()?:0)
        isPlayerPlaying = true
        updateNotification()
    }

    fun getCurrentSurahID() = surahData?.SurahId?:0

    fun playPause()
    {

        Log.e("currentlyPlayingPos",currentlyPlayingPos.toString())

        if(isQuranPlayComplete){

            isQuranPlayComplete = false
            surahData?.let {
                playQuran(it)
                return
            }

            return
        }

        if(isPlayerPlaying)
            pauseQuran()
        else
            resumeQuran()
    }

    fun stopQuranPlayer() {

        isServiceRunning = false
        Log.d("QuranPlayerService", "Stopping service")

        pauseQuran(true)
        // Release resources
        Log.d("QuranPlayerService", "Unregistering receiver")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver)

        Log.d("QuranPlayerService", "Releasing media player")

        Log.d("QuranPlayerService", "Releasing media session")
        if(this::mediaSession.isInitialized)
        mediaSession.release()

        releaseMediaPlayer()


        notificationManager.cancelAll()

        // Notify any callbacks about the service stop
        Log.d("QuranPlayerService", "Notifying callbacks")

        // Stop foreground and remove notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }

        quranPlayerCallback?.isQuranStop()
        quranPlayerCallback = null
    }


    fun playQuran(data: Data) {

        surahData = data

        isPlayerPlaying = true
        currentPlayingTime = 0
        currentlyPlayingPos = -1
        curProgress = 0
        formattedTime = "0:00".numberLocale()
        updateNotification()
        //val audioFile = File(this.filesDir,data.folderLocation+"/"+formatNumber(data.SurahId)+"001.mp3")
        val audioFile = File(data.folderLocation, "${formatNumber(data.SurahId)}${formatNumber(1)}.mp3")

        playRawAudioFile(this,audioFile)
        //playAudioFromUrl(getQuranAudioUrl("$BASE_CONTENT_URL_SGP${data[pos].AudioUrl}",getQariWiseFolder()))
    }


    private fun playNextAyat() {
        val tempNextPos = currentlyPlayingPos + 1
        surahData?.let {
            if(/*it.TotalAyat.toInt() > tempNextPos && */tempNextPos+1 < it.TotalAyat.toInt()){
                currentlyPlayingPos = tempNextPos
                val audioFile = File(it.folderLocation, "${formatNumber(it.SurahId)}${formatNumber(tempNextPos+2)}.mp3")
                //val audioFile = File(this.filesDir,it.folderLocation+"/"+formatNumber(it.SurahId)+formatNumber(currentlyPlayingPos)+".mp3")
                playRawAudioFile(this,audioFile)
            }
            else {
                isQuranPlayComplete = true
                pauseQuran()
            }
        }?:pauseQuran()

    }


    fun playRawAudioFile(context: Context, file: File)
    {
        try {

            mMediaPlayer?.reset()
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer()
            }

            val uri = Uri.fromFile(file)

            mMediaPlayer?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                    setAudioAttributes(attributes)
                } else {
                    setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)  // Deprecated method, but necessary for older devices
                }
                setDataSource(context,uri)
                prepareAsync()
                isPlayerPlaying = true
            }

            mMediaPlayer?.setOnPreparedListener {
                mMediaPlayer?.start()
                apAdapterCallback?.isPlaying(currentlyPlayingPos, mMediaPlayer?.duration?.toLong(), surahID)
                if(notificationUpdateState == 0){
                    updateNotification()
                    notificationUpdateState = 1
                }
                playerProgress(currentlyPlayingPos+1,mMediaPlayer?.duration)
                quranPlayerCallback?.isQuranPlaying(currentlyPlayingPos+1, mMediaPlayer?.duration?.toLong(),surahData?.TotalAyat?.toInt()?:0)

            }

            mMediaPlayer?.setOnErrorListener { mp, what, extra ->

                when (what) {
                    MediaPlayer.MEDIA_ERROR_UNKNOWN,
                    MediaPlayer.MEDIA_ERROR_SERVER_DIED,
                    MediaPlayer.MEDIA_ERROR_IO,
                    MediaPlayer.MEDIA_ERROR_MALFORMED,
                    MediaPlayer.MEDIA_ERROR_UNSUPPORTED,
                    MediaPlayer.MEDIA_ERROR_TIMED_OUT-> {
                        if(retryPlayAudio<5) {
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(3000)
                                playRawAudioFile(context,file)
                            }

                            retryPlayAudio++
                        }
                        else
                        {
                            if(isPlayerPlaying)
                                pauseQuran(true)
                        }
                    }

                }

                true
            }

            mMediaPlayer?.setOnCompletionListener {

                //isPlayerPlaying = false
                playNextAyat()
                if(currentlyPlayingPos>=0){
                    apAdapterCallback?.isComplete(currentlyPlayingPos-1,surahID)
                }

            }

        }
        catch (e:Exception)
        {
            Log.e("OfflinePlayerExc",e.toString())
            if(retryPlayAudio<5) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(3000)
                    playRawAudioFile(context,file)
                }

                retryPlayAudio++
            }
            else
                pauseQuran(true)
        }
    }

    fun releaseMediaPlayer() {
        countDownTimer?.cancel()
        mMediaPlayer?.stop()
        mMediaPlayer?.release()
        mMediaPlayer = null
        apAdapterCallback?.isPause(currentlyPlayingPos,true)
        quranPlayerCallback?.isQuranStop()
    }

    fun playerProgress(position: Int, duration: Int?) {

        val progress:Double = (100.0 / surahData?.TotalAyat?.toDouble()!!)

        var currentProgress = progress * position

        if(currentProgress <= 0.0)
            currentProgress = 0.5
        else if(currentProgress > 100.0)
            currentProgress = 100.0

        countDownTimer?.cancel()
        countDownTimer =
            duration?.let {

                object : CountDownTimer(duration.toLong(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {

                        val progressPerSecond =
                            progress / TimeUnit.MILLISECONDS.toSeconds(duration.toLong())
                        currentProgress += progressPerSecond

                        currentPlayingTime +=  1000


                        curProgress = currentProgress.toInt()

                        if (currentProgress <= 0.0)
                            currentProgress = 0.5
                        else if (currentProgress > 100.0)
                            currentProgress = 100.0

                        // currentPlayingTime += 1000

                        val seconds = (currentPlayingTime / 1000).toInt()
                        val minutes = seconds / 60
                        val remainingSeconds = seconds % 60

                        // Format the time
                        formattedTime =
                            String.format("%d:%02d", minutes, remainingSeconds).numberLocale()

                        updateNotification()

                    }

                    override fun onFinish() {
                        countDownTimer?.cancel()
                    }
                }
            }
        countDownTimer?.start()

    }

    companion object {
        const val ACTION_PLAY = "play_action"
        const val ACTION_PAUSE = "pause_action"
        const val ACTION_NEXT = "next_action"
        const val ACTION_PREV = "prev_action"
        const val ACTION_STOP = "stop_action"
        @Volatile
        var isServiceRunning = false
            private set
    }

}