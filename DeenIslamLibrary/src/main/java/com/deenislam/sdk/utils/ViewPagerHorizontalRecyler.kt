package com.deenislam.sdk.utils

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import java.lang.Math.abs

internal class ViewPagerHorizontalRecyler {

    private var instance: ViewPagerHorizontalRecyler? = null

    fun getInstance(): ViewPagerHorizontalRecyler {
        if (instance == null)
            instance = ViewPagerHorizontalRecyler()

        return instance as ViewPagerHorizontalRecyler
    }

    fun load(recyclerView: RecyclerView)
    {
        val gestureDetector = GestureDetector(recyclerView.context, object: GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                recyclerView.parent.requestDisallowInterceptTouchEvent(true)
                return super.onDown(e)
            }


            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {


                if (kotlin.math.abs(distanceX) > abs(distanceY)) {
                    recyclerView.parent.requestDisallowInterceptTouchEvent(true)
                } else if (abs(distanceY) > 0) {
                    recyclerView.parent.requestDisallowInterceptTouchEvent(false)
                }

                Log.e("DISTANCEY",e2.action.toString())
                //recyclerView.parent.requestDisallowInterceptTouchEvent(true)
                return super.onScroll(e1, e2, distanceX, distanceY)
            }
        })

        recyclerView.addOnItemTouchListener(object: RecyclerView.OnItemTouchListener {
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }
        })
    }
}