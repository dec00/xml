package de.cyberhoe.iptv;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView view; private ArrayList<String> urls; private int urlIndex; private long resumePosition; private boolean switching; private boolean live;
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);view=new PlayerView(this);setContentView(view);urls=getIntent().getStringArrayListExtra("urls");if(urls==null)urls=new ArrayList<>();String legacy=getIntent().getStringExtra("url");if(urls.isEmpty()&&legacy!=null)urls.add(legacy);live=getIntent().getBooleanExtra("is_live",true);setTitle(getIntent().getStringExtra("title"));}
    private void initializePlayer(){
        if(player!=null||urls.isEmpty())return;
        DefaultLoadControl load=new DefaultLoadControl.Builder().setBufferDurationsMs(8000,45000,1000,2500).setPrioritizeTimeOverSizeThresholds(true).build();
        player=new ExoPlayer.Builder(this).setLoadControl(load).build(); view.setPlayer(player);
        player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException error){switchServer();}});
        playCurrent();
    }
    private void playCurrent(){if(player==null||urlIndex>=urls.size())return;switching=false;player.setMediaItem(MediaItem.fromUri(urls.get(urlIndex)),live?0:resumePosition);player.prepare();player.play();}
    private void switchServer(){
        if(switching)return; switching=true; if(player!=null&&!live)resumePosition=player.getCurrentPosition();
        if(urlIndex+1>=urls.size()){switching=false;Toast.makeText(this,"Alle verfügbaren Server sind momentan gestört.",Toast.LENGTH_LONG).show();return;}
        urlIndex++; Toast.makeText(this,"Serverwechsel …",Toast.LENGTH_SHORT).show(); new Handler(Looper.getMainLooper()).postDelayed(this::playCurrent,350);
    }
    @Override protected void onStart(){super.onStart();initializePlayer();}
    @Override protected void onStop(){if(player!=null){if(!live)resumePosition=player.getCurrentPosition();player.release();player=null;}super.onStop();}
}
