package com.deenislam.sdk.views.dashboard

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deenislam.sdk.DeenSDKCore
import com.deenislam.sdk.R
import com.deenislam.sdk.databinding.FragmentDashboardBinding
import com.deenislam.sdk.service.callback.AdvertisementCallback
import com.deenislam.sdk.service.callback.Allah99NameCallback
import com.deenislam.sdk.service.callback.DashboardPatchCallback
import com.deenislam.sdk.service.callback.RamadanCallback
import com.deenislam.sdk.service.callback.ViewInflationListener
import com.deenislam.sdk.service.callback.quran.QuranPlayerCallback
import com.deenislam.sdk.service.di.DatabaseProvider
import com.deenislam.sdk.service.di.NetworkProvider
import com.deenislam.sdk.service.libs.advertisement.Advertisement
import com.deenislam.sdk.service.models.CommonResource
import com.deenislam.sdk.service.models.DashboardResource
import com.deenislam.sdk.service.models.prayer_time.PrayerNotificationResource
import com.deenislam.sdk.service.models.prayer_time.PrayerTimeResource
import com.deenislam.sdk.service.models.ramadan.StateModel
import com.deenislam.sdk.service.network.ApiResource
import com.deenislam.sdk.service.network.response.dashboard.Data
import com.deenislam.sdk.service.network.response.dashboard.Item
import com.deenislam.sdk.service.network.response.prayertimes.PrayerTimesResponse
import com.deenislam.sdk.service.repository.DashboardRepository
import com.deenislam.sdk.service.repository.PrayerTimesRepository
import com.deenislam.sdk.utils.*
import com.deenislam.sdk.utils.transformDashboardItemForKhatamQuran
import com.deenislam.sdk.viewmodels.DashboardViewModel
import com.deenislam.sdk.viewmodels.PrayerTimesViewModel
import com.deenislam.sdk.views.adapters.common.gridmenu.MenuCallback
import com.deenislam.sdk.views.adapters.dashboard.DashboardPatchAdapter
import com.deenislam.sdk.views.adapters.dashboard.PrayerTimeCallback
import com.deenislam.sdk.views.adapters.dashboard.TYPE_WIDGET11
import com.deenislam.sdk.views.adapters.dashboard.TYPE_WIDGET7
import com.deenislam.sdk.views.base.BaseFragment
import com.deenislam.sdk.views.main.MainActivityDeenSDK
import com.deenislam.sdk.views.main.actionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


