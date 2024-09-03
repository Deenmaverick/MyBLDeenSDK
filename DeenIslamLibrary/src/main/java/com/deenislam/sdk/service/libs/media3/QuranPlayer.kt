package com.deenislam.sdk.service.libs.media3

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.deenislam.sdk.DeenSDKCore
import com.deenislam.sdk.R
import com.deenislam.sdk.service.callback.AlQuranAyatCallback
import com.deenislam.sdk.service.callback.quran.QuranPlayerCallback
import com.deenislam.sdk.service.di.NetworkProvider
import com.deenislam.sdk.service.models.quran.quranplayer.OnlineQuranPlayer
import com.deenislam.sdk.service.network.ApiResource
import com.deenislam.sdk.service.network.response.quran.qurangm.ayat.Ayath
import com.deenislam.sdk.service.network.response.quran.qurangm.ayat.Qari
import com.deenislam.sdk.service.network.response.quran.qurangm.surahlist.Data
import com.deenislam.sdk.service.repository.quran.AlQuranRepository
import com.deenislam.sdk.utils.BASE_CONTENT_URL_SGP
import com.deenislam.sdk.utils.CallBackProvider
import com.deenislam.sdk.utils.deleteSingleFile
import com.deenislam.sdk.utils.getLocalContext
import com.deenislam.sdk.utils.numberLocale
import com.deenislam.sdk.utils.retrieveSingleFile
import com.deenislam.sdk.utils.tryCatch
import com.deenislam.sdk.views.adapters.quran.AlQuranAyatAdapter
import com.deenislam.sdk.views.main.MainActivityDeenSDK
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


internal class QuranPlayer: Service(){

    private val packageNameHash = "com.deenislam.sdk.mybl".hashCode()

    private var repository: AlQuranRepository = AlQuranRepository(
        deenService = NetworkProvider().getInstance().provideDeenService(),
        quranService = null
    )

    private val binder = LocalBinder()
    private var selectedQari = 931
    private var qarisData: ArrayList<Qari> = arrayListOf()
    private var audioFolderLocation = ""
    private var ayatList:ArrayList<Ayath> = arrayListOf()
    private var surahID:Int = 0
    private var juzID:Int = 0
    private var surahList:ArrayList<Data> = arrayListOf()
    private var currentlyPlayingPos:Int = -1
    private var mMediaPlayer: MediaPlayer? = null
    private var totalAyat = 0
    private var currentPageNo:Int = 0
    private var retryFetchNetwork = 0
    private var retryPlayAudio = 0
    private var curProgress = 0
    private var formattedTime = "0:00".numberLocale()

    private lateinit var mediaSession: MediaSessionCompat

    private var alQuranAyatCallback: AlQuranAyatCallback? = null
    private var apAdapterCallback:APAdapterCallback ? = null
    private var quranPlayerCallback: QuranPlayerCallback? = null
    private var globalQuranPlayerCallback: QuranPlayerCallback? = null
    private var countDownTimer: CountDownTimer?=null

    // juz (para)

    private var quranJuzList:ArrayList<com.deenislam.sdk.service.network.response.quran.qurangm.paralist.Data> = arrayListOf()
    private var quranJuz:com.deenislam.sdk.service.network.response.quran.qurangm.paralist.Data ? =null


