package com.fz.viewpager2.indicator

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.text.TextUtilsCompat
import androidx.core.util.Pools
import androidx.core.util.Pools.SynchronizedPool
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.fz.viewpager2.R
import com.google.android.material.tabs.TabLayout
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class VerticalTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {
    private var mColorIndicator = 0
    private var mTabMargin = 0
    private var mIndicatorWidth = 0
    private var mIndicatorHeight = 0
    private var mTabHeight = 0
    private var mTabTextSize = 0
    private var mTabPaddingStart = 0
    private var mTabPaddingTop = 0
    private var mTabPaddingEnd = 0
    private var mTabPaddingBottom = 0
    private var mTabTextColors: ColorStateList? = null
    private var mTabTitleBg: Drawable? = null

    @ColorInt
    private var mTabColor = 0

    @ColorInt
    private var mTabSelectedColor = 0

    /**
     * configure for [.setupWithViewPager]
     */
    private var mViewPager: ViewPager2? = null
    private var mPagerAdapter: RecyclerView.Adapter<*>? = null
    private var mTabPageChangeListener: OnTabPageChangeListener? = null
    private var currentVpSelectedListener: ViewPagerOnVerticalTabSelectedListener? = null
    private var mPagerAdapterObserver: RecyclerView.AdapterDataObserver? = null
    private var mTabStrip: TabStrip? = null
    private var mSelectedTab: VerticalTab? = null
    private val tabs: MutableList<VerticalTab> = ArrayList()
    private val mTabSelectedListeners: MutableList<OnTabSelectedListener> = ArrayList()
    private val mTabViewPool: Pools.Pool<TabView> = Pools.SimplePool(12)
    private var selectedFontFamily: Typeface? = null
    private var fontFamily: Typeface? = null
    private fun initStyleConfigure(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerticalTabLayout)
        mColorIndicator =
            typedArray.getColor(R.styleable.VerticalTabLayout_indicator_color, Color.RED)
        mIndicatorWidth = typedArray.getDimension(
            R.styleable.VerticalTabLayout_indicator_width,
            dpToPx(3f).toFloat()
        ).toInt()
        mIndicatorHeight = typedArray.getDimension(
            R.styleable.VerticalTabLayout_indicator_height,
            dpToPx(3f).toFloat()
        ).toInt()
        mTabMargin = typedArray.getDimension(R.styleable.VerticalTabLayout_tab_margin, 0f)
            .toInt()
        val defaultTabHeight = LinearLayout.LayoutParams.WRAP_CONTENT
        mTabHeight = typedArray.getDimension(
            R.styleable.VerticalTabLayout_tab_height,
            defaultTabHeight.toFloat()
        ).toInt()
        mTabTextSize = typedArray.getDimension(R.styleable.VerticalTabLayout_tab_textSize, 16f)
            .toInt()
        mTabPaddingBottom =
            typedArray.getDimensionPixelSize(R.styleable.VerticalTabLayout_tab_padding, 0)
        mTabPaddingEnd = mTabPaddingBottom
        mTabPaddingTop = mTabPaddingEnd
        mTabPaddingStart = mTabPaddingTop
        mTabPaddingStart = typedArray.getDimensionPixelSize(
            R.styleable.VerticalTabLayout_tab_paddingStart,
            mTabPaddingStart
        )
        mTabPaddingTop = typedArray.getDimensionPixelSize(
            R.styleable.VerticalTabLayout_tab_paddingTop,
            mTabPaddingTop
        )
        mTabPaddingEnd = typedArray.getDimensionPixelSize(
            R.styleable.VerticalTabLayout_tab_paddingEnd,
            mTabPaddingEnd
        )
        mTabPaddingBottom = typedArray.getDimensionPixelSize(
            R.styleable.VerticalTabLayout_tab_paddingBottom,
            mTabPaddingBottom
        )
        mTabColor = typedArray.getColor(
            R.styleable.VerticalTabLayout_tab_color,
            Color.parseColor("#f7f7f7")
        )
        mTabSelectedColor =
            typedArray.getColor(R.styleable.VerticalTabLayout_tab_selectedColor, Color.WHITE)
        val textColor =
            typedArray.getColor(R.styleable.VerticalTabLayout_tab_textColor, Color.BLACK)
        val selected =
            typedArray.getColor(R.styleable.VerticalTabLayout_tab_textSelectedColor, Color.RED)
        mTabTextColors = createColorStateList(textColor, selected)
        val attrsBg =
            intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
        val typedArrayBg = getContext().theme.obtainStyledAttributes(attrsBg)
        mTabTitleBg = typedArrayBg.getDrawable(0)
        try {
            val selectedFontFamily =
                typedArray.getResourceId(R.styleable.VerticalTabLayout_tab_selectedFontFamily, 0)
            val fontFamily =
                typedArray.getResourceId(R.styleable.VerticalTabLayout_tab_fontFamily, 0)
            this.fontFamily = ResourcesCompat.getFont(context, fontFamily)
            this.selectedFontFamily = ResourcesCompat.getFont(context, selectedFontFamily)
        } catch (e: Exception) {
        }
        typedArrayBg.recycle()
        typedArray.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 0) {
            removeAllViews()
        }
        initTabStrip()
    }

    private fun initTabStrip() {
        mTabStrip = TabStrip(context)
        addView(mTabStrip, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    val tabCount: Int
        get() = tabs.size
    val selectedTabPosition: Int
        get() = if (mSelectedTab != null) mSelectedTab!!.position else -1

    fun addOnTabSelectedListener(listener: OnTabSelectedListener?) {
        if (listener != null && !mTabSelectedListeners.contains(listener)) {
            mTabSelectedListeners.add(listener)
        }
    }

    fun removeOnTabSelectedListener(listener: OnTabSelectedListener?) {
        if (listener != null) {
            mTabSelectedListeners.remove(listener)
        }
    }

    fun newTab(): VerticalTab {
        var tab = S_TAB_POOL.acquire()
        if (tab == null) {
            tab = VerticalTab()
        }
        tab.mParent = this
        tab.mView = createTabView(tab)
        return tab
    }

    fun addTab(tab: VerticalTab, selected: Boolean) {
        addTab(tab, tabs.size, selected)
    }

    @JvmOverloads
    fun addTab(tab: VerticalTab, position: Int = tabs.size, selected: Boolean = false) {
        configureTab(tab, position)
        addTabView(tab)
        if (selected) {
            tab.select()
        }
    }

    private fun createTabView(tab: VerticalTab): TabView {
        var tabView = mTabViewPool.acquire()
        if (tabView == null) {
            tabView = TabView(context)
        }
        tabView.setFont(fontFamily)
        tabView.setSelectedFont(selectedFontFamily)
        tabView.tab = tab
        tabView.isFocusable = true
        return tabView
    }

    private fun configureTab(tab: VerticalTab, position: Int) {
        tab.position = position
        tabs.add(position, tab)
        val count = tabs.size
        for (i in position + 1 until count) {
            tabs[i].position = i
        }
    }

    private fun addTabView(tab: VerticalTab) {
        val tabView = tab.mView
        addTabWithMode(tabView)
        if (mTabStrip!!.indexOfChild(tabView) == 0) {
            tabView!!.isSelected = true
            tabView.setBackgroundColor(mTabSelectedColor)
            mSelectedTab = tab
            mTabStrip!!.post { mTabStrip!!.moveIndicator(0f) }
        }
    }

    private fun addTabWithMode(tabView: TabView?) {
        val params = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        initTabWithMode(params)
        mTabStrip!!.addView(tabView, params)
    }

    private fun initTabWithMode(params: LinearLayout.LayoutParams) {
        params.height = mTabHeight
        params.weight = 0f
        params.setMargins(0, mTabMargin, 0, 0)
        isFillViewport = false
    }

    fun removeAllTabs() {
        for (i in mTabStrip!!.childCount - 1 downTo 0) {
            removeTabViewAt(i)
        }
        val i = tabs.iterator()
        while (i.hasNext()) {
            val tab = i.next()
            i.remove()
            tab.reset()
            S_TAB_POOL.release(tab)
        }
        mSelectedTab = null
    }

    private fun removeTabViewAt(position: Int) {
        val view = mTabStrip?.getChildAt(position) as? TabView
        mTabStrip?.removeViewAt(position)
        if (view != null) {
            view.reset()
            mTabViewPool.release(view)
        }
        requestLayout()
    }

    fun getTabAt(index: Int): VerticalTab? {
        return if (index < 0 || index >= tabCount) null else tabs[index]
    }

    @JvmOverloads
    fun selectTab(tab: VerticalTab?, updateIndicator: Boolean = true) {
        val currentTab = mSelectedTab
        if (currentTab == tab) {
            if (currentTab != null) {
                scrollToTab(tab!!.position)
            }
        } else {
            val newPosition = tab?.position ?: TabLayout.Tab.INVALID_POSITION
            setSelectedTabView(newPosition)
            if (updateIndicator) {
                mTabStrip!!.moveIndicatorWithAnimator(newPosition)
            }
            mSelectedTab = tab
            if (tab != null) {
                scrollToTab(tab.position)
                dispatchTabSelected(tab)
            }
        }
    }

    private fun setSelectedTabView(position: Int) {
        val tabCount = mTabStrip!!.childCount
        if (position < tabCount) {
            for (i in 0 until tabCount) {
                val child = mTabStrip!!.getChildAt(i)
                val isSelected = i == position
                child.isSelected = isSelected
                child.setBackgroundColor(if (isSelected) mTabSelectedColor else mTabColor)
            }
        }
    }

    private fun dispatchTabSelected(tab: VerticalTab) {
        for (i in mTabSelectedListeners.indices) {
            mTabSelectedListeners[i].onTabSelected(tab)
        }
    }

    fun setupWithViewPager(viewPager: ViewPager2?, smoothScroll: Boolean) {
        if (mViewPager != null && mTabPageChangeListener != null) {
            mViewPager!!.unregisterOnPageChangeCallback(mTabPageChangeListener!!)
        }
        if (viewPager != null) {
            val adapter = viewPager.adapter
                ?: throw IllegalArgumentException("ViewPager does not have a PagerAdapter set")
            mViewPager = viewPager
            if (mTabPageChangeListener == null) {
                mTabPageChangeListener = OnTabPageChangeListener(this)
            }
            viewPager.registerOnPageChangeCallback(mTabPageChangeListener!!)
            if (currentVpSelectedListener == null) {
                currentVpSelectedListener =
                    ViewPagerOnVerticalTabSelectedListener(viewPager, smoothScroll)
            }
            addOnTabSelectedListener(currentVpSelectedListener)
            setPagerAdapter(adapter)
        } else {
            mViewPager = null
            setPagerAdapter(null)
        }
        populateFromPagerAdapter()
    }

    fun setupWithViewPager(viewPager: ViewPager2?) {
        setupWithViewPager(viewPager, true)
    }

    private fun setPagerAdapter(adapter: RecyclerView.Adapter<*>?) {
        if (mPagerAdapter != null && mPagerAdapterObserver != null) {
            mPagerAdapter!!.unregisterAdapterDataObserver(mPagerAdapterObserver!!)
        }
        mPagerAdapter = adapter
        if (adapter != null) {
            if (mPagerAdapterObserver == null) {
                mPagerAdapterObserver = PagerAdapterObserver()
            }
            adapter.registerAdapterDataObserver(mPagerAdapterObserver!!)
        }
    }

    private fun populateFromPagerAdapter() {
        removeAllTabs()
        val adapter = mPagerAdapter
        if (adapter is OnViewPager2Title) {
            val adapterCount = adapter.itemCount
            for (i in 0 until adapterCount) {
                val title = adapter.getPageTitle(i).toString()
                addTab(newTab().setText(title), false)
            }
            if (mViewPager != null && adapterCount > 0) {
                val curItem = mViewPager!!.currentItem
                if (curItem != selectedTabPosition && curItem < tabCount) {
                    selectTab(getTabAt(curItem))
                }
            }
        }
    }

    private fun scrollToTab(position: Int) {
        val tabView = getTabAt(position)?.mView ?: return
        val y = scrollY
        val tabTop = tabView.top + tabView.height / 2 - y
        val target = height / 2
        if (tabTop > target) {
            smoothScrollBy(0, tabTop - target)
        } else if (tabTop < target) {
            smoothScrollBy(0, tabTop - target)
        }
    }

    private fun setScrollPosition(position: Int, positionOffset: Float) {
        mTabStrip!!.moveIndicator(positionOffset + position)
    }

    private fun dpToPx(dps: Float): Int {
        return (resources.displayMetrics.density * dps).roundToInt()
    }

    private inner class TabStrip(context: Context?) : LinearLayout(context) {
        private var mIndicatorTopY = 0f
        private var mIndicatorX = 0f
        private var mIndicatorBottomY = 0f
        private val mLastWidth = 0
        private val mIndicatorPaint: Paint
        private val mIndicatorRect: RectF
        private val mIndicatorPath: Path
        private var mAutoTabHeight: Boolean = false
        private var mIndicatorAnimatorSet: AnimatorSet? = null

        init {
            mAutoTabHeight = mTabHeight <= 0
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            setIndicatorGravity()
        }

        protected fun setIndicatorGravity() {
            if (mLastWidth != 0) {
                mIndicatorWidth = mLastWidth
            }
            mIndicatorX = if (isRtl) {
                (measuredWidth - mIndicatorWidth).toFloat()
            } else {
                0f
            }
            post {
                if (isRtl) {
                    mIndicatorX = (measuredWidth - mIndicatorWidth).toFloat()
                }
                invalidate()
            }
        }

        private fun calcIndicatorY(offset: Float) {
            val index = floor(offset.toDouble()).toInt()
            val childView = getChildAt(index) ?: return
            if (mAutoTabHeight) {
                mTabHeight = childView.measuredHeight
            }
            if (floor(offset.toDouble()) != (childCount - 1).toDouble() && ceil(offset.toDouble()) != 0.0) {
                val nextView = getChildAt(index + 1)
                mIndicatorTopY = childView.top + (nextView.top - childView.top) * (offset - index)
                mIndicatorBottomY =
                    childView.bottom + (nextView.bottom - childView.bottom) * (offset - index)
            } else {
                mIndicatorTopY = childView.top.toFloat()
                mIndicatorBottomY = childView.bottom.toFloat()
            }
            if (mIndicatorTopY < 0) {
                mIndicatorTopY = 0f
            }
            if (mIndicatorBottomY <= 0) {
                mIndicatorBottomY = mTabHeight.toFloat()
            }
        }

        protected fun updateIndicator() {
            moveIndicatorWithAnimator(selectedTabPosition)
        }

        fun moveIndicator(offset: Float) {
            calcIndicatorY(offset)
            invalidate()
        }

        /**
         * move indicator to a tab location
         *
         * @param index tab location's index
         */
        fun moveIndicatorWithAnimator(index: Int) {
            val direction = index - selectedTabPosition
            val childView = getChildAt(index)
            val targetTop = childView.top.toFloat()
            val targetBottom = childView.bottom.toFloat()
            if (mIndicatorTopY == targetTop && mIndicatorBottomY == targetBottom) {
                return
            }
            if (mIndicatorAnimatorSet != null && mIndicatorAnimatorSet!!.isRunning) {
                mIndicatorAnimatorSet!!.end()
            }
            val duration = 100
            post {
                var startAnim: ValueAnimator? = null
                var endAnim: ValueAnimator? = null
                if (direction > 0) {
                    startAnim = ValueAnimator.ofFloat(mIndicatorBottomY, targetBottom)
                        .setDuration(duration.toLong())
                    startAnim.addUpdateListener(AnimatorUpdateListener { animation ->
                        mIndicatorBottomY = animation.animatedValue.toString().toFloat()
                        invalidate()
                    })
                    endAnim = ValueAnimator.ofFloat(mIndicatorTopY, targetTop)
                        .setDuration(duration.toLong())
                    endAnim.addUpdateListener(AnimatorUpdateListener { animation ->
                        mIndicatorTopY = animation.animatedValue.toString().toFloat()
                        invalidate()
                    })
                } else if (direction < 0) {
                    startAnim = ValueAnimator.ofFloat(mIndicatorTopY, targetTop)
                        .setDuration(duration.toLong())
                    startAnim.addUpdateListener(AnimatorUpdateListener { animation ->
                        mIndicatorTopY = animation.animatedValue.toString().toFloat()
                        invalidate()
                    })
                    endAnim = ValueAnimator.ofFloat(mIndicatorBottomY, targetBottom)
                        .setDuration(duration.toLong())
                    endAnim.addUpdateListener(AnimatorUpdateListener { animation ->
                        mIndicatorBottomY = animation.animatedValue.toString().toFloat()
                        invalidate()
                    })
                }
                if (startAnim != null) {
                    mIndicatorAnimatorSet = AnimatorSet()
                    mIndicatorAnimatorSet!!.play(endAnim).after(startAnim)
                    mIndicatorAnimatorSet!!.start()
                }
            }
        }

        override fun onDrawForeground(canvas: Canvas) {
            super.onDrawForeground(canvas)
            mIndicatorRect.left = mIndicatorX
            mIndicatorRect.top = mIndicatorTopY + (mTabHeight - mIndicatorHeight) / 2
            mIndicatorRect.right = mIndicatorX + mIndicatorWidth
            mIndicatorRect.bottom = mIndicatorRect.top + mIndicatorHeight
            canvas.drawRoundRect(
                mIndicatorRect,
                dpToPx(1.5f).toFloat(),
                dpToPx(1.5f).toFloat(),
                mIndicatorPaint
            )
        }

        init {
            setWillNotDraw(false)
            orientation = VERTICAL
            mIndicatorPaint = Paint()
            mIndicatorPaint.isAntiAlias = true
            mIndicatorPaint.color = mColorIndicator
            mIndicatorRect = RectF()
            mIndicatorPath = Path()
        }
    }

    /**
     * modify from [TabLayout.Tab]
     *
     *
     * [VerticalTabLayout]的子单元
     * 通过[VerticalTabLayout.addTab]添加item
     * 通过[VerticalTabLayout.newTab]构建实例
     */
    class VerticalTab internal constructor() {
        var tag: Any? = null
            private set
        var icon: Drawable? = null
            private set
        var text: CharSequence? = null
            private set
        var contentDescription: CharSequence? = null
            private set
        var position = INVALID_POSITION
        var customView: View? = null
            private set
        var renderListener: OnCustomTabViewRenderListener? = null
        var mParent: VerticalTabLayout? = null
        var mView: TabView? = null
        fun setTag(tag: Any?): VerticalTab {
            this.tag = tag
            return this
        }

        fun setCustomView(view: View?, listener: OnCustomTabViewRenderListener?): VerticalTab {
            customView = view
            renderListener = listener
            updateView()
            return this
        }

        fun setCustomView(
            @LayoutRes resId: Int,
            listener: OnCustomTabViewRenderListener?
        ): VerticalTab {
            val inflater = LayoutInflater.from(mView!!.context)
            return setCustomView(inflater.inflate(resId, mView, false), listener)
        }

        fun setIcon(icon: Drawable?): VerticalTab {
            this.icon = icon
            updateView()
            return this
        }

        fun setIcon(@DrawableRes resId: Int): VerticalTab {
            requireNotNull(mParent) { "Tab not attached to a TabLayout" }
            return setIcon(AppCompatResources.getDrawable(mParent!!.context, resId))
        }

        fun setText(text: CharSequence?): VerticalTab {
            this.text = text
            updateView()
            return this
        }

        fun setText(@StringRes resId: Int): VerticalTab {
            requireNotNull(mParent) { "Tab not attached to a TabLayout" }
            return setText(mParent!!.resources.getText(resId))
        }

        fun select() {
            requireNotNull(mParent) { "Tab not attached to a TabLayout" }
            mParent!!.selectTab(this)
        }

        val isSelected: Boolean
            get() {
                requireNotNull(mParent) { "Tab not attached to a TabLayout" }
                return mParent!!.selectedTabPosition == position
            }

        fun setContentDescription(@StringRes resId: Int): VerticalTab {
            requireNotNull(mParent) { "Tab not attached to a TabLayout" }
            return setContentDescription(mParent!!.resources.getText(resId))
        }

        fun setContentDescription(contentDesc: CharSequence?): VerticalTab {
            contentDescription = contentDesc
            updateView()
            return this
        }

        fun updateView() {
            if (mView != null) {
                mView!!.update()
                // 如果view是自定义的view，通过接口将渲染事件传递出去
                if (renderListener != null) {
                    renderListener!!.onRender(this)
                }
            }
        }

        fun reset() {
            mParent = null
            mView = null
            tag = null
            icon = null
            text = null
            contentDescription = null
            position = INVALID_POSITION
            customView = null
            renderListener = null
        }

        companion object {
            const val INVALID_POSITION = -1
        }
    }

    /**
     * modify from [TabView]
     *
     *
     * tab的视图，由一个简单的[ImageView]+ [TextView] 组成
     * 如果需要复杂的视图效果可以通过[VerticalTab.setCustomView]设置自定义的view
     */
    inner class TabView(context: Context?) : LinearLayout(context) {
        private var mTab: VerticalTab? = null
        private var mTextView: TextView? = null
        private var mCustomView: View? = null
        private var selectedFontFamily: Typeface? = null
        private var fontFamily: Typeface? = null
        fun setSelectedFont(fontFamily: Typeface?) {
            selectedFontFamily = fontFamily
        }

        fun setFont(fontFamily: Typeface?) {
            this.fontFamily = fontFamily
        }

        override fun performClick(): Boolean {
            val handled = super.performClick()
            return if (mTab != null) {
                if (!handled) {
                    playSoundEffect(SoundEffectConstants.CLICK)
                }
                mTab!!.select()
                true
            } else {
                handled
            }
        }

        override fun setSelected(selected: Boolean) {
            super.setSelected(selected)
            if (mTextView != null) {
                mTextView!!.isSelected = selected
                mTextView!!.typeface = if (selected) selectedFontFamily else fontFamily
            }
            if (mCustomView != null) {
                mCustomView!!.isSelected = selected
            }
        }

        fun reset() {
            tab = null
            isSelected = false
        }

        fun update() {
            val tab = mTab
            val custom = tab?.customView
            if (custom != null) {
                val customParent = custom.parent
                if (customParent !== this) {
                    if (customParent != null) {
                        (customParent as ViewGroup).removeView(custom)
                    }
                    addView(custom)
                }
                mCustomView = custom
                if (mTextView != null) {
                    mTextView!!.visibility = GONE
                }
            } else {
                // We do not have a custom view. Remove one if it already exists
                if (mCustomView != null) {
                    removeView(mCustomView)
                    mCustomView = null
                }
            }
            if (mCustomView == null) {
                if (mTextView == null) {
                    val textView = TextView(context)
                    textView.textDirection = TEXT_DIRECTION_LOCALE
                    textView.textAlignment = TEXT_ALIGNMENT_VIEW_START
                    textView.ellipsize = TextUtils.TruncateAt.END
                    textView.maxLines = 2
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTabTextSize.toFloat())
                    // 避免drawable状态共享
                    textView.background = mTabTitleBg!!.constantState!!.newDrawable()
                    addView(textView)
                    mTextView = textView
                }
                if (mTabTextColors != null) {
                    mTextView!!.setTextColor(mTabTextColors)
                }
                updateTextAndIcon(mTextView)
            } else {
                if (tab!!.renderListener != null) {
                    tab.renderListener!!.onRender(tab)
                }
            }
            isSelected = tab != null && tab.isSelected
        }

        private fun updateTextAndIcon(textView: TextView?) {
            val text = if (mTab != null) mTab!!.text else null
            val contentDesc = if (mTab != null) mTab!!.contentDescription else null
            val hasText = !TextUtils.isEmpty(text)
            if (textView != null) {
                if (hasText) {
                    textView.text = text
                    textView.visibility = VISIBLE
                    visibility = VISIBLE
                } else {
                    textView.visibility = GONE
                    textView.text = null
                }
                textView.contentDescription = contentDesc
            }
            TooltipCompat.setTooltipText(this, if (hasText) null else contentDesc)
        }

        var tab: VerticalTab?
            get() = mTab
            set(tab) {
                if (mTab != tab) {
                    mTab = tab
                    update()
                }
            }

        init {
            ViewCompat.setPaddingRelative(
                this,
                mTabPaddingStart + mIndicatorWidth,
                mTabPaddingTop,
                mTabPaddingEnd,
                mTabPaddingBottom
            )
            gravity = Gravity.CENTER
            orientation = VERTICAL
            isClickable = true
        }
    }

    /**
     * [ViewPager]和[VerticalTabLayout]的联动
     * 监听[ViewPager]的变化，更新[VerticalTabLayout]
     */
    private class OnTabPageChangeListener(tabLayout: VerticalTabLayout) : OnPageChangeCallback() {
        private var mPreviousScrollState = 0
        private val mTabLayoutRef: WeakReference<VerticalTabLayout>
        private var mScrollState = 0
        var mUpdateIndicator = false
        override fun onPageScrollStateChanged(state: Int) {
            mPreviousScrollState = mScrollState
            mScrollState = state
            mUpdateIndicator =
                !(mScrollState == ViewPager.SCROLL_STATE_SETTLING && mPreviousScrollState == ViewPager.SCROLL_STATE_IDLE)
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val tabLayout = mTabLayoutRef.get()
            if (mUpdateIndicator && tabLayout != null) {
                tabLayout.setScrollPosition(position, positionOffset)
            }
        }

        override fun onPageSelected(position: Int) {
            val tabLayout = mTabLayoutRef.get()
            if (tabLayout != null && tabLayout.selectedTabPosition != position && position < tabLayout.tabCount) {
                tabLayout.selectTab(tabLayout.getTabAt(position), !mUpdateIndicator)
            }
        }

        init {
            mTabLayoutRef = WeakReference(tabLayout)
        }
    }

    /**
     * 监听[ViewPager]的数据源[PagerAdapter]的变化
     */
    private inner class PagerAdapterObserver : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            populateFromPagerAdapter()
        }
    }

    /**
     * [ViewPager]和[VerticalTabLayout]的联动
     * 监听[VerticalTabLayout]的变化，更新[ViewPager]
     */
    class ViewPagerOnVerticalTabSelectedListener(
        viewPager: ViewPager2,
        private val smoothScroll: Boolean
    ) :
        OnTabSelectedListener {
        private val viewPagerRef: WeakReference<ViewPager2>
        override fun onTabSelected(tab: VerticalTab) {
            val viewPager = viewPagerRef.get()
            if (viewPager != null && viewPager.adapter!!.itemCount >= tab.position) {
                viewPager.setCurrentItem(tab.position, smoothScroll)
            }
        }

        init {
            viewPagerRef = WeakReference(viewPager)
        }
    }

    protected val isRtl: Boolean
        get() = TextUtilsCompat.getLayoutDirectionFromLocale(
            ConfigurationCompat.getLocales(
                context.resources.configuration
            )[0]
        ) == ViewCompat.LAYOUT_DIRECTION_RTL

    interface OnTabSelectedListener {
        /**
         * tab选中
         * @param tab tab
         */
        fun onTabSelected(tab: VerticalTab)
    }

    interface OnCustomTabViewRenderListener {
        /**
         * 当需要tab的item需要自定义view的时候，通过这个接口方法通知视图渲染
         *
         * @param tab tab
         */
        fun onRender(tab: VerticalTab?)
    }

    companion object {
        private val S_TAB_POOL: Pools.Pool<VerticalTab> = SynchronizedPool(16)
        private fun createColorStateList(defaultColor: Int, selectedColor: Int): ColorStateList {
            val states = arrayOfNulls<IntArray>(2)
            val colors = IntArray(2)
            var i = 0
            states[i] = SELECTED_STATE_SET
            colors[i] = selectedColor
            i++

            // Default enabled state
            states[i] = EMPTY_STATE_SET
            colors[i] = defaultColor
            return ColorStateList(states, colors)
        }
    }

    init {
        initStyleConfigure(context, attrs)
    }
}