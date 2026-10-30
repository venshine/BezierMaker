package com.wx.beziermaker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * BezierView 测试：通过公开接口操作，逐帧调用 draw() 驱动动画（每次绘制触发一帧），检查真实绘制结果
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BezierViewTest {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 480;

    private BezierView view;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        view = new BezierView(activity);
    }

    private void layout() {
        view.measure(View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, WIDTH, HEIGHT);
    }

    private Bitmap drawFrame() {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        shadowOf(Looper.getMainLooper()).idle();
        return bitmap;
    }

    /**
     * 播放动画直到结束，返回曲线完整画出时的最后一帧
     */
    private Bitmap playToEnd() {
        view.start();
        Bitmap last = null;
        for (int i = 0; i < 1000; i++) {
            Bitmap frame = drawFrame();
            if (hasRed(frame)) {
                last = frame;
            } else if (last != null) {
                // 第一帧曲线长度为 0 还没有红色；画过曲线后再没有红色，说明动画已结束，只剩控制点
                return last;
            }
        }
        throw new AssertionError("animation did not finish");
    }

    private static boolean hasRed(Bitmap bitmap) {
        for (int x = 0; x < bitmap.getWidth(); x += 2) {
            for (int y = 0; y < bitmap.getHeight(); y += 2) {
                if (bitmap.getPixel(x, y) == Color.RED) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 只读取控制点坐标：addPoint 随机选择新控制点的位置，公开接口拿不到坐标
     */
    @SuppressWarnings("unchecked")
    private List<PointF> controlPoints() throws Exception {
        Field field = BezierView.class.getDeclaredField("mControlPoints");
        field.setAccessible(true);
        List<PointF> points = new ArrayList<>();
        for (PointF p : (List<PointF>) field.get(view)) {
            points.add(new PointF(p.x, p.y));
        }
        return points;
    }

    /**
     * 伯恩斯坦多项式形式的贝塞尔曲线，与库里的 de Casteljau 递推独立实现
     */
    private static PointF bernstein(List<PointF> points, double t) {
        int n = points.size() - 1;
        double x = 0;
        double y = 0;
        for (int i = 0; i <= n; i++) {
            double b = binomial(n, i) * Math.pow(t, i) * Math.pow(1 - t, n - i);
            x += b * points.get(i).x;
            y += b * points.get(i).y;
        }
        return new PointF((float) x, (float) y);
    }

    private static double binomial(int n, int k) {
        double r = 1;
        for (int i = 1; i <= k; i++) {
            r = r * (n - k + i) / i;
        }
        return r;
    }

    private void assertCurvePasses(Bitmap bitmap, List<PointF> points) {
        assertNotNull(bitmap);
        for (double t : new double[]{0.25, 0.5, 0.75}) {
            PointF p = bernstein(points, t);
            assertEquals("t=" + t + " at " + p, Color.RED,
                    bitmap.getPixel(Math.round(p.x), Math.round(p.y)));
        }
    }

    // 二阶曲线经过伯恩斯坦公式算出的位置
    @Test
    public void quadraticCurveMatchesBernstein() throws Exception {
        layout();
        assertEquals(2, view.getOrder());
        List<PointF> points = controlPoints();

        assertCurvePasses(playToEnd(), points);
    }

    // 三阶曲线经过伯恩斯坦公式算出的位置
    @Test
    public void cubicCurveMatchesBernstein() throws Exception {
        layout();
        assertTrue(view.addPoint());
        assertEquals(3, view.getOrder());
        List<PointF> points = controlPoints();

        assertCurvePasses(playToEnd(), points);
    }

    // 阶数范围是 1 到 7；addPoint / delPoint 返回 true 时阶数必须真的变化
    @Test
    public void orderStaysWithinOneToSeven() {
        layout();
        for (int i = 0; i < 10; i++) {
            int before = view.getOrder();
            boolean added = view.addPoint();
            assertEquals("addPoint returned " + added + " at order " + before,
                    added ? before + 1 : before, view.getOrder());
        }
        assertEquals(7, view.getOrder());
        assertFalse(view.addPoint());

        for (int i = 0; i < 10; i++) {
            int before = view.getOrder();
            boolean deleted = view.delPoint();
            assertEquals(deleted ? before - 1 : before, view.getOrder());
        }
        assertEquals(1, view.getOrder());
        assertFalse(view.delPoint());
    }

    @Test
    public void setOrderChangesOrder() {
        layout();
        view.setOrder(5);
        assertEquals(5, view.getOrder());
        view.setOrder(2);
        assertEquals(2, view.getOrder());
    }

    // 播放过程中不能增删控制点，播放结束后可以
    @Test
    public void pointsAreLockedWhileRunning() {
        layout();
        view.start();
        drawFrame();
        assertFalse(view.addPoint());
        assertFalse(view.delPoint());
        assertEquals(2, view.getOrder());

        view.stop();
        assertTrue(view.addPoint());
        assertEquals(3, view.getOrder());
    }

    // 布局完成前（如在 onCreate 中）调用 addPoint：旧实现返回 true 却没有加上控制点
    @Test
    public void addPointBeforeLayoutAddsPoint() {
        assertTrue(view.addPoint());
        assertEquals(3, view.getOrder());
    }

    // 布局完成前调用 setOrder 应生效，旧实现阶数保持不变；控制点应落在屏幕短边围成的区域内
    @Test
    public void setOrderBeforeLayoutTakesEffect() throws Exception {
        view.setOrder(7);
        assertEquals(7, view.getOrder());

        DisplayMetrics metrics = view.getResources().getDisplayMetrics();
        int size = Math.min(metrics.widthPixels, metrics.heightPixels);
        for (PointF p : controlPoints()) {
            assertTrue(p.toString(), p.x > 0 && p.x < size && p.y > 0 && p.y < size);
        }
    }

    // 控件太小、找不到合法位置时 addPoint 应返回 false，旧实现总是返回 true
    @Test
    public void addPointReturnsFalseWhenThereIsNoRoom() {
        view.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 100, 100);

        assertFalse(view.addPoint());
        assertEquals(2, view.getOrder());
    }
}
