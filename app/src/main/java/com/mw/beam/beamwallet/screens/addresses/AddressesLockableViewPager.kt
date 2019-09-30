package com.mw.beam.beamwallet.screens.addresses

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

class AddressesLockableViewPager(context: Context, attributeSet: AttributeSet) : ViewPager(context, attributeSet) {

    private var locked = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (locked) {
            true -> false
            false -> super.onTouchEvent(event)
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return when (locked) {
            true -> false
            false -> super.onInterceptTouchEvent(event)
        }
    }

    fun setMode(mode: AddressesFragment.Mode) {
        when(mode){
            AddressesFragment.Mode.EDIT -> this.locked = true
            else -> this.locked = false
        }
    }

}