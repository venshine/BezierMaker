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

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.wx.beziermaker.BezierView

/**
 * Demo
 *
 * @author venshine
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bezierView: BezierView

    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // targetSdk 35 起，Android 15+ 强制全面屏，内容需要避开状态栏和导航栏
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        // 浅色背景上使用深色状态栏图标
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        bezierView = findViewById(R.id.bezier)
        textView = findViewById(R.id.textview)
        updateOrderText()

        findViewById<SeekBar>(R.id.seekbar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                bezierView.setRate(progress.coerceAtLeast(1) * 2)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        findViewById<CompoundButton>(R.id.loop).apply {
            isChecked = false
            setOnCheckedChangeListener { _, isChecked -> bezierView.setLoop(isChecked) }
        }
        findViewById<CompoundButton>(R.id.tangent).apply {
            isChecked = true
            setOnCheckedChangeListener { _, isChecked -> bezierView.setTangent(isChecked) }
        }
    }

    fun start(view: View) {
        bezierView.start()
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun stop(view: View) {
        bezierView.stop()
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun add(view: View) {
        if (bezierView.addPoint()) {
            updateOrderText()
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } else {
            Toast.makeText(this, "Add point failed.", Toast.LENGTH_SHORT).show()
        }
    }

    fun del(view: View) {
        if (bezierView.delPoint()) {
            updateOrderText()
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } else {
            Toast.makeText(this, "Delete point failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOrderText() {
        textView.text = getString(R.string.order_format, bezierView.orderStr)
    }
}
