package com.deenislam.sdk.views.base

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.findNavController
import com.deenislam.sdk.DeenSDKCore
import com.deenislam.sdk.R
import com.deenislam.sdk.service.di.NetworkProvider
import com.deenislam.sdk.service.network.response.quran.qurangm.surahlist.Data
import com.deenislam.sdk.service.repository.UserTrackRepository
import com.deenislam.sdk.utils.AsyncViewStub
import com.deenislam.sdk.utils.LocaleUtil
import com.deenislam.sdk.utils.Subscription
import com.deenislam.sdk.utils.dp
import com.deenislam.sdk.utils.get9DigitRandom
import com.deenislam.sdk.utils.hide
import com.deenislam.sdk.utils.show
import com.deenislam.sdk.utils.visible
import com.deenislam.sdk.viewmodels.UserTrackViewModel
import com.deenislam.sdk.views.main.MainActivityDeenSDK
import com.deenislam.sdk.views.main.actionCallback
import com.deenislam.sdk.views.main.searchCallback
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Locale


internal abstract class BaseRegularFragment: Fragment() {

    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var actionCallback:otherFagmentActionCallback ? =null
    private var isBacktoHome:Boolean = false
    private var isBackPressed:Boolean = false
    lateinit var userTrackViewModel: UserTrackViewModel

    lateinit var localContext:Context
    lateinit var localInflater: LayoutInflater
    private lateinit var childFragment: Fragment
    private var lastClickTime = 0L

    // Common view ( loading,no internet etc)
    private  var progressLayout: LinearLayout? = null
    private  var nodataLayout: NestedScrollView? = null
    private  var noInternetLayout: NestedScrollView? =null
    private  var noInternetRetry: MaterialButton? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localContext = LocaleUtil.createLocaleContext(requireContext(), Locale("bn"))

