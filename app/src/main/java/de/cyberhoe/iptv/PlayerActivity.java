package de.cyberhoe.iptv;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    @Override protected void onCreate(Bundle b){super.onCreate(b);PlayerView view=new PlayerView(this);setContentView(view);String url=getIntent().getStringExtra("url");setTitle(getIntent().getStringExtra("title"));DefaultLoadControl load=new DefaultLoadControl.Builder().setBufferDurationsMs(5000,30000,1500,3000).build();player=new ExoPlayer.Builder(this).setLoadControl(load).build();view.setPlayer(player);player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();}
    @Override protected void onStop(){if(player!=null){player.release();player=null;}super.onStop();}
}
