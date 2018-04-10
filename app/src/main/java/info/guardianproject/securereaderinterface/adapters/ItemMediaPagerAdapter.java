package info.guardianproject.securereaderinterface.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import info.guardianproject.securereader.ModeSettings;
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.models.ItemViewModel;
import info.guardianproject.securereaderinterface.models.MediaItemViewModel;
import info.guardianproject.securereaderinterface.views.media.MediaPreviewLayout;

/**
 * Created by N-Pex on 2017-01-30.
 */
public class ItemMediaPagerAdapter extends PagerAdapter {
    private final ItemViewModel mItem;
    private final Context mContext;
    private final ItemMediaPagerAdapterListener mListener;
    private final boolean mShowDownloadViews;
    private ArrayList<MediaItemViewModel> mMediaList;
    private MediaPreviewLayout mCurrentView;

    public interface ItemMediaPagerAdapterListener {
        void onMediaDownloaded(ItemViewModel item, MediaItemViewModel mediaItem);
        void onMediaSelected(ItemViewModel item, MediaItemViewModel mediaItem);
    }

    public ItemMediaPagerAdapter(Context context, ItemViewModel item, boolean showDownloadViews, ItemMediaPagerAdapterListener listener) {
        mContext = context;
        mItem = item;
        mListener = listener;
        mShowDownloadViews = showDownloadViews;
        mMediaList = new ArrayList<>();
        updateList();
    }

    private Context getContext() {
        return mContext;
    }

    private void updateList() {
        synchronized (mItem) {
            mMediaList.clear();
            for (MediaItemViewModel media : mItem.getMediaItems(!mShowDownloadViews)) {
                if (media.isDownloaded()) {
                    // Media is already loaded, show it!
                    mMediaList.add(media);
                } else if (App.getInstance().socialReader.syncSettingsForCurrentNetwork().contains(ModeSettings.Sync.Media)) {
                    // Let it flow mode, the media will be downloaded soon
                    mMediaList.add(media);
                } else if (mShowDownloadViews) {
                    // Show a placeholder so the user can download the media
                    mMediaList.add(media);
                }
            }
        }
    }

    /**
     * Get the MediaContent that generated the view that is currently displayed in the viewpager.
     * @return MediaContent for current view or null.
     */
    public MediaItemViewModel getCurrentMedia() {
        if (mCurrentView != null) {
            return mCurrentView.getMediaItem();
        }
        return null;
    }

    public void scrollToPreferredMediaItem(final ViewPager viewPager, MediaItemViewModel preferredMediaItem) {
        if (preferredMediaItem != null) {
            final int index = mMediaList.indexOf(preferredMediaItem);
            if (index != -1) {
                viewPager.setCurrentItem(index);
            }
        }
    }

    @Override
    public int getCount() {
        synchronized (mItem) {
            return mMediaList.size();
        }
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        mCurrentView = (MediaPreviewLayout) object;
        mItem.preferredMediaItem = mCurrentView.getMediaItem();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((MediaPreviewLayout)object).isViewFromObject(view);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        synchronized (mItem) {
            final MediaItemViewModel media = mMediaList.get(position);
            MediaPreviewLayout view = new MediaPreviewLayout(container.getContext(), media);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null && media.isDownloaded()) {
                        mListener.onMediaSelected(mItem, media);
                    }
                }
            });
            view.setListener(new MediaPreviewLayout.MediaPreviewLayoutListener() {
                @Override
                public void onMediaDownloaded(MediaItemViewModel mediaItem) {
                    if (mListener != null && media.isDownloaded()) {
                        mListener.onMediaDownloaded(mItem, media);
                    }
                }
            });
            view.addToContainer(container);
            return view;
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        MediaPreviewLayout view = (MediaPreviewLayout) object;
        view.removeFromContainer(container);
        view.recycle();
    }
}
