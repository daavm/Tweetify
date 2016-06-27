package dgz.sp.tweetify;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.*;
import com.twitter.sdk.android.core.identity.*;

import io.fabric.sdk.android.Fabric;
import twitter4j.auth.AccessToken;

public class Login extends Activity {
    private TwitterLoginButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        final String TWITTER_KEY = getString(R.string.KEY) ;
        final String TWITTER_SECRET = getString(R.string.SECRET);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig), new Crashlytics());
        loginButton = (TwitterLoginButton) findViewById(R.id.twitter_login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                // The TwitterSession is also available through:
                // Twitter.getInstance().core.getSessionManager().getActiveSession()
                TwitterSession session = result.data;
                SharedPreferences.Editor editor = preferences.edit();
                final String userTokens = preferences.getString("userTokens", "");
                final String userSecrets = preferences.getString("userSecrets", "");
                String authToken = " " + session.getAuthToken();
                String userToken = authToken.substring(7, 57);
                String userSecret = authToken.substring(65);
                editor.remove("userTokens" + String.valueOf(userTokens));
                editor.remove("userSecrets" + String.valueOf(userSecrets));
                editor.apply();
                editor.putString("userTokens", userToken);
                editor.putString("userSecrets", userSecret);
                // TODO: Remove toast and use the TwitterSession's userID
                // with your app's user model
                Toast.makeText(getApplicationContext(), userToken + "+" + userSecret, Toast.LENGTH_LONG).show();
                final Boolean loged = preferences.getBoolean("loged", false);
                editor.remove("loged" + Boolean.valueOf(loged));
                editor.apply();
                editor.putBoolean("loged", true);
                editor.commit();
                Intent TL = new Intent(Login.this, EmbeddedTimelineActivity.class);
                startActivity(TL);
            }
            @Override
            public void failure(TwitterException exception) {
                Log.d("TwitterKit", "Login with Twitter failure", exception);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Make sure that the loginButton hears the result from any
        // Activity that it triggered.
        loginButton.onActivityResult(requestCode, resultCode, data);
    }
}
