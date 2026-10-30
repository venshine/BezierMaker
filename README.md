# BezierMaker  

[![JitPack](https://jitpack.io/v/venshine/BezierMaker.svg)](https://jitpack.io/#venshine/BezierMaker)  [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-BezierMaker-green.svg?style=true)](https://android-arsenal.com/details/1/3852)    [![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=14)  

通过de Casteljau算法绘制贝塞尔曲线，并计算它的切线，实现1-7阶贝塞尔曲线的形成动画。
德卡斯特里奥算法可以计算出Bezier曲线上的一个点，进而绘制出Bezier曲线。想深入了解德卡斯特里奥算法的同学可以参考我翻译的一篇文章[《德卡斯特里奥算法——找到Bezier曲线上的一个点》](http://blog.csdn.net/venshine/article/details/51750906)。  

[**bezier.apk**](/bezier.apk)

Features
--
* 支持增加和删除控制点
* 支持1阶到7阶贝塞尔曲线，限于屏幕大小，理论上可以支持N阶贝塞尔曲线
* 支持自由移动控制点
* 支持显示贝塞尔曲线形成过程的切线
* 支持循环显示贝塞尔曲线的形成动画
* 支持贝塞尔曲线显示速率
* 支持显示控制点坐标
* 支持设置贝塞尔曲线阶数

ScreenShot
--
<img src="/screenshot/1.gif" width="33.3%"><img src="/screenshot/8.gif" width="33.3%">
<br/>
<img src="/screenshot/2.gif" width="33.3%"><img src="/screenshot/3.gif" width="33.3%"><img src="/screenshot/4.gif" width="33.3%">
<br/>
<img src="/screenshot/5.gif" width="33.3%"><img src="/screenshot/6.gif" width="33.3%"><img src="/screenshot/7.gif" width="33.3%">
<br/>

Usage
--
##### Gradle:
1. 在 `settings.gradle` 中添加 JitPack 仓库：
```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = 'https://jitpack.io' }
    }
}
```
2. 在模块的 `build.gradle` 中添加依赖：
```groovy
implementation 'com.github.venshine:BezierMaker:2.0.0'
```

##### Gradle Kotlin DSL:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// build.gradle.kts
implementation("com.github.venshine:BezierMaker:2.0.0")
```

Demo
--
库本身用 Java 编写，Java 和 Kotlin 都可以直接调用，不会给项目额外引入 Kotlin 依赖。

##### XML:
```xml
<com.wx.beziermaker.BezierView
    android:id="@+id/bezier"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

##### Kotlin:
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var bezierView: BezierView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bezierView = findViewById(R.id.bezier)
        bezierView.setRate(10) // 移动速率
        bezierView.setTangent(true) // 显示切线
        bezierView.setLoop(true) // 循环播放
    }

    fun start(view: View) = bezierView.start()

    fun stop(view: View) = bezierView.stop()

    fun add(view: View) {
        bezierView.addPoint() // 增加控制点，返回 false 表示已达上限
    }

    fun del(view: View) {
        bezierView.delPoint()
    }
}
```

##### Java:
```Java
public class MainActivity extends Activity {

    BezierView mBezierView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBezierView = findViewById(R.id.bezier);
    }

    public void start(View view) {
        mBezierView.start();
    }

    public void stop(View view) {
        mBezierView.stop();
    }

    public void add(View view) {
        mBezierView.addPoint();
    }

    public void del(View view) {
        mBezierView.delPoint();
    }

}
```

##### Methods:
| method 方法          | description 描述 |
|:---				 |:---|
| void **start**()  	     | 开始贝塞尔曲线（required） |
| void **stop**()	     | 停止贝塞尔曲线（optional） |
| boolean **addPoint**() 	     | 增加控制点（optional） |
| boolean **delPoint**() 	     | 删除控制点（optional） |
| int **getOrder**() 	     | 获取贝塞尔曲线阶数（optional） |
| void **setRate**(int rate) 	     | 设置移动速率（optional） |
| void **setTangent**(boolean tangent)  	     | 设置是否显示切线（optional） |
| void **setLoop**(boolean loop)  	     | 设置是否循环（optional） |
| void **setOrder**(int order)  	     | 设置贝塞尔曲线阶数（optional） |


About
--
* [Android 绘制N阶Bezier曲线](http://blog.csdn.net/venshine/article/details/51758841)
* Email：venshine.cn@gmail.com

License
--
    Copyright (C) 2016 venshine.cn@gmail.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

