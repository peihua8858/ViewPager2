package com.fz.viewpager2.indicator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.fz.viewpager2.AutoScrollLoopViewPager2
import com.fz.viewpager2.R
import com.fz.viewpager2.AutoScrollLoopPagerAdapter
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Draws a line for each page. The current page line is colored differently
 * than the unselected page lines.
 */
class LinePageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.vpiLinePageIndicatorStyle
) : AbstractPageIndicator(context, attrs, defStyle) {
    private val mPaintUnselected = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintSelected = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mCentered: Boolean
    private var mLineWidth: Float
    private var mGapWidth: Float
    private val mTouchSlop: Int
    private var mLastMotionX = -1f
    private var mActivePointerId = INVALID_POINTER
    private var mIsDragging = false
    var isCentered: Boolean
        get() = mCentered
        set(centered) {
            mCentered = centered
            invalidate()
        }
    var unselectedColor: Int
        get() = mPaintUnselected.color
        set(unselectedColor) {
            mPaintUnselected.color = unselectedColor
            invalidate()
        }
    var selectedColor: Int
        get() = mPaintSelected.color
        set(selectedColor) {
            mPaintSelected.color = selectedColor
            invalidate()
        }
    var lineWidth: Float
        get() = mLineWidth
        set(lineWidth) {
            mLineWidth = lineWidth
            invalidate()
        }
    var strokeWidth: Float
        get() = mPaintSelected.strokeWidth
        set(lineHeight) {
            mPaintSelected.strokeWidth = lineHeight
            mPaintUnselected.strokeWidth = lineHeight
            invalidate()
        }
    var gapWidth: Float
        get() = mGapWidth
        set(gapWidth) {
            mGapWidth = gapWidth
            invalidate()
        }

//    val itemCount: Int
//        get() {
//            val adapter = mViewPager?.adapter
//            if (adapter is AutoScrollLoopPagerAdapter) {
//                return adapter.realItemCount
//            }
//            return adapter?.itemCount ?: 0
//        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mViewPager == null) {
            return
        }
        val count = itemCount
        if (count == 0) {
            return
        }
        if (mCurrentPage >= count) {
//            setCurrentItem(count - 1)
            return
        }
        val lineWidthAndGap = mLineWidth + mGapWidth
        val indicatorWidth = count * lineWidthAndGap - mGapWidth
        val paddingTop = paddingTop.toFloat()
        val paddingLeft = paddingLeft.toFloat()
        val paddingRight = paddingRight.toFloat()
        val verticalOffset = paddingTop + (height - paddingTop - paddingBottom) / 2.0f
        var horizontalOffset = paddingLeft
        if (mCentered) {
            horizontalOffset += (width - paddingLeft - paddingRight) / 2.0f - indicatorWidth / 2.0f
        }
        Log.d(TAG, "onDraw: $mCurrentPage")
        //Draw stroked circles
        for (i in 0 until count) {
            val dx1 = horizontalOffset + i * lineWidthAndGap
            val dx2 = dx1 + mLineWidth
            canvas.drawLine(
                dx1,
                verticalOffset,
                dx2,
                verticalOffset,
                if (i == mCurrentPage) mPaintSelected else mPaintUnselected
            )
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (super.onTouchEvent(ev)) {
            return true
        }
        if (mViewPager == null || itemCount == 0) {
            return false
        }
        when (val action = ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = ev.getPointerId(0)
                mLastMotionX = ev.x
            }
            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = ev.findPointerIndex(mActivePointerId)
                val x = ev.getX(activePointerIndex)
                val deltaX = x - mLastMotionX
                if (!mIsDragging) {
                    if (abs(deltaX) > mTouchSlop) {
                        mIsDragging = true
                    }
                }
                if (mIsDragging) {
                    mLastMotionX = x
                    if (mViewPager!!.isFakeDragging || mViewPager!!.beginFakeDrag()) {
                        mViewPager!!.fakeDragBy(deltaX)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (!mIsDragging) {
                    val count = itemCount
                    val width = width
                    val halfWidth = width / 2f
                    val sixthWidth = width / 6f
                    if (mCurrentPage > 0 && ev.x < halfWidth - sixthWidth) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            mViewPager!!.currentItem = mCurrentPage - 1
                        }
                        return true
                    } else if (mCurrentPage < count - 1 && ev.x > halfWidth + sixthWidth) {
                        if (action != MotionEvent.ACTION_CANCEL) {
                            mViewPager!!.currentItem = mCurrentPage + 1
                        }
                        return true
                    }
                }
                mIsDragging = false
                mActivePointerId = INVALID_POINTER
                if (mViewPager!!.isFakeDragging) mViewPager!!.endFakeDrag()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                mLastMotionX = ev.getX(index)
                mActivePointerId = ev.getPointerId(index)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = ev.actionIndex
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mActivePointerId = ev.getPointerId(newPointerIndex)
                }
                mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId))
            }
            else -> {
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    /**
     * Determines the width of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private fun measureWidth(measureSpec: Int): Int {
        var result: Float
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY || mViewPager == null) {
            //We were told how big to be
            result = specSize.toFloat()
        } else {
            //Calculate the width according the views count
            val count = itemCount
            result = paddingLeft + paddingRight + count * mLineWidth + (count - 1) * mGapWidth
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = result.coerceAtMost(specSize.toFloat())
            }
        }
        return ceil(result.toDouble()).toInt()
    }

    /**
     * Determines the height of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private fun measureHeight(measureSpec: Int): Int {
        var result: Float
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            //We were told how big to be
            result = specSize.toFloat()
        } else {
            //Measure the height
            result = mPaintSelected.strokeWidth + paddingTop + paddingBottom
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = result.coerceAtMost(specSize.toFloat())
            }
        }
        return ceil(result.toDouble()).toInt()
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        mCurrentPage = savedState.currentPage
        requestLayout()
    }

    public override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.currentPage = mCurrentPage
        return savedState
    }

    internal class SavedState : BaseSavedState {
        var currentPage = 0

        constructor(superState: Parcelable?) : super(superState) {}
        private constructor(`in`: Parcel) : super(`in`) {
            currentPage = `in`.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(currentPage)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        private const val INVALID_POINTER = -1
        const val TAG = "LinePageIndicator"
    }

    init {
        val res = resources

        //Load defaults from resources
        val defaultSelectedColor = res.getColor(R.color.default_line_indicator_selected_color)
        val defaultUnselectedColor = res.getColor(R.color.default_line_indicator_unselected_color)
        val defaultLineWidth = res.getDimension(R.dimen.default_line_indicator_line_width)
        val defaultGapWidth = res.getDimension(R.dimen.default_line_indicator_gap_width)
        val defaultStrokeWidth = res.getDimension(R.dimen.default_line_indicator_stroke_width)
        val defaultCentered = res.getBoolean(R.bool.default_line_indicator_centered)

        //Retrieve styles attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.LinePageIndicator, defStyle, 0)
        mCentered = a.getBoolean(R.styleable.LinePageIndicator_centered, defaultCentered)
        mLineWidth = a.getDimension(R.styleable.LinePageIndicator_lineWidth, defaultLineWidth)
        mGapWidth = a.getDimension(R.styleable.LinePageIndicator_gapWidth, defaultGapWidth)
        strokeWidth = a.getDimension(R.styleable.LinePageIndicator_strokeWidth, defaultStrokeWidth)
        mPaintUnselected.color =
            a.getColor(R.styleable.LinePageIndicator_unselectedColor, defaultUnselectedColor)
        mPaintSelected.color =
            a.getColor(R.styleable.LinePageIndicator_selectedColor, defaultSelectedColor)
        val background = a.getDrawable(R.styleable.LinePageIndicator_android_background)
        background?.let { setBackground(it) }
        a.recycle()
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledPagingTouchSlop
    }
}