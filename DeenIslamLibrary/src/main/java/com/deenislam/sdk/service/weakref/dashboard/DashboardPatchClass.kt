package com.deenislam.sdk.service.weakref.dashboard

import android.util.Log
import com.deenislam.sdk.views.dashboard.patch.Allah99Names
import com.deenislam.sdk.views.dashboard.patch.Billboard
import com.deenislam.sdk.views.dashboard.patch.Compass
import java.lang.ref.WeakReference

internal object DashboardPatchClass {

    // patch helper class instances with WeakReference
    private var billboardRef: Billboard? = null
    private var compassRef: WeakReference<Compass>? = null
    private var allah99NamesRef: WeakReference<Allah99Names>? = null

    // Getter methods for instances
    fun getBillboardInstance(): Billboard? = billboardRef
    fun getCompassInstance(): Compass? = compassRef?.get()
    fun getAllah99NamesInstance(): Allah99Names? = allah99NamesRef?.get()

    // Update methods for instances
    fun updateBillboard(billboard: Billboard) {
        Log.e("updateBillboard",billboard.toString())
        billboardRef = billboard
    }

    fun updateCompass(compass: Compass) {
        compassRef = WeakReference(compass)
    }

    fun updateAllah99Names(allah99Names: Allah99Names) {
        allah99NamesRef = WeakReference(allah99Names)
    }


    // Clear references method
    fun clearReferences() {
        //billboardRef?.clear()
        compassRef?.clear()
        allah99NamesRef?.clear()
    }
}

