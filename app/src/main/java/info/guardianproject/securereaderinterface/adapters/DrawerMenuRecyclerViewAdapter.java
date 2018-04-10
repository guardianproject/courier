package info.guardianproject.securereaderinterface.adapters;


import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.tinymission.rss.Feed;

import java.util.ArrayList;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.MainActivity;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.models.FeedSelection;
import info.guardianproject.securereaderinterface.ui.UIBroadcaster;

public class DrawerMenuRecyclerViewAdapter
        extends DrawerMenuRecyclerViewAdapterBase {

    public interface DrawerMenuCallbacks {
        void runAfterMenuClose(Runnable runnable);
    }

    protected DrawerMenuCallbacks mCallbacks;
    private int mCountFavorites;
    private int mCountShared;

    public DrawerMenuRecyclerViewAdapter(Context context, DrawerMenuCallbacks callbacks) {
        super(context);
        mCallbacks = callbacks;
        if (mCallbacks == null)
            throw new IllegalArgumentException("Callbacks need to be set!");
        setHasStableIds(false);
    }

    /**
     *
     * Called on background thread to do costly operations.
     */
    public void recalculateData() {
        mCountFavorites = App.getInstance().socialReader.getAllFavoritesCount();
        mCountShared = App.getInstance().socialReader.getAllSharedCount();
    }

    public void update(ArrayList<Feed> feeds) {
        clear();
        addAllFeedsItem();
        addFavoritesItem(mCountFavorites);
        addNearbyItem(mCountShared);

        // Feeds
        if (feeds != null && feeds.size() > 0) {
            for (Feed feed : feeds) {
                addFeedItem(feed);
            }
        }

        addAddFeedItem();
        notifyDataSetChanged();
    }

    protected void addAllFeedsItem() {
        add(new MenuEntry(R.drawable.ic_menu_news, R.string.feed_filter_all_feeds, 0, true, -1, new SimpleMenuItemCallback() {
            @Override
            public boolean isRefreshing() {
                return App.getInstance().socialReader.manualSyncInProgress();
            }

            @Override
            public void onClicked() {
                mCallbacks.runAfterMenuClose(new Runnable() {
                    @Override
                    public void run() {
                        UIBroadcaster.setFeedFilter(getContext(), FeedSelection.ALL_FEEDS);
                        App.getInstance().onCommand(getContext(), R.integer.command_news_list, null);
                    }
                });
            }

            @Override
            public void onRefresh() {
                UIBroadcaster.requestResync(getContext(), FeedSelection.ALL_FEEDS);
            }

            @Override
            public boolean isSelected() {
                return getContext() instanceof MainActivity && App.getInstance().getCurrentFeedSelection() == FeedSelection.ALL_FEEDS;
            }
        }));
    }

    protected void addFavoritesItem(int count) {
        add(new MenuEntry(R.drawable.ic_filter_favorites, R.string.feed_filter_favorites, 0, false, count, new SimpleMenuItemCallback() {
            @Override
            public void onClicked() {
                mCallbacks.runAfterMenuClose(new Runnable() {
                    @Override
                    public void run() {
                        UIBroadcaster.setFeedFilter(getContext(), FeedSelection.FAVORITES);
                        App.getInstance().onCommand(getContext(), R.integer.command_news_list, null);
                    }
                });
            }

            @Override
            public boolean isSelected() {
                return getContext() instanceof MainActivity && App.getInstance().getCurrentFeedSelection() == FeedSelection.FAVORITES;
            }
        }));
    }

    protected void addNearbyItem(int count) {
        add(new MenuEntry(R.drawable.ic_filter_secure_share, R.string.feed_filter_shared_stories, R.string.menu_receive_share, false, count, new SimpleMenuItemCallback() {
            @Override
            public void onClicked() {
                mCallbacks.runAfterMenuClose(new Runnable() {
                    @Override
                    public void run() {
                        UIBroadcaster.setFeedFilter(getContext(), FeedSelection.SHARED);
                        App.getInstance().onCommand(getContext(), R.integer.command_news_list, null);
                    }
                });
            }

            @Override
            public void onShortcutClicked() {
                mCallbacks.runAfterMenuClose(new Runnable() {
                    @Override
                    public void run() {
                        App.getInstance().onCommand(getContext(), R.integer.command_receiveshare, null);
                    }
                });
            }

            @Override
            public boolean isSelected() {
                return getContext() instanceof MainActivity && App.getInstance().getCurrentFeedSelection() == FeedSelection.SHARED;
            }
        }));
    }

    protected void addFeedItem(final Feed feed) {
        MenuEntry entry = new MenuEntry(R.drawable.ic_filter_logo_placeholder,
                feed.getTitle(),
                0,
                true, -1, new SimpleMenuItemCallback() {
            @Override
            public void onClicked() {
                mCallbacks.runAfterMenuClose(new Runnable() {
                    @Override
                    public void run() {
                        UIBroadcaster.setFeedFilter(getContext(), new FeedSelection(feed.getDatabaseId()));
                        App.getInstance().onCommand(getContext(), R.integer.command_news_list, null);
                    }
                });
            }

            @Override
            public boolean isRefreshing() {
                if (App.getInstance().socialReader.getSyncService() != null)
                    return App.getInstance().socialReader.getSyncService().isFeedSyncing(feed);
                return false;
            }

            @Override
            public void onRefresh() {
                UIBroadcaster.requestResync(getContext(), new FeedSelection(feed.getDatabaseId()));
            }

            @Override
            public boolean isSelected() {
                return getContext() instanceof MainActivity && App.getInstance().getCurrentFeedSelection() != null && App.getInstance().getCurrentFeedSelection().equals(feed.getDatabaseId());
            }
        });

        // Set icon uri if we have it
        File feedIconFile = new File(App.getInstance().socialReader.getFileSystemDir(), SocialReader.FEED_ICON_FILE_PREFIX + feed.getDatabaseId());
        if (feedIconFile.exists()) {
            entry.uriIcon = Uri.parse(feedIconFile.getAbsolutePath());
        }

        add(entry);
    }

    protected void addAddFeedItem() {
        add(new MenuEntry(R.drawable.ic_action_add_feed, R.string.feed_filter_add_new, 0, false, -1, new SimpleMenuItemCallback() {
            @Override
            public void onClicked() {
                mCallbacks.runAfterMenuClose(new Runnable() {
                    @Override
                    public void run() {
                        App.getInstance().onCommand(getContext(), R.integer.command_feed_add, null);
                    }
                });
            }

            @Override
            public boolean isSelected() {
                return false;
            }
        }));
    }
}