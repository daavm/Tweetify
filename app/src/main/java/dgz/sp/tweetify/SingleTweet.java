package dgz.sp.tweetify;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.twitter.sdk.android.tweetui.CompactTweetView;
import com.twitter.sdk.android.tweetui.TweetViewFetchAdapter;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by David on 23/06/2016.
 */

public class SingleTweet extends AppCompatActivity {
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

    final TweetViewFetchAdapter adapter =
            new TweetViewFetchAdapter<CompactTweetView>(SingleTweet.this);
    URL url2 = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Bundle bundle = getIntent().getExtras();
        final long ID = bundle.getLong("ID");
        String tweet_text = preferences.getString(ID+"text", "");
        String profile = preferences.getString(ID+"profile", "");
        String RTprofile = preferences.getString(ID+"RTprofile", "");
        String media = preferences.getString(ID+"media", "");
        String username = preferences.getString(ID+"username", "");
        String replyScreen = preferences.getString(ID+"replyScreen", "");
        String name = preferences.getString(ID+"name", "");
        boolean RTstatus = preferences.getBoolean(ID+"RTstatus", false);
        String RTname = preferences.getString(ID+"RTname", "");
        String RTusername = preferences.getString(ID+"RTusername", "");

        String lk = preferences.getString(ID+"LKcount", "");
        String rt = preferences.getString(ID+"RTcount", "");

        if(RTstatus == true){
            if(media != ""){
                setContentView(R.layout.tweet_custom_rt_image);
            } else{
                setContentView(R.layout.tweet_custom_rt_noimage);
            }
        }else {
            if(media != ""){
                setContentView(R.layout.tweet_custom_nort_image);
            } else{
                setContentView(R.layout.tweet_custom_nort_noimage);
            }
        }


        TextView replytext = (TextView)findViewById(R.id.replytext);
        TextView user_name = (TextView)findViewById(R.id.user_name);
        TextView RTuser = (TextView)findViewById(R.id.RTuser);
        TextView usernameText = (TextView)findViewById(R.id.username);
        ImageView imageView2 = (ImageView)findViewById(R.id.imageView3);
        ImageView imageView = (ImageView)findViewById(R.id.imageView2);
        TextView RTcount = (TextView)findViewById(R.id.RTcount);
        TextView LKcount = (TextView)findViewById(R.id.LKcount);
        final ImageButton RT = (ImageButton)findViewById(R.id.RT);
        final ImageButton LIKE = (ImageButton)findViewById(R.id.LIKE);
        TextView tweetText = (TextView)findViewById(R.id.tweetText);

