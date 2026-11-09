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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.wx.beziermaker.BezierView

/**
 * 首页：循环播放的实时预览 + 示例入口 + 接入方式
 *
 * @author venshine
 */
class MainActivity : BaseActivity() {

    private lateinit var bezier: BezierView

    /** 示例 App 与库同版本发布，版本号只在 sample/build.gradle 的 versionName 维护一处 */
    private val version: String by lazy {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.source_button).setOnClickListener { openSource() }
        findViewById<TextView>(R.id.version_chip).text = "v$version"
        initPreview()
        initCards()
        initInstall()
    }

    /**
     * 预览：循环播放 + 显示切线；「换一阶」在 2 到 5 阶之间轮换，阶数再高画面会太挤
     */
    private fun initPreview() {
        findViewById<View>(R.id.preview_canvas).usePaperBackground()
        bezier = findViewById(R.id.preview_bezier)
        bezier.setRate(PREVIEW_RATE)
        bezier.setTangent(true)
        bezier.setLoop(true)
        bezier.afterLayout {
            bezier.setOrder(DemoBezier.DEFAULT_ORDER)
            bezier.start()
            renderPreview()
        }
        findViewById<View>(R.id.next_order).setOnClickListener {
            val next = if (bezier.order >= PREVIEW_MAX_ORDER) PREVIEW_MIN_ORDER else bezier.order + 1
            // setOrder 会先停止动画，换完再重新开始
            bezier.setOrder(next)
            bezier.start()
            renderPreview()
        }
        renderPreview()
    }

    private fun renderPreview() {
        val order = bezier.order
        findViewById<TextView>(R.id.preview_chip).text = getString(R.string.order_chip_format, order)
        findViewById<TextView>(R.id.preview_order).text = getString(R.string.order_format, bezier.orderStr)
        findViewById<TextView>(R.id.preview_points).text = getString(R.string.home_points_format, order + 1)
    }

    private fun initCards() {
        findViewById<View>(R.id.card_playground).setOnClickListener { open(PlaygroundActivity::class.java) }
        bindCard(R.id.card_algorithm, R.drawable.ic_layers, R.string.demo_algorithm, R.string.demo_algorithm_desc,
            AlgorithmActivity::class.java)
        bindCard(R.id.card_api, R.drawable.ic_code, R.string.demo_api, R.string.demo_api_desc,
            ApiActivity::class.java)
    }

    private fun bindCard(@IdRes id: Int, @DrawableRes icon: Int, @StringRes title: Int, @StringRes desc: Int,
                         target: Class<out Activity>) {
        val card = findViewById<View>(id)
        card.findViewById<ImageView>(R.id.card_icon).setImageResource(icon)
        card.findViewById<TextView>(R.id.card_title).setText(title)
        card.findViewById<TextView>(R.id.card_desc).setText(desc)
        card.setOnClickListener { open(target) }
    }

    private fun initInstall() {
        val dependency = "com.github.venshine:BezierMaker:$version"
        val code = "// settings.gradle\nmaven { url = 'https://jitpack.io' }\n\n// build.gradle\nimplementation '$dependency'"
        findViewById<TextView>(R.id.install_code).text =
            CodeHighlighter.highlight(this, code, listOf("implementation", "maven"))
        findViewById<TextView>(R.id.copy_dependency).apply {
            setStartIcon(tintedIcon(R.drawable.ic_copy, colorOf(R.color.text), 16f))
            setOnClickListener {
                copyToClipboard("implementation '$dependency'")
                flashCopied(R.string.home_copied_dependency, R.string.home_copy_dependency)
            }
        }
    }

    private fun open(target: Class<out Activity>) = startActivity(Intent(this, target))

    private fun openSource() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, SOURCE_URL, Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val PREVIEW_RATE = 6
        const val PREVIEW_MIN_ORDER = 2
        const val PREVIEW_MAX_ORDER = 5
        const val SOURCE_URL = "https://github.com/venshine/BezierMaker"
    }
}
