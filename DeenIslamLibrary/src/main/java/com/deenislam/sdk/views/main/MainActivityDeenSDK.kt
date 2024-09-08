package com.deenislam.sdk.views.main

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.deenislam.sdk.DeenSDKCore
import com.deenislam.sdk.R
import com.deenislam.sdk.service.callback.quran.QuranPlayerCallback
import com.deenislam.sdk.service.libs.media3.AudioManager
import com.deenislam.sdk.service.libs.media3.ExoVideoManager
import com.deenislam.sdk.service.libs.media3.QuranPlayer
import com.deenislam.sdk.service.libs.media3.QuranPlayerBroadcast
import com.deenislam.sdk.service.libs.media3.QuranPlayerOffline
import com.deenislam.sdk.service.libs.notification.AlarmReceiver
import com.deenislam.sdk.service.libs.sessiontrack.SessionReceiver
import com.deenislam.sdk.service.network.response.prayertimes.PrayerTimesResponse
import com.deenislam.sdk.service.network.response.quran.qurangm.ayat.Ayath
import com.deenislam.sdk.service.network.response.quran.qurangm.ayat.Qari
import com.deenislam.sdk.service.network.response.quran.qurangm.surahlist.Data
import com.deenislam.sdk.service.weakref.dashboard.DashboardBillboardPatchClass
import com.deenislam.sdk.service.weakref.dashboard.DashboardPatchClass
import com.deenislam.sdk.service.weakref.main.MainActivityInstance
import com.deenislam.sdk.utils.*
import com.deenislam.sdk.utils.DraggableView
import com.deenislam.sdk.views.adapters.quran.AlQuranAyatAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import java.util.concurrent.TimeUnit


internal class MainActivityDeenSDK : AppCompatActivity(), QuranPlayerCallback {

    private lateinit var navController:NavController
    private var actionCallback:actionCallback ? =null
    private var searchCallback:searchCallback ? =null

    lateinit var localContext:Context

    private var trackingID:Long = 0
    private var sessionStartTime:Long = System.currentTimeMillis()/1000

    private val _viewPager: ViewPager2 by lazy { findViewById(R.id.viewPager) }
     private lateinit var frameContainerView: FragmentContainerView

    private val searchbar:ConstraintLayout by lazy { findViewById(R.id.searchbar) }
    private val searchBackBtn:AppCompatImageView by lazy { searchbar.findViewById(R.id.btnBack) }
    private val searchInput: TextInputEditText by lazy { searchbar.findViewById(R.id.search_input) }


    private val actionbar:ConstraintLayout by lazy { findViewById(R.id.actionbar) }
    private val action1Btn:AppCompatImageView by lazy { actionbar.findViewById(R.id.action1) }
    private val action2Btn:AppCompatImageView by lazy { actionbar.findViewById(R.id.action2) }
    private val btnBack:AppCompatImageView by lazy { actionbar.findViewById(R.id.btnBack) }
    private val title:AppCompatTextView by lazy { actionbar.findViewById(R.id.title) }

    var childFragmentAnimForward:Boolean = false
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private var isDisableBackPress = false

    private var isQuranPlayerBound = false
    private var isQuranOfflinePlayerBound = false

    // Quran player basic data
    private var quranQueuedData: ArrayList<Ayath> = arrayListOf()
    private var quranQueuedPos: Int = -1
    private var quranQueuedSurahList: ArrayList<Data> = arrayListOf()
    private var quranQueuedJuzList:ArrayList<com.deenislam.sdk.service.network.response.quran.qurangm.paralist.Data> = arrayListOf()
    private var quranQueuedJuz:com.deenislam.sdk.service.network.response.quran.qurangm.paralist.Data ? =null
    private var quranQueuedSurahID: Int = -1
    private var quranQueuedQarisData: ArrayList<Qari> = arrayListOf()
    private var quranQueuedTotalVerseCount: Int = -1
    private var quranQueuedPageNo: Int = -1
    private var quranQueuedSelectedQari: Int = 931
    private var quranQueuedIsSurahMode: Boolean = true
    private var isQuranPlayInQueue = false
    private var quranPlayerAdapterCallback: AlQuranAyatAdapter.ViewHolder ? = null
    private var isQuranPlayerWasRunning = false

    // offline
    private var quranSurahQueuedData: Data? = null

    // global mini player
    private lateinit var mini_player: DraggableView
    private lateinit var surahTitile:AppCompatTextView
    private lateinit var surahAyat:AppCompatTextView
    private lateinit var ic_prev:AppCompatImageView
    private lateinit var ic_play_pause:AppCompatImageView
    private lateinit var ic_next:AppCompatImageView
    private lateinit var ic_close:AppCompatImageView
    private lateinit var playerProgress: LinearProgressIndicator
    private lateinit var playLoading: CircularProgressIndicator
    private var currentSurahDetails: Data? = null
    private var currentJuzDetails: com.deenislam.sdk.service.network.response.quran.qurangm.paralist.Data? = null
    private var countDownTimer: CountDownTimer?=null
    // Mini player drag element
    private var initialX: Float = 0f

