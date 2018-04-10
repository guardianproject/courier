package info.guardianproject.securereaderinterface.adapters;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tinymission.rss.Item;

import java.util.ArrayList;
import java.util.List;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.models.ItemViewModel;
import info.guardianproject.securereaderinterface.models.MediaItemViewModel;
import info.guardianproject.securereaderinterface.ui.ItemAdapterListener;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;

public class DownloadsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemAdapterListener {
    public static final String LOGTAG = "DownloadsAdapter";
    public static final boolean LOGGING = false;

    private static final int VIEW_TYPE_HEADER_COMPLETE = 0;
    private static final int VIEW_TYPE_HEADER_IN_PROGRESS = 1;
    private static final int VIEW_TYPE_ITEM_COMPLETE = 2;
    private static final int VIEW_TYPE_ITEM_IN_PROGRESS = 3;

    private static final LongSparseArray<ItemViewModel> gComplete = new LongSparseArray<>();
    private static final LongSparseArray<ItemViewModel> gInProgress = new LongSparseArray<>();
    private static final ArrayList<DownloadsAdapter> gInstances = new ArrayList<>();

    private final Context context;
    private DownloadsAdapterListener listener;

    public interface DownloadsAdapterListener {
        void onItemSelected(ItemViewModel item);
    }

    public DownloadsAdapter(Context context) {
        this.context = context;
    }

