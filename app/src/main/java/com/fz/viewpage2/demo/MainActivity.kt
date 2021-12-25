package com.fz.viewpage2.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fz.imageloader.ImageLoader
import com.fz.imageloader.glide.ImageGlideFetcher
import com.fz.viewpager2.AutoScrollLoopViewPager2
import com.fz.viewpager2.indicator.LinePageIndicator

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ImageLoader.getInstance().createProcessor(ImageGlideFetcher())
        val viewPager = findViewById<AutoScrollLoopViewPager2>(R.id.view_pager)
        val pagerIndicator = findViewById<LinePageIndicator>(R.id.pager_indicator)
        val images = mutableListOf<String>()
        images.add("https://img95.699pic.com/photo/40094/7630.jpg_wh300.jpg")
        images.add("https://rumenz.com/static/cimg/img/demo2.jpg")
        images.add("https://img.iplaysoft.com/wp-content/uploads/2019/free-images/free_stock_photo.jpg")
        images.add("https://up.enterdesk.com/edpic_360_360/27/8f/93/278f938be4b460a57962d542eee989f6.jpg")
        images.add("https://cdn.pixabay.com/photo/2013/05/12/18/55/balance-110850__480.jpg")
        images.add("https://www.asqql.com/tpqsy/demo.jpg")
        val adapter = BannerViewPagerAdapter()
        adapter.setNewInstance(images)
        viewPager.adapter = adapter
//        viewPager.setIndicator(pagerIndicator)
        pagerIndicator.setViewPager(viewPager)
        viewPager.setAutoTurning(3000)

    }
}