internal class DashboardFragment(private var customargs: Bundle?) : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate),
    actionCallback, MenuCallback, PrayerTimeCallback, ViewInflationListener,
    DashboardPatchCallback,
    SensorEventListener,
    QuranPlayerCallback,
    Allah99NameCallback,
    RamadanCallback, AdvertisementCallback{

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var prayerViewModel:  PrayerTimesViewModel
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var dashboardPatchMain: DashboardPatchAdapter
    private var prayerdate: String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())

    private var prayerTrackLastWakt = ""
    private var currentState = "dhaka"

    // pagging locally dashboard data
    private var dashboardData:List<Data> ? = null
    private var hasMoreData = true
    private var itemsToLoadAhead = 6
    private var lastVisibleItemPosition = 0
    // Compass
    private var compassBG = ""
    private lateinit var mSensorManager: SensorManager
    private var isPageShowing = false
    private var locationListener: LocationListener? = null
    private var bearing: Double? = null
    private var locationManager: LocationManager? = null
    private var isLocationEnabledDialogShow: Boolean = false
    override fun OnCreate() {
        super.OnCreate()

       /* isHomePage(true)
        setupBackPressCallback(this)
*/
        CallBackProvider.setFragment(this)

        val dashboardRepository = DashboardRepository(authenticateService = NetworkProvider().getInstance().provideAuthService())

        val prayerTimesRepository = PrayerTimesRepository(
            deenService = NetworkProvider().getInstance().provideDeenService(),
            prayerNotificationDao = DatabaseProvider().getInstance().providePrayerNotificationDao(),
            prayerTimesDao = DatabaseProvider().getInstance().providePrayerTimesDao()
        )

        val factory = VMFactory(dashboardRepository,prayerTimesRepository)
        dashboardViewModel = ViewModelProvider(
            requireActivity(),
            factory
        )[DashboardViewModel::class.java]

        val factoryPrayer = VMFactoryPrayer(prayerTimesRepository)
        prayerViewModel = ViewModelProvider(
            requireActivity(),
            factoryPrayer
        )[PrayerTimesViewModel::class.java]


    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }


    private var prayerTimesResponse:PrayerTimesResponse?=null

    private var firstload:Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        loadingState()
        ViewCompat.setTranslationZ(binding.progressLayout.root, 10F)
        ViewCompat.setTranslationZ(binding.noInternetLayout.root, 10F)
        binding.noInternetLayout.root.isClickable = true
        binding.progressLayout.root.isClickable = true

        binding.dashboardMain.itemAnimator = null


        binding.dashboardMain.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = linearLayoutManager.childCount
                val totalItemCount = linearLayoutManager.itemCount
                val firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition()

                if (firstVisibleItem < lastVisibleItemPosition) {
                    // Scrolling up
                    lastVisibleItemPosition = firstVisibleItem
                }

                if ((visibleItemCount + firstVisibleItem) >= totalItemCount && firstVisibleItem >= 0) {
                    if (firstVisibleItem > lastVisibleItemPosition) {
                        lastVisibleItemPosition = firstVisibleItem
                        itemsToLoadAhead = 3
                        // Load next page here
                        if (hasMoreData && dashboardPatchMain.itemCount < (dashboardData?.size ?: 0)) {
                            loadNextPage()
                        }
                    }
                }
            }
        })


        initObserver()
        loadPage()
        binding.noInternetLayout.noInternetRetry.setOnClickListener {
            loadDataAPI()
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = location.latitude
                val longitude = location.longitude

                val bearingPoint = getBearingBetweenTwoPoints(latitude, longitude)

                bearing = if (bearingPoint > 0) {
                    bearingPoint
                } else {
                    90 + bearingPoint
                }

                initKaabaDistance(latitude, longitude)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

            override fun onProviderEnabled(provider: String) {
            }

            override fun onProviderDisabled(provider: String) {
            }
        }

    }

    private fun loadNextPage() {
        val newItems = fetchData(dashboardPatchMain.itemCount, itemsToLoadAhead) // You start from the current adapter size as an offset
        newItems?.let {
            if (it.size < itemsToLoadAhead) {
                hasMoreData = false
            }
            hasMoreData = false
            dashboardPatchMain.updateDashData(it,dashboardData?.size?:0)
        }
    }

    private fun fetchData(offset: Int, limit: Int): List<Data>? {
        val end = dashboardData?.size?.let { (offset + limit).coerceAtMost(it) }  // Ensure we don't go past the end of the list
        return end?.let { dashboardData?.subList(offset, it) }
    }


    override fun onResume() {
        super.onResume()

            loadPage()

        if (isPageShowing && isPatchVisible(TYPE_WIDGET7)) {
            val mSensor = mSensorManager.getDefaultSensor(3)
            if (mSensor != null) {
                mSensorManager.registerListener(this, mSensor, 1)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        locationListener?.let {
            locationManager?.removeUpdates(it)
            locationListener = null
        }
    }



    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        if(menuVisible)
        {
          /*  Log.e("setMenuVisibility","DAASHBOARD")
            setupAction(R.drawable.ic_menu,0,this@DashboardFragment,localContext.resources.getString(R.string.app_name))
        */

            val miniPlayerHeight = getMiniPlayerHeight()

            binding.dashboardMain.setPadding(binding.dashboardMain.paddingStart,binding.dashboardMain.paddingTop,binding.dashboardMain.paddingRight,if(miniPlayerHeight>0) miniPlayerHeight else binding.dashboardMain.paddingBottom)

            CallBackProvider.setFragment(this)
            dashboardPatchMain.getAllah99NameInstance()?.reInitCallback()
            nextPrayerCountownFinish()
        }

    }

    fun loadDataAPI()
    {
          loadingState()
            lifecycleScope.launch {
                dashboardViewModel.getDashboard(currentState, getLanguage(), prayerdate)

                DeenSDKCore.baseContext?.let {
                    prayerNotification(DeenSDKCore.isPrayerNotificationEnabled(it))
                }

            }
    }

   /* override fun BASE_API_CALL_STATE() {
        super.BASE_API_CALL_STATE()
        loadDataAPI()
    }
*/


    private fun prayerNotification(isEnabled: Boolean)
    {

        CoroutineScope(Dispatchers.IO).launch {

            if(isEnabled)
            {

                /* token = AuthenticateRepository(
                     authenticateService = NetworkProvider().getInstance().provideAuthService(),
                     userPrefDao = DatabaseProvider().getInstance().provideUserPrefDao()
                 ).authDeen(msisdn)

                     if (token != null && token?.isNotEmpty() == true) {*/


                val prayerTimesRepository = PrayerTimesRepository(
                    deenService = NetworkProvider().getInstance().provideDeenService(),
                    prayerNotificationDao = DatabaseProvider().getInstance()
                        .providePrayerNotificationDao(),
                    prayerTimesDao = null
                )


                val getPrayerTime = async { prayerTimesRepository.getPrayerTimes("Dhaka",
                    DeenSDKCore.GetDeenLanguage(),
                    DeenSDKCore.GetDeenPrayerDate()
                ) }.await()
                val getPrayerTimeNextDay = async { prayerTimesRepository.getPrayerTimes("Dhaka",
                    DeenSDKCore.GetDeenLanguage(), DeenSDKCore.GetDeenPrayerDate().StringTimeToMillisecond("dd/MM/yyyy").MilliSecondToStringTime("dd/MM/yyyy",1)) }.await()


                when (getPrayerTime) {
                    is ApiResource.Failure -> {

                    }

                    is ApiResource.Success -> {

                        getPrayerTime.value?.let {

                            val isha =
                                "${DeenSDKCore.GetDeenPrayerDate()} ${it.Data.Isha}".StringTimeToMillisecond("dd/MM/yyyy HH:mm:ss")

                            val currentTime =
                                SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(Date()).StringTimeToMillisecond("dd/MM/yyyy HH:mm:ss")

                            if(currentTime>isha)
                                return@launch
                            else
                                setupPrayerNotification(prayerTimesRepository,it)
                        }
                    }
                }

                when (getPrayerTimeNextDay) {
                    is ApiResource.Failure -> {

                    }

                    is ApiResource.Success -> {

                        getPrayerTimeNextDay.value?.let {
                            setupPrayerNotification(prayerTimesRepository,it)
                        }
                    }
                }

                //}


            }else
            {

                val prayerTimesRepository = PrayerTimesRepository(
                    deenService = NetworkProvider().getInstance().provideDeenService(),
                    prayerNotificationDao = DatabaseProvider().getInstance()
                        .providePrayerNotificationDao(),
                    prayerTimesDao = null
                )

               // prayerTimesRepository.clearPrayerNotification()

                prayerTimesRepository.updatePrayerNotification(
                    "",
                    "Notification",
                    0,
                    "",
                    null
                )

            }

        }
    }

    private suspend fun setupPrayerNotification(
        prayerTimesRepository: PrayerTimesRepository,
        prayerTimesResponse: PrayerTimesResponse
    )
    {
        prayerTimesResponse.let {



            var prayerNotifyCount = 0

            if (prayerTimesRepository.updatePrayerNotification(
                    DeenSDKCore.GetDeenPrayerDate(),
                    "pt1",
                    3,
                    "",
                    it,
                    isFromInsideSDK = false
                ) == 1
            )
                prayerNotifyCount++

            if (prayerTimesRepository.updatePrayerNotification(
                    DeenSDKCore.GetDeenPrayerDate(),
                    "pt3",
                    3,
                    "",
                    it,
                    isFromInsideSDK = false
                ) == 1
            )
                prayerNotifyCount++
            if (prayerTimesRepository.updatePrayerNotification(
                    DeenSDKCore.GetDeenPrayerDate(),
                    "pt4",
                    3,
                    "",
                    it,
                    isFromInsideSDK = false
                ) == 1)
                prayerNotifyCount++

            if (prayerTimesRepository.updatePrayerNotification(
                    DeenSDKCore.GetDeenPrayerDate(),
                    "pt5",
                    3,
                    "",
                    it,
                    isFromInsideSDK = false
                ) == 1
            )
                prayerNotifyCount++

            if (prayerTimesRepository.updatePrayerNotification(
                    DeenSDKCore.GetDeenPrayerDate(),
                    "pt6",
                    3,
                    "",
                    it,
                    isFromInsideSDK = false
                ) == 1
            )
                prayerNotifyCount++

            prayerTimesRepository.updatePrayerNotification(
                "",
                "Notification",
                1,
                "",
                null
            )

            Log.e("DEEN_NOTIFY",prayerNotifyCount.toString())


        }
    }


    private fun initObserver()
    {


        dashboardViewModel.prayerTimes.observe(viewLifecycleOwner)
        {
            when(it)
            {
                is PrayerTimeResource.postPrayerTime -> viewStatePrayerTime(it.data)

            }
        }

        dashboardViewModel.dashLiveData.observe(viewLifecycleOwner)
        {
            when(it)
            {
                CommonResource.API_CALL_FAILED -> nointernetState()
                is DashboardResource.DashboardData -> viewState(it.data)

            }
        }

        prayerViewModel.selecteStateLiveData.observe(viewLifecycleOwner)
        {
            when (it) {
                is PrayerTimeResource.selectedState -> {
                    currentState = it.state.state

                    dashboardPatchMain.updateState(it.state)
                    //prayerTimesAdapter.updateState(it.state)
                    lifecycleScope.launch {
                        dashboardViewModel.getPrayerTime(currentState, getLanguage(), prayerdate)
                    }
                }
            }
        }

        prayerViewModel.prayerTimesNotification.observe(viewLifecycleOwner)
        {
            when(it)
            {
                is PrayerNotificationResource.prayerTrackFailed ->  requireContext().toast(localContext.getString(
                                    R.string.failed_to_set_prayer_track))
                is PrayerNotificationResource.prayerTrackData ->
                {

                    if(prayerTrackLastWakt.isNotEmpty())
                        requireContext().toast("আলহামদুলিল্লাহ আপনি ${prayerTrackLastWakt.prayerMomentLocaleForToast()} নামাজ আদায় করেছেন।")

                    updatePrayerTrackingView(it.data)
                }
            }
        }

        /*dashboardViewModel.prayerTimesNotification.observe(viewLifecycleOwner)
        {
            when(it)
            {
                is PrayerNotificationResource.dateWiseNotificationData -> updateprayerTracker(it.data)
                else -> Unit
            }
        }*/

    }

    private fun updatePrayerTrackingView(data: com.deenislam.sdk.service.network.response.prayertimes.tracker.Data)
    {
        dashboardPatchMain.updatePrayerTracker(data)
    }

    private fun viewStatePrayerTime(data: PrayerTimesResponse)
    {
        if(prayerTimesResponse==null) {
            prayerTimesResponse = data
        }

        updatePrayerAdapterOnly(data)

    }

    private fun viewState(data: List<Data>)
    {
        dashboardData = data

        val matchingItem: Data? = data.find { it.AppDesign == "Compass" }
        matchingItem?.let {
            if(it.Items.isNotEmpty())
                compassBG = it.Items[0].imageurl1
        }

        /*lifecycleScope.launch {

            val dashbTask = async { dashboardPatchMain.updateDashData(data) }

            dashbTask.await()

            prayerTimesResponse?.let {
                dashboardPatchMain.updatePrayerTime(it)
                //binding.progressLayout.root.visible(false)
                binding.noInternetLayout.root.visible(false)
            }//?:nointernetState()
        }*/

        loadNextPage()

        prayerTimesResponse?.let {
            dashboardPatchMain.updatePrayerTime(it)
            //binding.progressLayout.root.visible(false)
            binding.noInternetLayout.root.visible(false)
        }
    }


    private fun updatePrayerAdapterOnly(data: PrayerTimesResponse)
    {
        Log.e("updatePrayerAdapterOnly","Called")
        binding.dashboardMain.post {
            dashboardPatchMain.updatePrayerTime(data)
            //dashboardPatchMain.notifyDataSetChanged()
        }
    }

    private fun dataState()
    {
        binding.dashboardMain.visible(true)
        binding.progressLayout.root.visible(false)
        binding.progressLayout.root.visible(false)
        binding.noInternetLayout.root.visible(false)

        val isRcPremium = customargs?.getBoolean("isPremium",false)
        val getRcCode = customargs?.getString("rc")

        when(getRcCode){

            "live_ijtema" ->{
                dashboardPatchMain.getDashboardData().let {
                    if(it.isEmpty())
                        return

                    it.let {banner->

                        val ijtemaBanners = banner.filter { bannerData -> bannerData.AppDesign == "ijtema" }

                        ijtemaBanners.forEach { ijtemaData ->

                            val bundle = Bundle()
                            bundle.putString("videoid", ijtemaData.Items[0].ArabicText)
                            bundle.putString("title",ijtemaData.Items[0].MText)
                            gotoFrag(R.id.action_global_ijtemaLiveFragment,bundle)

                        }

                    }
                }
            }

            "khatam_e_quran_ramadan" ->{
                dashboardPatchMain.getDashboardData().let {
                    if(it.isEmpty())
                        return

                    it.let {banner->
                        val getMenuData = dashboardPatchMain.getDashboardData().filter { it.AppDesign == "Services" }
                        val getData: Item? = getMenuData.flatMap {menu-> menu.Items }.firstOrNull {menu-> menu.Text == MENU_KHATAM_E_QURAN_RAMADAN }
                        getData?.let {
                            item->
                            val bundle = Bundle()
                            bundle.putBoolean("isRamadan",true)
                            bundle.putString("date",item.Meaning)
                            gotoFrag(R.id.action_global_khatamEquranHomeFragment,bundle)

                        }

                    }
                }
            }

            "ramadan" ->{
                dashboardPatchMain.getDashboardData().let {
                    if(it.isEmpty())
                        return

                    it.let {banner->
                        val getMenuData = dashboardPatchMain.getDashboardData().filter { it.AppDesign == "Services" }
                        val getData: Item? = getMenuData.flatMap {menu-> menu.Items }.firstOrNull {menu-> menu.Text == MENU_RAMADAN }
                        getData?.let {
                                item->
                            val bundle = Bundle()
                            bundle.putString("date",item.Meaning)
                            gotoFrag(R.id.action_global_ramadanFragment,bundle)

                        }

                    }
                }
            }
            else ->{

                val getDestination = getRcCode?.getRCDestination()

                if(getDestination == 0) {
                    return
                }

                if (getDestination != null) {
                    gotoFrag(getDestination)
                }

            }


        }

        hasMoreData = true

        customargs = null

       /* val getBannerData = dashboardPatchMain.getDashboardData().filter { it.AppDesign == "Banners" }
        val getRamadanPatchData: Item? = getBannerData.flatMap { it.Items }.firstOrNull { it.ContentType == "rot" }

        if (getRamadanPatchData != null && getRamadanPatchData.MText.isNotEmpty()) {
            tryCatch {
                val ramadanExpectedTimeInMill = getRamadanPatchData.MText.toLong() * 1000
                val remainTime = ramadanExpectedTimeInMill - System.currentTimeMillis()
                if (remainTime > 0) {
                    ramadanCountDownTimerSetup(remainTime,getRamadanPatchData.Meaning)
                }
            }
        }*/


        // ramadan floating card
        val getBannerData = dashboardPatchMain.getDashboardData().filter { it.AppDesign == "Banners" }
        val getRamadanRegularPatchData: Item? = getBannerData.flatMap { it.Items }.firstOrNull { it.ContentType == "rot" }
        val getRamadanPatchData: Item? = getBannerData.flatMap { it.Items }.firstOrNull { it.ContentType == "rr" }

        if (getRamadanRegularPatchData != null && getRamadanRegularPatchData.MText.isNotEmpty()) {
            tryCatch {
                val ramadanExpectedTimeInMill = getRamadanRegularPatchData.MText.toLong() * 1000
                val remainTime = ramadanExpectedTimeInMill - System.currentTimeMillis()
                if (remainTime > 0) {
                    ramadanCountDownTimerSetup(remainTime,getRamadanRegularPatchData.Meaning)
                }
            }
        }else{

            if(activity!=null && getRamadanPatchData!=null) {
                prayerTimesResponse?.let {
                    (activity as MainActivityDeenSDK).showRamadanDayFloatingCard(
                        it.Data.IslamicDate.split(
                            ","
                        ).firstOrNull() ?: "--",it
                    )
                }

            }
        }

    }

    private fun nointernetState()
    {
        binding.progressLayout.root.visible(false)
        binding.noInternetLayout.root.visible(true)
    }

    private fun loadingState()
    {
        binding.progressLayout.root.visible(true)
        binding.noInternetLayout.root.visible(false)
    }

    fun loadPage()
    {
        if(firstload != 0)
            return
        firstload = 1

        ViewCompat.setTranslationZ(binding.progressLayout.root, 10F)

        //dashboardPatchMain.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

         linearLayoutManager =
            LinearLayoutManager(requireView().context, LinearLayoutManager.VERTICAL, false)


        binding.dashboardMain.apply {
                dashboardPatchMain = DashboardPatchAdapter()
                adapter = dashboardPatchMain
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                isNestedScrollingEnabled = false
            }
                layoutManager = linearLayoutManager
                //overScrollMode = View.OVER_SCROLL_NEVER
                post {

                    loadDataAPI()

                    this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                            if (!isPatchVisible(TYPE_WIDGET11))
                                dashboardPatchMain.getAllah99NameInstance()?.stop99NamePlaying()

                            if (!isPatchVisible(TYPE_WIDGET7))
                                mSensorManager.unregisterListener(this@DashboardFragment)
                            else {
                                val mSensor = mSensorManager.getDefaultSensor(3)
                                if (mSensor != null) {
                                    mSensorManager.registerListener(this@DashboardFragment, mSensor, 1)
                                }
                            }

                        }

                        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                            super.onScrollStateChanged(recyclerView, newState)

                            when (newState) {
                                RecyclerView.SCROLL_STATE_IDLE -> {
                                    // RecyclerView has stopped scrolling
                                }

                                RecyclerView.SCROLL_STATE_DRAGGING -> {
                                    // RecyclerView is being dragged by the user
                                    if (!isPatchVisible(TYPE_WIDGET11))
                                        dashboardPatchMain.getAllah99NameInstance()?.stop99NamePlaying()

                                    if (!isPatchVisible(TYPE_WIDGET7))
                                        mSensorManager.unregisterListener(this@DashboardFragment)
                                    else {
                                        val mSensor = mSensorManager.getDefaultSensor(3)
                                        if (mSensor != null) {
                                            mSensorManager.registerListener(
                                                this@DashboardFragment,
                                                mSensor,
                                                1
                                            )
                                        }
                                    }
                                }

                                RecyclerView.SCROLL_STATE_SETTLING -> {
                                    // RecyclerView is settling into a final position
                                }
                            }
                        }
                    })
                }
            }

    /*    lifecycleScope.launch {

            userTrackViewModel.trackUser(
                language = getLanguage(),
                msisdn = DeenSDKCore.GetDeenMsisdn(),
                pagename = "home",
                trackingID = get9DigitRandom()
            )
        }*/

    }


    override fun action1() {

        gotoFrag(R.id.moreFragment)
    }

    override fun action2() {

    }

    override fun nextPrayerCountownFinish() {

        prayerTimesResponse?.let { updatePrayerAdapterOnly(it) }
    }

    override fun allPrayerPage() {
        gotoFrag(R.id.prayerTimesFragment)
    }

    override fun prayerTask(momentName: String?, isPrayed: Boolean) {

        lifecycleScope.launch {

            prayerTrackLastWakt = if(isPrayed)
                momentName?.getWaktNameByTag().toString()
            else
                ""

            if (momentName?.isNotEmpty() == true) {
               prayerViewModel.setPrayerTrack(language = getLanguage(),prayer_tag=momentName.getWaktNameByTag(),isPrayed)
            }
            /*else
                prayerTimeViewModel.getDateWisePrayerNotificationData(prayerdate)*/
        }
    }

    override fun billboard_prayer_load_complete() {
        prayerTimesResponse?.let { dashboardPatchMain.updatePrayerTime(it) }
    }

    inner class VMFactory(
        private val dashboardRepository: DashboardRepository,
        private val prayerTimesRepository : PrayerTimesRepository
        ) : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(dashboardRepository,prayerTimesRepository) as T
        }
    }

    inner class VMFactoryPrayer(
        private val prayerTimesRepository : PrayerTimesRepository) : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrayerTimesViewModel(prayerTimesRepository) as T
        }
    }


    override fun onAllViewsInflated() {
        dataState()
        isPageShowing = true
        val mSensor = mSensorManager.getDefaultSensor(3)
        if (mSensor != null) {
            mSensorManager.registerListener(this, mSensor, 1)
        }
    }


    private fun initKaabaDistance(latitude: Double, longitude: Double) {
        val distance = getDistance(
            latitude,
            longitude,
            MAKKAH_LATITUDE,
            MAKKAH_LONGITUDE
        ).toDouble()
        val distanceOfMakkah = localContext.getString(
            R.string.compass_distance_of_makka,
            "${DecimalFormat("##.##").format(distance / 1000)}".numberLocale()
        )

        dashboardPatchMain.updateCompass(distanceOfMakkah)
    }

    fun getDistance(lat_a: Double, lng_a: Double, lat_b: Double, lng_b: Double): Int {
        var miter = 0
        try {
            val earthRadius = 3958.75
            val latDiff = Math.toRadians(lat_b - lat_a)
            val lngDiff = Math.toRadians(lng_b - lng_a)
            val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                    Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                    Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            val distance = earthRadius * c
            val meterConversion = 1609
            miter = (distance * meterConversion.toFloat().toInt()).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return miter
    }

    fun degreesToRadians(degrees: Double): Double {
        return degrees * (3.1416 / 180.0)
    }

    fun radiansToDegrees(radians: Double): Double {
        return radians * (180.0 / 3.1416)
    }

    fun getBearingBetweenTwoPoints(longitude: Double, latitude: Double): Double {
        val lat1 = degreesToRadians(degrees = longitude)
        val lon1 = degreesToRadians(degrees = latitude)

        val lat2 = degreesToRadians(degrees = MAKKAH_LATITUDE)
        val lon2 = degreesToRadians(degrees = MAKKAH_LONGITUDE)

        val dLon = lon2 - lon1

        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        val radiansBearing = Math.atan2(y, x)
        return radiansToDegrees(radiansBearing)
    }

    private fun isPatchVisible(design: String): Boolean {
        var result = false
        val firstVisiblePosition = linearLayoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition()

        if (firstVisiblePosition != RecyclerView.NO_POSITION && lastVisiblePosition != RecyclerView.NO_POSITION) {
            for (position in firstVisiblePosition..lastVisiblePosition) {
                // Handle or check each visible position here
                // For example, you might print them:
                if (dashboardPatchMain.getViewTypePosition(design) == position)
                    result = true
            }
        }

        return result

    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {

        if(!isVisible)
            return

        if (isPatchVisible(TYPE_WIDGET7)) {

            val degree = Math.round(sensorEvent?.values!!.get(0))

            val degreeTxt = localContext.getString(
                R.string.compass_degree_txt,
                "${degree.toString() + 0x00B0.toChar()}".numberLocale()
            )

            dashboardPatchMain.updateCompass(-degree.toFloat(), degreeTxt)
        }

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun stateSelected(stateModel: StateModel) {
        lifecycleScope.launch {
            prayerViewModel.updateSelectedState(stateModel)
        }
    }

    override fun adClicked(items: com.deenislam.sdk.service.network.response.advertisement.Data) {
        lifecycleScope.launch {
           async {  dashboardViewModel.saveAdvertisementrecord(items.Id,"redirect") }.await()
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(items.redirecturl))
        if (context?.packageManager?.let { intent.resolveActivity(it) } != null) {
            context?.startActivity(intent)
        }
    }

    override fun menuClicked(pagetag: String, getMenu: Item?) {
        when(pagetag)
        {
            MENU_PRAYER_TIME ->  gotoFrag(R.id.action_global_prayerTimesFragment)
            MENU_AL_QURAN -> gotoFrag(R.id.action_global_quranFragment)
            MENU_HADITH -> gotoFrag(R.id.action_global_hadithFragment)
            MENU_DUA -> gotoFrag(R.id.action_global_dailyDuaFragment)
            MENU_ZAKAT -> gotoFrag(R.id.action_global_zakatFragment)
            MENU_DIGITAL_TASBEEH -> gotoFrag(R.id.action_global_tasbeehFragment)
            MENU_QIBLA_COMPASS -> {
                val bundle = Bundle()
                bundle.putString("compassBG",compassBG)
                gotoFrag(R.id.action_global_compassFragment,bundle)
            }
            MENU_ISLAMIC_NAME -> gotoFrag(R.id.action_global_islamicNameFragment)
            MENU_IJTEMA -> gotoFrag(R.id.action_global_ijtemaLiveFragment)
            MENU_ISLAMIC_EVENT -> gotoFrag(R.id.action_global_islamicEventHomeFragment)
            MENU_PRAYER_LEARNING -> gotoFrag(R.id.action_global_prayerLearningFragment)
            MENU_KHATAM_E_QURAN -> gotoFrag(R.id.action_global_khatamEquranHomeFragment)
            MENU_KHATAM_E_QURAN_RAMADAN -> {
                val bundle = Bundle()
                bundle.putBoolean("isRamadan",true)
                bundle.putString("date",getMenu?.Meaning)
                gotoFrag(R.id.action_global_khatamEquranHomeFragment,bundle)
            }
            MENU_QURAN_CLASS -> gotoFrag(R.id.action_global_quranLearningFragment)
            MENU_RAMADAN_OTHER_DAY -> gotoFrag(R.id.action_global_ramadanOtherDayFragment)
            MENU_RAMADAN -> {
                val bundle = Bundle()
                bundle.putString("date",getMenu?.Meaning)
                gotoFrag(R.id.action_global_ramadanFragment,bundle)
            }
            MENU_99_NAME_OF_ALLAH -> gotoFrag(R.id.action_global_allah99NamesFragment)
            MENU_NEAREST_MOSQUE -> {
                val bundle = Bundle()
                bundle.putString("pageTitle",localContext.getString(R.string.nearest_mosque))
                bundle.putString("query","mosque")
                gotoFrag(R.id.action_global_nearestMosqueWebviewFragment,bundle)
            }
            MENU_LIVE_MAKKAH_MADINA -> gotoFrag(R.id.action_global_makkahLiveFragment)
            MENU_LIVE_PODCAST -> gotoFrag(R.id.action_global_livePodcastFragment)
            MENU_ISLAMIC_EDUCATION_VIDEO -> gotoFrag(R.id.action_global_islamicEducationVideoHomeFragment)
            MENU_ISLAMIC_BOYAN -> gotoFrag(R.id.action_global_islamicBoyanHomeFragment)
            MENU_EID_JAMAT -> gotoFrag(R.id.action_global_eidJamatHomeFragment)
            MENU_DUA_AMOL -> {
                val bundle = Bundle()
                bundle.putString("date",getMenu?.Meaning)
                gotoFrag(R.id.action_global_duaAmalHomeFragment,bundle)
            }
            MENU_HAJJ_AND_UMRAH -> gotoFrag(R.id.action_global_hajjAndUmrahFragment)
            MENU_QURBANI -> gotoFrag(R.id.action_global_qurbaniFragment)
            else -> context?.toast(localContext.getString(R.string.feature_coming_soon))
        }
    }

    override fun dashboardPatchClickd(patch: String, data: Item?) {
        when(patch)
        {
            "qr","qrs" -> gotoFrag(R.id.action_global_quranFragment)
            "hd","hdd" -> gotoFrag(R.id.action_global_hadithFragment)
            "zk" -> gotoFrag(R.id.action_global_zakatFragment)
            "tb" -> gotoFrag(R.id.action_global_tasbeehFragment)
            "in" -> gotoFrag(R.id.action_global_islamicNameFragment)
            "cp" -> {
                val bundle = Bundle()
                bundle.putString("compassBG",compassBG)
                gotoFrag(R.id.action_global_compassFragment,bundle)
            }
            "du" -> {
                if(data!=null && data.CategoryId!=0){
                    val bundle = Bundle().apply {
                        putInt("category", data.CategoryId)
                        putString("catName", data.ArabicText)
                    }
                    gotoFrag(R.id.action_global_allDuaPreviewFragment,data = bundle)

                }else
                    gotoFrag(R.id.action_global_dailyDuaFragment)

            }
            "ijtema" -> {

                data?.let {
                    val bundle = Bundle()
                    bundle.putString("videoid", it.MText)
                    bundle.putString("title",it.ArabicText)
                    gotoFrag(R.id.action_global_ijtemaLiveFragment,bundle)
                }
            }

            // more patch

            "pt" -> gotoFrag(R.id.action_global_prayerTimesFragment)
            "ip" -> gotoFrag(R.id.action_global_livePodcastFragment)
            "99noa" -> gotoFrag(R.id.action_global_allah99NamesFragment)
            "pl" -> gotoFrag(R.id.action_global_prayerLearningFragment)
            "lmm" -> gotoFrag(R.id.action_global_makkahLiveFragment)
            "rot" -> gotoFrag(R.id.action_global_ramadanOtherDayFragment)
            "rr" -> {
                val bundle = Bundle()
                bundle.putString("date",data?.Meaning)
                gotoFrag(R.id.action_global_ramadanFragment,bundle)
            }
            "hau" -> gotoFrag(R.id.action_global_hajjAndUmrahFragment)
            "ie" -> {

                if(data!=null && data.CategoryId!=0){
                    val bundle = Bundle()
                    bundle.putInt("categoryID", data.CategoryId)
                    bundle.putString("pageTitle",data.ArabicText)
                    bundle.putString("pageTag", MENU_ISLAMIC_EVENT)
                    bundle.putBoolean("shareable",false)

                    gotoFrag(R.id.action_global_subCatCardListFragment,bundle)

                }else
                    gotoFrag(R.id.action_global_islamicEventHomeFragment)



            }
            "qc" -> gotoFrag(R.id.action_global_quranLearningFragment)
            "dqc" -> {

                data?.let {
                    val bundle = Bundle()
                    bundle.putString("title", it.MText)
                    bundle.putInt("courseId", data.Id)

                    gotoFrag(R.id.action_global_quranLearningDetailsFragment, bundle)
                }

            }

            "qsa" -> gotoFrag(R.id.action_global_quranLearningTpFragment)

            "fhdd" -> {
                val bundle = Bundle()
                bundle.putInt("redirectPage",1)
                gotoFrag(R.id.action_global_hadithFragment,bundle)
            }

            "fqrs" -> changeMainViewPager(1)


            "rkhq" -> {

                if(data?.FeatureName == "partnerQurbaniVideos"){

                    if(!Subscription.isSubscribe){
                        gotoFrag(R.id.action_global_subscriptionFragment)
                        return
                    }
                }

                if(data?.FeatureTitle == "Banners"){
                    val bundle = Bundle()
                    bundle.putBoolean("isRamadan",true)
                    bundle.putString("date", data.Meaning)
                    gotoFrag(R.id.action_global_khatamEquranHomeFragment,bundle)
                    return
                }

                dashboardPatchMain.getDashboardData().let {

                    var dataIndex = -1
                    var itemIndex = 0

                    it.forEachIndexed { index, dataValue ->
                        dataValue.Items.forEachIndexed { innerIndex, item ->
                            // Replace with the condition to check if the items match
                            if (item.ContentType == data?.ContentType && item.Id == data.Id) {
                                dataIndex = index
                                itemIndex = innerIndex
                                return@forEachIndexed
                            }
                        }
                    }

                    if(dataIndex !=-1) {
                        val bundle = Bundle()
                        bundle.putInt("khatamQuranvideoPosition", itemIndex)
                        bundle.putBoolean("isRamadan",true)
                        bundle.putParcelableArray("khatamQuranvideoList", it[dataIndex].Items.map { it1-> transformDashboardItemForKhatamQuran(it1) }.toTypedArray())
                        gotoFrag(R.id.action_global_khatamEQuranVideoFragment, bundle)
                    }

                }


            }

            "khq" -> {

                if(data?.FeatureTitle == "Banners"){
                    gotoFrag(R.id.action_global_khatamEquranHomeFragment)
                    return
                }

                dashboardPatchMain.getDashboardData().let {

                    var dataIndex = -1
                    var itemIndex = 0

                    it.forEachIndexed { index, dataValue ->
                        dataValue.Items.forEachIndexed { innerIndex, item ->
                            // Replace with the condition to check if the items match
                            if (item.ContentType == data?.ContentType && item.Id == data.Id) {
                                dataIndex = index
                                itemIndex = innerIndex
                                return@forEachIndexed
                            }
                        }
                    }

                    if(dataIndex !=-1) {
                        val bundle = Bundle()
                        bundle.putInt("khatamQuranvideoPosition", itemIndex)
                        bundle.putParcelableArray("khatamQuranvideoList", it[dataIndex].Items.map { it1-> transformDashboardItemForKhatamQuran(it1) }.toTypedArray())
                        gotoFrag(R.id.action_global_khatamEQuranVideoFragment, bundle)
                    }

                }


            }
            "ies" -> gotoFrag(R.id.action_global_islamicEducationVideoHomeFragment)
            "nm" -> {
                val bundle = Bundle()
                bundle.putString("pageTitle",localContext.getString(R.string.nearest_mosque))
                bundle.putString("query","mosque")
                gotoFrag(R.id.action_global_nearestMosqueWebviewFragment,bundle)
            }

            "ib" -> {


                if(data!=null && data.SubCategoryId!=0){
                    val bundle = Bundle()
                    bundle.putInt("id", data.SubCategoryId)
                    bundle.putString("videoType", "scholar")
                    bundle.putString("title",data.ArabicText)
                    gotoFrag(R.id.action_global_boyanVideoPreviewFragment, bundle)
                }else if(data!=null && data.CategoryId!=0){
                    val bundle = Bundle()
                    bundle.putInt("id", data.CategoryId)
                    bundle.putString("videoType", "category")
                    bundle.putString("title",data.ArabicText)
                    gotoFrag(R.id.action_global_boyanVideoPreviewFragment, bundle)
                }else{
                    gotoFrag(R.id.action_global_islamicBoyanHomeFragment)
                }

            }

            "ej" -> gotoFrag(R.id.action_global_eidJamatHomeFragment)
            "ida" -> {

                data?.let {
                    val bundle = Bundle()
                    bundle.putString("date", it.Meaning)
                    gotoFrag(R.id.action_global_duaAmalHomeFragment, bundle)
                }
            }
            "sub" -> gotoFrag(R.id.action_global_subscriptionFragment)
            "blnk" -> Unit
            "qurb" -> gotoFrag(R.id.action_global_qurbaniFragment)
            else -> context?.toast(localContext.getString(R.string.feature_coming_soon))
        }
    }

    override fun allahNameClicked(position: Int) {
        gotoFrag(R.id.action_global_allah99NamesFragment)
    }

    override fun globalMiniPlayerClosed(){
        binding.dashboardMain.setPadding(binding.dashboardMain.paddingStart,binding.dashboardMain.paddingTop,binding.dashboardMain.paddingRight,16.dp)
    }

}