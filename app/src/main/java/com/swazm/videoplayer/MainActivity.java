package com.swazm.videoplayer;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(getApplicationContext(), com.swazm.swazmplayer.Player.class);
        intent.putExtra("videoUrl", "http://vjs.zencdn.net/v/oceans.mp4");
        startActivity(intent);
    }
}
