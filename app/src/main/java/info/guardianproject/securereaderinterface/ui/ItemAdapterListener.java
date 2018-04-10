package info.guardianproject.securereaderinterface.ui;

import info.guardianproject.securereaderinterface.models.ItemViewModel;
import info.guardianproject.securereaderinterface.models.MediaItemViewModel;

/**
 * Created by N-Pex on 2017-01-31.
 */

public interface ItemAdapterListener {
    /**
     * Called when an item has been clicked on.
     * @param item The clicked item.
     */
    void onItemSelected(ItemViewModel item);

    /**
     * Called when the user has clicked on a tag in one of the items.
     * @param tag The tag that was clicked.
     */
    void onTagSelected(String tag);

    /**
     * Called when user has selected a feed by clicking on the "source" text.
     * @param feedId The selected feed.
     */
    void onSourceSelected(long feedId);

    /**
     * Called when user tapped on a media item preview view
     * @param item Item the media belongs to
     * @param mediaItem The media that generated the view
     */
    void onMediaSelected(ItemViewModel item, MediaItemViewModel mediaItem);

    /**
     * Called when a media item has been downloaded from the net
     * @param item Item the media belongs to
     * @param mediaItem The media that was downloaded
     */
    void onMediaDownloaded(ItemViewModel item, MediaItemViewModel mediaItem);

    /**
     * Called when the current database cursor has changed.
     */
    void onCursorUpdated();
}