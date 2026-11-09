/*
 * Copyright (C) 2016 venshine.cn@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wx.beziermaker.demo

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 各示例页的公共部分：全面屏内边距、状态栏图标颜色、顶栏返回与深浅色切换
 *
 * @author venshine
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // targetSdk 35 起，Android 15+ 强制全面屏，内容需要避开状态栏和导航栏
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val light = !DemoTheme.isNight(this)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }

        findViewById<View>(R.id.toolbar_back)?.setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.theme_toggle)?.let { button ->
            val night = DemoTheme.isNight(this)
            button.setImageResource(if (night) R.drawable.ic_sun else R.drawable.ic_moon)
            button.contentDescription = getString(if (night) R.string.theme_to_light else R.string.theme_to_dark)
            button.setOnClickListener { DemoTheme.toggle(this) }
        }
    }

    /** 设置二级页顶栏的标题和右侧 API 标签 */
    protected fun setToolbar(@StringRes title: Int, chip: String) {
        findViewById<TextView>(R.id.toolbar_title).setText(title)
        findViewById<TextView>(R.id.toolbar_chip).apply {
            text = chip
            visibility = View.VISIBLE
        }
    }
}
