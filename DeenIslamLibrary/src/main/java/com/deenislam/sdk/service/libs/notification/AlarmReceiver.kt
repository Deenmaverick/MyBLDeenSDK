package com.deenislam.sdk.service.libs.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.deenislam.sdk.DeenSDKCore
import com.deenislam.sdk.service.database.entity.PrayerNotification
import com.deenislam.sdk.service.di.DatabaseProvider
import com.deenislam.sdk.service.di.NetworkProvider
import com.deenislam.sdk.service.repository.PrayerTimesRepository
import com.deenislam.sdk.utils.MilliSecondToStringTime
import com.deenislam.sdk.utils.StringTimeToMillisecond
import com.deenislam.sdk.utils.sendNotification
import com.deenislam.sdk.utils.tryCatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if(DeenSDKCore.appContext == null)
        DeenSDKCore.appContext = context.applicationContext
        if(DeenSDKCore.baseContext == null)
        DeenSDKCore.baseContext = context
        if(DeenSDKCore.GetDeenToken().isEmpty())
            DeenSDKCore.SetDeenToken("MyBLSDK")

        AzanPlayer.releaseMediaPlayer()

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {

            }
            else ->
            {

                AzanPlayer.releaseMediaPlayer()

                val prayer_notification_id = intent?.extras?.getInt("pid",0)?:0
                val system_notification_id = intent?.extras?.getInt("notification_id",-1)?:-1

                Log.e("AlarmReceiver_Prayer", prayer_notification_id.toString())

                if(prayer_notification_id>0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        processNotification(prayer_notification_id, context)
                    }
                }

                val isNotificationDismiss = intent?.extras?.getString("dismiss").toString()

                if(isNotificationDismiss == "ok")
                    AzanPlayer.releaseMediaPlayer()

                if (system_notification_id != -1) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(system_notification_id)
                }


                /*val service = Intent(context, AlarmReceiverService::class.java)
                service.putExtra("pid",intent.extras?.getInt("pid",0)?:0 )
                service.putExtra("dismiss",intent.extras?.getString("dismiss").toString() )
                service.putExtra("notification_id",intent.extras?.getInt("notification_id",-1)?:-1)

                context.startService(service)*/
             /*   val service = Intent(context, Notificationservice::class.java)
                service.putExtra("pid",intent.extras?.getInt("pid",0)?:0 )
                service.putExtra("dismiss",intent.extras?.getString("dismiss").toString() )
                context.startService(service)
*/
               /* val prayer_notification_id = intent.extras?.getInt("pid",0)?:0

                Log.e("AlarmReceiver_Prayer", prayer_notification_id.toString())

                if(prayer_notification_id>0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        processNotification(prayer_notification_id, context)
                    }
                }

                val isNotificationDismiss = intent.extras?.getString("dismiss").toString()

                if(isNotificationDismiss == "ok")
                    AzanPlayer.releaseMediaPlayer()*/

            }
        }

    }

    private suspend fun processNotification(prayer_notification_id: Int, context: Context)
    {

        if(!IsNotificationEnable())
            return

        get_prayer_notification_data(prayer_notification_id)?.let {

            if(it.state!=3 && it.state!=2)
                return


            val prayerName = get_prayer_name_by_tag(it.prayer_tag)

            prayerName?.let {
                    name->
                // sendNotification
                val notificationManager = ContextCompat.getSystemService(
                    context,
                    NotificationManager::class.java
                ) as NotificationManager

                val pid = it.id
                val prayerDate = it.date.StringTimeToMillisecond("dd/MM/yyyy").MilliSecondToStringTime("dd/MM/yyyy",1)

                clear_prayer_notification_by_id(pid)

                if(prayerName == "Isha") {
                    val prayerTimesRepository = PrayerTimesRepository(
                        deenService = NetworkProvider().getInstance().provideDeenService(),
                        prayerNotificationDao = DatabaseProvider().getInstance()
                            .providePrayerNotificationDao(),
                        prayerTimesDao = null
                    )

                    prayerTimesRepository.refill_prayer_notification_for_alarm_service(prayerDate)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if(!notificationManager.areNotificationsEnabled())
                        return
                }
                else if(!NotificationManagerCompat.from(context).areNotificationsEnabled())
                    return



                notificationManager.sendNotification(
                    applicationContext = context,
                    title = "Prayer Alert!",
                    messageBody = "it's time for $prayerName",
                    largeImg = null,
                    channelID = "Prayer Time",
                    notification_id = prayer_notification_id
                )

                if(it.state == 3) {

                    /* if (prayerName == "Fajr")
                         AzanPlayer.playAdanFromRawFolder(context, R.raw.azan_common_fajr)
                     else
                         AzanPlayer.playAdanFromRawFolder(context, R.raw.azan_common)*/

                    if (prayerName == "Fajr")
                        AzanPlayer.playAdanFromUrl("https://islamic-content.sgp1.digitaloceanspaces.com/Content/SDK/Azan/azan_common_fajr.mp3")
                    else
                        AzanPlayer.playAdanFromUrl("https://islamic-content.sgp1.digitaloceanspaces.com/Content/SDK/Azan/azan_common.mp3")

                }

            }

        }
    }

    private suspend fun clear_prayer_notification_by_id(pid:Int) =

        withContext(Dispatchers.IO)
        {
            val prayerNotificationDao = DatabaseProvider().getInstance().providePrayerNotificationDao()

            //prayerNotificationDao?.clearNotificationByID(pid)

            tryCatch {
                prayerNotificationDao?.deleteNotificationByID(pid)
            }

        }

    private suspend fun get_prayer_notification_data(pid:Int): PrayerNotification? =

        withContext(Dispatchers.IO)
        {

            val prayerNotificationDao = DatabaseProvider().getInstance().providePrayerNotificationDao()

            val prayerNotificationData = prayerNotificationDao?.select(pid)

            if (prayerNotificationData?.isNotEmpty() == true)
                prayerNotificationData[0]
            else
                null

        }

    private suspend fun IsNotificationEnable():Boolean =
        withContext(Dispatchers.IO)
        {
            val prayerTimesRepository = PrayerTimesRepository(
                deenService = null,
                prayerNotificationDao = DatabaseProvider().getInstance()
                    .providePrayerNotificationDao(),
                prayerTimesDao = null
            )

            val getStatus = prayerTimesRepository.getNotificationData(
                date = "",
                prayer_tag = "Notification",
                finalState = 0
            )

            getStatus?.state == 1

        }


    private fun get_prayer_name_by_tag(tag:String): String? =
        when(tag)
        {
            "pt1"-> "Fajr"
            "pt2"-> "Sunrise"

            "pt3"-> "Dhuhr"

            "pt4"-> "Asr"

            "pt5"-> "Maghrib"

            "pt6"-> "Isha"

            "opt3"-> "Tahajjud"

            "opt1"-> "Suhoor"

            "opt4"-> "Ishraq"

            "opt2"-> "Iftaar"

            else -> null

        }

}