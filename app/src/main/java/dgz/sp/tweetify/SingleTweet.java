package dgz.sp.tweetify;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.internal.TwitterCollection;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.StatusesService;
import com.twitter.sdk.android.tweetui.CompactTweetView;
import com.twitter.sdk.android.tweetui.FixedTweetTimeline;
import com.twitter.sdk.android.tweetui.Timeline;
import com.twitter.sdk.android.tweetui.TweetView;
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.tweetui.TweetViewFetchAdapter;

import org.w3c.dom.Text;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TweetEntity;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Created by David on 23/06/2016.
 */

public class SingleTweet extends AppCompatActivity {

    final TweetViewFetchAdapter adapter =
            new TweetViewFetchAdapter<CompactTweetView>(SingleTweet.this);
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tweet_custom);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Bundle bundle = getIntent().getExtras();
        final long ID = bundle.getLong("ID");
        final ImageButton RT = (ImageButton)findViewById(R.id.RT);
        final ImageButton LIKE = (ImageButton)findViewById(R.id.LIKE);

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
        new SimpleTask().execute();
    }
    public void loadTweet() throws IOException {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Bundle bundle = getIntent().getExtras();
        final long ID = bundle.getLong("ID");
        final ImageButton RT = (ImageButton)findViewById(R.id.RT);
        final ImageButton LIKE = (ImageButton)findViewById(R.id.LIKE);
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
            TextView tweetText = (TextView)findViewById(R.id.tweetText);
            TextView user_name = (TextView)findViewById(R.id.user_name);
            ImageView imageView = (ImageView)findViewById(R.id.imageView2);
            TextView username = (TextView)findViewById(R.id.username);
            ImageView imageView2 = (ImageView)findViewById(R.id.imageView3);
            URL url2 = new URL(tweet.getUser().getProfileImageURL());
            Bitmap bmp2 = BitmapFactory.decodeStream(url2.openConnection().getInputStream());
            imageView2.setImageBitmap(bmp2);
            MediaEntity[] media = tweet.getMediaEntities();
            tweetText.setText(tweet.getText());
            user_name.setText(tweet.getUser().getName());
            username.setText("@"+tweet.getUser().getScreenName());
            for(MediaEntity m : media){
                URL url = new URL(m.getMediaURL());
                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                imageView.setImageBitmap(bmp);
            }
            if(tweet.isRetweetedByMe()) {
                //Toast.makeText(getApplicationContext(), "Is retweeted by you", Toast.LENGTH_LONG).show();
            }
            if(tweet.isFavorited()) {
                Toast.makeText(getApplicationContext(), "Is liked by you", Toast.LENGTH_LONG).show();
            }
            RT.setVisibility(View.VISIBLE);
            LIKE.setVisibility(View.VISIBLE);
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

    public void checkRetweet(){
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            final Bundle bundle = getIntent().getExtras();
            final long ID = bundle.getLong("ID");
            final String IDRT = preferences.getString(String.valueOf(ID), "");
            if (IDRT.contains(String.valueOf(ID))){
                ImageButton RT = (ImageButton)findViewById(R.id.RT);
                RT.setBackground(getResources().getDrawable(R.drawable.rted_alpha_1));
            } else {
            }

    }
    public void checkLike(){
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Bundle bundle = getIntent().getExtras();
        final long ID = bundle.getLong("ID");
        final String IDRT = preferences.getString(String.valueOf(ID)+"liked", "");
        if (IDRT.contains(String.valueOf(ID)+"liked")){
            ImageButton LIKE = (ImageButton)findViewById(R.id.LIKE);
            LIKE.setBackground(getResources().getDrawable(R.drawable.liked_alpha_1));
        } else {
        }

    }
    private class SimpleTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            runOnUiThread(new Runnable() {
                public void run(){
                    checkLike();
                    checkRetweet();
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