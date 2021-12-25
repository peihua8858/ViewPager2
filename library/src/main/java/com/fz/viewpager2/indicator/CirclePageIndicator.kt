package com.fz.viewpager2.indicator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.fz.viewpager2.AutoScrollLoopViewPager2
import com.fz.viewpager2.R
import kotlin.math.abs

/**
 * Draws circles (one for each view). The current view position is filled and
 * others are only stroked.
 */
class CirclePageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.vpiCirclePageIndicatorStyle
) : AbstractPageIndicator(context, attrs, defStyle){
    private var mRadius: Float
    private val showBorder: Boolean
    private val mPaintPageFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintStroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintBorder = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mSnapPage = 0
    private var mPageOffset = 0f
    private var mScrollState = 0
    private var mOrientation: Int
    private var mCentered: Boolean
    private var mSnap: Boolean
    private val mTouchSlop: Int
    private var mLastMotionX = -1f
    private var mActivePointerId = INVALID_POINTER
    private var mIsDragging = false
    private var mExtraSpacing: Float
    fun setExtraSpacing(extraSpacing: Float) {
        mExtraSpacing = extraSpacing
    }

    var isCentered: Boolean
        get() = mCentered
        set(centered) {
            mCentered = centered
            invalidate()
        }
    var pageColor: Int
        get() = mPaintPageFill.color
        set(pageColor) {
            mPaintPageFill.color = pageColor
            invalidate()
        }
    var fillColor: Int
        get() = mPaintFill.color
        set(fillColor) {
            mPaintFill.color = fillColor
            invalidate()
        }
    var orientation: Int
        get() = mOrientation
        set(orientation) {
            when (orientation) {
                LinearLayout.HORIZONTAL, LinearLayout.VERTICAL -> {
                    mOrientation = orientation
                    requestLayout()
                }
                else -> throw IllegalArgumentException("Orientation must be either HORIZONTAL or VERTICAL.")
            }
        }
    var strokeColor: Int
        get() = mPaintStroke.color
        set(strokeColor) {
            mPaintStroke.color = strokeColor
            invalidate()
        }
    var strokeWidth: Float
        get() = mPaintStroke.strokeWidth
        set(strokeWidth) {
            mPaintStroke.strokeWidth = strokeWidth
            invalidate()
        }
    var radius: Float
        get() = mRadius
        set(radius) {
            mRadius = radius
            invalidate()
        }
    var isSnap: Boolean
        get() = mSnap
        set(snap) {
            mSnap = snap
            invalidate()
        }

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
            return
        }
        val longSize: Int //
        val longPaddingBefore: Int //左边间距
        val longPaddingAfter: Int
        val shortPaddingBefore: Int
        if (mOrientation == LinearLayout.HORIZONTAL) {
            longSize = width //控件宽度
            longPaddingBefore = paddingLeft
            longPaddingAfter = paddingRight
            shortPaddingBefore = paddingTop
        } else {
            longSize = height
            longPaddingBefore = paddingTop
            longPaddingAfter = paddingBottom
            shortPaddingBefore = paddingLeft
        }
        val threeRadius = mRadius * 3 + mExtraSpacing
        val shortOffset = shortPaddingBefore + mRadius
        var longOffset = longPaddingBefore + mRadius
        if (mCentered) {
            longOffset += (longSize - longPaddingBefore - longPaddingAfter) / 2.0f - count * threeRadius / 2.0f
        }
        var dX: Float
        var dY: Float
        var pageFillRadius = mRadius
        if (mPaintStroke.strokeWidth > 0) {
            pageFillRadius -= mPaintStroke.strokeWidth / 2.0f
        }

        //Draw stroked circles
        for (iLoop in 0 until count) {
            val drawLong = longOffset + iLoop * threeRadius
            if (mOrientation == LinearLayout.HORIZONTAL) {
                dX = drawLong
                dY = shortOffset
            } else {
                dX = shortOffset
                dY = drawLong
            }
            // Only paint fill if not completely transparent
            if (mPaintPageFill.alpha > 0) {
                canvas.drawCircle(dX, dY, pageFillRadius, mPaintPageFill)
                if (showBorder) {
                    canvas.drawCircle(
                        dX,
                        dY,
                        pageFillRadius + mPaintPageFill.strokeWidth / 2 + mPaintBorder.strokeWidth / 2,
                        mPaintBorder
                    )
                }
            }

            // Only paint stroke if a stroke width was non-zero
            if (pageFillRadius != mRadius) {
                canvas.drawCircle(dX, dY, mRadius, mPaintStroke)
            }
        }

        //Draw the filled circle according to the current scroll
        var cx = (if (mSnap) mSnapPage else mCurrentPage) * threeRadius
        if (!mSnap) {
            cx += mPageOffset * threeRadius
        }
        if (mOrientation == LinearLayout.HORIZONTAL) {
            dX = longOffset + cx
            dY = shortOffset
        } else {
            dX = shortOffset
            dY = longOffset + cx
        }
        canvas.drawCircle(dX, dY, mRadius, mPaintFill)
        if (showBorder) {
            canvas.drawCircle(
                dX,
                dY,
                mRadius + mPaintFill.strokeWidth / 2 + mPaintBorder.strokeWidth / 2,
                mPaintBorder
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
                    if (mViewPager!!.beginFakeDrag() || mViewPager!!.isFakeDragging) { //参考 https://github.com/JakeWharton/ViewPagerIndicator/pull/257
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
                if (mViewPager != null && mViewPager!!.isFakeDragging) {
                    //fabric  #1119
                    try {
                        mViewPager!!.endFakeDrag()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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

    /*
     * (non-Javadoc)
     *
     * @see android.view.View#onMeasure(int, int)
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mOrientation == LinearLayout.HORIZONTAL) {
            setMeasuredDimension(measureLong(widthMeasureSpec), measureShort(heightMeasureSpec))
        } else {
            setMeasuredDimension(measureShort(widthMeasureSpec), measureLong(heightMeasureSpec))
        }
    }

    /**
     * Determines the width of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private fun measureLong(measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY || mViewPager == null) {
            //We were told how big to be
            result = specSize
        } else {
            //Calculate the width according the views count
            val count = itemCount
            result = (paddingLeft + paddingRight
                    + count * 2 * mRadius + (count - 1) * (mRadius + mExtraSpacing) + 1).toInt()
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = result.coerceAtMost(specSize)
            }
        }
        return result
    }

    /**
     * Determines the height of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private fun measureShort(measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            //We were told how big to be
            result = specSize
        } else {
            //Measure the height
            result = (2 * mRadius + paddingTop + paddingBottom + 1).toInt()
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = result.coerceAtMost(specSize)
            }
        }
        return result
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        mCurrentPage = savedState.currentPage
        mSnapPage = savedState.currentPage
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
    }

    init {
        //Load defaults from resources
        val res = resources
        val defaultPageColor =
            ContextCompat.getColor(context, R.color.default_circle_indicator_page_color)
        val defaultFillColor =
            ContextCompat.getColor(context, R.color.default_circle_indicator_fill_color)
        val defaultOrientation = res.getInteger(R.integer.default_circle_indicator_orientation)
        val defaultStrokeColor =
            ContextCompat.getColor(context, R.color.default_circle_indicator_stroke_color)
        val defaultStrokeWidth = res.getDimension(R.dimen.default_circle_indicator_stroke_width)
        val defaultRadius = res.getDimension(R.dimen.default_circle_indicator_radius)
        val defaultCentered = res.getBoolean(R.bool.default_circle_indicator_centered)
        val defaultSnap = res.getBoolean(R.bool.default_circle_indicator_snap)

        //Retrieve styles attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.CirclePageIndicator, defStyle, 0)
        mCentered = a.getBoolean(R.styleable.CirclePageIndicator_centered, defaultCentered)
        mOrientation =
            a.getInt(R.styleable.CirclePageIndicator_android_orientation, defaultOrientation)
        mPaintPageFill.style = Paint.Style.FILL
        mPaintPageFill.color =
            a.getColor(R.styleable.CirclePageIndicator_pageColor, defaultPageColor)
        mPaintStroke.style = Paint.Style.STROKE
        mPaintStroke.color =
            a.getColor(R.styleable.CirclePageIndicator_strokeColor, defaultStrokeColor)
        mPaintStroke.strokeWidth =
            a.getDimension(R.styleable.CirclePageIndicator_strokeWidth, defaultStrokeWidth)
        mPaintFill.style = Paint.Style.FILL
        mPaintFill.color = a.getColor(R.styleable.CirclePageIndicator_fillColor, defaultFillColor)
        mPaintBorder.color = Color.WHITE
        mRadius = a.getDimension(R.styleable.CirclePageIndicator_radius, defaultRadius)
        showBorder = a.getBoolean(R.styleable.CirclePageIndicator_showIndicatorBorder, false)
        mSnap = a.getBoolean(R.styleable.CirclePageIndicator_snap, defaultSnap)
        mExtraSpacing =
            a.getDimension(R.styleable.CirclePageIndicator_extraSpacing, 0f)
        val borderWidth =
            (a.getDimension(R.styleable.CirclePageIndicator_indicatorBorderWidth, 1f) + 0.5).toInt()
        val borderColor =
            a.getColor(R.styleable.CirclePageIndicator_indicatorBorderColor, Color.WHITE)
        mPaintBorder.strokeWidth = borderWidth.toFloat()
        mPaintBorder.color = borderColor
        mPaintBorder.style = Paint.Style.STROKE
        val background = a.getDrawable(R.styleable.CirclePageIndicator_android_background)
        background?.let { setBackground(it) }
        a.recycle()
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledPagingTouchSlop
    }
}