    private val localReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action != null) {
                handleIntentAction(intent)
            }
        }
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var wakeLock: PowerManager.WakeLock? = null

    private var notificationUpdateState = 0
    private var isPlayerPlaying = false
    private var currentPlayingTime: Long = 0L // Variable to store elapsed time
    private var isSurahMode:Boolean = true

    override fun onCreate() {
        super.onCreate()

        // Acquire a WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuranPlayerSDK::WakeLock")
        wakeLock?.acquire()
    }

    inner class LocalBinder : Binder() {
        fun getService(): QuranPlayer {
            return this@QuranPlayer
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null) {
            handleIntentAction(intent)
        }

        //restoreState()

        isServiceRunning = true

      /*  // Create a MediaSession
        mediaSession = MediaSessionCompat(this, "Quran")

// Set the media session's playback state (adjust as needed)
        val playbackState = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)*/

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


        val afChangeListener = android.media.AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                android.media.AudioManager.AUDIOFOCUS_LOSS -> {
                    // Lost audio focus, pause or stop playback
                    // ...
                    pauseQuran(true)
                }
                android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Temporary loss of audio focus, pause playback
                    pauseQuran(true)
                    // ...
                }
                android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                    // Gained audio focus, resume or start playback
                    // ...
                    playPause()
                }
            }
        }



       /* val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val result = audioManager.requestAudioFocus(
            afChangeListener,
            android.media.AudioManager.STREAM_MUSIC,
            android.media.AudioManager.AUDIOFOCUS_GAIN
        )*/

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0 (Oreo) and newer
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val focusRequest = AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(afChangeListener)
                .build()

            val result = audioManager.requestAudioFocus(focusRequest)

            if (result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Start playback
                // ...
            }
        } else {
            // For versions prior to Android 8.0 (Oreo)
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                afChangeListener,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN
            )

            if (result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Start playback
                // ...
            }
        }



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


    fun updateAdapterCallback(viewHolder: AlQuranAyatAdapter.ViewHolder)
    {
        apAdapterCallback = viewHolder
    }

    fun updateQuranPlayer(qari: Int?=null) {

        if(qari!=null && qari!=1)
            selectedQari = qari

        Log.e("updateQuranPlayer",qari.toString())

    }

    fun setGlobalMiniPlayerCallback(callback: QuranPlayerCallback)
    {
        quranPlayerCallback = callback
    }

    fun initGlobalMiniPlayer(){
        if(isSurahMode)
            getCurrentSurahDetails()?.let { quranPlayerCallback?.updateSurahDetails(it) }
        else
            getCurrentJuzDetails()?.let { quranPlayerCallback?.updateJuzDetails(it) }

        if(isPlayerPlaying) {
            quranPlayerCallback?.isQuranPlaying(
                currentlyPlayingPos,
                mMediaPlayer?.duration?.toLong(),
                totalAyat
            )

            globalQuranPlayerCallback = CallBackProvider.get<QuranPlayerCallback>()
            globalQuranPlayerCallback?.isQuranPlaying(
                currentlyPlayingPos,
                mMediaPlayer?.duration?.toLong(),
                totalAyat
            )

        }
    }

    private fun createNotification(): NotificationCompat.Builder {

        val localeContext = this.getLocalContext()
        // custom notification view
        val miniCustomLayout = RemoteViews(packageName, R.layout.item_quran_player_notification)
        val largeCustomLayout = RemoteViews(packageName, R.layout.item_quran_player_large_notification)


        val notificationIntent = Intent(this, MainActivityDeenSDK::class.java)
        notificationIntent.putExtra("destination",R.id.action_global_dashboardFakeFragment)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if(mMediaPlayer?.isPlaying == true)
            isPlayerPlaying = true


        val currentSurahDetails = getCurrentSurahDetails()
        val currentJuzDetails = getCurrentJuzDetails()


        // mini player

        if (isPlayerPlaying)
            miniCustomLayout.setImageViewResource(R.id.ic_play_pause,R.drawable.ic_pause_fill)
        else
            miniCustomLayout.setImageViewResource(R.id.ic_play_pause,R.drawable.ic_play_fill)

        miniCustomLayout.setTextViewText(R.id.surahTitile,if(isSurahMode) currentSurahDetails?.SurahName else localeContext.getString(R.string.quran_para_adapter_title,currentJuzDetails?.JuzId.toString().numberLocale()))
        miniCustomLayout.setOnClickPendingIntent(R.id.icPrev, createActionIntent(ACTION_PREV))
        miniCustomLayout.setOnClickPendingIntent(R.id.ic_play_pause, if(isPlayerPlaying)createActionIntent(
            ACTION_PAUSE) else createActionIntent(ACTION_PLAY))
        miniCustomLayout.setOnClickPendingIntent(R.id.icNext, createActionIntent(ACTION_NEXT))


        // large player

        if (isPlayerPlaying)
            largeCustomLayout.setImageViewResource(R.id.ic_play_pause,R.drawable.ic_pause_fill)
        else
            largeCustomLayout.setImageViewResource(R.id.ic_play_pause,R.drawable.ic_play_fill)

        largeCustomLayout.setTextViewText(R.id.surahTitile,if(isSurahMode) currentSurahDetails?.SurahName else  localeContext.getString(R.string.quran_para_adapter_title,currentJuzDetails?.JuzId.toString().numberLocale()))
        largeCustomLayout.setOnClickPendingIntent(R.id.icPrev, createActionIntent(ACTION_PREV))
        largeCustomLayout.setOnClickPendingIntent(R.id.ic_play_pause, if(isPlayerPlaying)createActionIntent(
            ACTION_PAUSE) else createActionIntent(ACTION_PLAY))
        largeCustomLayout.setOnClickPendingIntent(R.id.icNext, createActionIntent(ACTION_NEXT))
        largeCustomLayout.setOnClickPendingIntent(R.id.icStop, createActionIntent(ACTION_STOP))
        largeCustomLayout.setTextViewText(R.id.currentTime,"0:00".numberLocale())

        notificationDarkLight(miniCustomLayout,largeCustomLayout)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, "Quran")
        } else {
            NotificationCompat.Builder(this)
        }
            .setContentTitle("Deen")
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSmallIcon(android.R.color.transparent)
            .setOngoing(true)
            .setCustomContentView(miniCustomLayout)
            .setCustomBigContentView(largeCustomLayout)  // For expanded view
            .setCustomHeadsUpContentView(miniCustomLayout)  // For heads-up notification

    }


    fun updateNotification() {

        if(!isServiceRunning)
            return

        val localeContext = this.getLocalContext()

        // custom notification view
        val miniCustomLayout = RemoteViews(packageName, R.layout.item_quran_player_notification)
        val largeCustomLayout = RemoteViews(packageName, R.layout.item_quran_player_large_notification)


        val notificationIntent = Intent(this, MainActivityDeenSDK::class.java)
        notificationIntent.putExtra("destination",R.id.action_global_dashboardFakeFragment)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if(mMediaPlayer?.isPlaying == true)
            isPlayerPlaying = true

        val currentSurahDetails = getCurrentSurahDetails()
        val currentJuzDetails = getCurrentJuzDetails()


        // mini player

        if (isPlayerPlaying)
            miniCustomLayout.setImageViewResource(R.id.ic_play_pause,R.drawable.ic_pause_fill)
        else
            miniCustomLayout.setImageViewResource(R.id.ic_play_pause,R.drawable.ic_play_fill)

        miniCustomLayout.setTextViewText(R.id.surahTitile,if(isSurahMode) currentSurahDetails?.SurahName else localeContext.getString(R.string.quran_para_adapter_title,currentJuzDetails?.JuzId.toString().numberLocale()))
        miniCustomLayout.setOnClickPendingIntent(R.id.icPrev, createActionIntent(ACTION_PREV))
        miniCustomLayout.setOnClickPendingIntent(R.id.ic_play_pause, if(isPlayerPlaying)createActionIntent(
            ACTION_PAUSE) else createActionIntent(ACTION_PLAY))
        miniCustomLayout.setOnClickPendingIntent(R.id.icNext, createActionIntent(ACTION_NEXT))


        // large player

        if (isPlayerPlaying)
            largeCustomLayout.setImageViewResource(R.id.ic_play_pause,R.drawable.ic_pause_fill)
        else
            largeCustomLayout.setImageViewResource(R.id.ic_play_pause,R.drawable.ic_play_fill)

        largeCustomLayout.setTextViewText(R.id.surahTitile,if(isSurahMode) currentSurahDetails?.SurahName else localeContext.getString(R.string.quran_para_adapter_title,currentJuzDetails?.JuzId.toString().numberLocale()))
        largeCustomLayout.setOnClickPendingIntent(R.id.icPrev, createActionIntent(ACTION_PREV))
        largeCustomLayout.setOnClickPendingIntent(R.id.ic_play_pause, if(isPlayerPlaying)createActionIntent(
            ACTION_PAUSE) else createActionIntent(ACTION_PLAY))
        largeCustomLayout.setOnClickPendingIntent(R.id.icNext, createActionIntent(ACTION_NEXT))
        largeCustomLayout.setOnClickPendingIntent(R.id.icStop, createActionIntent(ACTION_STOP))
        largeCustomLayout.setInt(R.id.playerProgress, "setProgress", curProgress);
        largeCustomLayout.setTextViewText(R.id.totalAyat,localeContext.resources.getString(R.string.quran_popular_surah_ayat,if(isSurahMode) currentSurahDetails?.TotalAyat?.numberLocale() else currentJuzDetails?.TotalAyat?.toString()?.numberLocale(),""))
        largeCustomLayout.setTextViewText(R.id.currentTime,formattedTime)

        notificationDarkLight(miniCustomLayout,largeCustomLayout)

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
            .setOngoing(true)
            .setCustomContentView(miniCustomLayout)
            .setCustomBigContentView(largeCustomLayout)  // For expanded view
            .setCustomHeadsUpContentView(miniCustomLayout)  // For heads-up notification

        // Notify the notification manager with the updated notification
        notificationManager.notify(packageNameHash, updatedNotificationBuilder.build())
    }


    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, QuranPlayerBroadcast::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    private fun getCurrentSurahDetails(): Data? {

        val getIndex = surahList.indexOfFirst { it.SurahId == surahID }

        return if(getIndex!=-1) {
            surahList[getIndex]
        } else
            null
    }

    private fun getCurrentJuzDetails(): com.deenislam.sdk.service.network.response.quran.qurangm.paralist.Data? {

        val getIndex = quranJuzList.indexOfFirst { it.JuzId == juzID }

        return if(getIndex!=-1) {
            quranJuzList[getIndex]
        } else
            null
    }

   /* override fun onTaskRemoved(rootIntent: Intent?) {
        saveState()
        super.onTaskRemoved(rootIntent)
    }*/

    /*override fun onDestroy() {
        saveState()
        *//*Log.e("QPSERVICE","DESTROY")
        isServiceRunning = false
        mediaSession.release()
        releaseMediaPlayer()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver)
        quranPlayerCallback?.isQuranStop()*//*
        super.onDestroy()
    }*/

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

                playNextSurahOrJuz()
            }
            ACTION_PREV -> {
                playPrevSurahOrJuz()
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
        globalQuranPlayerCallback = CallBackProvider.get<QuranPlayerCallback>()
        countDownTimer?.start()
        mMediaPlayer?.start()
        apAdapterCallback?.isPlaying(currentlyPlayingPos,mMediaPlayer?.duration?.toLong(),surahID)
        quranPlayerCallback?.isQuranPlaying(currentlyPlayingPos,mMediaPlayer?.duration?.toLong(),totalAyat)
        globalQuranPlayerCallback?.isQuranPlaying(currentlyPlayingPos,mMediaPlayer?.duration?.toLong(),totalAyat)
        isPlayerPlaying = true
        updateNotification()
    }

    fun getCurrentSurahID() = surahID
    fun getCurrentJuzID() = juzID

    fun playPause()
    {
        if(isPlayerPlaying)
            pauseQuran()
        else
            resumeQuran()
    }

    fun stopQuranPlayer() {

        isServiceRunning = false
        Log.d("QuranPlayerService", "Stopping service")

        pauseQuran(true)
        this.deleteSingleFile("player", "online_quran_player.json")
        // Release resources
        Log.d("QuranPlayerService", "Unregistering receiver")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver)

        // Cancel the CountDownTimer
        countDownTimer?.cancel()

        Log.d("QuranPlayerService", "Releasing media player")
        releaseMediaPlayer()

        notificationManager.cancelAll()

        tryCatch {
            wakeLock?.release()
        }

        // Stop the service
        Log.d("QuranPlayerService", "Stopping self")
        //stopSelf()


        // Stop foreground and remove notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }

        // Notify any callbacks about the service stop
        Log.d("QuranPlayerService", "Notifying callbacks")
        quranPlayerCallback?.isQuranStop()
        quranPlayerCallback = null

        Log.d("QuranPlayerService", "Service stopped")
    }


    fun playQuran(
        data: ArrayList<Ayath>,
        pos: Int,
        surahList: ArrayList<Data>,
        surahID: Int,
        qarisData: ArrayList<Qari>,
        totalVerseCount: Int,
        pageNo: Int,
        selectedQari: Int,
        isSurahMode: Boolean,
        quranJuzList: ArrayList<com.deenislam.sdk.service.network.response.quran.qurangm.paralist.Data>?,
        quranJuz: com.deenislam.sdk.service.network.response.quran.qurangm.paralist.Data?
    )
    {

        this.isSurahMode = isSurahMode
        currentPageNo = pageNo
        totalAyat = totalVerseCount

        ayatList.clear()

        //ayatList = ArrayList((ayatList + data).distinctBy { it.VerseId })
        ayatList.addAll(data)


        this.qarisData = qarisData
        this.surahID = surahID

        if(this.surahList.isEmpty() && surahList.isNotEmpty())
            this.surahList.addAll(surahList)

        if(this.quranJuzList.isEmpty() && quranJuzList!=null)
            this.quranJuzList.addAll(quranJuzList)

        if(isSurahMode)
            this.quranJuzList.clear()
        else
            this.surahList.clear()

        this.quranJuz = quranJuz
        quranJuz?.let {
            juzID = it.JuzId
        }

        currentlyPlayingPos = pos
        retryFetchNetwork = 0
        this.selectedQari = selectedQari

        isPlayerPlaying = true
        currentPlayingTime = 0
        curProgress = 0
        formattedTime = "0:00".numberLocale()
        updateNotification()

        playAudioFromUrl(getQuranAudioUrl("${BASE_CONTENT_URL_SGP}${data[pos].AudioUrl}",getQariWiseFolder()))
    }


    private fun playNextAyat() {
        val tempNextPos = currentlyPlayingPos + 1

        if ((tempNextPos) <= totalAyat - 1) {
            if (tempNextPos <= ayatList.size - 1) {

                currentlyPlayingPos = tempNextPos

                Log.e("playNextAyat", "Next ayat $tempNextPos") // Logging the tempNextPos instead of currentlyPlayingPos


                playAudioFromUrl(
                    getQuranAudioUrl(
                        "${BASE_CONTENT_URL_SGP}${ayatList[currentlyPlayingPos].AudioUrl}",
                        getQariWiseFolder()
                    )
                )

            } else {
                Log.e("playNextAyat", "Next Page")
                currentPageNo++
                processNextDataFromNetwork()
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                if(isSurahMode)
                    playNextSurah()
                else
                    playNextJuz()
            }
        }
    }

    fun playNextJuz()
    {
        //saveState()

        if(juzID >30) {
            pauseQuran(true)
            return
        }

        releaseMediaPlayer()
        currentPageNo = 1
        currentlyPlayingPos = -1
        currentPlayingTime = 0
        curProgress = 0
        formattedTime = "0:00".numberLocale()
        ayatList.clear()
        juzID++

        //Log.e("playNextSurah",surahID.toString())

        if(juzID <= 30) {
            processNextDataFromNetwork()
        }
        else {
            juzID = 30
            pauseQuran(true)
            return
        }

        isPlayerPlaying = true
        updateNotification()

        alQuranAyatCallback = CallBackProvider.get<AlQuranAyatCallback>()
        alQuranAyatCallback?.playNextJuz(true)
        getCurrentJuzDetails()?.let { quranPlayerCallback?.updateJuzDetails(it) }

    }

    fun playNextSurahOrJuz(){
        if(isSurahMode)
            playNextSurah()
        else
            playNextJuz()
    }

    fun playPrevSurahOrJuz(){
        if(isSurahMode)
            playPrevSurah()
        else
            playPrevJuz()
    }


    fun playNextSurah()
    {

        saveState()

        if(surahID+1>114)
        {
            pauseQuran(true)
            return
        }

        releaseMediaPlayer()
        currentPageNo = 1
        currentlyPlayingPos = -1
        currentPlayingTime = 0
        curProgress = 0
        formattedTime = "0:00".numberLocale()
        ayatList.clear()
        surahID++

        //Log.e("playNextSurah",surahID.toString())

        if(surahID <= 114) {
            processNextDataFromNetwork()
        }
        else {
            surahID = 144
            pauseQuran(true)
            return
        }

        isPlayerPlaying = true
        updateNotification()

        alQuranAyatCallback = CallBackProvider.get<AlQuranAyatCallback>()
        alQuranAyatCallback?.playNextSurah(true)
        getCurrentSurahDetails()?.let { quranPlayerCallback?.updateSurahDetails(it) }
    }

    fun playPrevSurah()
    {

        if(surahID-1<=0)
        {
            return
        }

        releaseMediaPlayer()
        currentPageNo = 1
        currentlyPlayingPos = -1
        currentPlayingTime = 0
        curProgress = 0
        formattedTime = "0:00".numberLocale()
        ayatList.clear()
        surahID--
        if(surahID > 0) {
            processNextDataFromNetwork()
        }
        else {
            surahID = 1
            pauseQuran(true)
            return
        }

        isPlayerPlaying = true
        updateNotification()

        alQuranAyatCallback = CallBackProvider.get<AlQuranAyatCallback>()
        alQuranAyatCallback?.playPrevSurah(true)
        getCurrentSurahDetails()?.let { quranPlayerCallback?.updateSurahDetails(it) }
    }

    fun playPrevJuz()
    {

        if(juzID-1<=0)
        {
            return
        }

        releaseMediaPlayer()
        currentPageNo = 1
        currentlyPlayingPos = -1
        currentPlayingTime = 0
        curProgress = 0
        formattedTime = "0:00".numberLocale()
        ayatList.clear()
        juzID--
        if(juzID > 0) {
            processNextDataFromNetwork()
        }
        else {
            juzID = 1
            pauseQuran(true)
            return
        }

        isPlayerPlaying = true
        updateNotification()

        alQuranAyatCallback = CallBackProvider.get<AlQuranAyatCallback>()
        alQuranAyatCallback?.playPrevJuz(true)
        getCurrentJuzDetails()?.let { quranPlayerCallback?.updateJuzDetails(it) }
    }

    private fun getQariWiseFolder(): String {
        val idsToFilter = setOf(selectedQari)
        audioFolderLocation = qarisData.filter { it.title in idsToFilter }.getOrNull(0)?.contentFolder.toString()

        Log.e("getQariWiseFolder",audioFolderLocation+selectedQari)
        return audioFolderLocation

    }

    private fun processNextDataFromNetwork()
    {
        CoroutineScope(Dispatchers.IO).launch {

            when(val response = if(isSurahMode){repository.getVersesByChapter(
                language = DeenSDKCore.GetDeenLanguage(),
                page = currentPageNo,
                contentCount = 10,
                chapter_number = surahID,
                isReadingMode = false
            )} else{
                repository.getVersesByJuz(
                    language = DeenSDKCore.GetDeenLanguage(),
                    page = currentPageNo,
                    contentCount = 10,
                    juz_number = juzID,
                    isReadingMode = false
                )
            })
            {
                is ApiResource.Failure -> {
                    if(retryFetchNetwork<5){
                        processNextDataFromNetwork()
                    }
                    retryFetchNetwork++
                }
                is ApiResource.Success -> {

                    if(response.value?.Success == true){
                        //ayatList = ArrayList( (ayatList + response.value.Data.Ayaths).distinctBy { it.VerseId })

                        if(response.value.Data.Ayaths.isNotEmpty())
                            ayatList.addAll(response.value.Data.Ayaths)
                        totalAyat = if(isSurahMode)
                            response.value.Data.SurahInfo?.TotalAyat?:0
                        else
                            response.value.Pagination.TotalData
                        playNextAyat()
                    }
                    else{
                        if(retryFetchNetwork<5){
                            processNextDataFromNetwork()
                        }
                        retryFetchNetwork++
                    }
                }
            }
        }
    }

    fun getQuranAudioUrl(url: String, folder: String): String {

        return url.replace("<folder>", folder, true)
    }

    fun playAudioFromUrl(url:String, position:Int = -1)
    {

        try {

            mMediaPlayer?.reset()

            if(mMediaPlayer == null)
                mMediaPlayer = MediaPlayer()


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
                setDataSource(url)
                prepareAsync()
                isPlayerPlaying = true
            }

            mMediaPlayer?.setOnPreparedListener {
                //alQuranAyatCallback = CallBackProvider.get<AlQuranAyatCallback>()
                mMediaPlayer?.start()
                apAdapterCallback?.isPlaying(currentlyPlayingPos, mMediaPlayer?.duration?.toLong(), surahID)
                if(notificationUpdateState == 0){
                    updateNotification()
                    notificationUpdateState = 1
                }
                playerProgress(currentlyPlayingPos,mMediaPlayer?.duration)

                globalQuranPlayerCallback = CallBackProvider.get<QuranPlayerCallback>()
                globalQuranPlayerCallback?.isQuranPlaying(currentlyPlayingPos, mMediaPlayer?.duration?.toLong(),totalAyat)
                quranPlayerCallback?.isQuranPlaying(currentlyPlayingPos, mMediaPlayer?.duration?.toLong(),totalAyat)

                //alQuranAyatCallback?.isAyatPlaying(currentlyPlayingPos,mMediaPlayer?.duration)

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
                                playAudioFromUrl(url, position)
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
            if(retryPlayAudio<5) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(3000)
                    playAudioFromUrl(url, position)
                }

                retryPlayAudio++
            }
            else
                pauseQuran(true)
            //releaseMediaPlayer()
        }

    }

    fun releaseMediaPlayer() {
        countDownTimer?.cancel()
        mMediaPlayer?.stop()
        mMediaPlayer?.release()
        mMediaPlayer = null
        apAdapterCallback?.isPause(currentlyPlayingPos,true)
        quranPlayerCallback?.isQuranPause()
    }

    fun playerProgress(position: Int, duration: Int?) {

        val progress:Double = 100.0/totalAyat

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

    private fun saveState(){

        val data = OnlineQuranPlayer(
            selectedQari = selectedQari,
            qarisData = qarisData,
            ayatList = ayatList,
            surahID = surahID,
            surahList = surahList,
            currentlyPlayingPos = currentlyPlayingPos,
            totalAyat = totalAyat,
            currentPageNo = currentPageNo,
            retryFetchNetwork = retryFetchNetwork,
            retryPlayAudio = retryPlayAudio,
            curProgress = curProgress,
            formattedTime = formattedTime,
            quranJuzList = quranJuzList
        )

        Log.e("QuransaveState",Gson().toJson(data))

        //this.saveSingleFile("online_quran_player.json","player/online_quran_player.json",Gson().toJson(data))
    }

    private fun restoreState(){

        val jsonData = this.retrieveSingleFile("player/online_quran_player.json")
        if (jsonData != null) {
            // Perform deserialization later using Gson
            val onlineQuranPlayer = Gson().fromJson(jsonData, OnlineQuranPlayer::class.java)

            selectedQari = onlineQuranPlayer.selectedQari
            qarisData = onlineQuranPlayer.qarisData
            ayatList = onlineQuranPlayer.ayatList
            surahID = onlineQuranPlayer.surahID
            surahList = onlineQuranPlayer.surahList
            currentlyPlayingPos = onlineQuranPlayer.currentlyPlayingPos
            totalAyat = onlineQuranPlayer.totalAyat
            currentPageNo = onlineQuranPlayer.currentPageNo
            retryFetchNetwork = onlineQuranPlayer.retryFetchNetwork
            retryPlayAudio = onlineQuranPlayer.retryPlayAudio
            curProgress = onlineQuranPlayer.curProgress
            formattedTime = onlineQuranPlayer.formattedTime
            quranJuzList = onlineQuranPlayer.quranJuzList

            // Continue processing with onlineQuranPlayer
        } else {
            // Handle the case when the file data is not available
            //stopQuranPlayer()
        }
    }

    private fun notificationDarkLight(
        miniCustomLayout: RemoteViews,
        largeCustomLayout: RemoteViews
    ) {
        val configuration: Configuration = resources.configuration

// Check if the current theme is in night mode
        val isDarkModeEnabled =
            configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        if (isDarkModeEnabled) {
            // Dark mode is enabled
            miniCustomLayout.setInt(R.id.surahTitile, "setTextColor", ContextCompat.getColor(this, R.color.deen_white))
            miniCustomLayout.setInt(R.id.icPrev, "setColorFilter", ContextCompat.getColor(this, R.color.deen_white))
            miniCustomLayout.setInt(R.id.ic_play_pause, "setColorFilter", ContextCompat.getColor(this, R.color.deen_white))
            miniCustomLayout.setInt(R.id.icNext, "setColorFilter", ContextCompat.getColor(this, R.color.deen_white))
            largeCustomLayout.setInt(R.id.icStop, "setColorFilter", ContextCompat.getColor(this, R.color.deen_white))

            largeCustomLayout.setInt(R.id.surahTitile, "setTextColor", ContextCompat.getColor(this, R.color.deen_white))
            largeCustomLayout.setInt(R.id.totalAyat, "setTextColor", ContextCompat.getColor(this, R.color.deen_white_70))
            largeCustomLayout.setInt(R.id.currentTime, "setTextColor", ContextCompat.getColor(this, R.color.deen_white_70))
            largeCustomLayout.setInt(R.id.icPrev, "setColorFilter", ContextCompat.getColor(this, R.color.deen_white))
            largeCustomLayout.setInt(R.id.ic_play_pause, "setColorFilter", ContextCompat.getColor(this, R.color.deen_white))
            largeCustomLayout.setInt(R.id.icNext, "setColorFilter", ContextCompat.getColor(this, R.color.deen_white))
            largeCustomLayout.setInt(R.id.icStop, "setColorFilter", ContextCompat.getColor(this, R.color.deen_white))


        } else {
            // Light mode is enabled

            miniCustomLayout.setInt(R.id.surahTitile, "setTextColor", ContextCompat.getColor(this, R.color.deen_txt_black_deep))
            miniCustomLayout.setInt(R.id.icPrev, "setColorFilter", ContextCompat.getColor(this, R.color.deen_txt_black_deep))
            miniCustomLayout.setInt(R.id.ic_play_pause, "setColorFilter", ContextCompat.getColor(this, R.color.deen_txt_black_deep))
            miniCustomLayout.setInt(R.id.icNext, "setColorFilter", ContextCompat.getColor(this, R.color.deen_txt_black_deep))
            largeCustomLayout.setInt(R.id.icStop, "setColorFilter", ContextCompat.getColor(this, R.color.deen_txt_black_deep))


            largeCustomLayout.setInt(R.id.surahTitile, "setTextColor", ContextCompat.getColor(this, R.color.deen_txt_black_deep))
            largeCustomLayout.setInt(R.id.totalAyat, "setTextColor", ContextCompat.getColor(this, R.color.deen_txt_ash))
            largeCustomLayout.setInt(R.id.currentTime, "setTextColor", ContextCompat.getColor(this, R.color.deen_txt_ash))
            largeCustomLayout.setInt(R.id.icPrev, "setColorFilter", ContextCompat.getColor(this, R.color.deen_txt_black_deep))
            largeCustomLayout.setInt(R.id.ic_play_pause, "setColorFilter", ContextCompat.getColor(this, R.color.deen_txt_black_deep))
            largeCustomLayout.setInt(R.id.icNext, "setColorFilter", ContextCompat.getColor(this, R.color.deen_txt_black_deep))
            largeCustomLayout.setInt(R.id.icStop, "setColorFilter", ContextCompat.getColor(this, R.color.deen_txt_black_deep))
        }
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
