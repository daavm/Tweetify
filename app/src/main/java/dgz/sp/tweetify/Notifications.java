package dgz.sp.tweetify;

import android.app.ListActivity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.AbsListView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.FavoriteService;
import com.twitter.sdk.android.core.services.StatusesService;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;
import com.twitter.sdk.android.tweetui.FixedTweetTimeline;
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter;
import com.twitter.sdk.android.tweetui.TweetViewAdapter;


import java.util.List;

import io.fabric.sdk.android.Fabric;
import twitter4j.api.FavoritesResources;


public class Notifications extends ListActivity
            implements SwipeRefreshLayout.OnRefreshListener{
    Twitter twitter;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        final String TWITTER_KEY = getString(R.string.KEY) ;
        final String TWITTER_SECRET = getString(R.string.SECRET);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig), new TweetComposer(), new Crashlytics());
        setContentView(R.layout.activity_notifications);
        onLoadTweets();
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setEnabled(false);
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
    }
    public void onLoadTweets(){
        long max_id = 30;
        long since_id = 20;


        final StatusesService service = com.twitter.sdk.android.Twitter.getInstance().getApiClient().getStatusesService(); //mentions 6null, home7null
        service.mentionsTimeline(800, null, null , null, true, true, new Callback<List<Tweet>>() {
                    @Override
                    public void success(Result<List<Tweet>> result) {
                        final FixedTweetTimeline searchTimeline = new FixedTweetTimeline.Builder()
                                .setTweets(result.data)
                                .build();
                        final TweetTimelineListAdapter adapter2 = new TweetTimelineListAdapter.Builder(Notifications.this)
                                .setTimeline(searchTimeline)
                                .build();
                        setListAdapter(adapter2);
                        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
                        swipeLayout.setRefreshing(false);
                    }

                    @Override
                    public void failure(com.twitter.sdk.android.core.TwitterException error) {
                        Toast.makeText(Notifications.this, "Failed to retrieve timeline",
                                Toast.LENGTH_SHORT).show();
                        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
                        swipeLayout.setRefreshing(false);
                    }
                }
        );
    }
    public void onRefresh() {
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        onLoadTweets();
        swipeLayout.setRefreshing(true);
    }
}
