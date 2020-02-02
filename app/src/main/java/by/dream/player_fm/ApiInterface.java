package by.dream.player_fm;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiInterface {

    String JSONURL = "https://radiogomelfm.by/";

    @GET("info.json")
    public Call<List<MusicModel>> getMusic();
}
