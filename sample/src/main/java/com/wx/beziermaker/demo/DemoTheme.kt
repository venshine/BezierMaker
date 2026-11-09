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

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * 深浅色主题：默认浅色，用户切换后记住选择
 *
 * 真值源是 SharedPreferences 里的 [KEY_DARK]；界面颜色只读当前 Configuration（[isNight]），
 * 切换时 AppCompat 会重建所有 Activity，颜色资源自动换成 values-night 的版本。
 *
 * @author venshine
 */
object DemoTheme {

    private const val PREFS = "demo_theme"
    private const val KEY_DARK = "dark"

    /** 启动时恢复上次的选择，未选择过时为浅色 */
    fun restore(context: Context) {
        AppCompatDelegate.setDefaultNightMode(modeOf(savedDark(context)))
    }

    fun toggle(context: Context) {
        val dark = !isNight(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, dark).apply()
        AppCompatDelegate.setDefaultNightMode(modeOf(dark))
    }

    fun isNight(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun savedDark(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)

    private fun modeOf(dark: Boolean) =
        if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
}

/**
 * 在任何 Activity 创建前恢复主题
 */
class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DemoTheme.restore(this)
    }
}