    // Global video and Audio manager
    private var globalExoVideoManager: ExoVideoManager? = null
    private var globalAudioManager: AudioManager? = null

    // floating view ramadan
    private lateinit var ramadanRemainCard:ConstraintLayout
    private lateinit var ramadanRemainTxt:AppCompatTextView
    private lateinit var floatGestureDetector: GestureDetector
    private lateinit var ramadanCloseBtn:AppCompatImageView
    private lateinit var ramadanTxt:AppCompatTextView
    private var isRamadanRemainCardClosed = false
    private lateinit var ramadanCustomAlertDialogView : View
    private var ramadanExpectedTimeInMill:Long = 0
    private var isRamadanRunning = false
    private var ramadanDate:String=""
    private var ramadanCountDownTimer: CountDownTimer?=null


    // Custom dialog
    private lateinit var materialAlertDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var customAlertDialogView : View
    private var dialog: Dialog? = null

    // pip mode
    private var xOffset = 0f
    private var yOffset = 0f

    companion object
    {
        var instance: MainActivityDeenSDK? = null
    }


    private val quranOnlineserviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as QuranPlayer.LocalBinder
            MainActivityInstance.updateQuranPlayer(binder.getService())
            isQuranPlayerBound = true
            if(isQuranPlayInQueue)
            {

                isQuranPlayInQueue = false

                MainActivityInstance.getQuranPlayerInstance()?.playQuran(
                    data = quranQueuedData,
                    pos = quranQueuedPos,
                    surahList = quranQueuedSurahList,
                    surahID = quranQueuedSurahID,
                    qarisData = quranQueuedQarisData,
                    totalVerseCount = quranQueuedTotalVerseCount,
                    pageNo = quranQueuedPageNo,
                    selectedQari = quranQueuedSelectedQari,
                    isSurahMode = quranQueuedIsSurahMode,
                    quranJuzList = quranQueuedJuzList,
                    quranJuz = quranQueuedJuz
                )

                quranPlayerAdapterCallback?.let { setAdapterCallbackQuranPlayer(it) }
            }

