package com.deenislam.sdk.views.quran.quranplayer

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.deenislam.sdk.R
import com.deenislam.sdk.service.di.DatabaseProvider
import com.deenislam.sdk.service.models.quran.quranplayer.ThemeResource
import com.deenislam.sdk.service.repository.quran.quranplayer.PlayerControlRepository
import com.deenislam.sdk.utils.numberLocale
import com.deenislam.sdk.viewmodels.quran.quranplayer.PlayerControlViewModel
import com.deenislam.sdk.views.base.BaseRegularFragment
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

internal class PlayerThemeFragment : BaseRegularFragment() {

    private lateinit var viewmodel: PlayerControlViewModel

    private lateinit var fontControl:Slider
    private lateinit var ayatArabic:AppCompatTextView
    private lateinit var defaultFontBtn:AppCompatTextView

    private var updateSettingCall:Boolean = false
    //setting
    private var theme_font_size:Float = 0F
    private var arabic_font:Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // init viewmodel
        val repository = PlayerControlRepository(DatabaseProvider().getInstance().providePlayerSettingDao())

        val factory = VMFactory(repository)
        viewmodel = ViewModelProvider(
            requireActivity(),
            factory
        )[PlayerControlViewModel::class.java]

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val mainview = localInflater.inflate(R.layout.fragment_player_theme, container, false)

        //init view
        fontControl = mainview.findViewById(R.id.fontControl)
        ayatArabic = mainview.findViewById(R.id.ayatArabic)
        defaultFontBtn = mainview.findViewById(R.id.defaultFontBtn)

        return mainview
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSetting()
        initObserver()

        fontControl.addOnChangeListener { slider, value, fromUser ->
            // Responds to when slider's value is changed
            Log.e("fontControl",value.toString())
            setupFontControl(value)
            lifecycleScope.launch {
                theme_font_size = value
                updateSettingCall = true
                viewmodel.updateThemeSetting(theme_font_size,arabic_font)
            }

        }
    }

    private fun loadSetting()
    {
        lifecycleScope.launch {
            updateSettingCall = false
            viewmodel.getSetting()
        }
    }

    private fun initObserver()
    {
        viewmodel.themeLiveData.observe(viewLifecycleOwner)
        {
            when(it)
            {
                is ThemeResource.playerSettings ->
                {
                    it.setting?.theme_font_size?.let { it1 -> setupFontControl(it1) }
                }
            }
        }
    }

    private fun setupFontControl(value:Float)
    {


        when(value)
        {
             0F ->
            {
                ayatArabic.setTextSize(TypedValue.COMPLEX_UNIT_SP,24F)
                defaultFontBtn.text = localContext.resources.getString(R.string.default_txt)

            }

            20F ->
            {
                ayatArabic.setTextSize(TypedValue.COMPLEX_UNIT_SP,26F)
                defaultFontBtn.text = "120%".numberLocale()
            }

            40F ->
            {
                ayatArabic.setTextSize(TypedValue.COMPLEX_UNIT_SP,28F)
                defaultFontBtn.text = "140%".numberLocale()
            }

            60F ->
            {
                ayatArabic.setTextSize(TypedValue.COMPLEX_UNIT_SP,30F)
                defaultFontBtn.text = "160%".numberLocale()
            }

            80F->
            {
                ayatArabic.setTextSize(TypedValue.COMPLEX_UNIT_SP,32F)
                defaultFontBtn.text = "180%".numberLocale()
            }

            100F ->
            {
                ayatArabic.setTextSize(TypedValue.COMPLEX_UNIT_SP,34F)
                defaultFontBtn.text = "200%".numberLocale()
            }
            else -> updateSettingCall = true
        }

        if(!updateSettingCall)
            fontControl.value = value
    }

    inner class VMFactory(
        private val repository: PlayerControlRepository
    ) : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerControlViewModel(repository) as T
        }
    }

}