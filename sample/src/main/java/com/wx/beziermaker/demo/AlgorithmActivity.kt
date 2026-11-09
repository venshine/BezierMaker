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

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.wx.beziermaker.BezierView

/**
 * 原理拆解：放慢播放 de Casteljau 的逐层插值，图例按当前阶数列出每一层的颜色和点数
 *
 * @author venshine
 */
class AlgorithmActivity : BaseActivity() {

    private lateinit var bezier: BezierView
    private lateinit var orders: Segmented

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_algorithm)
        setToolbar(R.string.demo_algorithm, "de Casteljau")
        findViewById<View>(R.id.canvas).usePaperBackground()

        // 一直循环播放：播放中 BezierView 不响应拖动，外层 ScrollView 可以正常滚动
        bezier = findViewById(R.id.bezier)
        bezier.setRate(RATE)
        bezier.setTangent(true)
        bezier.setLoop(true)

        val labels = (MIN_ORDER..DemoBezier.MAX_ORDER).map { it.toString() }
        orders = Segmented(findViewById(R.id.order_segment), labels, labels.map { "$it 阶" }) { i ->
            // setOrder 会先停止动画，换完再重新开始
            bezier.setOrder(MIN_ORDER + i)
            bezier.start()
            render()
        }

        val formula = """
            |// 第 0 层是控制点
            |P[0][j] = Pj
            |// 每层由上一层相邻两点插值，点数减 1
            |P[i][j] = (1 - t) · P[i-1][j] + t · P[i-1][j+1]
            |// n 阶曲线插值 n 层后只剩一个点
            |B(t) = P[n][0]
        """.trimMargin()
        findViewById<TextView>(R.id.formula_text).text = CodeHighlighter.highlight(this, formula, listOf("B"))
        render()
        bezier.afterLayout {
            bezier.setOrder(DemoBezier.DEFAULT_ORDER)
            bezier.start()
            render()
        }
    }

    private fun render() {
        val n = bezier.order
        orders.select(n - MIN_ORDER)
        findViewById<TextView>(R.id.order_chip).text = getString(R.string.order_chip_format, n)

        val legend = findViewById<LinearLayout>(R.id.legend_container)
        legend.removeAllViews()
        legend.addRow(DemoBezier.POLYGON_COLOR, dot = false, getString(R.string.algo_polygon),
            getString(R.string.algo_polygon_desc, n, n + 1))
        // 第 k 层有 n - k + 1 个点，BezierView 画出第 1 到 n - 1 层，第 n 层就是 B(t)
        for (k in 1 until n) {
            legend.addRow(DemoBezier.TANGENT_COLORS[k - 1], dot = false, getString(R.string.algo_level_format, k),
                getString(R.string.algo_level_desc, n - k + 1, n - k))
        }
        legend.addRow(DemoBezier.POINT_COLOR, dot = true, getString(R.string.algo_point), getString(R.string.algo_point_desc))
        legend.addRow(DemoBezier.CURVE_COLOR, dot = false, getString(R.string.algo_curve), getString(R.string.algo_curve_desc))
    }

    /** 图例一行：色条（或圆点）+ 名称 + 说明 */
    private fun LinearLayout.addRow(@ColorInt color: Int, dot: Boolean, title: String, desc: String) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(44f)
        }
        val swatch = View(context).apply {
            background = GradientDrawable().apply {
                setColor(color)
                if (dot) shape = GradientDrawable.OVAL else cornerRadius = dp(2f).toFloat()
            }
        }
        val swatchBox = FrameLayout(context)
        val size = if (dot) FrameLayout.LayoutParams(dp(12f), dp(12f)) else FrameLayout.LayoutParams(dp(22f), dp(5f))
        swatchBox.addView(swatch, size.apply { gravity = Gravity.CENTER })
        row.addView(swatchBox, LinearLayout.LayoutParams(dp(28f), dp(20f)))
        row.addView(TextView(context).apply {
            text = title
            textSize = 14f
            setTextColor(colorOf(R.color.text))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10f) })
        row.addView(TextView(context).apply {
            text = desc
            textSize = 12f
            typeface = font(R.font.jetbrains_mono)
            setTextColor(colorOf(R.color.text_muted))
        })
        addView(row)
    }

    private companion object {
        /** 放慢播放：每帧前进 3 / 1000，约 334 帧 */
        const val RATE = 3

        /** 一阶只有一条线段，没有中间层，从二阶开始 */
        const val MIN_ORDER = 2
    }
}