            MainActivityInstance.getQuranPlayerInstance()?.setGlobalMiniPlayerCallback(this@MainActivityDeenSDK)
            MainActivityInstance.getQuranPlayerInstance()?.initGlobalMiniPlayer()

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isQuranPlayerBound = false
            mini_player.hide()
        }
    }


    private val quranOfflineserviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as QuranPlayerOffline.LocalBinder
            MainActivityInstance.updateQuranPlayerOffline(binder.getService())
            isQuranOfflinePlayerBound = true
            if(isQuranPlayInQueue)
            {
                isQuranPlayInQueue = false

                quranSurahQueuedData?.let {
                    MainActivityInstance.getQuranPlayerOfflineInstance()?.playQuran(
                        data = it,
                    )
                }
            }

            MainActivityInstance.getQuranPlayerOfflineInstance()?.setGlobalMiniPlayerCallback(this@MainActivityDeenSDK)
            MainActivityInstance.getQuranPlayerOfflineInstance()?.initGlobalMiniPlayer()

            (mini_player.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = 0
            mini_player.show()
            ic_prev.hide()
            ic_next.hide()
            mini_player.post {
                frameContainerView.setPadding(0,0,0,mini_player.height)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isQuranOfflinePlayerBound = false
            mini_player.hide()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        setContentView(R.layout.activity_main_deen)
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        frameContainerView = findViewById(R.id.fragmentContainerView)
        navController = findNavController(R.id.fragmentContainerView)

        // quran global mini player
        mini_player = findViewById(R.id.mini_player)
        surahTitile = mini_player.findViewById(R.id.surahTitile)
        surahAyat = mini_player.findViewById(R.id.surahAyat)
        ic_prev = mini_player.findViewById(R.id.ic_prev)
        ic_play_pause = mini_player.findViewById(R.id.ic_play_pause)
        ic_next = mini_player.findViewById(R.id.ic_next)
        ic_close = mini_player.findViewById(R.id.ic_close)
        playerProgress = mini_player.findViewById(R.id.playerProgress)
        playLoading = mini_player.findViewById(R.id.playLoading)

        //ramadan floating card
        ramadanRemainCard = findViewById(R.id.ramadanRemainCard)
        ramadanRemainTxt = findViewById(R.id.ramadanRemainTxt)
        ramadanCloseBtn = findViewById(R.id.ramadanCloseBtn)
        ramadanTxt = findViewById(R.id.ramadanTxt)
        ramadanTxt.text = this.getLocalContext().getString(R.string.ramadan_remaining)

        frameContainerView.visible(true)
        changeLanguage()
        //bottom_navigation.setupWithNavController(navController)
        searchInput.hint = localContext.getString(R.string.search)
        // test notification
        createChannel("Prayer Time","Prayer Time","Prayer Alert")
        createSilentChannel("Quran","Quran","Quran Player")
        //Log.e("CUR_TIME_NOTIFY",Calendar.getInstance().timeInMillis.toString())
      /*  setNotification(  SystemClock.elapsedRealtime()+600,1)
        setNotification(SystemClock.elapsedRealtime()+1200,2)
*/

        //action button click
        action1Btn.setOnClickListener {
            actionCallback?.action1()
        }

        action2Btn.setOnClickListener {
            actionCallback?.action2()
        }

        searchBackBtn.setOnClickListener {
            searchCallback?.searchBack()
        }


        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchCallback?.searchSubmit(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchInput.setOnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2

            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (searchInput.right - searchInput.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                    // Right drawable clicked
                    // Add your desired action here
                    searchCallback?.searchSubmit(searchInput.text.toString())
                    return@setOnTouchListener true
                }
            }
            false
        }


        // Global mini player

        ic_play_pause.setOnClickListener {

            MainActivityInstance.getQuranPlayerInstance()?.playPause()
            MainActivityInstance.getQuranPlayerOfflineInstance()?.playPause()
        }

        ic_prev.setOnClickListener {
            MainActivityInstance.getQuranPlayerInstance()?.playPrevSurahOrJuz()
        }

        ic_next.setOnClickListener {
            MainActivityInstance.getQuranPlayerInstance()?.playNextSurahOrJuz()
        }


        mini_player.setDragReleaseCallback(object : DraggableView.DragReleaseCallback {
            override fun onDragReleased() {
                val callback = CallBackProvider.get<QuranPlayerCallback>()
                MainActivityInstance.getQuranPlayerInstance()?.stopQuranPlayer()
                MainActivityInstance.getQuranPlayerOfflineInstance()?.stopQuranPlayer()
                callback?.globalMiniPlayerClosed()
            }

            override fun onMiniQuranPlayerClicked() {
                currentSurahDetails?.let {
                    val bundle = Bundle()
                    bundle.putInt("surahID", it.SurahId)
                    bundle.putString("surahName", it.SurahName)
                    navController.navigate(R.id.action_global_alQuranFragment,bundle)
                }
            }
        })

        if (QuranPlayer.isServiceRunning) {
            isQuranPlayerWasRunning = true
            startQuranPlayerService()
        }

        if (QuranPlayerOffline.isServiceRunning) {
            isQuranPlayerWasRunning = true
            startQuranPlayerOfflineService()
        }

        floatGestureDetector = GestureDetector(this, FloatCardGestureListener())


        initNavChangeObsever()

        mini_player.post {
            initialX = mini_player.x
        }

        ramadanRemainCard.setOnTouchListener { _, event ->
            floatGestureDetector.onTouchEvent(event)
            handlePiPTouch(event)
        }

        intent.getIntExtra("destination",0).let {
            if(it>0)
                stackNavigation(destination = it,intent)
        }

        intent.getStringExtra("logout").let {

            if(it == "ok") {
                logout()
            }
        }

        setupBackPressCallback()

    }

    // dragable mini player
    private fun handlePiPTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                xOffset = event.rawX - ramadanRemainCard.x
                yOffset = event.rawY - ramadanRemainCard.y
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = event.rawX - xOffset
                val newY = event.rawY - yOffset

                ramadanRemainCard.x = newX
                ramadanRemainCard.y = newY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        if(!isDisableBackPress) {
            setupBackPressCallback()
        }

        sessionStartTime = System.currentTimeMillis()/1000

    }

    override fun isQuranPlaying(position: Int, duration: Long?, totalAyat: Int) {
        globalAudioManager?.pauseMediaPlayer()
        globalExoVideoManager?.pauseVideoPlayer()
        ic_play_pause.show()
        playLoading.hide()

        ic_play_pause.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_pause_fill
            )
        )

        val progress:Double = 100.0/totalAyat

        var currentProgress = progress * position

        if(currentProgress <= 0.0)
            currentProgress = 0.5
        else if(currentProgress > 100.0)
            currentProgress = 100.0


        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer((duration?:0L).toLong(),1000) {
            override fun onTick(millisUntilFinished: Long) {

                duration?.let {
                    val progressPerSecond = progress  / TimeUnit.MILLISECONDS.toSeconds(duration.toLong())
                    currentProgress +=progressPerSecond

                    val curProgress = currentProgress.toInt()

                    if(currentProgress <= 0.0)
                        currentProgress = 0.5
                    else if(currentProgress > 100.0)
                        currentProgress = 100.0

                    playerProgress.progress = curProgress

                }
            }

            override fun onFinish() {
                countDownTimer?.cancel()
            }
        }
        countDownTimer?.start()
    }

    override fun isQuranPause() {
        ic_play_pause.show()
        playLoading.hide()
        countDownTimer?.cancel()
        ic_play_pause.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_quran_play_fill
            )
        )
    }

    override fun isQuranStop() {

        stopQuranPlayerService()
        stopQuranOfflinePlayerService()


        ic_play_pause.show()
        playLoading.hide()

        countDownTimer?.cancel()
        ic_play_pause.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_quran_play_fill
            )
        )
        mini_player.hide()
        val callback = CallBackProvider.get<QuranPlayerCallback>()
        callback?.globalMiniPlayerClosed()
        frameContainerView.setPadding(0,0,0,0)
    }

    override fun updateSurahDetails(currentSurahDetails: Data) {

        ic_play_pause.visibility = View.INVISIBLE
        playLoading.show()
        countDownTimer?.cancel()
        ic_play_pause.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_quran_play_fill
            )
        )
        playerProgress.progress = 0
        this.currentSurahDetails = currentSurahDetails
        surahTitile.text = currentSurahDetails.SurahName
        surahAyat.text = this.getLocalContext().resources.getString(R.string.quran_popular_surah_ayat,currentSurahDetails.TotalAyat.numberLocale(),"")
        /*if(navController.currentDestination?.id != R.id.alQuranFragment)
        mini_player.show()
        else
            mini_player.hide()*/
    }

    override fun updateJuzDetails(currentJuzDetails: com.deenislam.sdk.service.network.response.quran.qurangm.paralist.Data) {

        val localeContext = this.getLocalContext()

        ic_play_pause.visibility = View.INVISIBLE
        playLoading.show()
        countDownTimer?.cancel()
        ic_play_pause.setImageDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_quran_play_fill
            )
        )
        playerProgress.progress = 0
        this.currentJuzDetails = currentJuzDetails


        surahTitile.text = localeContext.getString(R.string.quran_para_adapter_title,currentJuzDetails.JuzId.toString().numberLocale())
        surahAyat.text = localeContext.resources.getString(R.string.quran_popular_surah_ayat,currentJuzDetails.TotalAyat.toString().numberLocale(),"")
        /*if(navController.currentDestination?.id != R.id.alQuranFragment)
        mini_player.show()
        else
            mini_player.hide()*/
    }

    private fun stopQuranPlayerService() {
        if (isQuranPlayerBound) {
            unbindService(quranOnlineserviceConnection)
            isQuranPlayerBound = false
        }

        val intent = Intent(this, QuranPlayer::class.java)
        stopService(intent)
    }

    private fun stopQuranOfflinePlayerService() {
        if (isQuranOfflinePlayerBound) {
            unbindService(quranOfflineserviceConnection)
            isQuranOfflinePlayerBound = false
        }

        val intent = Intent(this, QuranPlayerOffline::class.java)
        stopService(intent)
    }

    fun closeDeenSDK()
    {
        finish()
    }
 /*   override fun onBackPressed() {
        if(this::onBackPressedCallback.isInitialized)
        onBackPressedCallback.handleOnBackPressed()
    }*/


    private fun setupBackPressCallback()
    {
        onBackPressedCallback =
            this.onBackPressedDispatcher.addCallback {
                Log.e("setupBackPressCallback",navController.previousBackStackEntry?.destination?.id.toString())
                // Handle the back button event
                if (navController.previousBackStackEntry?.destination?.id?.equals(
                        navController.graph.startDestinationId
                    ) != true &&
                    navController.previousBackStackEntry?.destination?.id?.equals(
                        R.id.blankFragment
                    ) != true && navController.previousBackStackEntry?.destination?.id != null)
                    navController.popBackStack()
                /*else
                    navController.popBackStack()*/

            }
        onBackPressedCallback.isEnabled = true

    }

    fun playOfflineQuran(
        data: Data
    ) {


        if(isQuranPlayerBound){
            MainActivityInstance.getQuranPlayerInstance()?.stopQuranPlayer()
        }

        if (isQuranOfflinePlayerBound && MainActivityInstance.getQuranPlayerOfflineInstance() != null) {
            // Call methods on the audioService as needed
            MainActivityInstance.getQuranPlayerOfflineInstance()?.playQuran(
                data = data
            )

            (mini_player.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = 0
            mini_player.show()
            ic_prev.hide()
            ic_next.hide()
            mini_player.post {
                frameContainerView.setPadding(0,0,0,mini_player.height)
            }
        }
        else
        {
            quranSurahQueuedData = data
            isQuranPlayInQueue = true
            startQuranPlayerOfflineService()
        }
    }

    private fun startQuranPlayerOfflineService()
    {
        // Start and bind to the quran player service
        val intent = Intent(this, QuranPlayerOffline::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(Intent(this, QuranPlayerOffline::class.java))
        }
        bindService(intent, quranOfflineserviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun isQuranMiniPlayerRunning(): Boolean {
        return if(QuranPlayer.isServiceRunning)
            true
        else QuranPlayerOffline.isServiceRunning
    }

    fun stopOfflineQuran(surahId: Int) {

        if(surahId == 0)
            return

        if(MainActivityInstance.getQuranPlayerOfflineInstance()?.getCurrentSurahID() == surahId)
            MainActivityInstance.getQuranPlayerOfflineInstance()?.stopQuranPlayer()
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
    ) {

        if(isQuranOfflinePlayerBound){
            MainActivityInstance.getQuranPlayerOfflineInstance()?.stopQuranPlayer()
        }

        if (isQuranPlayerBound && MainActivityInstance.getQuranPlayerInstance() != null) {
            // Call methods on the audioService as needed
            MainActivityInstance.getQuranPlayerInstance()?.playQuran(
                data = data,
                pos = pos,
                surahList = surahList,
                surahID = surahID,
                qarisData = qarisData,
                totalVerseCount = totalVerseCount,
                pageNo = pageNo,
                selectedQari = selectedQari,
                isSurahMode = isSurahMode,
                quranJuzList = quranJuzList,
                quranJuz = quranJuz
            )
        }
        else
        {
            quranQueuedData = data
            quranQueuedPos = pos
            quranQueuedSurahList = surahList
            quranQueuedJuzList = quranJuzList?: arrayListOf()
            quranQueuedJuz = quranJuz
            quranQueuedSurahID = surahID
            quranQueuedQarisData = qarisData
            quranQueuedTotalVerseCount = totalVerseCount
            quranQueuedPageNo = pageNo
            quranQueuedIsSurahMode = isSurahMode
            quranQueuedSelectedQari = selectedQari
            isQuranPlayInQueue = true
            startQuranPlayerService()
        }
    }

    private fun startQuranPlayerService()
    {
        // Start and bind to the quran player service
        val intent = Intent(this, QuranPlayer::class.java)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(Intent(this, QuranPlayer::class.java))
        }
        bindService(intent, quranOnlineserviceConnection, Context.BIND_AUTO_CREATE)
    }


    fun updateQuranPlayer(qari: Int?=null) {
        if (isQuranPlayerBound && MainActivityInstance.getQuranPlayerInstance() != null) {
            MainActivityInstance.getQuranPlayerInstance()?.updateQuranPlayer(qari)
        }
    }

    fun getCurrentSurahID(): Int {
        return if (isQuranPlayerBound && MainActivityInstance.getQuranPlayerInstance() != null) {
            // Call methods on the audioService as needed
            MainActivityInstance.getQuranPlayerInstance()?.getCurrentSurahID()?:0
        } else
            0
    }

    fun getCurrentJuzID(): Int {
        return if (isQuranPlayerBound && MainActivityInstance.getQuranPlayerInstance() != null) {
            // Call methods on the audioService as needed
            MainActivityInstance.getQuranPlayerInstance()?.getCurrentJuzID()?:0
        } else
            0
    }

    fun globalMiniPlayerForHome(height: Int) {

        if (QuranPlayer.isServiceRunning) {
            //val bottomNavHeight = resources.getDimensionPixelSize(com.google.android.material.R.dimen.design_bottom_navigation_height)
            (mini_player.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = height
            mini_player.show()
            ic_next.show()
            ic_prev.show()

        }
        else if (QuranPlayerOffline.isServiceRunning) {
            //val bottomNavHeight = resources.getDimensionPixelSize(com.google.android.material.R.dimen.design_bottom_navigation_height)
            (mini_player.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = height
            mini_player.show()
            ic_next.hide()
            ic_prev.hide()
        }
    }

    fun pauseQuran()
    {
        val intent = Intent(this, QuranPlayerBroadcast::class.java)
        intent.action = "pause_action"
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        pendingIntent.send()
    }

    fun stopQuran()
    {
        val intent = Intent(this, QuranPlayerBroadcast::class.java)
        intent.action = "stop_action"
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        pendingIntent.send()
    }

    fun getMiniPlayerHeight(): Int {
        return if(QuranPlayer.isServiceRunning)
            mini_player.height
        else if(QuranPlayerOffline.isServiceRunning)
            mini_player.height
        else 0
    }

    fun setAdapterCallbackQuranPlayer(viewHolder: AlQuranAyatAdapter.ViewHolder)
    {
        if (isQuranPlayerBound && MainActivityInstance.getQuranPlayerInstance() != null) {
            // Call methods on the audioService as needed
            MainActivityInstance.getQuranPlayerInstance()?.updateAdapterCallback(viewHolder)
        }
        else{
            quranPlayerAdapterCallback = viewHolder
        }
    }


    override fun onPause() {
        sendSessionToServer()

        Handler(Looper.getMainLooper()).post {
            if (this::onBackPressedCallback.isInitialized) {
                onBackPressedCallback.isEnabled = false
                onBackPressedCallback.remove()
            }

        }

        val callback = CallBackProvider.get<QuranPlayerCallback>()
        callback?.globalMiniPlayerClosed()

        super.onPause()
    }
    override fun onDestroy() {

        if(this::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.isEnabled = false
            onBackPressedCallback.remove()
        }

        val callback = CallBackProvider.get<QuranPlayerCallback>()
        MainActivityInstance.getQuranPlayerInstance()?.stopQuranPlayer()
        MainActivityInstance.getQuranPlayerOfflineInstance()?.stopQuranPlayer()
        callback?.globalMiniPlayerClosed()

        DashboardPatchClass.clearReferences()
        DashboardBillboardPatchClass.clearReferences()
        super.onDestroy()

    }

    private fun sendSessionToServer()
    {
      /*  val service = Intent(this, SessionReceiverService::class.java)
        service.putExtra("msisdn",DeenSDKCore.GetDeenMsisdn())
        service.putExtra("sessionStart",sessionStartTime)
        service.putExtra("sessionEnd",System.currentTimeMillis()/1000)
        this.startService(service)
*/
        Log.e("SDKDEEN_DES","OK")

        val notifyIntent = Intent(DeenSDKCore.appContext, SessionReceiver::class.java)
        notifyIntent.putExtra("msisdn",DeenSDKCore.GetDeenMsisdn())
        notifyIntent.putExtra("sessionStart",sessionStartTime)
        notifyIntent.putExtra("sessionEnd",System.currentTimeMillis()/1000)

        val notifyPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                DeenSDKCore.appContext,
                200,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

        val alarmManager = DeenSDKCore.appContext?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000,
            notifyPendingIntent
        )


    }

    fun changeLanguage()
    {
        localContext = LocaleUtil.createLocaleContext(this, Locale("bn"))
    /*if (DeenSDKCore.GetDeenLanguage() == "en") {
            LocaleUtil.createLocaleContext(this, Locale("en"))
        } else {
            LocaleUtil.createLocaleContext(this, Locale("bn"))
        }*/
    }

    fun disableBackPress()
    {
        isDisableBackPress = true
        if(this::onBackPressedCallback.isInitialized)
            onBackPressedCallback.isEnabled = false
    }

    fun enableBackPress()
    {
        isDisableBackPress = false
        Log.e("enableBackPress",this::onBackPressedCallback.isInitialized.toString())
        setupBackPressCallback()
    }

    fun getTrackingID() = trackingID
    fun setTrackingID(id:Long)
    {
        trackingID =id
    }



    private fun createChannel(channelId: String, channelName: String,description:String) {
        //create a channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                // change importance of notication
                NotificationManager.IMPORTANCE_HIGH
            )//disable badges for this channel
                .apply {
                    setShowBadge(false)
                }

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = description

            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }

    private fun createSilentChannel(channelId: String, channelName: String,description:String) {
        //create a channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                // change importance of notication
                NotificationManager.IMPORTANCE_LOW
            )//disable badges for this channel
                .apply {
                    setShowBadge(false)
                }

            notificationChannel.enableLights(false)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(false)
            notificationChannel.description = description

            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }

    private fun setNotification(time:Long,reqcode:Int)
    {
        val notifyIntent = Intent(this, AlarmReceiver::class.java)
        notifyIntent.putExtra("pid",reqcode)

        val notifyPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                this,
                reqcode,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            time,
            notifyPendingIntent
        )
    }

    fun isDashboardVisible() = _viewPager.isVisible

     fun dashboardComponent(bol:Boolean)
    {
        _viewPager.visible(bol)
        //bottom_navigation.visible(bol)
        actionbar.visible(bol)
        if (bol) {
            setupActionbar(localContext.resources.getString(R.string.app_name))
        }

        frameContainerView.post {
            frameContainerView.visible(!bol)
        }
    }

    fun setupOtherFragment(bol:Boolean)
    {
        _viewPager.visible(!bol)
        frameContainerView.visible(bol)
        actionbar.visible(!bol)
       // bottom_navigation.visible(!bol)

        Log.e("setupOtherFragment",bol.toString())
    }

     fun logout()
    {
       /* lifecycleScope.launch(Dispatchers.Main)
        {
            instance?.toast("Session expired! Please login again.")
            clearAllInstance()
            dashboardComponent(false)
            stackNavigation(destination = R.id.loginFragment)
        }*/

    }


    fun setViewPager(page:Int)
    {
        //instance?.bottomNavClicked = true
        childFragmentAnimForward = _viewPager.currentItem<page
        _viewPager.setCurrentItem(page,false)
    }

    //dashboard
    fun setupActionbar(titile:String,isBackVisible:Boolean = true,dashboard:Boolean)
    {
        title.text = titile

        if(!isBackVisible)
        btnBack.visible(isBackVisible)

        btnBack.setOnClickListener {
           onBackPressedDispatcher.onBackPressed()
        }
    }

    fun setupActionbar(titile:String,backEnable:Boolean=false)
    {
        if(titile.isEmpty() || titile == localContext.resources.getString(R.string.app_name) && !backEnable) {
            title.text = localContext.resources.getString(R.string.app_name)
            btnBack.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.bl_islamic_icon))
            btnBack.visible(true)

            btnBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        else if(backEnable)
        {
            btnBack.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.baseline_keyboard_backspace_24))
            btnBack.visible(true)
            btnBack.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            title.text = titile
            title.setTextColor(ContextCompat.getColor(title.context,R.color.deen_txt_black_deep))

        }
        else
        {

            (title.layoutParams as ConstraintLayout.LayoutParams).apply {
                leftMargin=16.dp
            }

            title.text = titile
            btnBack.visible(false)

        }
    }


    fun setupActionbar(titile:String,view: View,isBackVisible:Boolean = true)
    {
        val btnBack:ImageButton = view.findViewById(R.id.btnBack)
        val title:AppCompatTextView = view.findViewById(R.id.title)

        title.text = titile

        if(!isBackVisible)
            (title.layoutParams as ConstraintLayout.LayoutParams).apply {
                marginStart = 16.dp
            }

        btnBack.visible(isBackVisible)

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }


    fun setupActionbar(action1:Int,action2:Int,callback: actionCallback?=null)
    {
        if(action1>0) {
            action1Btn.setImageDrawable(AppCompatResources.getDrawable(this, action1))
            action1Btn.visible(true)
        }
        else
            action1Btn.visible(false)

        if(action2>0) {
            action2Btn.setImageDrawable(AppCompatResources.getDrawable(this, action2))
            action2Btn.visible(true)
        }
        else
            action2Btn.visible(false)

        actionCallback = callback
    }

    fun setupSearchbar(callback: searchCallback?)
    {
        searchCallback = callback
        actionbar.hide()
        searchbar.show()
    }

    fun hideSearch()
    {
        searchCallback = null
        //actionbar.show()
        searchbar.hide()
    }

    fun setupActionbar(action1:Int,action2:Int)
    {
        if(action1>0) {
            action1Btn.setImageDrawable(AppCompatResources.getDrawable(this, action1))
            action1Btn.visible(true)
        }
        else
            action1Btn.visible(false)

        if(action2>0) {
            action2Btn.setImageDrawable(AppCompatResources.getDrawable(this, action2))
            action2Btn.visible(true)
        }
        else
            action2Btn.visible(false)

    }

    fun showBottomNav(bol:Boolean)
    {
       // bottom_navigation.visible(bol)
    }

    private fun navigateDestination(destination:Int)
    {
        navController.navigate(destination)
    }

    private fun stackNavigation(destination:Int,intent: Intent)
    {
        //showBottomNav(destination.isBottomNavFragment)
        when(destination)
        {
            R.id.dashboardFakeFragment ->
            {

                navController.navigate(destination,intent.extras)

            }
            else ->
            {
                navController.navigate(R.id.dashboardFakeFragment)
                navController.navigate(destination,intent.extras)

            }
        }

    }


    private fun initNavChangeObsever() {
        val destinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->


            if(destination.id == R.id.dashboardFakeFragment){
                if(!isRamadanRemainCardClosed && (ramadanExpectedTimeInMill>0 || isRamadanRunning))
                    ramadanRemainCard.show()
                else
                    ramadanRemainCard.hide()
            }
            else
                ramadanRemainCard.hide()


            when (destination.id) {

                R.id.alQuranFragment -> {
                    mini_player.hide()
                    frameContainerView.setPadding(0,0,0,0)
                }

                R.id.dashboardFakeFragment ->{

                    frameContainerView.setPadding(0,0,0,0)
                }

                else -> {

                    if (QuranPlayer.isServiceRunning) {
                        (mini_player.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = 0
                        //mini_player.show()
                        ic_prev.show()
                        ic_next.show()
                        mini_player.post {
                            frameContainerView.setPadding(0,0,0,mini_player.height)
                        }

                    }
                    else if(QuranPlayerOffline.isServiceRunning) {
                        (mini_player.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = 0
                        //mini_player.show()
                        ic_prev.hide()
                        ic_next.hide()
                        mini_player.post {
                            frameContainerView.setPadding(0,0,0,mini_player.height)
                        }

                    }
                    else{
                        frameContainerView.setPadding(0,0,0,0)
                    }
                }
            }
        }

        navController.addOnDestinationChangedListener(destinationChangedListener)
    }


    private inner class FloatCardGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // Handle click event here
            return if (isTouchCloseBtn(e.x, e.y)) {
                // Handle ImageView click event here
                isRamadanRemainCardClosed = true
                ramadanRemainCard.hide()
                true
            } else{
                ramadanRemainCard.hide()
                showRamadanRemainDialog()
                true
            }

        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        private fun isTouchCloseBtn(x: Float, y: Float): Boolean {
            // Calculate ImageView's bounds relative to the parent ConstraintLayout
            val imageViewBounds = Rect()
            ramadanCloseBtn.getGlobalVisibleRect(imageViewBounds)
            val parentLayoutLocation = IntArray(2)
            ramadanRemainCard.getLocationOnScreen(parentLayoutLocation)
            imageViewBounds.offset(-parentLayoutLocation[0], -parentLayoutLocation[1])

            // Check if touch coordinates are within ImageView's bounds
            return imageViewBounds.contains(x.toInt(), y.toInt())
        }

    }


    private fun showRamadanRemainDialog(){

        val localContext = LocaleUtil.createLocaleContext(this, Locale(DeenSDKCore.GetDeenLanguage()))

        val themedContext = ContextThemeWrapper(localContext, R.style.DeenSDKTheme) // Replace with your theme

        val localInflater = layoutInflater.cloneInContext(themedContext)

        if(ramadanExpectedTimeInMill>0 && !isRamadanRunning) {
            materialAlertDialogBuilder =
                MaterialAlertDialogBuilder(this, R.style.DeenMaterialAlertDialog_Rounded)

            ramadanCustomAlertDialogView =
                localInflater.inflate(R.layout.dialog_ramadan_remaining_time, null, false)

            val dayleftTxt:AppCompatTextView = ramadanCustomAlertDialogView.findViewById(R.id.dayleftTxt)
            val timerCountTxt:AppCompatTextView = ramadanCustomAlertDialogView.findViewById(R.id.timerCountTxt)
            val progress:CircularProgressIndicator = ramadanCustomAlertDialogView.findViewById(R.id.progress)
            val okBtn: MaterialButton = ramadanCustomAlertDialogView.findViewById(R.id.okBtn)

            val progressTotalTime = ramadanExpectedTimeInMill.getMillis30DaysRamadan()+ramadanExpectedTimeInMill
            val totaltime = if(progressTotalTime>0) progressTotalTime else System.currentTimeMillis()
            val progressPercentage = ((ramadanExpectedTimeInMill.toDouble() / totaltime) * 100).toInt()
            progress.progress = progressPercentage

            val remainDay = ramadanExpectedTimeInMill.DayDiffForRamadan().toInt()
            val remainingDaysEng = when {
                remainDay == 1 -> "01 day"
                else -> "$remainDay days"
            }

            val remainingDaysBn = when {
                remainDay == 1 -> "০১ দিন"
                else -> "$remainDay দিন".numberLocale()
            }

            timerCountTxt.text =  ramadanExpectedTimeInMill.TimeDiffForRamadan().numberLocale()
            dayleftTxt.text = if(DeenSDKCore.GetDeenLanguage() == "en") remainingDaysEng else remainingDaysBn


            dialog = materialAlertDialogBuilder
                .setView(ramadanCustomAlertDialogView)
                .setCancelable(true)
                .setOnCancelListener {
                    if(!isRamadanRemainCardClosed)
                        ramadanRemainCard.show()
                    else
                        ramadanRemainCard.hide()
                }
                .show()


            okBtn.setOnClickListener {
                isRamadanRemainCardClosed = true
                dialog?.dismiss()
                navController.navigate(R.id.action_global_ramadanOtherDayFragment)
                ramadanRemainCard.hide()
                isRamadanRemainCardClosed = false
            }
        }else
            navController.navigate(R.id.action_global_ramadanFragment)
    }

    fun ramadanCountDownTimerSetup(ramadanExpectedTimeInMill: Long, date: String) {

        if(ramadanRemainCard.isShown || isRamadanRemainCardClosed) {
            return
        }

        ramadanDate = date

        ramadanCountDownTimer?.cancel()
        ramadanCountDownTimer = object : CountDownTimer(ramadanExpectedTimeInMill, 60000) {
            override fun onTick(millisUntilFinished: Long) {

                this@MainActivityDeenSDK.ramadanExpectedTimeInMill = millisUntilFinished
                tryCatch {
                    val remainDay = millisUntilFinished.DayDiffForRamadan().toInt()
                    val remainingDaysEng = when {
                        remainDay == 1 -> "01 day"
                        else -> "$remainDay days"
                    }

                    val remainingDaysBn = when {
                        remainDay == 1 -> "০১ দিন"
                        else -> "$remainDay দিন".numberLocale()
                    }

                    ramadanRemainTxt.text = if(DeenSDKCore.GetDeenLanguage() == "en") remainingDaysEng else remainingDaysBn

                    if(this@MainActivityDeenSDK::ramadanCustomAlertDialogView.isInitialized){
                        val dayleftTxt:AppCompatTextView = ramadanCustomAlertDialogView.findViewById(R.id.dayleftTxt)
                        val timerCountTxt:AppCompatTextView = ramadanCustomAlertDialogView.findViewById(R.id.timerCountTxt)
                        val progress:CircularProgressIndicator = ramadanCustomAlertDialogView.findViewById(R.id.progress)

                        timerCountTxt.text =  millisUntilFinished.TimeDiffForRamadan().numberLocale()
                        dayleftTxt.text = if(DeenSDKCore.GetDeenLanguage() == "en") remainingDaysEng else remainingDaysBn

                        val progressTotalTime = ramadanExpectedTimeInMill.getMillis30DaysRamadan()+ramadanExpectedTimeInMill
                        val totaltime = if(progressTotalTime>0) progressTotalTime else System.currentTimeMillis()

                        val progressPercentage = ((ramadanExpectedTimeInMill.toDouble() / totaltime) * 100).toInt()
                        progress.progress = progressPercentage



                    }
                }

            }
            override fun onFinish() {
                ramadanCountDownTimer?.cancel()
                ramadanRemainCard.hide()
                isRamadanRemainCardClosed = true
            }
        }
        ramadanCountDownTimer?.start()
        isRamadanRemainCardClosed = false
        if( navController.currentDestination?.id == R.id.dashboardFakeFragment)
        ramadanRemainCard.show()
    }

    fun showRamadanDayFloatingCard(ramadan: String, prayerTimesResponse: PrayerTimesResponse) {

        if (ramadanRemainCard.isShown || isRamadanRemainCardClosed) {
            return
        }
        isRamadanRunning = true
        ramadanTxt.text = localContext.getString(R.string.ramadan_mubarak)
        ramadanRemainTxt.text = ramadan
        isRamadanRemainCardClosed = false
        if( navController.currentDestination?.id == R.id.dashboardFakeFragment)
            ramadanRemainCard.show()

    }

}

internal interface actionCallback
{
    fun action1()
    fun action2()
}
internal interface searchCallback
{
    fun searchBack()
    fun searchSubmit(query: String)
}