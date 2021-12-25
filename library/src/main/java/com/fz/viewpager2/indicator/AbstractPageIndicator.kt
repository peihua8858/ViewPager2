package com.fz.viewpager2.indicator

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.fz.viewpager2.AutoScrollLoopPagerAdapter
import com.fz.viewpager2.AutoScrollLoopViewPager2

/**
 * ViewPager2指示器基础封装
 * @author dingpeihua
 * @date 2021/12/25 12:37
 * @version 1.0
 */
abstract class AbstractPageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle), PageIndicator {
    private var mInnerCallback: ViewPager2.OnPageChangeCallback? = null
    protected var mListener: ViewPager2.OnPageChangeCallback? = null
    protected var mViewPager: ViewPager2? = null
    protected var mCurrentPage = 0
    override fun setViewPager(view: ViewPager2) {
        if (mViewPager == view) {
            return
        }
        if (mViewPager != null) {
            //Clear us from the old pager.
            mViewPager!!.unregisterOnPageChangeCallback(mInnerCallback!!)
        }
        checkNotNull(view.adapter) { "ViewPager does not have adapter instance." }
        mViewPager = view
        if (mInnerCallback == null) {
            mInnerCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    if (mListener != null) {
                        mListener!!.onPageScrolled(
                            position,
                            positionOffset,
                            positionOffsetPixels
                        )
                    }
                }

                override fun onPageSelected(position: Int) {
                    mCurrentPage = position
                    invalidate()
                    if (mListener != null) {
                        mListener!!.onPageSelected(mCurrentPage)
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {
                    if (mListener != null) {
                        mListener!!.onPageScrollStateChanged(state)
                    }
                }
            }
        }
        mViewPager?.registerOnPageChangeCallback(mInnerCallback!!)
        invalidate()
    }

    override fun setViewPager(view: ViewPager2, initialPosition: Int) {
        setViewPager(view)
        setCurrentItem(initialPosition)
    }

    override fun setViewPager(view: AutoScrollLoopViewPager2) {
        if (mViewPager == view.viewPager2) {
            return
        }
        checkNotNull(view.adapter) { "ViewPager does not have adapter instance." }
        mViewPager = view.viewPager2
        if (mInnerCallback == null) {
            mInnerCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    Log.d(
                        LinePageIndicator.TAG,
                        "onPageScrolled: $position positionOffset: $positionOffset positionOffsetPixels: $positionOffsetPixels"
                    )
                    if (mListener != null) {
                        mListener!!.onPageScrolled(
                            position,
                            positionOffset,
                            positionOffsetPixels
                        )
                    }
                }

                override fun onPageSelected(position: Int) {
                    Log.d(LinePageIndicator.TAG, "onPageSelected: $position")
                    mCurrentPage = position
                    invalidate()
                    if (mListener != null) {
                        mListener!!.onPageSelected(position)
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {
                    if (mListener != null) {
                        mListener!!.onPageScrollStateChanged(state)
                    }
                }
            }
        }
        view.setOnPageChangeCallback(mInnerCallback!!)
        invalidate()
    }

    override fun setViewPager(view: AutoScrollLoopViewPager2, initialPosition: Int) {
        setViewPager(view)
        setCurrentItem(initialPosition)
    }

    override fun setCurrentItem(item: Int) {
        checkNotNull(mViewPager) { "ViewPager has not been bound." }
        mViewPager!!.currentItem = item
        mCurrentPage = item
        invalidate()
    }

    override fun notifyDataSetChanged() {
        invalidate()
    }

    override fun setOnPageChangeListener(listener: ViewPager2.OnPageChangeCallback) {
        mListener = listener
    }

    override val itemCount: Int
        get() {
            val adapter = mViewPager?.adapter
            if (adapter is AutoScrollLoopPagerAdapter) {
                return adapter.realItemCount
            }
            return adapter?.itemCount ?: 0
        }
}