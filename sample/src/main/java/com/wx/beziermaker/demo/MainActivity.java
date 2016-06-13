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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

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

    private SeekBar mSeekBar;

    private TextView mTextView;

    private Switch mLoop, mTangent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBezierView = (BezierView) findViewById(R.id.bezier);
        mTextView = (TextView) findViewById(R.id.textview);
        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mLoop = (Switch) findViewById(R.id.loop);
        mTangent = (Switch) findViewById(R.id.tangent);

        mTextView.setText(mBezierView.getOrderStr() + "阶贝塞尔曲线");

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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

        mLoop.setChecked(false);
        mTangent.setChecked(true);
        mLoop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mBezierView.setLoop(isChecked);
            }
        });
        mTangent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mBezierView.setTangent(isChecked);
            }
        });
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
            mTextView.setText(mBezierView.getOrderStr() + "阶贝塞尔曲线");
            VibratorUtils.vibrate(this, 300);
        } else {
            ToastUtils.showToast("Add point failed.");
        }
    }

    public void del(View view) {
        if (mBezierView.delPoint()) {
            mTextView.setText(mBezierView.getOrderStr() + "阶贝塞尔曲线");
            VibratorUtils.vibrate(this, 300);
        } else {
            ToastUtils.showToast("Delete point failed.");
        }
    }

}
