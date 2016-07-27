package dgz.sp.tweetify;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.digits.sdk.android.EmailErrorCodes;
import com.twitter.sdk.android.Twitter;
import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.models.TweetEntities;
import com.twitter.sdk.android.core.services.StatusesService;
import com.twitter.sdk.android.tweetcomposer.ComposerActivity;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;
import com.twitter.sdk.android.tweetui.BaseTweetView;
import com.twitter.sdk.android.tweetui.CompactTweetView;
import com.twitter.sdk.android.tweetui.FixedTweetTimeline;
import com.twitter.sdk.android.tweetui.Timeline;
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter;
import com.twitter.sdk.android.tweetui.TweetUi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.Fabric;
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TweetEntity;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import static java.security.AccessController.getContext;

/**
 * Created by David on 21/06/2016.
 */

public class EmbeddedTimelineActivity extends ListActivity
implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener {
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor editor = preferences.edit();
        StrictMode.setThreadPolicy(policy);
        editor.putInt("page", 1);
        final String TWITTER_KEY = getString(R.string.KEY);
        final String TWITTER_SECRET = getString(R.string.SECRET);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig), new TweetComposer(), new Crashlytics());
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        final FloatingActionButton floatingActionButton = (FloatingActionButton) this.findViewById(R.id.fab);
        floatingActionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), "Post a new tweet", Toast.LENGTH_LONG).show();
                return false;
            }
        });
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    final TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
                    final Intent intent = new ComposerActivity.Builder(EmbeddedTimelineActivity.this).session(session).hashtags()
                            .createIntent();
                    startActivity(intent);
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar_title.setText("Tweetify");

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.action_settings) {
                    Intent mentions = new Intent(EmbeddedTimelineActivity.this, Notifications.class);
                    startActivity(mentions);
                    return true;
                }
                if (id == R.id.search) {
                    Intent mentions = new Intent(EmbeddedTimelineActivity.this, search.class);
                    startActivity(mentions);
                    return true;
                }
                if (id == R.id.logout) {
                    CookieSyncManager.createInstance(EmbeddedTimelineActivity.this);
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.removeSessionCookie();
                    Twitter.getSessionManager().clearActiveSession();
                    Twitter.logOut();
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    final Boolean loged = preferences.getBoolean("loged", false);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove("loged" + Boolean.valueOf(loged));
                    editor.apply();
                    editor.putBoolean("loged", false);
                    editor.commit();
                    Toast.makeText(getApplicationContext(), "Loged out", Toast.LENGTH_LONG).show();
                    Intent login = new Intent(EmbeddedTimelineActivity.this, Login.class);
                    startActivity(login);
                }

                return true;
            }
        });
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setEnabled(false);
        // Inflate a menu to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.menu_main2);
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition =
                        (getListView() == null || getListView().getChildCount() == 0) ?
                                0 : getListView().getChildAt(0).getTop();
                swipeLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
             /**   int lastInScreen = firstVisibleItem + visibleItemCount;
                if((lastInScreen == totalItemCount)){
                    onLoadTimeline2();
                } **/
            }

        });
        getListView().setOnItemClickListener(EmbeddedTimelineActivity.this);
        Runnable task = new Runnable(){
            @Override
            public void run() {
                new SimpleTask2().execute();
            }
        };
        worker.schedule(task, 2, TimeUnit.SECONDS);
    }
    @Override
    public void onRefresh() {
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        TextView title = (TextView)findViewById(R.id.toolbar_title);
        title.setText("Refreshing timeline...");
        swipeLayout.setRefreshing(false);
        new SimpleTask2().execute();

    }
    @Override
    public void onBackPressed() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);
    }

    private class SimpleTask2 extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            runOnUiThread(new Runnable() {
                public void run(){
                    final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
                    swipeLayout.setRefreshing(true);

                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    //final int page = preferences.getInt("page", 1);
                    Paging paging = new Paging();
                    paging.setCount(100);
                    SharedPreferences.Editor editor = preferences.edit();
                    //editor.putInt("page", paging.getPage());

                    final ArrayList<String> listItems = new ArrayList<String>();
                    ArrayAdapter<String> adapter;
                    adapter = new ArrayAdapter<String>(EmbeddedTimelineActivity.this,
                            R.layout.item_list, android.R.id.text1,
                            listItems);

                    setListAdapter(adapter);
                    final String userTokens = preferences.getString("userTokens", "");
                    final String userSecrets = preferences.getString("userSecrets", "");
                    final String consumerKey = getString(R.string.KEY);
                    final String consumerSecret = getString(R.string.SECRET);
                    ConfigurationBuilder cb = new ConfigurationBuilder();
                    cb.setIncludeEntitiesEnabled(false);
                    TwitterFactory factory = new TwitterFactory();
                    twitter4j.Twitter twitter = factory.getInstance();
                    twitter.setOAuthConsumer(consumerKey, consumerSecret);
                    AccessToken accessToken = new AccessToken(userTokens, userSecrets);
                    twitter.setOAuthAccessToken(accessToken);
                    List<twitter4j.Status> statuses = null;
                    try {
                        statuses = twitter.getHomeTimeline(paging);
                        for (twitter4j.Status status : statuses) {
                            System.out.println(status.getUser().getName() + ":" +
                                    status.getText());

                            String text = status.getText();
                            if (status.isRetweet() == true) {
                                if(status.getInReplyToScreenName() != null){
                                    twitter4j.Status RT = status.getRetweetedStatus();
                                    String textRT = RT.getText();
                                    editor.putString(status.getId()+"text", textRT);
                                    listItems.add(System.getProperty("line.separator") + "@" + status.getRetweetedStatus().getUser().getScreenName() + "  (Retweeted by @" + status.getUser().getScreenName() + ")" + System.getProperty("line.separator") + System.getProperty("line.separator") + textRT + System.getProperty("line.separator") + System.getProperty("line.separator") + "RT " + status.getRetweetCount() + "     LIKE " + status.getFavoriteCount() + "        In reply to @" + status.getInReplyToScreenName() + System.getProperty("line.separator"));
                                    editor.putLong(System.getProperty("line.separator") + "@" + status.getRetweetedStatus().getUser().getScreenName() + "  (Retweeted by @" + status.getUser().getScreenName() + ")" + System.getProperty("line.separator") + System.getProperty("line.separator") + textRT + System.getProperty("line.separator") + System.getProperty("line.separator") + "RT " + status.getRetweetCount() + "     LIKE " + status.getFavoriteCount() + "        In reply to @" + status.getInReplyToScreenName() + System.getProperty("line.separator"), status.getId());
                                } else{
                                    twitter4j.Status RT = status.getRetweetedStatus();
                                    String textRT = RT.getText();
                                    editor.putString(status.getId()+"text", textRT);
                                    listItems.add(System.getProperty("line.separator") + "@" + status.getRetweetedStatus().getUser().getScreenName() + "  (Retweeted by @" + status.getUser().getScreenName() + ")" + System.getProperty("line.separator") + System.getProperty("line.separator") + textRT + System.getProperty("line.separator") + System.getProperty("line.separator") + "RT " + status.getRetweetCount() + "     LIKE " + status.getFavoriteCount() + System.getProperty("line.separator"));
                                    editor.putLong(System.getProperty("line.separator") + "@" + status.getRetweetedStatus().getUser().getScreenName() + "  (Retweeted by @" + status.getUser().getScreenName() + ")" + System.getProperty("line.separator") + System.getProperty("line.separator") + textRT + System.getProperty("line.separator") + System.getProperty("line.separator") + "RT " + status.getRetweetCount() + "     LIKE " + status.getFavoriteCount() + System.getProperty("line.separator"), status.getId());
                                }

                            } else {
                                if(status.getInReplyToScreenName() != null){
                                    editor.putString(status.getId()+"text", text);
                                        listItems.add(System.getProperty("line.separator") + "@" + status.getUser().getScreenName() + System.getProperty("line.separator") + System.getProperty("line.separator") + text + System.getProperty("line.separator") + System.getProperty("line.separator") + "RT " + status.getRetweetCount() + "     LIKE " + status.getFavoriteCount() + "        In reply to @" + status.getInReplyToScreenName() + System.getProperty("line.separator"));
                                    editor.putLong(System.getProperty("line.separator") + "@" + status.getUser().getScreenName() + System.getProperty("line.separator") + System.getProperty("line.separator") + text + System.getProperty("line.separator") + System.getProperty("line.separator") + "RT " + status.getRetweetCount() + "     LIKE " + status.getFavoriteCount() + "        In reply to @" + status.getInReplyToScreenName() + System.getProperty("line.separator"), status.getId());
                                } else{
                                    editor.putString(status.getId()+"text", text);
                                    listItems.add(System.getProperty("line.separator") + "@" + status.getUser().getScreenName() + System.getProperty("line.separator") + System.getProperty("line.separator") + text + System.getProperty("line.separator") + System.getProperty("line.separator") + "RT " + status.getRetweetCount() + "     LIKE " + status.getFavoriteCount() + System.getProperty("line.separator"));
                                    editor.putLong(System.getProperty("line.separator") + "@" + status.getUser().getScreenName() + System.getProperty("line.separator") + System.getProperty("line.separator") + text + System.getProperty("line.separator") + System.getProperty("line.separator") + "RT " + status.getRetweetCount() + "     LIKE " + status.getFavoriteCount() + System.getProperty("line.separator"), status.getId());
                                }
                            }
                            editor.putString(status.getId()+"profile", status.getUser().getProfileImageURL());
                            editor.putString(status.getId()+"username", status.getUser().getScreenName());
                            editor.putString(status.getId()+"RTcount", ""+status.getRetweetCount());
                            if (status.isRetweet()){
                                editor.putString(status.getId()+"RTusername", ""+status.getRetweetedStatus().getUser().getScreenName());
                                editor.putString(status.getId()+"RTname", ""+status.getRetweetedStatus().getUser().getName());
                                editor.putBoolean(status.getId()+"RTstatus", true);
                                editor.putString(status.getId()+"RTprofile", status.getRetweetedStatus().getUser().getProfileImageURL());
                            } else {
                                editor.putBoolean(status.getId() + "RTstatus", false);
                            }
                            editor.putString(status.getId()+"replyScreen", status.getInReplyToScreenName());
                            editor.putString(status.getId()+"LKcount", ""+status.getFavoriteCount());
                            editor.putString(status.getId()+"name", status.getUser().getName());
                            MediaEntity[] media = status.getMediaEntities();
                            for(MediaEntity m : media) {
                                editor.putString(status.getId()+"media", m.getMediaURL().toString());
                            }
                            editor.commit();
                        }
                    } catch (twitter4j.TwitterException e) {
                        e.printStackTrace();
                    }

                    swipeLayout.setRefreshing(false);
                    TextView title = (TextView)findViewById(R.id.toolbar_title);
                    title.setText("Tweetify");
                }
            });
            return null;
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Long tweet_id = preferences.getLong(getListView().getItemAtPosition(position).toString(), 0L);

        Intent intent = new Intent(EmbeddedTimelineActivity.this, SingleTweet.class);
        intent.putExtra("ID", tweet_id);
        startActivity(intent);
    }
}
