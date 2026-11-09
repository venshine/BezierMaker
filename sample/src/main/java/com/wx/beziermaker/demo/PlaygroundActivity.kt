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

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.wx.beziermaker.BezierView

/**
 * 画板：阶数、速率、切线、循环、增删控制点、播放与停止，覆盖 BezierView 的全部公开方法
 *
 * 阶数一律从 [BezierView.getOrder] 读，不另存一份：setOrder / addPoint 在没有空位时会少加点，
 * 以控件的实际结果为准
 *
 * @author venshine
 */
class PlaygroundActivity : BaseActivity() {

    private lateinit var bezier: BezierView
    private lateinit var orders: Segmented
    private lateinit var status: TextView

    private var rate = DEFAULT_RATE
    private var tangent = true
    private var loop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)
        setToolbar(R.string.demo_playground, "BezierView")
        findViewById<View>(R.id.canvas).usePaperBackground()
        status = findViewById(R.id.status_text)

        // 切换深浅色会重建页面，恢复阶数和参数；控制点位置不恢复，由 BezierView 重新生成
        savedInstanceState?.let {
            rate = it.getInt(KEY_RATE, DEFAULT_RATE)
            tangent = it.getBoolean(KEY_TANGENT, true)
            loop = it.getBoolean(KEY_LOOP, false)
        }
        bezier = findViewById(R.id.bezier)
        bezier.setRate(rate)
        bezier.setTangent(tangent)
        bezier.setLoop(loop)
        val initialOrder = savedInstanceState?.getInt(KEY_ORDER) ?: DemoBezier.DEFAULT_ORDER

        val labels = (1..DemoBezier.MAX_ORDER).map { it.toString() }
        orders = Segmented(findViewById(R.id.order_segment), labels, labels.map { "$it 阶" }) { i -> changeOrder(i + 1) }
        initRate()
        initToggles()
        initButtons()
        renderOrder()
        bezier.afterLayout {
            bezier.setOrder(initialOrder)
            renderOrder()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ORDER, bezier.order)
        outState.putInt(KEY_RATE, rate)
        outState.putBoolean(KEY_TANGENT, tangent)
        outState.putBoolean(KEY_LOOP, loop)
    }

    private fun changeOrder(order: Int) {
        bezier.setOrder(order)
        renderOrder()
        showStatus(if (bezier.order < order) getString(R.string.pg_status_no_room, bezier.orderStr)
        else getString(R.string.pg_status_order, bezier.orderStr, bezier.order + 1))
    }

    private fun initRate() {
        val valueText = findViewById<TextView>(R.id.rate_value)
        val tint = ColorStateList.valueOf(colorOf(R.color.accent_ink))
        findViewById<SeekBar>(R.id.rate_seek).apply {
            // minSdk 23 没有 SeekBar.min，进度 0 对应速率 1
            max = MAX_RATE - 1
            progress = rate - 1
            progressTintList = tint
            thumbTintList = tint
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    rate = progress + 1
                    bezier.setRate(rate)
                    valueText.text = getString(R.string.pg_rate_format, rate, DemoBezier.framesOf(rate))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
        valueText.text = getString(R.string.pg_rate_format, rate, DemoBezier.framesOf(rate))
    }

    private fun initToggles() {
        val tangentChip = findViewById<View>(R.id.toggle_tangent)
        val tangentLabel = findViewById<TextView>(R.id.toggle_tangent_label)
        tangentChip.setOnClickListener {
            tangent = !tangent
            // 播放中也可以切换，下一帧生效
            bezier.setTangent(tangent)
            bindToggle(tangentChip, tangentLabel, tangent)
            showStatus(if (tangent) R.string.pg_status_tangent_on else R.string.pg_status_tangent_off)
        }
        bindToggle(tangentChip, tangentLabel, tangent)

        val loopChip = findViewById<View>(R.id.toggle_loop)
        val loopLabel = findViewById<TextView>(R.id.toggle_loop_label)
        loopChip.setOnClickListener {
            loop = !loop
            bezier.setLoop(loop)
            bindToggle(loopChip, loopLabel, loop)
            showStatus(if (loop) R.string.pg_status_loop_on else R.string.pg_status_loop_off)
        }
        bindToggle(loopChip, loopLabel, loop)
    }

    private fun initButtons() {
        bindButton(R.id.play_button, R.id.play_label, R.drawable.ic_play, colorOf(R.color.on_accent)) {
            bezier.start()
            showStatus(if (loop) R.string.pg_status_play_loop else R.string.pg_status_play_once)
        }
        bindButton(R.id.stop_button, R.id.stop_label, R.drawable.ic_stop, colorOf(R.color.text)) {
            bezier.stop()
            showStatus(R.string.pg_status_stopped)
        }
        bindButton(R.id.add_button, R.id.add_label, R.drawable.ic_plus, colorOf(R.color.text)) { addPoint() }
        bindButton(R.id.del_button, R.id.del_label, R.drawable.ic_minus, colorOf(R.color.text)) { delPoint() }
    }

    private fun bindButton(@IdRes id: Int, @IdRes labelId: Int, @DrawableRes icon: Int, @ColorInt tint: Int,
                           onClick: () -> Unit) {
        findViewById<TextView>(labelId).setStartIcon(tintedIcon(icon, tint, ICON_SIZE))
        findViewById<View>(id).setOnClickListener { onClick() }
    }

    /** addPoint 在播放中、已到上限、附近没有空位时都返回 false，库不区分原因，这里能确定的只有上限 */
    private fun addPoint() {
        val before = bezier.order
        when {
            bezier.addPoint() -> showStatus(getString(R.string.pg_status_added, bezier.order))
            before >= DemoBezier.MAX_ORDER -> showStatus(R.string.pg_status_max)
            else -> showStatus(R.string.pg_status_add_failed)
        }
        renderOrder()
    }

    /** delPoint 只在播放中或只剩 2 个控制点时返回 false */
    private fun delPoint() {
        val before = bezier.order
        when {
            bezier.delPoint() -> showStatus(getString(R.string.pg_status_deleted, before))
            before <= 1 -> showStatus(R.string.pg_status_min)
            else -> showStatus(R.string.pg_status_busy)
        }
        renderOrder()
    }

    private fun renderOrder() {
        val order = bezier.order
        orders.select(order - 1)
        findViewById<TextView>(R.id.order_text).text = getString(R.string.order_format, bezier.orderStr)
        findViewById<TextView>(R.id.order_chip).text = getString(R.string.order_chip_format, order)
    }

    private fun showStatus(@StringRes text: Int) = showStatus(getString(text))

    private fun showStatus(text: String) {
        status.text = text
    }

    private companion object {
        const val DEFAULT_RATE = 10
        const val MAX_RATE = 30
        const val ICON_SIZE = 20f
        const val KEY_ORDER = "order"
        const val KEY_RATE = "rate"
        const val KEY_TANGENT = "tangent"
        const val KEY_LOOP = "loop"
    }
}
