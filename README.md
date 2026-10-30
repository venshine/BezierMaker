# BezierMaker  

English | [简体中文](README-CN.md)

[![JitPack](https://jitpack.io/v/venshine/BezierMaker.svg)](https://jitpack.io/#venshine/BezierMaker)  [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-BezierMaker-green.svg?style=true)](https://android-arsenal.com/details/1/3852)    [![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=14)  

Draws Bézier curves with the de Casteljau algorithm, computes their tangents, and animates how Bézier curves of order 1 to 7 are formed.
The de Casteljau algorithm computes a point on a Bézier curve, from which the whole curve can be drawn. To learn more about it, see my Chinese translation [De Casteljau's Algorithm: Finding a Point on a Bézier Curve](http://blog.csdn.net/venshine/article/details/51750906).  

[**Download the sample APK**](https://github.com/venshine/BezierMaker/releases/download/2.0.0/BezierMaker-sample-2.0.0.apk)

Features
--
* Add and remove control points
* Bézier curves of order 1 to 7 (limited by screen size; order N is supported in theory)
* Drag control points freely
* Show the tangents while the curve is being formed
* Loop the forming animation
* Adjust the animation rate
* Show control point coordinates
* Set the order of the curve

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
1. Add the JitPack repository to `settings.gradle`:
```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = 'https://jitpack.io' }
    }
}
```
2. Add the dependency to your module's `build.gradle`:
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
The library is written in Java and can be called directly from both Java and Kotlin. It adds no Kotlin dependency to your project.

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
        bezierView.setRate(10) // animation rate
        bezierView.setTangent(true) // show tangents
        bezierView.setLoop(true) // loop the animation
    }

    fun start(view: View) = bezierView.start()

    fun stop(view: View) = bezierView.stop()

    fun add(view: View) {
        bezierView.addPoint() // add a control point; returns false when the maximum is reached
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
| method          | description |
|:---				 |:---|
| void **start**()  	     | Start the animation (required) |
| void **stop**()	     | Stop the animation (optional) |
| boolean **addPoint**() 	     | Add a control point (optional) |
| boolean **delPoint**() 	     | Remove a control point (optional) |
| int **getOrder**() 	     | Get the order of the curve (optional) |
| void **setRate**(int rate) 	     | Set the animation rate (optional) |
| void **setTangent**(boolean tangent)  	     | Show or hide tangents (optional) |
| void **setLoop**(boolean loop)  	     | Loop the animation or not (optional) |
| void **setOrder**(int order)  	     | Set the order of the curve (optional) |


About
--
* [Drawing N-order Bézier curves on Android (in Chinese)](http://blog.csdn.net/venshine/article/details/51758841)
* Email: venshine.cn@gmail.com

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

