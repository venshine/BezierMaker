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
package com.wx.beziermaker.demo;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wx.android.common.util.ToastUtils;
import com.wx.android.common.util.VibratorUtils;
import com.wx.beziermaker.BezierView;

/**
 * Demo
 *
 * @author venshine
 */
public class MainActivity extends AppCompatActivity {

    private BezierView mBezierView;

    private TextView mTextView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        ToastUtils.init(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBezierView = findViewById(R.id.bezier);
        mTextView = findViewById(R.id.textview);
        mTextView.setText(String.format("%s阶贝塞尔曲线", mBezierView.getOrderStr()));

        SeekBar seekBar = findViewById(R.id.seekbar);
        Switch loop = findViewById(R.id.loop);
        Switch tangent = findViewById(R.id.tangent);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    progress = 1;
                }
                mBezierView.setRate(progress * 2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        loop.setChecked(false);
        tangent.setChecked(true);
        loop.setOnCheckedChangeListener((buttonView, isChecked) -> mBezierView.setLoop(isChecked));
        tangent.setOnCheckedChangeListener((buttonView, isChecked) -> mBezierView.setTangent(isChecked));
    }

    public void start(View view) {
        mBezierView.start();
        VibratorUtils.vibrate(this, 500);
    }

    public void stop(View view) {
        mBezierView.stop();
        VibratorUtils.vibrate(this, 500);
    }

    public void add(View view) {
        if (mBezierView.addPoint()) {
            mTextView.setText(String.format("%s阶贝塞尔曲线", mBezierView.getOrderStr()));
            VibratorUtils.vibrate(this, 300);
        } else {
            ToastUtils.showToast("Add point failed.");
        }
    }

    public void del(View view) {
        if (mBezierView.delPoint()) {
            mTextView.setText(String.format("%s阶贝塞尔曲线", mBezierView.getOrderStr()));
            VibratorUtils.vibrate(this, 300);
        } else {
            ToastUtils.showToast("Delete point failed.");
        }
    }

}
