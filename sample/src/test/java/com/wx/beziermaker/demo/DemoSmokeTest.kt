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
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.wx.beziermaker.BezierView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * 示例页冒烟测试：每个页面在浅色、深色下都能启动，关键交互的结果符合预期
 *
 * 屏幕尺寸接近常见手机，BezierView 加点时才有足够的空位
 *
 * @author venshine
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h860dp-xxhdpi")
class DemoSmokeTest {

    private val controllers = mutableListOf<ActivityController<out Activity>>()

    /**
     * 每个用例结束都销毁启动过的页面：AppCompat 用静态列表记着所有未销毁的页面，
     * 切换深浅色时会对它们逐个 recreate，遗留页面里循环播放的 BezierView 会让 Robolectric 重建时出错
     */
    @After
    fun tearDown() {
        controllers.forEach { it.pause().stop().destroy() }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
    }

    private fun <T : Activity> launch(type: Class<T>): T {
        val controller = Robolectric.buildActivity(type).setup()
        controllers += controller
        idle()
        return controller.get()
    }

    private fun Activity.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun Activity.segment(index: Int): View = findViewById<ViewGroup>(R.id.order_segment).getChildAt(index)

    private val screens = listOf(
        MainActivity::class.java, PlaygroundActivity::class.java, AlgorithmActivity::class.java, ApiActivity::class.java,
    )

    @Test
    fun allScreensLaunchInLightMode() {
        screens.forEach { type ->
            val activity = launch(type)
            assertTrue("${type.simpleName} 应为浅色", !DemoTheme.isNight(activity))
        }
    }

    @Test
    fun allScreensLaunchInDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        screens.forEach { type ->
            val activity = launch(type)
            assertTrue("${type.simpleName} 应为深色", DemoTheme.isNight(activity))
        }
    }

    @Test
    fun themeToggleSwitchesToDark() {
        val activity = launch(MainActivity::class.java)
        activity.findViewById<View>(R.id.theme_toggle).performClick()
        idle()
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun homePreviewCyclesOrder() {
        val activity = launch(MainActivity::class.java)
        assertEquals("三阶贝塞尔曲线", activity.text(R.id.preview_order))
        activity.findViewById<View>(R.id.next_order).performClick()
        assertEquals(4, activity.findViewById<BezierView>(R.id.preview_bezier).order)
        assertEquals("四阶贝塞尔曲线", activity.text(R.id.preview_order))
        assertEquals("5 个控制点", activity.text(R.id.preview_points))
        // 到 5 阶后回到 2 阶
        activity.findViewById<View>(R.id.next_order).performClick()
        activity.findViewById<View>(R.id.next_order).performClick()
        assertEquals(2, activity.findViewById<BezierView>(R.id.preview_bezier).order)
    }

    @Test
    fun playgroundOrderSegmentSetsOrder() {
        val activity = launch(PlaygroundActivity::class.java)
        val bezier = activity.findViewById<BezierView>(R.id.bezier)
        assertEquals(3, bezier.order)
        assertTrue(activity.segment(2).isSelected)

        activity.segment(4).performClick()
        assertEquals(5, bezier.order)
        assertTrue(activity.segment(4).isSelected)
        assertEquals("Order 5", activity.text(R.id.order_chip))
        assertEquals("五阶贝塞尔曲线", activity.text(R.id.order_text))
    }

    @Test
    fun playgroundAddAndDeletePoints() {
        val activity = launch(PlaygroundActivity::class.java)
        val bezier = activity.findViewById<BezierView>(R.id.bezier)

        activity.findViewById<View>(R.id.add_button).performClick()
        assertEquals(4, bezier.order)
        assertEquals("已添加 P4", activity.text(R.id.status_text))

        activity.findViewById<View>(R.id.del_button).performClick()
        assertEquals(3, bezier.order)
        assertEquals("已删除 P4", activity.text(R.id.status_text))

        // 一阶时再删点，库返回 false，状态说明原因
        activity.segment(0).performClick()
        activity.findViewById<View>(R.id.del_button).performClick()
        assertEquals(1, bezier.order)
        assertEquals("一阶至少要 2 个控制点", activity.text(R.id.status_text))
    }

    @Test
    fun playgroundCannotDeleteWhilePlaying() {
        val activity = launch(PlaygroundActivity::class.java)
        val bezier = activity.findViewById<BezierView>(R.id.bezier)
        activity.findViewById<View>(R.id.play_button).performClick()
        activity.findViewById<View>(R.id.del_button).performClick()
        assertEquals(3, bezier.order)
        assertEquals("播放中不能删点，先停止", activity.text(R.id.status_text))

        activity.findViewById<View>(R.id.stop_button).performClick()
        activity.findViewById<View>(R.id.del_button).performClick()
        assertEquals(2, bezier.order)
    }

    @Test
    fun playgroundTangentTogglesWhilePlaying() {
        val activity = launch(PlaygroundActivity::class.java)
        val bezier = activity.findViewById<BezierView>(R.id.bezier)
        val tangent = activity.findViewById<View>(R.id.toggle_tangent)
        // 先关切线再播放，播放中重新打开：库旧版本在下一帧崩溃
        tangent.performClick()
        activity.findViewById<View>(R.id.play_button).performClick()
        idle()
        tangent.performClick()
        idle()
        assertTrue(tangent.isSelected)
        assertEquals("已显示切线", activity.text(R.id.status_text))
        // 切换切线不会停止动画，播放中仍不能删点
        activity.findViewById<View>(R.id.del_button).performClick()
        assertEquals(3, bezier.order)
    }

    @Test
    fun algorithmLegendFollowsOrder() {
        val activity = launch(AlgorithmActivity::class.java)
        val legend = activity.findViewById<ViewGroup>(R.id.legend_container)
        // 控制多边形 + 第 1、2 层 + B(t) + 轨迹
        assertEquals(5, legend.childCount)

        activity.segment(3).performClick()
        assertEquals(5, activity.findViewById<BezierView>(R.id.bezier).order)
        assertEquals(7, legend.childCount)
        assertEquals("Order 5", activity.text(R.id.order_chip))
    }

    @Test
    fun apiListsAllMethods() {
        val activity = launch(ApiActivity::class.java)
        // 10 个方法，中间 9 条分隔线
        assertEquals(19, activity.findViewById<ViewGroup>(R.id.method_container).childCount)
    }
}