        if(replyScreen != null && replyScreen != ""){
            replytext.setText(System.getProperty("line.separator") + "In reply to @" + replyScreen);
        }
        if(RTstatus == true){
            try {
                url2 = new URL(RTprofile);
                Bitmap bmp2 = BitmapFactory.decodeStream(url2.openConnection().getInputStream());
                imageView2.setImageBitmap(bmp2);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            user_name.setText(RTname);
            usernameText.setText("@"+RTusername);
            RTuser.setText("Retweeted by @" + username);
        } else{
            try {
                url2 = new URL(profile);
                Bitmap bmp2 = BitmapFactory.decodeStream(url2.openConnection().getInputStream());
                imageView2.setImageBitmap(bmp2);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            user_name.setText(name);
            usernameText.setText("@"+username);
        }
        RTcount.setText("RETWEETS" + System.getProperty("line.separator") + rt);
        LKcount.setText("LIKES" + System.getProperty("line.separator") + lk);
        tweetText.setText(tweet_text);
        RT.setVisibility(View.VISIBLE);
        LIKE.setVisibility(View.VISIBLE);
        RT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RT.setBackground(getResources().getDrawable(R.drawable.rted_alpha_1));
                retweet();
            }
        });
        LIKE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String IDRT = preferences.getString(String.valueOf(ID)+"liked", "");
                if (IDRT.contains(String.valueOf(ID)+"liked")){
                    LIKE.setBackground(getResources().getDrawable(R.drawable.like_alpha_1));

                } else {
                    LIKE.setBackground(getResources().getDrawable(R.drawable.liked_alpha_1));
                }
                like();
            }
        });

        Runnable task = new Runnable(){
            @Override
            public void run() {
                new SimpleTask().execute();
            }
        };
        if(media != ""){
            ProgressBar loading = (ProgressBar)findViewById(R.id.loading);
            loading.setVisibility(View.VISIBLE);
        } else {
        }
        worker.schedule(task, 1, TimeUnit.SECONDS);

    }
    public void loadTweet() throws IOException {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Bundle bundle = getIntent().getExtras();
        final long ID = bundle.getLong("ID");
        final String userTokens = preferences.getString("userTokens", "");
        final String userSecrets = preferences.getString("userSecrets", "");
        final String consumerKey = getString(R.string.KEY);
        final String consumerSecret = getString(R.string.SECRET);

        TwitterFactory factory = new TwitterFactory();
        Twitter twitter = factory.getInstance();
        twitter.setOAuthConsumer(consumerKey, consumerSecret);
        AccessToken accessToken = new AccessToken(userTokens, userSecrets);
        twitter.setOAuthAccessToken(accessToken);
        try {
            Status tweet = twitter.showStatus(ID);
            ImageView imageView = (ImageView)findViewById(R.id.imageView2);
            MediaEntity[] media = tweet.getMediaEntities();
            for(MediaEntity m : media){
                URL url = new URL(m.getMediaURL());
                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                imageView.setImageBitmap(bmp);
            }
            ProgressBar loading = (ProgressBar)findViewById(R.id.loading);
            String mediaurl = preferences.getString(ID+"media", "");

            if(mediaurl != ""){
                loading.setVisibility(View.INVISIBLE);
            }
            if(tweet.isRetweetedByMe()) {
                ImageButton RT = (ImageButton)findViewById(R.id.RT);
                RT.setBackground(getResources().getDrawable(R.drawable.rted_alpha_1));
            }
            if(tweet.isFavorited()) {
                ImageButton LIKE = (ImageButton)findViewById(R.id.LIKE);
                LIKE.setBackground(getResources().getDrawable(R.drawable.liked_alpha_1));
            }
        } catch (twitter4j.TwitterException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void retweet(){
        try {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            final String userTokens = preferences.getString("userTokens", "");
            final String userSecrets = preferences.getString("userSecrets", "");
            final String consumerKey = getString(R.string.KEY);
            final String consumerSecret = getString(R.string.SECRET);
            final Bundle bundle = getIntent().getExtras();
            final Long ID = bundle.getLong("ID");
            final String IDRT = preferences.getString(String.valueOf(ID), "");

            TwitterFactory factory = new TwitterFactory();
            Twitter twitter = factory.getInstance();

            twitter.setOAuthConsumer(consumerKey, consumerSecret);
            AccessToken accessToken = new AccessToken(userTokens, userSecrets);
            twitter.setOAuthAccessToken(accessToken);
            if (IDRT.contains(String.valueOf(ID))){
                Toast.makeText(getApplicationContext(), "You have already retweeted this tweet :(", Toast.LENGTH_LONG).show();
            } else {
                twitter.retweetStatus(ID);
                //TODO probar metodo mas efectivo getRetweeted.isRetweetedByMe... --1er intento, no parece funcionar
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(String.valueOf(ID), String.valueOf(ID));
                editor.commit();
                Toast.makeText(getApplicationContext(), "Retweeted!", Toast.LENGTH_LONG).show();
            }

        } catch (twitter4j.TwitterException e){
            Toast.makeText(getApplicationContext(), "Something went wrong :'(", Toast.LENGTH_LONG).show();
            Log.d("TwitterKit", "Retweeting with Twitter failure", e);
        }
    }

    public void like(){
        try {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            final String userTokens = preferences.getString("userTokens", "");
            final String userSecrets = preferences.getString("userSecrets", "");
            final String consumerKey = getString(R.string.KEY);
            final String consumerSecret = getString(R.string.SECRET);
            final Bundle bundle = getIntent().getExtras();
            final Long ID = bundle.getLong("ID");

            TwitterFactory factory = new TwitterFactory();
            Twitter twitter = factory.getInstance();
            twitter.setOAuthConsumer(consumerKey, consumerSecret);
            AccessToken accessToken = new AccessToken(userTokens, userSecrets);
            twitter.setOAuthAccessToken(accessToken);
            final String IDRT = preferences.getString(String.valueOf(ID)+"liked", "");
            if (IDRT.contains(String.valueOf(ID)+"liked")){
                twitter.destroyFavorite(ID);
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove(String.valueOf(String.valueOf(ID)+"liked") + String.valueOf(String.valueOf(String.valueOf(ID)+"liked")));
                editor.commit();
                Toast.makeText(getApplicationContext(), "Liked!", Toast.LENGTH_LONG).show();
            } else {
                twitter.createFavorite(ID);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(String.valueOf(ID)+"liked", String.valueOf(ID)+"liked");
                editor.commit();
                Toast.makeText(getApplicationContext(), "Liked!", Toast.LENGTH_LONG).show();
            }

        } catch (twitter4j.TwitterException e){
            Toast.makeText(getApplicationContext(), "Something went wrong :'(", Toast.LENGTH_LONG).show();
            Log.d("TwitterKit", "Liking with Twitter failure", e);
        }
    }
    private class SimpleTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            runOnUiThread(new Runnable() {
                public void run(){
                    try {
                        loadTweet();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            return null;
        }
        protected void onPostExecute(Void result) {
        }
    }

}