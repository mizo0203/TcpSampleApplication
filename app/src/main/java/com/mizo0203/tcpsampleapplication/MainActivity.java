package com.mizo0203.tcpsampleapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 受信側スレッド
        new Thread(
                        () -> {
                            try {
                                new InternetProtocolSuite().broadcast("Hello");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                .start();
    }
}
