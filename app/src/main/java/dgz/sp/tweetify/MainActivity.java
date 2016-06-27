package dgz.sp.tweetify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {
        @Override
    protected void onCreate(Bundle savedInstanceState) {

            final String TWITTER_KEY = getString(R.string.KEY) ;
            final String TWITTER_SECRET = getString(R.string.SECRET);
        super.onCreate(savedInstanceState);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Boolean loged = preferences.getBoolean("loged", false);
        Fabric.with(this, new Twitter(authConfig), new Crashlytics());
        if(loged == false){
            Intent TL = new Intent(this, Login.class);
            startActivity(TL);
        } else if (loged == true) {
            Intent TL = new Intent(this, EmbeddedTimelineActivity.class);
            startActivity(TL);
        }
        setContentView(R.layout.activity_main);
    }
}
