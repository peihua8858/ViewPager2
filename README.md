:heartpulse:ViewPager2:heartpulse:
 一款针对Android平台下的ViewPager2的指示器及循环轮播组件<br>

 [效果体验](https://github.com/peihua8858/ViewPager2/raw/master/demo/demo_2023-11-13_1723_v1.0.11.apk)<br>

[![Jitpack](https://jitpack.io/v/peihua8858/ViewPager2.svg)](https://github.com/peihua8858)
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen.svg)](https://github.com/peihua8858)
[![Star](https://img.shields.io/github/stars/peihua8858/ViewPager2.svg)](https://github.com/peihua8858/ViewPager2)


## 目录
-[最新版本](https://github.com/peihua8858/ViewPager2/releases/tag/1.0.11)<br>
-[如何引用](#如何引用)<br>
-[进阶使用](#进阶使用)<br>
-[演示效果](#演示效果)<br>
-[如何提Issues](https://github.com/peihua8858/ViewPager2/wiki/%E5%A6%82%E4%BD%95%E6%8F%90Issues%3F)<br>
-[License](#License)<br>

## 如何引用
* 把 `maven { url 'https://jitpack.io' }` 加入到 repositories 中
* 添加如下依赖，末尾的「latestVersion」指的是ViewPager2 [![Download](https://jitpack.io/v/peihua8858/ViewPager2.svg)](https://jitpack.io/#peihua8858/ViewPager2) 里的版本名称，请自行替换。
使用Gradle
```sh
repositories {
  google()
  maven { url 'https://jitpack.io' }
}

dependencies {
  // PictureSelector
  implementation 'com.github.peihua8858:ViewPager2:${latestVersion}'
}
```

或者Maven:

```xml
<dependency>
  <groupId>com.github.peihua8858</groupId>
  <artifactId>ViewPager2</artifactId>
  <version>${latestVersion}</version>
</dependency>
```

## 进阶使用

简单用例如下所示:

1、XML 布局文件

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.fz.viewpager2.AutoScrollLoopViewPager2
        android:id="@+id/view_pager"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent" />
    <com.fz.viewpager2.indicator.CirclePageIndicator
        android:id="@+id/circle_indicator"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:padding="6dp"
        app:extraSpacing="4dp"
        app:fillColor="@color/white"
        app:layout_constraintBottom_toBottomOf="@id/view_pager"
        app:pageColor="#66000000"
        app:radius="4dp"
        app:showIndicatorBorder="false"
        app:strokeWidth="0dp" />
</androidx.constraintlayout.widget.ConstraintLayout>
```
2、展示数据
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ImageLoader.getInstance().createProcessor(ImageGlideFetcher())
        val viewPager = findViewById<AutoScrollLoopViewPager2>(R.id.view_pager)
        val pagerIndicator = findViewById<CirclePageIndicator>(R.id.circle_indicator)
        val images = mutableListOf<String>()
        images.add("https://up.enterdesk.com/edpic_360_360/27/8f/93/278f938be4b460a57962d542eee989f6.jpg")
        images.add("https://rumenz.com/static/cimg/img/demo2.jpg")
        images.add("https://img95.699pic.com/photo/40094/7630.jpg_wh300.jpg")
        images.add("https://img.iplaysoft.com/wp-content/uploads/2019/free-images/free_stock_photo.jpg")
        images.add("https://cdn.pixabay.com/photo/2013/05/12/18/55/balance-110850__480.jpg")
        val adapter = BannerViewPagerAdapter()
        adapter.setNewInstance(images)
        viewPager.adapter = adapter
        pagerIndicator.setViewPager(viewPager)
        viewPager.setAutoTurning(3000)

    }
}

```

## License
```sh
Copyright 2023 peihua

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

