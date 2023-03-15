package com.fz.viewpager2

import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import java.lang.ref.WeakReference

class AutoScrollLoopViewPager2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : NestedScrollableHost(
    context,
    attrs,
    defStyleAttr, defStyleRes
), DefaultLifecycleObserver {
    companion object {
        const val TAG = "AutoScrollLoopViewPager"
    }

    private var mViewPager2 = ViewPager2(context)
    private var canAutoTurning = false
    private var autoTurningTime: Long = 0
    private var isTurning = false
    private var mAutoTurningRunnable: AutoTurningRunnable? = null
    private var mPendingCurrentItem = RecyclerView.NO_POSITION
    private val mAdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            val itemCount = adapter?.itemCount ?: 0
            if (itemCount <= 1) {
                if (isTurning) {
                    stopAutoTurning()
                }
            } else {
                if (!isTurning) {
                    startAutoTurning()
                }
            }
        }
    }
    private var onPageChangeCallback: OnPageChangeCallback? = null

    init {
        mViewPager2.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mViewPager2.offscreenPageLimit = 1
        val mCycleOnPageChangeCallback = CycleOnPageChangeCallback()
        mViewPager2.registerOnPageChangeCallback(mCycleOnPageChangeCallback)
        mAutoTurningRunnable = AutoTurningRunnable(this)
        addView(mViewPager2)
    }

    fun setOnPageChangeCallback(callback: OnPageChangeCallback) {
        this.onPageChangeCallback = callback
    }

    fun setAutoTurning(autoTurningTime: Long) {
        setAutoTurning(true, autoTurningTime)
    }

    fun setAutoTurning(canAutoTurning: Boolean, autoTurningTime: Long) {
        this.canAutoTurning = canAutoTurning
        this.autoTurningTime = autoTurningTime
        stopAutoTurning()
        startAutoTurning()
    }

    fun startAutoTurning() {
        if (!canAutoTurning || autoTurningTime <= 0 || isTurning) {
            return
        }
        isTurning = true
        postDelayed(mAutoTurningRunnable, autoTurningTime)
    }

    fun stopAutoTurning() {
        isTurning = false
        removeCallbacks(mAutoTurningRunnable)
    }

    private var mInnerAdapter: AutoScrollLoopPagerAdapter<*>? = null
    var adapter: RecyclerView.Adapter<*>?
        get() = mInnerAdapter
        set(adapter) {
            mInnerAdapter = AutoScrollLoopPagerAdapter(adapter!!)
            mInnerAdapter!!.registerAdapterDataObserver(mAdapterDataObserver)
            mViewPager2.adapter = mInnerAdapter
            setInnerCurrentItem(1, false)
        }
    private val pagerRealCount: Int
        get() = mInnerAdapter!!.realItemCount

    @get:ViewPager2.Orientation
    var orientation: Int
        get() = mViewPager2.orientation
        set(orientation) {
            mViewPager2.orientation = orientation
        }

    fun setPageTransformer(transformer: ViewPager2.PageTransformer?) {
        mViewPager2.setPageTransformer(transformer)
    }

    fun addItemDecoration(decor: ItemDecoration) {
        mViewPager2.addItemDecoration(decor)
    }

    fun addItemDecoration(decor: ItemDecoration, index: Int) {
        mViewPager2.addItemDecoration(decor, index)
    }

    fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        val position = if (item >= pagerRealCount) {
            pagerRealCount
        } else if (item <= 0) {
            1
        } else item + 1
        setInnerCurrentItem(position, smoothScroll)
    }

    private fun setInnerCurrentItem(item: Int, smoothScroll: Boolean) {
        Log.d(TAG, "setCurrentItem $item")
        try {
            mViewPager2.setCurrentItem(item, smoothScroll)
            if (!smoothScroll) {
                onPageChangeCallback?.onPageSelected(getRealPosition(item))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var currentItem: Int
        get() = getRealPosition(viewPager2.currentItem)
        set(item) {
            setCurrentItem(item, true)
        }

    fun getRealPosition(position: Int): Int {
        return mInnerAdapter?.getRealPosition(position) ?: 0
    }

    var offscreenPageLimit: Int
        get() = mViewPager2.offscreenPageLimit
        set(limit) {
            mViewPager2.offscreenPageLimit = limit
        }

    fun registerOnPageChangeCallback(callback: OnPageChangeCallback) {
        mViewPager2.registerOnPageChangeCallback(callback)
    }

    fun unregisterOnPageChangeCallback(callback: OnPageChangeCallback) {
        mViewPager2.unregisterOnPageChangeCallback(callback)
    }

    val viewPager2: ViewPager2
        get() = mViewPager2

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            if (canAutoTurning && isTurning) {
                stopAutoTurning()
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
            if (canAutoTurning) {
                startAutoTurning()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAutoTurning()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoTurning()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.mCurrentItem = currentItem
        Log.d(TAG, "onSaveInstanceState: " + ss.mCurrentItem)
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val ss = state
        super.onRestoreInstanceState(ss.superState)
        mPendingCurrentItem = ss.mCurrentItem
        Log.d(TAG, "onRestoreInstanceState: $mPendingCurrentItem")
        restorePendingState()
    }

    private fun restorePendingState() {
        if (mPendingCurrentItem == RecyclerView.NO_POSITION) {
            // No state to restore, or state is already restored
            return
        }
        val currentItem = Math.max(
            0, Math.min(
                mPendingCurrentItem, mInnerAdapter?.itemCount ?: 0 - 1
            )
        )
        Log.d(TAG, "restorePendingState: $currentItem")
        mPendingCurrentItem = RecyclerView.NO_POSITION
        setInnerCurrentItem(currentItem, false)
    }

    private inner class CycleOnPageChangeCallback : OnPageChangeCallback() {
        private var isBeginPagerChange = false
        private val INVALID_ITEM_POSITION = -1
        private var mTempPosition = INVALID_ITEM_POSITION
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            Log.d(
                TAG,
                "onPageScrolled: $position positionOffset: $positionOffset positionOffsetPixels: $positionOffsetPixels,realCurrentItem: ${
                    getRealPosition(
                        position
                    )
                }"
            )
            onPageChangeCallback?.onPageScrolled(
                position,
                positionOffset,
                positionOffsetPixels
            )
        }

        override fun onPageSelected(position: Int) {
            Log.d(TAG, "onPageSelected: $position,realCurrentItem: ${getRealPosition(position)}")
            if (isBeginPagerChange) {
                mTempPosition = position
            }
            onPageChangeCallback?.onPageSelected(getRealPosition(position))
        }

        override fun onPageScrollStateChanged(state: Int) {
            Log.d(TAG, "onPageScrollStateChanged: state $state")
            if (state == ViewPager2.SCROLL_STATE_DRAGGING ||
                isTurning && state == ViewPager2.SCROLL_STATE_SETTLING
            ) {
                isBeginPagerChange = true
            } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                isBeginPagerChange = false
                val fixCurrentItem = getFixCurrentItem(mTempPosition)
                if (fixCurrentItem != INVALID_ITEM_POSITION && fixCurrentItem != mTempPosition) {
                    mTempPosition = INVALID_ITEM_POSITION
                    Log.d(TAG, "onPageScrollStateChanged: fixCurrentItem $fixCurrentItem")
                    setInnerCurrentItem(fixCurrentItem, false)
                }
            }
            onPageChangeCallback?.onPageScrollStateChanged(state)
        }

        private fun getFixCurrentItem(position: Int): Int {
            if (position == INVALID_ITEM_POSITION) {
                return INVALID_ITEM_POSITION
            }
            val lastPosition = (mInnerAdapter?.itemCount ?: 0) - 1
            var fixPosition = INVALID_ITEM_POSITION
            if (position == 0) {
                fixPosition = if (lastPosition == 0) 0 else lastPosition - 1
            } else if (position == lastPosition) {
                fixPosition = 1
            }
            return fixPosition
        }
    }

    internal class AutoTurningRunnable(cycleViewPager2: AutoScrollLoopViewPager2) : Runnable {
        private val reference = WeakReference(cycleViewPager2)
        override fun run() {
            val cycleViewPager2 = reference.get()
            if (cycleViewPager2 != null && cycleViewPager2.canAutoTurning && cycleViewPager2.isTurning) {
                val itemCount = cycleViewPager2.adapter?.itemCount ?: 0
                if (itemCount == 0) {
                    return
                }
                val currentItem = cycleViewPager2.currentItem
                val nextItem = (currentItem + 1) % itemCount
                cycleViewPager2.setInnerCurrentItem(nextItem, true)
                cycleViewPager2.postDelayed(
                    cycleViewPager2.mAutoTurningRunnable,
                    cycleViewPager2.autoTurningTime
                )
            }
        }

    }

    internal class SavedState : BaseSavedState {
        var mCurrentItem = 0

        constructor(source: Parcel) : super(source) {
            readValues(source, null)
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            readValues(source, loader)
        }

        constructor(superState: Parcelable?) : super(superState) {}

        private fun readValues(source: Parcel, loader: ClassLoader?) {
            mCurrentItem = source.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(mCurrentItem)
        }

        companion object {
            val CREATOR: Parcelable.Creator<SavedState> = object : ClassLoaderCreator<SavedState> {
                override fun createFromParcel(source: Parcel, loader: ClassLoader?): SavedState {
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) SavedState(
                        source,
                        loader
                    ) else SavedState(source)
                }

                override fun createFromParcel(source: Parcel): SavedState {
                    return createFromParcel(source, null)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    fun beginFakeDrag(): Boolean {
        return mViewPager2.beginFakeDrag()
    }

    val isFakeDragging: Boolean
        get() = mViewPager2.isFakeDragging

    fun fakeDragBy(@Px offsetPxFloat: Float): Boolean {
        return mViewPager2.fakeDragBy(offsetPxFloat)
    }

    fun endFakeDrag(): Boolean {
        return mViewPager2.endFakeDrag()
    }

    override fun onResume(owner: LifecycleOwner) {
        startAutoTurning()
    }

    override fun onPause(owner: LifecycleOwner) {
        stopAutoTurning()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopAutoTurning()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopAutoTurning()
    }
}