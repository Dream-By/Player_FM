package by.dream.player_fm;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private boolean playWhenReady = false;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private Timer timer;
    private TimerTask mTimerTask;
    private TextView textView;
    private TextView trackView;
    private String sArtist;
    private String sTrack;
    private int state = 0;

    private PlaybackStateListener playbackStateListener;
    private static final String TAG = MainActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playbackStateListener = new PlaybackStateListener();

        playerView = findViewById(R.id.video_view);
        textView = findViewById(R.id.textView); //ArtistName
        trackView = findViewById(R.id.trackView);//TrackName
    }

    private void getResponse() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiInterface.JSONURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiInterface api = retrofit.create(ApiInterface.class);
        Call<List<MusicModel>> call = api.getMusic();
        call.enqueue(new Callback<List<MusicModel>>() {
            @Override
            public void onResponse(Call<List<MusicModel>> call, Response<List<MusicModel>> response) {

                if (response.isSuccessful()) {
                    List<MusicModel> musicModelArrayList = response.body();
                    if (musicModelArrayList != null) {//get Artist & Track Name from response
                        sArtist = musicModelArrayList.get(0).getArtistName();
                        sTrack = musicModelArrayList.get(0).getTrackName();
                        textView.setText(sArtist);
                        trackView.setText(sTrack);
                    }
                    else {
                        Log.w("onResponse", "Returned empty response");
                    }
                } else {
                    Log.w("onResponse", "Not success response: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<MusicModel>> call, Throwable t) {
                Log.e("Retrofit", "onFailure: ", t);
            }
        });
    }

    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(this, "Gomel FM");
        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);
    }

    private void initializePlayer(){

        player = ExoPlayerFactory.newSimpleInstance(this);
        playerView.setPlayer(player);
        Uri uri = Uri.parse(getString(R.string.radio_url));
        MediaSource mediaSource = buildMediaSource(uri);
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow,playbackPosition);
        player.prepare(mediaSource,false,false);
        player.addListener(playbackStateListener);
        player.prepare(mediaSource, false, false);

    }



    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getResponse(); //parsing ArtistName & TrackName
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT>=24){
            initializePlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if ((Util.SDK_INT < 24 || player == null)){
            initializePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT >=24){
            releasePlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24 ){
            releasePlayer();
        }
    }

    private void releasePlayer(){
        if (player !=null){
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.removeListener(playbackStateListener);
            player.release();
            player = null;
            timer.cancel();
            mTimerTask.cancel();
        }
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }


    private class PlaybackStateListener  implements Player.EventListener{
        @Override
        public void onPlayerStateChanged(boolean playWhenReady,
                                         int playbackState) {
            String stateString;

            switch (playbackState) {

                case ExoPlayer.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    break;

                case ExoPlayer.STATE_READY:
                    if (playWhenReady)
                    {
                        if (timer != null)
                        {
                            timer.cancel();
                            mTimerTask.cancel();
                        }
                        timer = new Timer();
                        mTimerTask = new MyTimerTask();
                        {
                            timer.schedule(mTimerTask, 50, 1000);
                            getResponse(); //parsing ArtistName & TrackName
                        }
                    }

                    if (!playWhenReady && state !=0 && timer !=null)
                    {
                        timer.cancel();
                        mTimerTask.cancel();
                        textView.setText("");
                        trackView.setText("");
                    }
                    state++;

                    stateString = "ExoPlayer.STATE_READY     -";
                    break;

                case ExoPlayer.STATE_ENDED:
                    stateString = "ExoPlayer.STATE_ENDED     -";
                    break;

                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }
            Log.d(TAG, "changed state to " + stateString
                    + " playWhenReady: " + playWhenReady + "state - " + state);
        }
    }

}
