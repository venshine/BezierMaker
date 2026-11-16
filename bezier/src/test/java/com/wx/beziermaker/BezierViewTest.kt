package com.wx.beziermaker

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.os.Looper
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * BezierView 测试：通过公开接口操作，逐帧调用 draw() 驱动动画（每次绘制触发一帧），检查真实绘制结果
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BezierViewTest {

    private lateinit var view: BezierView

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        view = BezierView(activity)
    }

    private fun layout(width: Int = WIDTH, height: Int = HEIGHT) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width, height)
    }

    private fun drawFrame(): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        shadowOf(Looper.getMainLooper()).idle()
        return bitmap
    }

    /**
     * 播放动画直到结束，返回曲线完整画出时的最后一帧
     */
    private fun playToEnd(): Bitmap {
        view.start()
        var last: Bitmap? = null
        repeat(1000) {
            val frame = drawFrame()
            if (hasRed(frame)) {
                last = frame
            } else if (last != null) {
                // 第一帧曲线长度为 0 还没有红色；画过曲线后再没有红色，说明动画已结束，只剩控制点
                return last!!
            }
        }
        throw AssertionError("animation did not finish")
    }

    /**
     * 只读取控制点坐标：addPoint 随机选择新控制点的位置，公开接口拿不到坐标
     */
    private fun controlPoints(): List<PointF> {
        val field = BezierView::class.java.getDeclaredField("controlPoints")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return (field.get(view) as List<PointF>).map { PointF(it.x, it.y) }
    }

    private fun assertCurvePasses(bitmap: Bitmap?, points: List<PointF>) {
        assertNotNull(bitmap)
        for (t in doubleArrayOf(0.25, 0.5, 0.75)) {
            val p = bernstein(points, t)
            assertEquals("t=$t at $p", Color.RED, bitmap!!.getPixel(p.x.roundToInt(), p.y.roundToInt()))
        }
    }

    // 二阶曲线经过伯恩斯坦公式算出的位置
    @Test
    fun quadraticCurveMatchesBernstein() {
        layout()
        assertEquals(2, view.order)
        val points = controlPoints()

        assertCurvePasses(playToEnd(), points)
    }

    // 三阶曲线经过伯恩斯坦公式算出的位置
    @Test
    fun cubicCurveMatchesBernstein() {
        layout()
        assertTrue(view.addPoint())
        assertEquals(3, view.order)
        val points = controlPoints()

        assertCurvePasses(playToEnd(), points)
    }

    // 阶数范围是 1 到 7；addPoint / delPoint 返回 true 时阶数必须真的变化
    @Test
    fun orderStaysWithinOneToSeven() {
        layout()
        repeat(10) {
            val before = view.order
            val added = view.addPoint()
            assertEquals("addPoint returned $added at order $before",
                if (added) before + 1 else before, view.order)
        }
        assertEquals(7, view.order)
        assertFalse(view.addPoint())

        repeat(10) {
            val before = view.order
            val deleted = view.delPoint()
            assertEquals(if (deleted) before - 1 else before, view.order)
        }
        assertEquals(1, view.order)
        assertFalse(view.delPoint())
    }

    @Test
    fun setOrderChangesOrder() {
        layout()
        view.order = 5
        assertEquals(5, view.order)
        view.order = 2
        assertEquals(2, view.order)
    }

    // 播放过程中不能增删控制点，播放结束后可以
    @Test
    fun pointsAreLockedWhileRunning() {
        layout()
        view.start()
        drawFrame()
        assertFalse(view.addPoint())
        assertFalse(view.delPoint())
        assertEquals(2, view.order)

        view.stop()
        assertTrue(view.addPoint())
        assertEquals(3, view.order)
    }

    // 布局完成前（如在 onCreate 中）调用 addPoint：旧实现返回 true 却没有加上控制点
    @Test
    fun addPointBeforeLayoutAddsPoint() {
        assertTrue(view.addPoint())
        assertEquals(3, view.order)
    }

    // 布局完成前调用 setOrder 应生效，旧实现阶数保持不变；控制点应落在屏幕短边围成的区域内
    @Test
    fun setOrderBeforeLayoutTakesEffect() {
        view.order = 7
        assertEquals(7, view.order)

        val metrics = view.resources.displayMetrics
        val size = minOf(metrics.widthPixels, metrics.heightPixels)
        for (p in controlPoints()) {
            assertTrue(p.toString(), p.x > 0 && p.x < size && p.y > 0 && p.y < size)
        }
    }

    // 布局前按屏幕短边估算位置，控件比估算区域小时控制点会落到控件外；首次布局后应全部移回可拖动区域
    @Test
    fun pointsAddedBeforeLayoutMoveInsideView() {
        view.order = 7
        assertEquals(7, view.order)

        // 比 Robolectric 默认屏幕短边（320px）小，初始控制点 (2w/3, w/4) 就在控件外
        val width = 200
        val height = 240
        layout(width, height)

        assertEquals(7, view.order)
        // 30 是 BezierView.REGION_WIDTH：离边缘不足 30px 的位置不能拖入
        for (p in controlPoints()) {
            assertTrue(p.toString(), p.x > 30 && p.x < width - 30 && p.y > 30 && p.y < height - 30)
        }
    }

    // 关闭切线时开始播放，播放中再打开切线：旧实现切线点集为 null，下一帧崩溃
    @Test
    fun enablingTangentWhileRunningDrawsTangent() {
        layout()
        view.setTangent(false)
        view.start()
        drawFrame()
        drawFrame()

        view.setTangent(true)
        drawFrame()
        // 第 1 层切线颜色，对应 TANGENT_COLORS[0]
        assertTrue(hasColor(drawFrame(), Color.parseColor("#7fff00")))
    }

    // 控件太小、找不到合法位置时 addPoint 应返回 false，旧实现总是返回 true
    @Test
    fun addPointReturnsFalseWhenThereIsNoRoom() {
        layout(100, 100)

        assertFalse(view.addPoint())
        assertEquals(2, view.order)
    }

    private companion object {
        const val WIDTH = 320
        const val HEIGHT = 480

        fun hasRed(bitmap: Bitmap) = hasColor(bitmap, Color.RED)

        fun hasColor(bitmap: Bitmap, color: Int): Boolean {
            for (x in 0 until bitmap.width step 2) {
                for (y in 0 until bitmap.height step 2) {
                    if (bitmap.getPixel(x, y) == color) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * 伯恩斯坦多项式形式的贝塞尔曲线，与库里的 de Casteljau 递推独立实现
         */
        fun bernstein(points: List<PointF>, t: Double): PointF {
            val n = points.size - 1
            var x = 0.0
            var y = 0.0
            for (i in 0..n) {
                val b = binomial(n, i) * t.pow(i) * (1 - t).pow(n - i)
                x += b * points[i].x
                y += b * points[i].y
            }
            return PointF(x.toFloat(), y.toFloat())
        }

        fun binomial(n: Int, k: Int): Double {
            var r = 1.0
            for (i in 1..k) {
                r = r * (n - k + i) / i
            }
            return r
        }
    }
}
