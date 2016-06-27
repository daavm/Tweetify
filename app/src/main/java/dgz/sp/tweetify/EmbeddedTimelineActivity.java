package dgz.sp.tweetify;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

import io.fabric.sdk.android.Fabric;
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
implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener{
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        final String TWITTER_KEY = getString(R.string.KEY) ;
        final String TWITTER_SECRET = getString(R.string.SECRET);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig), new TweetComposer(), new Crashlytics());
        setContentView(R.layout.activity_timeline);
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setEnabled(false);
        final FloatingActionButton floatingActionButton = (FloatingActionButton) this.findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
                final Intent intent = new ComposerActivity.Builder(EmbeddedTimelineActivity.this).session(session).hashtags()
                        .createIntent();
                startActivity(intent);
            }
        });
        floatingActionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                return false;
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // Set an OnMenuItemClickListener to handle menu item clicks
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
            }
        });






        Paging paging = new Paging();
        paging.setCount(400);


        ArrayList<String> listItems=new ArrayList<String>();
        ArrayAdapter<String> adapter;
        adapter=new ArrayAdapter<String>(this,
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
        List<Status> statuses = null;
        ImageView tweetIcon = (ImageView)findViewById(R.id.tweetIcon);
        try {
            statuses = twitter.getHomeTimeline(paging);
        } catch (twitter4j.TwitterException e) {
            e.printStackTrace();
        }
            for (Status status : statuses) {
                SharedPreferences.Editor editor = preferences.edit();
                System.out.println(status.getUser().getName() + ":" +
                        status.getText());
                String text = status.getText();
                if(text.contains("RT @")){
                    String textRT = status.getRetweetedStatus().getText();
                    listItems.add(textRT);
                    editor.putLong(textRT, status.getId());
                    editor.commit();
                } else{
                    listItems.add(text);
                    editor.putLong(text, status.getId());
                    editor.commit();

                }

            }
        getListView().setOnItemClickListener(EmbeddedTimelineActivity.this);

    }
    @Override
    public void onItemClick (AdapterView<?> parent, View view, int position, long id) {


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Long tweet_id = preferences.getLong(getListView().getItemAtPosition(position).toString(), 0L);
        Toast.makeText(EmbeddedTimelineActivity.this, ""+tweet_id,
                Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(EmbeddedTimelineActivity.this, SingleTweet.class);
        intent.putExtra("ID", tweet_id);
        startActivity(intent);
     }
    public void onStop(){
        super.onStop();
    }
    public void onPause(){
        super.onPause();
    }
    @Override
    public void onBackPressed() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory( Intent.CATEGORY_HOME );
        startActivity(homeIntent);
    }
    public void onLoadTweetsStart(){
        int count = 200;

        final StatusesService service = Twitter.getInstance().getApiClient().getStatusesService(); //mentions 6null, home7null
        service.homeTimeline(count, null, null, null, false, true, null, new Callback<List<Tweet>>() {
                            @Override
                            public void success(Result<List<Tweet>> result) {
                                final FixedTweetTimeline searchTimeline = new FixedTweetTimeline.Builder()
                                        .setTweets(result.data)
                                        .build();
                                final CustomTweetTimelineListAdapter adapter2 = new CustomTweetTimelineListAdapter(EmbeddedTimelineActivity.this, searchTimeline);

                                setListAdapter(adapter2);
                                final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
                                swipeLayout.setRefreshing(false);
                            }
                            @Override
                            public void failure(TwitterException error) {
                                Log.d("TwitterKit", "Liking with Twitter failure", error);
                                Toast.makeText(EmbeddedTimelineActivity.this, "Failed to retrieve timeline",
                                        Toast.LENGTH_SHORT).show();
                                final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
                                swipeLayout.setRefreshing(false);
                            }
                        }
                );
    }
    public void onLoadTweets(){
        long max_id = 10;
        final StatusesService service = Twitter.getInstance().getApiClient().getStatusesService(); //mentions 6null, home7null
        service.homeTimeline(800, null, null, null, null, null, null, new Callback<List<Tweet>>() {
                    @Override
                    public void success(Result<List<Tweet>> result) {
                        final FixedTweetTimeline searchTimeline = new FixedTweetTimeline.Builder()
                                .setTweets(result.data)
                                .build();
                        final CustomTweetTimelineListAdapter adapter2 = new CustomTweetTimelineListAdapter(EmbeddedTimelineActivity.this, searchTimeline);

                        setListAdapter(adapter2);
                        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
                        swipeLayout.setRefreshing(false);
                    }
                    @Override
                    public void failure(TwitterException error) {
                        Log.d("TwitterKit", "Liking with Twitter failure", error);
                        Toast.makeText(EmbeddedTimelineActivity.this, "Failed to retrieve timeline",
                                Toast.LENGTH_SHORT).show();
                        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
                        swipeLayout.setRefreshing(false);
                    }
                }
        );
    }

    public void onRefresh() {
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        swipeLayout.setRefreshing(true);
       // onLoadTweets();
    }
    class CustomTweetTimelineListAdapter extends TweetTimelineListAdapter {
        public CustomTweetTimelineListAdapter(Context context, Timeline<Tweet> timeline) {
            super(context, timeline);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            //disable subviews
            if(view instanceof ViewGroup){
                //disableViewAndSubViews((ViewGroup) view); -- Disable like and share
            }

            //enable root view and attach custom listener
            view.setEnabled(true);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(EmbeddedTimelineActivity.this, SingleTweet.class);
                    intent.putExtra("ID", getItemId(position));
                    startActivity(intent);
                }
            });
            return view;
        }

        private void disableViewAndSubViews(ViewGroup layout) {
            layout.setEnabled(false);
            for (int i = 0; i < layout.getChildCount(); i++) {
                View child = layout.getChildAt(i);
                if (child instanceof ViewGroup) {
                    disableViewAndSubViews((ViewGroup) child);
                } else {
                    child.setEnabled(false);
                    child.setClickable(false);
                    child.setLongClickable(false);
                }
            }
        }

    }
}
