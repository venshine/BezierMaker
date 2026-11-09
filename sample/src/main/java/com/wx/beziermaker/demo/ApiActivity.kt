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
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 方法速查：BezierView 的全部公开方法和一段调用示例
 *
 * @author venshine
 */
class ApiActivity : BaseActivity() {

    private class Method(val signature: String, val desc: String, val required: Boolean = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api)
        setToolbar(R.string.demo_api, "BezierView")

        val container = findViewById<LinearLayout>(R.id.method_container)
        METHODS.forEachIndexed { i, method ->
            if (i > 0) {
                container.addView(View(this).apply { setBackgroundColor(colorOf(R.color.border)) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f)))
            }
            container.addView(methodRow(method))
        }

        val code = """
            |val bezier = findViewById<BezierView>(R.id.bezier)
            |// 默认 3 个控制点，即二阶曲线
            |bezier.setOrder(3)
            |// 每帧前进 10 / 1000，越大越快
            |bezier.setRate(10)
            |bezier.setTangent(true)
            |bezier.setLoop(true)
            |bezier.start()
        """.trimMargin()
        findViewById<TextView>(R.id.code_text).text = CodeHighlighter.highlight(this, code,
            listOf("val", "true", "setOrder", "setRate", "setTangent", "setLoop", "start"))
        findViewById<TextView>(R.id.copy_code).setOnClickListener {
            copyToClipboard(code)
            (it as TextView).flashCopied(R.string.copied, R.string.api_copy)
        }
    }

    private fun methodRow(method: Method): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = method.signature
            textSize = 13f
            typeface = font(R.font.jetbrains_mono)
            setTextColor(colorOf(R.color.text))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(TextView(this).apply {
            setText(if (method.required) R.string.api_required else R.string.api_optional)
            textSize = 11f
            if (method.required) {
                setTextColor(colorOf(R.color.accent_ink))
                setBackgroundResource(R.drawable.bg_chip)
            } else {
                setTextColor(colorOf(R.color.text_muted))
                setBackgroundResource(R.drawable.bg_pill_outline)
            }
            setPadding(dp(8f), dp(2f), dp(8f), dp(2f))
        })
        row.addView(header)
        row.addView(TextView(this).apply {
            text = method.desc
            textSize = 12.5f
            setLineSpacing(0f, 1.3f)
            setTextColor(colorOf(R.color.text_muted))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(6f) })
        return row
    }

    private companion object {
        // 说明按 BezierView 的实际行为写，改库时要同步
        val METHODS = listOf(
            Method("void start()", "开始播放生成动画；播放中或手指按住时调用无效", required = true),
            Method("void stop()", "停止动画，回到可拖动控制点的状态"),
            Method("boolean addPoint()", "在最后一个控制点附近加一个点；播放中、已到 7 阶或附近没有空位时返回 false"),
            Method("boolean delPoint()", "删除最后一个控制点；播放中或只剩 2 个控制点时返回 false"),
            Method("int getOrder()", "当前阶数，等于控制点数减 1"),
            Method("void setOrder(int order)", "设置阶数：先停止动画，再逐个增删控制点"),
            Method("String getOrderStr()", "阶数的中文写法，如「三」"),
            Method("void setRate(int rate)", "每帧在 1000 个采样点里前进的步数，越大越快，默认 10"),
            Method("void setTangent(boolean tangent)", "是否显示逐层插值的切线，默认显示；播放中也可切换"),
            Method("void setLoop(boolean loop)", "播完是否自动重播，默认不循环"),
        )
    }
}
