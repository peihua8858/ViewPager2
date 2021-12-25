package com.fz.viewpage2.demo

import android.content.Context
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.WindowManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fz.imageloader.widget.RatioImageView

/**
 * 通用的用于展示网络或本地图片的ViewPager适配器
 *
 */
class BannerViewPagerAdapter : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_image_view) {
    private var width = 0
    private var height = 0
    private var menuName: String? = null
    fun setMenuName(menuName: String?) {
        this.menuName = menuName
    }

    fun setWidth(width: Int) {
        this.width = width
    }

    fun setHeight(height: Int) {
        this.height = height
    }

    override fun convert(holder: BaseViewHolder, item: String) {
        val rootView = holder.itemView
        if (width <= 0) {
            width = getScreenWidth(context)
        }
        if (height <= 0) {
            height = (width * 3 / 4.0).toInt()
        }
        val imageView: RatioImageView = rootView.findViewById(R.id.image_view)
        var layoutParams = imageView.layoutParams
        if (layoutParams == null) {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        layoutParams.width = width
        layoutParams.height = height
        imageView.layoutParams = layoutParams
        imageView.setImageUrl(item, width, height)
    }

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    fun getScreenWidth(context: Context): Int {
        val wm = context
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.widthPixels
    }
}