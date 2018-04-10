package info.guardianproject.securereaderinterface.adapters;

import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tinymission.rss.Item;

import info.guardianproject.securereader.ModeSettings;
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.SettingsUI;
import info.guardianproject.securereaderinterface.ThreadedTask;
import info.guardianproject.securereaderinterface.models.FeedSelection;
import info.guardianproject.securereaderinterface.models.ItemViewModel;
import info.guardianproject.securereaderinterface.ui.ItemAdapterListener;

public class ItemCursorRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String LOGTAG = "ItemCursorRVAdapter";
    public static final boolean LOGGING = false;

    private static final int HEADER_ID = -100;
    private static final int FOOTER_ID = -101;

    private final Context mContext;
    private ItemAdapterListener mListener;

    private FeedSelection mFeedSelection;
    private String mFilterString;
    private boolean mShowTags;

    private View headerView;
    private int headerViewId;
    private View footerView;
    private UpdateTask mUpdateTask;
    private ItemCursor mItemCursor;
    private RecyclerView recyclerView;

    public ItemCursorRecyclerViewAdapter(Context context) {
        super();
        setHasStableIds(true);
        mContext = context;
        setFilterString(null);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        updateCursor();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
        synchronized (this) {
            if (mItemCursor != null) {
                mItemCursor.close();
                mItemCursor = null;
            }
        }
    }

    protected Context getContext() {
        return mContext;
    }

    public void setListener(ItemAdapterListener listener) {
        mListener = listener;
    }

    public void setFeedSelection(FeedSelection feedSelection) {
        mFeedSelection = feedSelection;
        updateCursor();
    }

    protected void update() {
        setFilterString(mFilterString);
    }

    public void setFilterString(String filterString) {
        mFilterString = filterString;
        updateCursor();
    }

    public View getHeaderView() {
        return headerView;
    }

    public void setHeaderView(View headerView) {
        this.headerView = headerView;
        update();
    }

    public View getFooterView() {
        return footerView;
    }

    public void setFooterView(View footerView) {
        this.footerView = footerView;
        update();
    }

    @Override
    public int getItemCount() {
        int count = (headerView == null ? 0 : 1) + (footerView == null ? 0 : 1);
        if (mItemCursor != null) {
            count += mItemCursor.getCount();
        }
        return count;
    }

    @Override
    public long getItemId(int position) {
        if (headerView != null) {
            if (position == 0) {
                return HEADER_ID;
            } else {
                position--;
            }
        }
        else if (position == getCursor().getCount() && footerView != null) {
            return FOOTER_ID;
        }
        if (mItemCursor != null) {
            return mItemCursor.getItemId(position);
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        long itemId = getItemId(position);
        if (itemId == HEADER_ID) {
            return headerViewId;
        } else if (itemId == FOOTER_ID) {
            return 2;
        }
        return 0;
    }

    public void setShowTags(boolean showTags) {
        if (showTags != mShowTags) {
            mShowTags = showTags;
            notifyDataSetChanged();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (headerViewId > 0 && viewType == headerViewId) {
            return new HeaderViewHolder(headerView);
        } else if (viewType == 2) {
            return new FooterViewHolder(footerView);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_item, parent, false);
        return new ViewHolderItem(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder || holder instanceof FooterViewHolder) {
            return; // No binding necessary
        }
        ViewHolderItem viewHolder = (ViewHolderItem) holder;
        Item i = App.getInstance().socialReader.itemFromCursor(mItemCursor, position - getHeaderOffset());
        ItemViewModel item = new ItemViewModel(i);
        viewHolder.bindModel(item, !App.getInstance().socialReader.syncSettingsForCurrentNetwork().contains(ModeSettings.Sync.Media), mShowTags, mListener);
    }

    /**
     * Get adapter offset where the real items start.
     * @return index of first data item.
     */
    public int getHeaderOffset() {
        return (headerView == null ? 0 : 1);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof ViewHolderItem) {
            ViewHolderItem viewHolder = (ViewHolderItem) holder;
            viewHolder.unbindModel();
        }
    }

    public ItemViewModel getDataItem(int index) {
        Item item = App.getInstance().socialReader.itemFromCursor(getCursor(), index);
        if (item == null) {
            return null;
        }
        return new ItemViewModel(item);
    }

    public int getDataItemCount() {
        return getCursor().getCount();
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        HeaderViewHolder(View view) {
            super(view);
        }
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        FooterViewHolder(View view) {
            super(view);
        }
    }

    private void updateCursor() {
        if (mUpdateTask != null) {
            if (LOGGING)
                Log.d(LOGTAG, "Cancel update task");
            mUpdateTask.cancel(true);
        }
        mUpdateTask = new UpdateTask();
        mUpdateTask.execute();
    }

    private class UpdateTask extends ThreadedTask<Void, Void, ItemCursor>
    {
        private ItemCursor itemCursor = null;
        private Cursor cursor = null;
        private int headerResourceId;

        UpdateTask()
        {
        }

        @Override
        protected ItemCursor doInBackground(Void... values)
        {
            if (ItemCursorRecyclerViewAdapter.LOGGING)
                Log.v(ItemCursorRecyclerViewAdapter.LOGTAG, "UpdateFeedListTask: doInBackground");

            try {
                SocialReader socialReader = App.getInstance().socialReader;
                headerResourceId = 0;

                Boolean viewed = null;
                //if (App.getSettings().getCurrentMode().articleExpiration() == ModeSettings.ArticleExpiration.AfterRead) {
                //    viewed = false;
                //}

                if (mFeedSelection == FeedSelection.SHARED) {
                    cursor = socialReader.getItemsCursor(-1, null, null, true, null, mFilterString, false, 0);
                    if (cursor == null || cursor.getCount() == 0) {
                        headerResourceId = R.layout.hint_no_shared;
                    }
                } else if (mFeedSelection == FeedSelection.FAVORITES) {
                    cursor = socialReader.getItemsCursor(-1, null, true, null, null, mFilterString, false, 0);
                    if (cursor == null || cursor.getCount() == 0) {
                        if (cursor != null) {
                            cursor.close();
                        }
                        // Generate some random ones
                        headerResourceId = R.layout.hint_add_favorite;
                        cursor = socialReader.getItemsCursor(-1, true, null, null, null, null, true, 5);
                    }
                } else if (mFeedSelection == FeedSelection.ALL_FEEDS || mFeedSelection == null) {
                    if (ItemCursorRecyclerViewAdapter.LOGGING)
                        Log.v(ItemCursorRecyclerViewAdapter.LOGTAG, "UpdateFeedsTask: all subscribed");
                    cursor = socialReader.getItemsCursor(-1, true, null, null, viewed, mFilterString, false, 0);
                } else {
                    if (ItemCursorRecyclerViewAdapter.LOGGING)
                        Log.v(ItemCursorRecyclerViewAdapter.LOGTAG, "UpdateFeedsTask");
                    cursor = socialReader.getItemsCursor(mFeedSelection.Value, null, null, null, viewed, mFilterString, false, 0);
                }

                if (isCancelled()) {
                    return null;
                }

                if (cursor != null) {
                    itemCursor = new ItemCursor(cursor);
                }
                if (getCursor() != null && itemCursor != null) {
                    long itemId = getCursor().getCurrentItemId();
                    if (itemId >= 0) {
                        if (ItemCursorRecyclerViewAdapter.LOGGING) {
                            Log.v(ItemCursorRecyclerViewAdapter.LOGTAG, "Get item id");
                        }
                        if (!isCancelled()) {
                            itemCursor.setCurrentItemId(itemId);
                        }
                        if (ItemCursorRecyclerViewAdapter.LOGGING) {
                            Log.v(ItemCursorRecyclerViewAdapter.LOGTAG, String.format("Cursor swap")); // - index was %d, is now %d", getCursor().getCurrentIndex(), newCursor.getCurrentIndex()));
                        }
                    }
                }
            } catch (Exception ignored) {};
            return itemCursor;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (itemCursor != null) {
                if (ItemCursorRecyclerViewAdapter.LOGGING)
                    Log.d(ItemCursorRecyclerViewAdapter.LOGTAG, "Cancelled - Close item cursor " + itemCursor.hashCode());
                itemCursor.close();
                itemCursor = null;
                cursor = null;
            } else if (cursor != null) {
                if (ItemCursorRecyclerViewAdapter.LOGGING)
                    Log.d(ItemCursorRecyclerViewAdapter.LOGTAG, "Cancelled - Close cursor " + cursor.hashCode());
                cursor.close();
                cursor = null;
            }
        }

        @Override
        protected void onPostExecute(ItemCursor cursor)
        {
            synchronized (ItemCursorRecyclerViewAdapter.this) {
                if (mUpdateTask == this)
                    mUpdateTask = null;
                if (ItemCursorRecyclerViewAdapter.LOGGING)
                    Log.v(ItemCursorRecyclerViewAdapter.LOGTAG, "RefreshFeedsTask: finished");
                //if (newCursor != null)
                //    DatabaseUtils.dumpCursor((net.sqlcipher.Cursor) newCursor.getWrappedCursor());

                if (getCursor() != null) {
                    if (ItemCursorRecyclerViewAdapter.LOGGING)
                        Log.v(ItemCursorRecyclerViewAdapter.LOGTAG, "Old cursor set, Close cursor " + getCursor().hashCode());
                    getCursor().close();
                }
                if (ItemCursorRecyclerViewAdapter.LOGGING)
                    Log.v(ItemCursorRecyclerViewAdapter.LOGTAG, cursor == null ? "No cursor" : ("Opened cursor " + cursor.hashCode()));
                mItemCursor = cursor;
                this.itemCursor = null;
                this.cursor = null;

                // Update header view state
                headerViewId = headerResourceId;
                if (headerResourceId != 0) {
                    headerView = LayoutInflater.from(mContext).inflate(headerResourceId, null, false);
                } else {
                    headerView = null;
                }
                notifyDataSetChanged();

                if (mListener != null) {
                    mListener.onCursorUpdated();
                }
            }
        }
    }

    public ItemCursor getCursor() {
        return mItemCursor;
    }
}