           /* if (DeenSDKCore.GetDeenLanguage() == "en") {
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

    open fun OnCreate(){

    }

    fun setupBackPressCallback(fragment: Fragment,isBacktoHome:Boolean=false)
    {
        Log.e("setupBackPressCallback","OK")
        (activity as MainActivityDeenSDK).disableBackPress()
        //isBacktoHome(isBacktoHome)
        childFragment = fragment
        onBackPressedCallback =
            fragment.requireActivity().onBackPressedDispatcher.addCallback {
                onBackPress()
            }
        onBackPressedCallback.isEnabled = true
    }

    fun changeLanguage(language:String)
    {
        //DeenSDKCore.SetDeenLanguage(language)
        (activity as MainActivityDeenSDK).changeLanguage()
    }

    fun getMiniPlayerHeight(): Int {
       /* return if(activity!=null)
            (activity as MainActivityDeenSDK).getMiniPlayerHeight()
        else
            0*/
        return 0
    }

    fun playOfflineQuran(data: Data) {
        Log.e("playBtnClicked","OK")
        if(activity!=null)
            (activity as MainActivityDeenSDK).playOfflineQuran(data = data)
    }

    fun stopOfflineQuran(surahId: Int) {
        if(activity!=null)
            (activity as MainActivityDeenSDK).stopOfflineQuran(surahId)
    }

    fun destoryDeenSDK() = (activity as MainActivityDeenSDK).closeDeenSDK()

    fun setTrackingID(id:Long)
    {
        (activity as MainActivityDeenSDK).setTrackingID(id)
    }

    fun getTrackingID() = (activity as MainActivityDeenSDK).getTrackingID()

    fun getLanguage():String = DeenSDKCore.GetDeenLanguage()


     fun isBacktoHome(bol:Boolean)
    {
        isBacktoHome = bol
    }

    fun isBackPressed():Boolean = isBackPressed


    open fun onBackPress() {
        Log.e("onBackPress","REGULAR")
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


    }

    override fun onPause() {
        super.onPause()
        if(this::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.isEnabled = false
            //onBackPressedCallback.remove()
        }
    }


    fun gotoFrag(
        destination: Int,
        data: Bundle? = null,
        navOptions: NavOptions? = null,
        navExtras: Navigator.Extras? = null
    ) {

        val navController = findNavController()
        navController.navigate(destination, data, navOptions, navExtras)

    }


    fun changeMainViewPager(page:Int)
    {
        (activity as MainActivityDeenSDK).setViewPager(page)
    }



    fun showBottomNav(bol:Boolean)
    {
        (activity as MainActivityDeenSDK).showBottomNav(bol)
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
        (requireActivity() as MainActivityDeenSDK).setupOtherFragment(bol)
    }

    fun setupActionForOtherFragment(
        action1:Int,
        action2:Int,
        callback: otherFagmentActionCallback?=null,
        actionnBartitle:String,
        backEnable:Boolean=true,
        view: View,
        isBackIcon:Boolean = false,
        isDarkActionBar:Boolean = false
    )
    {

        val action1Btn:AppCompatImageView = view.findViewById(R.id.action1)
        val action2Btn:AppCompatImageView = view.findViewById(R.id.action2)
        val btnBack:AppCompatImageView = view.findViewById(R.id.btnBack)
        val title:AppCompatTextView = view.findViewById(R.id.title)

        if(backEnable)
        {
            btnBack.setImageDrawable(AppCompatResources.getDrawable(view.context, R.drawable.ic_back))
            btnBack.visible(true)
            btnBack.setOnClickListener {
                this.requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            title.text = actionnBartitle
            title.setTextColor(ContextCompat.getColor(title.context,R.color.deen_txt_black_deep))

            if(isDarkActionBar)
                title.setTextColor(ContextCompat.getColor(title.context,R.color.deen_white))
            else
                title.setTextColor(ContextCompat.getColor(title.context,R.color.deen_txt_black_deep))

        }
        else
        {
            (title.layoutParams as ConstraintLayout.LayoutParams).apply {
                leftMargin=16.dp
            }
            title.text = actionnBartitle
            btnBack.visible(isBackIcon)
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

        actionCallback = callback

      /*  if(findNavController().currentDestination?.id?.isBottomNavFragment == false)
            setupOtherFragment(true)
        else
            setupOtherFragment(false)*/
    }

    fun setupGlobalMiniPlayerForHome(height: Int) {

        if(activity!=null)
            (activity as MainActivityDeenSDK).globalMiniPlayerForHome(height)
    }

    fun setupAction(action1:Int,action2:Int,callback: actionCallback)
    {
        (activity as MainActivityDeenSDK).setupActionbar(action1,action2,callback)
    }

    fun setupAction(action1:Int,action2:Int,callback: actionCallback?=null,title:String,backEnable:Boolean=false)
    {
        (activity as MainActivityDeenSDK).setupActionbar(title,backEnable)
        (activity as MainActivityDeenSDK).setupActionbar(action1,action2,callback)
    }

    fun setupSearchbar(callback: searchCallback?=null)
    {
        (activity as MainActivityDeenSDK).setupSearchbar(callback)
    }

    fun hideSearchbar()
    {
        (activity as MainActivityDeenSDK).hideSearch()
    }

    fun setupAction(action1:Int,action2:Int,title:String,backEnable:Boolean=false)
    {
        (activity as MainActivityDeenSDK).setupActionbar(title,backEnable)
        (activity as MainActivityDeenSDK).setupActionbar(action1,action2)
    }

    inline fun <T : View> prepareStubView(
        stub: AsyncViewStub,
        layoutID:Int,
        crossinline prepareBlock: T.() -> Unit
    ) {
        stub.layoutRes = layoutID
        val inflatedView = stub.inflatedId as? T

        if (inflatedView != null) {
            inflatedView.prepareBlock()
        } else {
            stub.inflate(AsyncLayoutInflater.OnInflateFinishedListener { view, _, _ ->
                (view as? T)?.prepareBlock()
            })
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

   /* override fun onDestroyView() {

        //unregister listener here
        if(this::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.isEnabled = false
            onBackPressedCallback.remove()
        }

        (activity as MainActivity).enableBackPress()

        super.onDestroyView()


    }*/

    override fun onResume() {
        super.onResume()

        if(this::onBackPressedCallback.isInitialized && this::childFragment.isInitialized)
            setupBackPressCallback(childFragment,isBacktoHome)


       /* if(this::onBackPressedCallback.isInitialized && this::childFragment.isInitialized)
            onBackPressedCallback.isEnabled = true


        requireView().addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            when (v.visibility) {
                View.VISIBLE -> {

                    if (this::onBackPressedCallback.isInitialized && !onBackPressedCallback.isEnabled) {
                        onBackPressedCallback.isEnabled = true
                    }

                    // View is now visible
                }

                else -> {
                    // View is not visible
                }
            }
        }*/
    }

    fun setupCommonLayout(mainview:View) {

        nodataLayout = mainview.findViewById(R.id.nodataLayout)
        noInternetLayout = mainview.findViewById(R.id.no_internet_layout)
        noInternetRetry = noInternetLayout?.findViewById(R.id.no_internet_retry)
        progressLayout = mainview.findViewById(R.id.progressLayout)

        progressLayout?.let {
            ViewCompat.setTranslationZ(it, 10F)
        }

        noInternetLayout?.let {
            ViewCompat.setTranslationZ(it, 10F)
        }

        nodataLayout?.let {
            ViewCompat.setTranslationZ(it, 10F)
        }

        noInternetRetry?.setOnClickListener {
            noInternetRetryClicked()
        }

    }

    open fun noInternetRetryClicked() {}

    fun baseLoadingState()
    {
        progressLayout?.visible(true)
        nodataLayout?.visible(false)
        noInternetLayout?.visible(false)
    }

    fun baseEmptyState()
    {
        progressLayout?.hide()
        nodataLayout?.show()
        noInternetLayout?.hide()
    }

    fun baseNoInternetState()
    {
        progressLayout?.hide()
        nodataLayout?.hide()
        noInternetLayout?.show()
    }

    fun baseViewState()
    {
        progressLayout?.hide()
        nodataLayout?.hide()
        noInternetLayout?.hide()
    }

}

internal interface otherFagmentActionCallback
{
    fun action1()
    fun action2()

    fun action3(){

    }
}