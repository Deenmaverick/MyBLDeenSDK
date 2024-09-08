package com.deenislam.sdk.views.base

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.deenislam.sdk.DeenSDKCore
import com.deenislam.sdk.R
import com.deenislam.sdk.service.di.NetworkProvider
import com.deenislam.sdk.service.network.response.quran.qurangm.ayat.Ayath
import com.deenislam.sdk.service.network.response.quran.qurangm.ayat.Qari
import com.deenislam.sdk.service.repository.UserTrackRepository
import com.deenislam.sdk.utils.LocaleUtil
import com.deenislam.sdk.utils.dp
import com.deenislam.sdk.utils.get9DigitRandom
import com.deenislam.sdk.utils.hide
import com.deenislam.sdk.utils.isBottomNavFragment
import com.deenislam.sdk.utils.show
import com.deenislam.sdk.utils.visible
import com.deenislam.sdk.viewmodels.FragmentViewModel
import com.deenislam.sdk.viewmodels.UserTrackViewModel
import com.deenislam.sdk.views.adapters.quran.AlQuranAyatAdapter
import com.deenislam.sdk.views.main.MainActivityDeenSDK
import com.deenislam.sdk.views.main.actionCallback
import kotlinx.coroutines.launch
import java.util.Locale

internal abstract class BaseFragment<VB:ViewBinding>(
    private val bindingInflater: (inflater:LayoutInflater)->VB
):Fragment() {

    lateinit var localContext:Context
    lateinit var localInflater: LayoutInflater
    lateinit var userTrackViewModel: UserTrackViewModel

    private val fragmentViewModel by viewModels<FragmentViewModel>()

    private var _binding:VB ? = null
    val binding:VB get() = _binding as VB
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var actionCallback:otherFagmentActionCallback ? =null
    private var isBacktoHome:Boolean = false
    lateinit var childFragment: Fragment
    private var isBackPressed:Boolean = false
    private var isHomePage:Boolean = false
    private var lastClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localContext = LocaleUtil.createLocaleContext(requireContext(), Locale("bn"))

        /*if (DeenSDKCore.GetDeenLanguage() == "en") {
            LocaleUtil.createLocaleContext(requireContext(), Locale("en"))
        } else {
            LocaleUtil.createLocaleContext(requireContext(), Locale("bn"))
        }*/


        val themedContext = ContextThemeWrapper(localContext, R.style.DeenSDKTheme) // Replace with your theme

        localInflater = layoutInflater.cloneInContext(themedContext)

        userTrackViewModel = UserTrackViewModel(
            repository = UserTrackRepository(authenticateService = NetworkProvider().getInstance().provideAuthService())
        )

        OnCreate()


    }

    fun isHomePage(bol:Boolean)
    {
        isHomePage = bol
    }

     fun getLanguage():String = DeenSDKCore.GetDeenLanguage()

    fun changeLanguage(language:String)
    {
        //DeenSDKCore.SetDeenLanguage(language)
        (activity as MainActivityDeenSDK).changeLanguage()
    }
    private fun isBacktoHome(bol:Boolean)
    {
        isBacktoHome = bol
    }

    fun ramadanCountDownTimerSetup(ramadanExpectedTimeInMill: Long, date: String) {
        if(activity!=null)
            (activity as MainActivityDeenSDK).ramadanCountDownTimerSetup(ramadanExpectedTimeInMill,date)
    }


    fun BASE_CHECK_API_STATE()
    {
        fragmentViewModel.check_api_state()
    }
    fun BASE_OBSERVE_API_CALL_STATE()
    {
        fragmentViewModel.isAPILoaded.observe(this)
        {

            when(it)
            {
                true -> Unit
                false -> BASE_API_CALL_STATE()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = bindingInflater.invoke(localInflater)

        if(_binding == null)
            throw java.lang.IllegalArgumentException("View cannot null")

        ON_CREATE_VIEW(binding.root)

        return binding.root
    }

    open fun ON_CREATE_VIEW(root: View)
    {

    }


    override fun onResume() {
        super.onResume()

        if(this::onBackPressedCallback.isInitialized && this::childFragment.isInitialized && !isHomePage)
            setupBackPressCallback(childFragment,isBacktoHome)

       /* if(this::onBackPressedCallback.isInitialized && this::childFragment.isInitialized)
            onBackPressedCallback.isEnabled = true
*/

      /*  requireView().addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            when (v.visibility) {
                View.VISIBLE -> {

                    if(this::onBackPressedCallback.isInitialized && !onBackPressedCallback.isEnabled)
                    {

                        if(isDashboardVisible())
                        {
                            Log.e("LayoutChangeListener",onBackPressedCallback.isEnabled.toString())
                            setupBackPressCallback(this)
                        }

                    }


                    // View is now visible
                }
                else -> {
                    // View is not visible
                }
            }

            //Log.e("LayoutChangeListener",onBackPressedCallback.isEnabled.toString())

        }*/
    }

    open fun BASE_API_CALL_STATE()
    {

    }

    open fun onBackPress() {

        Log.e("onBackPress","BASE")
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime < 500) {
            return  // The click is too fast, ignore it
        }
        lastClickTime = currentTime

        if (findNavController().previousBackStackEntry?.destination?.id?.equals(
                findNavController().graph.startDestinationId
            ) != true &&
            findNavController().previousBackStackEntry?.destination?.id?.equals(
                R.id.blankFragment
            ) != true && findNavController().previousBackStackEntry?.destination?.id != null)
            findNavController().popBackStack()

       /* if(isVisible) {
            isBackPressed = true

            if (isBacktoHome) {
                findNavController().popBackStack().apply {
                    setupOtherFragment(false)
                }
            } else {

                if (findNavController().previousBackStackEntry?.destination?.id?.equals(
                        findNavController().graph.startDestinationId
                    ) != true
                )
                    findNavController().popBackStack()
                else
                    setupOtherFragment(false)
            }

        }*/

    }

    fun getMiniPlayerHeight(): Int {

        return if(activity!=null)
            (activity as MainActivityDeenSDK).getMiniPlayerHeight()
        else
            0
    }


    fun setupOtherFragment(bol:Boolean)
    {

        return
        if(!bol)
        {
            lifecycleScope.launch {
                userTrackViewModel.trackUser(
                    language = getLanguage(),
                    msisdn = DeenSDKCore.GetDeenMsisdn(),
                    pagename = "home",
                    trackingID = get9DigitRandom()
                )
            }
        }


        (activity as MainActivityDeenSDK).setupOtherFragment(bol)
    }

    fun setupAction(action1:Int,action2:Int,callback: actionCallback)
    {
        (activity as MainActivityDeenSDK).setupActionbar(action1,action2,callback)
    }

    fun setupActionForOtherFragment(
        action1:Int,
        action2:Int,
        callback: otherFagmentActionCallback?=null,
        actionnBartitle:String,
        backEnable:Boolean=true,
        view: View,
        action3:Int=0,
        actionIconColor:Int = 0
    )
    {
        val action1Btn: AppCompatImageView = view.findViewById(R.id.action1)
        val action2Btn: AppCompatImageView = view.findViewById(R.id.action2)
        val btnBack: AppCompatImageView = view.findViewById(R.id.btnBack)
        val title: AppCompatTextView = view.findViewById(R.id.title)
        val action3Btn:AppCompatImageView = view.findViewById(R.id.action3)

        if(actionIconColor!=0)
        {
            action1Btn.setColorFilter(ContextCompat.getColor(view.context,actionIconColor))
            action2Btn.setColorFilter(ContextCompat.getColor(view.context,actionIconColor))
            action3Btn.setColorFilter(ContextCompat.getColor(view.context,actionIconColor))

        }

        if(backEnable)
        {
            btnBack.setImageDrawable(AppCompatResources.getDrawable(view.context, R.drawable.ic_back))
            btnBack.visible(true)
            btnBack.setOnClickListener {
                this.requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            title.text = actionnBartitle
            title.setTextColor(ContextCompat.getColor(title.context, R.color.deen_txt_black_deep))

        }
        else
        {
            (title.layoutParams as ConstraintLayout.LayoutParams).apply {
                leftMargin=16.dp
            }
            title.text = actionnBartitle
            btnBack.visible(false)
        }

        if(action1>0) {
            action1Btn.setImageDrawable(AppCompatResources.getDrawable(view.context, action1))
            action1Btn.visible(true)
            action1Btn.setOnClickListener {
                callback?.action1()
            }
        }
        else
            action1Btn.visible(false)

        if(action2>0) {
            action2Btn.setImageDrawable(AppCompatResources.getDrawable(view.context, action2))
            action2Btn.visible(true)
            action2Btn.setOnClickListener {
                callback?.action2()
            }
        }
        else
            action2Btn.visible(false)

        if(action3>0) {
            action3Btn.setImageDrawable(AppCompatResources.getDrawable(view.context, action3))
            action3Btn.show()
            action3Btn.setOnClickListener {
                callback?.action3()
            }
        }
        else
            action3Btn.hide()

        actionCallback = callback

        if(findNavController().currentDestination?.id?.isBottomNavFragment == false)
            setupOtherFragment(true)
        else
            setupOtherFragment(false)
    }

    fun changeMainViewPager(page:Int)
    {
        (activity as MainActivityDeenSDK).setViewPager(page)
    }

    fun setupAction(action1:Int,action2:Int,callback: actionCallback,title:String)
    {
        (activity as MainActivityDeenSDK).setupActionbar(title)
        (activity as MainActivityDeenSDK).setupActionbar(action1,action2,callback)
    }

    fun isDashboardVisible() = (activity as MainActivityDeenSDK).isDashboardVisible()

    fun gotoFrag(destination:Int,data:Bundle?=null,navOptions: NavOptions?=null)
    {

        if(this::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.isEnabled = false
            onBackPressedCallback.remove()
        }

        findNavController().navigate(destination,data,navOptions)

    }

    open fun OnCreate(){

    }

    fun playQuran(
        data: ArrayList<Ayath>,
        pos: Int,
        surahList: ArrayList<com.deenislam.sdk.service.network.response.quran.qurangm.surahlist.Data>,
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
        activity?.let {
            (it as MainActivityDeenSDK).playQuran(
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

    }

    fun updateQuranPlayer(qari: Int?=null) {

        if(activity !=null)
            (activity as MainActivityDeenSDK).updateQuranPlayer(qari)
    }

    fun pauseQuran()
    {
        if(activity !=null)
            (activity as MainActivityDeenSDK).pauseQuran()
    }

    fun setAdapterCallbackQuranPlayer(viewHolder: AlQuranAyatAdapter.ViewHolder)
    {
        if(activity !=null)
            (activity as MainActivityDeenSDK).setAdapterCallbackQuranPlayer(viewHolder)
    }

    fun getCurrentSurahIDFromQService():Int
    {
        return if(activity !=null)
            (activity as MainActivityDeenSDK).getCurrentSurahID()
        else
            0
    }

    fun getCurrentJuzIDFromQService():Int
    {
        return if(activity !=null)
            (activity as MainActivityDeenSDK).getCurrentJuzID()
        else
            0
    }


    fun setupBackPressCallback(fragment: Fragment,isBacktoHome:Boolean=false)
    {
        (activity as MainActivityDeenSDK).disableBackPress()
        //isBacktoHome(isBacktoHome)
        childFragment = fragment

        if(this::onBackPressedCallback.isInitialized)
            onBackPressedCallback.remove()

            onBackPressedCallback =
                fragment.requireActivity().onBackPressedDispatcher.addCallback {
                    onBackPress()
                }
            onBackPressedCallback.isEnabled = true

    }

    override fun onPause() {
        super.onPause()

        if(this::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.isEnabled = false
            //onBackPressedCallback.remove()
        }
    }

    override fun onStop() {
        //unregister listener here
        if(this::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.isEnabled = false
            onBackPressedCallback.remove()
            (activity as MainActivityDeenSDK).enableBackPress()
        }
        super.onStop()
    }


    /*override fun onDestroyView() {
      //unregister listener here
        if(this::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.isEnabled = false
            onBackPressedCallback.remove()
        }

        (activity as MainActivity).enableBackPress()


        super.onDestroyView()


    }*/

}