    public void setListener(DownloadsAdapterListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        gInstances.add(this);
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        gInstances.remove(this);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER_COMPLETE: {
                return new HeaderCompleteViewHolder(createHeaderView());
            }
            case VIEW_TYPE_HEADER_IN_PROGRESS: {
                return new HeaderViewHolder(createHeaderView());
            }
            case VIEW_TYPE_ITEM_COMPLETE: {
                return new ViewHolderItem(createItemCompleteView());
            }
            default: {
                return new ItemInProgressViewHolder(createItemInProgressView());
            }
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderCompleteViewHolder) {
            ((HeaderCompleteViewHolder) holder).title.setText(R.string.downloads_complete);
        } else if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).title.setText(R.string.downloads_in_progress);
        } else if (holder instanceof ViewHolderItem) {
            ((ViewHolderItem) holder).bindModel((ItemViewModel) getItem(position), true, false, this);
        } else if (holder instanceof ItemInProgressViewHolder) {
            populateItemInProgressView(holder.itemView, (ItemViewModel) getItem(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        int numComplete = getNumComplete();
        int numInProgress = getNumInProgress();
        if (numComplete > 0) {
            if (position == 0)
                return VIEW_TYPE_HEADER_COMPLETE;
            position -= 1;
            if (position < numComplete)
                return VIEW_TYPE_ITEM_COMPLETE;
            position -= numComplete;
        }
        if (position == 0)
            return VIEW_TYPE_HEADER_IN_PROGRESS;
        position -= 1;
        if (position < numInProgress)
            return VIEW_TYPE_ITEM_IN_PROGRESS;
        return -1;
    }

    public Object getItem(int position) {
        int numComplete = getNumComplete();
        int numInProgress = getNumInProgress();

        if (numComplete > 0) {
            if (position == 0)
                return null; // "Complete" header
            position -= 1;
            if (position < numComplete)
                return gComplete.get(gComplete.keyAt(position));
            position -= numComplete;
        }
        if (position == 0)
            return null; // "In progress" header
        position -= 1;
        if (position < numInProgress)
            return gInProgress.get(gInProgress.keyAt(position));
        return null;
    }

    @Override
    public long getItemId(int position) {
        Item item = (Item) getItem(position);
        if (item != null)
            return item.getDatabaseId();
        return 0;
    }

    @Override
    public int getItemCount() {
        int num = 0;
        if (getNumComplete() > 0) {
            num += getNumComplete() + 1;
        }
        num += 1; // For "In progress" header
        num += getNumInProgress();
        return num;
    }

    private View createHeaderView() {
        return LayoutInflater.from(context).inflate(R.layout.downloads_header, null);
    }

    private View createItemCompleteView() {
        return LayoutInflater.from(context).inflate(R.layout.downloads_item_complete, null);
    }

    private View createItemInProgressView() {
        return LayoutInflater.from(context).inflate(R.layout.downloads_item_in_progress, null);
    }

    private void populateItemInProgressView(View view, ItemViewModel itemViewModel) {
        TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
        tvTitle.setText(itemViewModel.item.getTitle());

        View operationButtons = view.findViewById(R.id.llOperationButtons);
        operationButtons.setVisibility(View.GONE);
        AnimationHelpers.fadeOut(operationButtons, 0, 0, false);

        View btnCancel = operationButtons.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new ItemCancelListener(itemViewModel));
        View btnRetry = operationButtons.findViewById(R.id.btnRefresh);
        btnRetry.setOnClickListener(new ItemRetryListener(itemViewModel, operationButtons));

        View menuView = view.findViewById(R.id.ivMenu);
        menuView.setOnClickListener(new View.OnClickListener() {
            private View mOperationView;

            @Override
            public void onClick(View v) {
                if (mOperationView.getVisibility() == View.GONE) {
                    mOperationView.setVisibility(View.VISIBLE);
                    AnimationHelpers.fadeIn(mOperationView, 500, 5000, false);
                }
            }

            View.OnClickListener init(View operationView) {
                mOperationView = operationView;
                return this;
            }
        }.init(operationButtons));
    }

    private static int getNumComplete() {
        if (LOGGING)
            Log.v(LOGTAG, "getNumComplete");
        return gComplete.size();
    }

    public static int getNumInProgress() {
        if (LOGGING)
            Log.v(LOGTAG, "getNumInProgress");
        return gInProgress.size();
    }

    public static void downloading(ItemViewModel itemViewModel, MediaItemViewModel mediaItemViewModel) {
        if (gComplete.get(itemViewModel.item.getDatabaseId()) != null) {
            gComplete.remove(itemViewModel.item.getDatabaseId());
        }
        gInProgress.put(itemViewModel.item.getDatabaseId(), itemViewModel);
        notifyDataChanged();
    }

    private static boolean isItemDownloading(ItemViewModel itemViewModel) {
        boolean downloading = false;
        List<MediaItemViewModel> mediaItems = itemViewModel.getMediaItems(false);
        if (mediaItems != null) {
            for (MediaItemViewModel media : mediaItems) {
                if (media.isDownloading()) {
                    downloading = true;
                    break;
                }
            }
        }
        return downloading;
    }

    public static void downloaded(ItemViewModel itemViewModel, MediaItemViewModel mediaItemViewModel) {
        boolean stillDownloading = isItemDownloading(itemViewModel);

        // Done?
        if (!stillDownloading) {
            onItemMediaDownloaded(itemViewModel);
        }
    }

    private static void onItemMediaDownloaded(ItemViewModel itemViewModel) {
        if (gInProgress.get(itemViewModel.item.getDatabaseId()) != null) {
            gInProgress.remove(itemViewModel.item.getDatabaseId());
        }
        if (gComplete.get(itemViewModel.item.getDatabaseId()) == null) {
            gComplete.put(itemViewModel.item.getDatabaseId(), itemViewModel);
        }
        notifyDataChanged();
    }

    public static void cancel(ItemViewModel itemViewModel) {
        // TODO can't cancel at this time
/*
        if (gInProgress.get(itemViewModel.item.getDatabaseId()) != null) {
			if (LOGGING)
				Log.v(LOGTAG, "Cancel media load for item " + itemViewModel.item.getTitle());

			MediaViewCollection mvc = gInProgress.get(itemLong);
			mvc.recycle();
			gInProgress.remove(itemLong);
			if (gInstance != null)
				gInstance.notifyDataSetChanged();

		}
*/
    }

    public static void retry(ItemViewModel itemViewModel) {
        boolean alreadyDownloaded = !isItemDownloading(itemViewModel);
        if (alreadyDownloaded) {
            onItemMediaDownloaded(itemViewModel);
        } else {
            if (itemViewModel.getMediaItems(false) != null) {
                for (MediaItemViewModel media : itemViewModel.getMediaItems(false)) {
                    if (media.isDownloading()) {
                        media.download();
                    }
                }
            }
        }
    }

    public static void viewed(ItemViewModel itemViewModel) {
        if (gComplete.get(itemViewModel.item.getDatabaseId()) != null) {
            gComplete.remove(itemViewModel.item.getDatabaseId());
            notifyDataChanged();
        }
    }

    @Override
    public void onItemSelected(ItemViewModel itemViewModel) {
        if (listener != null) {
            listener.onItemSelected(itemViewModel);
        }
    }

    @Override
    public void onTagSelected(String tag) {

    }

    @Override
    public void onSourceSelected(long feedId) {

    }

    @Override
    public void onMediaSelected(ItemViewModel item, MediaItemViewModel mediaItem) {

    }

    @Override
    public void onMediaDownloaded(ItemViewModel item, MediaItemViewModel mediaItem) {

    }

    @Override
    public void onCursorUpdated() {

    }

    private class ItemCancelListener implements View.OnClickListener {
        private ItemViewModel itemViewModel;

        ItemCancelListener(ItemViewModel itemViewModel) {
            this.itemViewModel = itemViewModel;
        }

        @Override
        public void onClick(View v) {
            DownloadsAdapter.cancel(itemViewModel);
        }
    }

    private class ItemRetryListener implements View.OnClickListener {
        private ItemViewModel itemViewModel;
        private View operationView;

        ItemRetryListener(ItemViewModel itemViewModel, View operationView) {
            this.itemViewModel = itemViewModel;
            this.operationView = operationView;
        }

        @Override
        public void onClick(View v) {
            AnimationHelpers.fadeOut(operationView, 500, 0, false);
            DownloadsAdapter.retry(itemViewModel);
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        HeaderViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.tvTitle);
        }
    }

    private class HeaderCompleteViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        HeaderCompleteViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.tvTitle);
        }
    }

    private class ItemInProgressViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        ItemInProgressViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.tvTitle);
        }
    }

    private static void notifyDataChanged() {
        for (DownloadsAdapter adapter : gInstances) {
            adapter.notifyDataSetChanged();
        }
    }
}
