package dgz.sp.tweetify;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Search;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.models.User;
import com.twitter.sdk.android.core.services.SearchService;
import com.twitter.sdk.android.core.services.StatusesService;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;
import com.twitter.sdk.android.tweetui.FixedTweetTimeline;
import com.twitter.sdk.android.tweetui.SearchTimeline;
import com.twitter.sdk.android.tweetui.Timeline;
import com.twitter.sdk.android.tweetui.TimelineResult;
import com.twitter.sdk.android.tweetui.TweetTimelineListAdapter;
import com.twitter.sdk.android.tweetui.TweetUi;
import com.twitter.sdk.android.tweetui.TweetViewAdapter;
import com.twitter.sdk.android.tweetui.UserTimeline;

import java.util.List;

import io.fabric.sdk.android.Fabric;


public class search extends ListActivity{
    private static final String SEARCH_RESULT_TYPE = "recent";
    private long maxId = 999999;
    private AlertDialog dialog;
    private String SEARCH_QUERY = "No search";

    public void onCreate(Bundle savedInstanceState) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        super.onCreate(savedInstanceState);
        final String TWITTER_KEY = getString(R.string.KEY);
        final String TWITTER_SECRET = getString(R.string.SECRET);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig), new TweetComposer(), new Crashlytics(), new TweetUi());
        setContentView(R.layout.activity_search);
        setTheme(R.style.tw__TweetDarkWithActionsStyle);
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        swipeLayout.setEnabled(false);
        final EditText pass = (EditText)findViewById(R.id.search);
        final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(pass, InputMethodManager.SHOW_IMPLICIT);
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
        final UserTimeline searchTimeline = new UserTimeline.Builder()
                .screenName("nextbitsys")
                .includeRetweets(true)
                .includeReplies(true)
                .build();
        final TweetTimelineListAdapter adapter2 = new TweetTimelineListAdapter.Builder(search.this)
                .setViewStyle(R.style.tw__TweetDarkWithActionsStyle)
                .setTimeline(searchTimeline)
                .build();
        setListAdapter(adapter2);
        pass.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    View view = search.this.getCurrentFocus();
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    String SEARCH_QUERY = pass.getText().toString();
                    Intent intent = new Intent(search.this, searched.class);
                    intent.putExtra("SEARCH_QUERY", SEARCH_QUERY);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setRefreshing(true);
                adapter2.refresh(new Callback<TimelineResult<Tweet>>() {
                    @Override
                    public void success(Result<TimelineResult<Tweet>> result) {
                        swipeLayout.setRefreshing(false);
                    }

                    @Override
                    public void failure(TwitterException exception) {
                        // Toast or some other action
                    }
                });
            }
        });
    }

}
