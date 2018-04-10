package info.guardianproject.securereaderinterface;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.tinymission.rss.Feed;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import info.guardianproject.securereader.OPMLParser;
import info.guardianproject.securereaderinterface.adapters.FeedListAdapterExplore;
import info.guardianproject.securereaderinterface.adapters.FeedRecyclerViewAdapter;

/**
 * Created by N-Pex on 2017-02-24.
 */

public class ImportOPMLActivity extends AppActivity implements FeedRecyclerViewAdapter.FeedRecyclerViewAdapterListener
{
    public static final String LOGTAG = "ImportOPMLActivity";
    public static final boolean LOGGING = false;
    private ProgressDialog mDialog;
    private RecyclerView listFeeds;
    private FeedRecyclerViewAdapter adapter;
    private Button buttonImport;

    @SuppressLint("NewApi") @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_opml);
        setMenuIdentifier(R.menu.activity_import_opml);
        AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        lp.setScrollFlags(0);
        mToolbar.setLayoutParams(lp);
        setDisplayHomeAsUp(true);
        setActionBarTitle(getTitle());

        listFeeds = (RecyclerView) findViewById(R.id.listFeeds);
        listFeeds.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        buttonImport = (Button) findViewById(R.id.btnImport);
        buttonImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Feed feed : adapter.getSelectedFeeds()) {
                    App.getInstance().socialReader.subscribeFeed(feed);
                }
                finish();
            }
        });

        Uri uri = getIntent().getData();
        if (uri == null)
        {
            finish();
        } else {
            // Import OPML
            AsyncTask<Intent, Void, ArrayList<OPMLParser.OPMLOutline>> task = new AsyncTask<Intent, Void, ArrayList<OPMLParser.OPMLOutline>>() {
                @Override
                protected ArrayList<OPMLParser.OPMLOutline> doInBackground(Intent... intents) {
                    if (intents.length > 0) {
                        Intent intent = intents[0];
                        Uri uri = intent.getData();
                        if (uri != null)
                        {
                            return parseOPML(uri);
                        }
                    }
                    return null;
                }

                @Override
                protected void onCancelled(ArrayList<OPMLParser.OPMLOutline> outlines) {
                    mDialog.dismiss();
                }

                @Override
                protected void onPostExecute(ArrayList<OPMLParser.OPMLOutline> outlines) {
                    super.onPostExecute(outlines);
                    mDialog.dismiss();
                    populateList(outlines);
                }
            };
            mDialog = new ProgressDialog(this, R.style.AppThemeProgressDialog);
            mDialog.setIndeterminate(true);
            mDialog.setMessage("Processing...");
            mDialog.show();
            task.execute(getIntent());
        }
        updateImportButton();
    }

    private void updateImportButton() {
        buttonImport.setEnabled(adapter != null && adapter.getSelectedFeeds().size() > 0);
    }

    public ArrayList<OPMLParser.OPMLOutline> parseOPML(Uri uri)
    {
        final ArrayList<OPMLParser.OPMLOutline> retOutlines = new ArrayList<>();
        InputStream is = null;
        try
        {
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()))
            {
                is = getContentResolver().openInputStream(uri);
            }
            else
            {
                URLConnection conn = new URL(uri.toString()).openConnection();
                conn.connect();
                is = conn.getInputStream();
            }

            if (is != null) {
                OPMLParser parser = new OPMLParser(is, new OPMLParser.OPMLParserListener() {
                    @Override
                    public void opmlParsed(ArrayList<OPMLParser.OPMLOutline> outlines) {
                        if (outlines != null) {
                            retOutlines.addAll(outlines);
                        }
                    }
                });
            }
        }
        catch (Exception e)
        {
            Log.d("IMPORT", e.toString());
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }
        return retOutlines;
    }

    private void populateList(ArrayList<OPMLParser.OPMLOutline> outlines) {
        ArrayList<Feed> feeds = new ArrayList<>();

        if (LOGGING)
            Log.v(LOGTAG, "Finished Parsing OPML");
        if (outlines != null && outlines.size() > 0) {
            for (int i = 0; i < outlines.size(); i++) {
                OPMLParser.OPMLOutline outlineElement = outlines.get(i);
                String title = outlineElement.text;
                if (TextUtils.isEmpty(title)) {
                    title = outlineElement.xmlUrl;
                }
                if (!TextUtils.isEmpty(outlineElement.xmlUrl)) {
                    Feed newFeed = new Feed(title, outlineElement.xmlUrl);
                    newFeed.setCategory(outlineElement.category);
                    newFeed.setSubscribed(false);
                    newFeed.setDescription(outlineElement.description);
                    feeds.add(newFeed);
                }
            }
        } else {
            if (LOGGING)
                Log.e(LOGTAG, "Received null OPML");
        }
        adapter = new FeedRecyclerViewAdapter(this, this);
        adapter.setFeeds(feeds);
        listFeeds.setAdapter(adapter);
    }

    @Override
    public void onFeedFollow(Feed feed) {
        updateImportButton();
    }

    @Override
    public void onFeedUnfollow(Feed feed) {
        updateImportButton();
    }
}
