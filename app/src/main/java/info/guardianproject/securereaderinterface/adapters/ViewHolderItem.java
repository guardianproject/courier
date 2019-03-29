package info.guardianproject.securereaderinterface.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;

import net.etuldan.sparss.utils.ArticleTextExtractor;
import net.etuldan.sparss.utils.HtmlUtils;
import net.etuldan.sparss.view.EntryView;

import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import info.guardianproject.iocipher.File;
import info.guardianproject.securereader.ModeSettings;
import info.guardianproject.securereader.HTMLToPlainTextFormatter;
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SimpleSyncTaskMediaFetcherCallback;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SyncStatus;
import info.guardianproject.securereader.SyncTaskMediaFetcher;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.BuildConfig;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.SettingsUI;
import info.guardianproject.securereaderinterface.models.ItemViewModel;
import info.guardianproject.securereaderinterface.models.MediaItemViewModel;
import info.guardianproject.securereaderinterface.ui.ItemAdapterListener;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.DottedProgressView;
import info.guardianproject.securereaderinterface.widgets.NestedViewPager;

/**
 * Created by N-Pex on 2017-01-30.
 *
 * This class acts as a view holder for item views, both in the list but also in "detail view".
 * It also handles updating the "time" views once a minute.
 */

public class ViewHolderItem extends RecyclerView.ViewHolder implements View.OnClickListener {
    public final View mediaLayout;
    private final NestedViewPager mediaPager;
    private final TextView title;
    private final TextView content;
    private final EntryView webContent;
    private final TextView publicationTime;
    private final TextView source;
    private final ImageView feedIcon;
    private final TextView author;
    private final TextView authorDate;
    private final TextView authorTime;
    private final View tagsLayoutContainer;
    private final LinearLayout tagsLayout;
    private final View readMore;
    private final Button btnFetchFullText;

    private float titleFontSize;
    private float contentFontSize;
    private float authorFontSize;

    private boolean showFullText;

    /**
     * The currently bound item, or null
     */
    protected ItemViewModel itemViewModel;

    public ViewHolderItem(View view) {
        super(view);
        mediaLayout = view.findViewById(R.id.layout_media);
        mediaPager = (NestedViewPager) view.findViewById(R.id.mediaPager);
        DottedProgressView mediaPagerIndicator = (DottedProgressView) view.findViewById(R.id.mediaPagerIndicator);
        title = (TextView) view.findViewById(R.id.tvTitle);
        content = (TextView) view.findViewById(R.id.tvContent);
        webContent = (EntryView) view.findViewById(R.id.tvWebContent);
        publicationTime = (TextView) view.findViewById(R.id.tvTime);
        source = (TextView) view.findViewById(R.id.tvSource);
        feedIcon = (ImageView) view.findViewById(R.id.ivFeedIcon);
        author = (TextView) view.findViewById(R.id.tvAuthor);
        authorDate = (TextView) view.findViewById(R.id.tvAuthorDate);
        authorTime = (TextView) view.findViewById(R.id.tvAuthorTime);
        tagsLayoutContainer = view.findViewById(R.id.layout_tags);
        tagsLayout = (LinearLayout) view.findViewById(R.id.llTags);
        readMore = view.findViewById(R.id.tvReadMore);
        btnFetchFullText = view.findViewById(R.id.btnFetchFullText);

        // Additional one-time configuration
        if (mediaPager != null)
        {
            if (mediaPagerIndicator != null) {
                mediaPager.setViewPagerIndicator(mediaPagerIndicator);
            }
        }
        if (title != null) {
            titleFontSize = title.getTextSize();
        }
        if (content != null) {
            contentFontSize = content.getTextSize();
            content.setMovementMethod(LinkMovementMethod.getInstance());
        }
        if (author != null) {
            authorFontSize = author.getTextSize();
        }
        if (btnFetchFullText != null) {
            Drawable[] d = btnFetchFullText.getCompoundDrawablesRelative();
            if (d[0] != null) {
                d[0].mutate();
                UIHelpers.colorizeDrawable(btnFetchFullText.getContext(), R.attr.colorAccent, d[0]);
            }
        }
    }

    private Context getContext() {
        return itemView.getContext();
    }

    @Override
    public String toString() {
        return super.toString() + " '" + title.getText() + "'";
    }

    public void bindModel(final ItemViewModel itemViewModel, boolean showOnlyDownloadedMedia, boolean showTags, final ItemAdapterListener listener) {
        this.itemViewModel = itemViewModel;
        Item item = itemViewModel.item;

        if (title != null) {
            title.setText(item.getTitle());
            title.setTextColor(ContextCompat.getColor(title.getContext(), item.getViewCount() > 0 ? R.color.grey : R.color.grey_dark_dark));
        }

        // Avoid getting full text if we'll never show it!
        if (content != null || webContent != null) {
            showFullText = SocialReader.getInstance(getContext()).hasFullTextForItem(item);
            populateContent(item, false);
        }
        if (btnFetchFullText != null) {
            btnFetchFullText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFullText = true;
                    populateContent(itemViewModel.item, true);
                }
            });
        }
        if (source != null) {
            source.setText(item.getSource());
            source.setOnClickListener(new View.OnClickListener()
            {
                private long feedId;

                View.OnClickListener init(long feedId) {
                    this.feedId = feedId;
                    return this;
                }

                @Override
                public void onClick(View v)
                {
                    if (feedId != -1)
                    {
                        if (listener != null)
                            listener.onSourceSelected(feedId);
                    }
                }
            }.init(item.getFeedId()));
        }
        if (feedIcon != null) {
            if (item.getFeedId() >= 0) {
                File feedIconFile = new File(App.getInstance().socialReader.getFileSystemDir(), SocialReader.FEED_ICON_FILE_PREFIX + item.getFeedId());
                ViewGroup.LayoutParams size = feedIcon.getLayoutParams();
                Picasso.get().load(Uri.parse(feedIconFile.getAbsolutePath()))
                        .resize(0, size.height)
                        .placeholder(R.drawable.ic_filter_logo_placeholder)
                        .into(feedIcon);
            } else {
                feedIcon.setImageDrawable(null);
            }
        }
        if (author != null) {
            String authorName = item.getCleanAuthor();
            if (!TextUtils.isEmpty(authorName)) {
                author.setText(getContext().getString(R.string.story_item_short_author, authorName));
            }
            author.setVisibility(TextUtils.isEmpty(authorName) ? View.GONE : View.VISIBLE);
        }
        if (authorDate != null) {
            authorDate.setText(UIHelpers.dateDateDisplayString(item.getPublicationTime(), getContext()));
        }
        if (authorTime != null) {
            authorTime.setText(UIHelpers.dateTimeDisplayString(item.getPublicationTime(), getContext()));
        }

        boolean willShowMedia = willShowMedia(itemViewModel, showOnlyDownloadedMedia);
        if (mediaPager != null) {
            MediaItemViewModel preferredMediaItem = itemViewModel.preferredMediaItem;
            ItemMediaPagerAdapter adapter = (willShowMedia ? new ItemMediaPagerAdapter(getContext(), itemViewModel, !showOnlyDownloadedMedia, new ItemMediaPagerAdapter.ItemMediaPagerAdapterListener() {
                @Override
                public void onMediaSelected(ItemViewModel item, MediaItemViewModel mediaItem) {
                    if (listener != null) {
                        listener.onMediaSelected(item, mediaItem);
                    }
                }

                @Override
                public void onMediaDownloaded(ItemViewModel item, MediaItemViewModel mediaItem) {
                    if (listener != null) {
                        listener.onMediaDownloaded(item, mediaItem);
                    }
                }
            }) : null);
            mediaPager.setAdapter(adapter);

            // If we have previously scrolled to another media item, do that here as well
            // so media is not jumping back and forth,
            if (preferredMediaItem != null && adapter != null) {
                adapter.scrollToPreferredMediaItem(mediaPager, preferredMediaItem);
            }
        }
        if (mediaLayout != null) {
            mediaLayout.setVisibility(willShowMedia ? View.VISIBLE : View.GONE);
        }
        if (tagsLayoutContainer != null) {
            if (showTags && item.getNumberOfTags() > 0 && tagsLayout != null) {
                tagsLayout.removeAllViews();
                for (final String tag : item.getTags()) {
                    View tagItem = LayoutInflater.from(getContext()).inflate(R.layout.item_tags_item, tagsLayout, false);
                    TextView tv = (TextView) tagItem.findViewById(R.id.tvTag);
                    tv.setText(tag);
                    tagItem.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (listener != null) {
                                listener.onTagSelected(tag);
                            }
                        }
                    });
                    tagsLayout.addView(tagItem);
                }
                tagsLayoutContainer.setVisibility(View.VISIBLE);
            } else {
                tagsLayoutContainer.setVisibility(View.GONE);
            }
        }
/*        if (readMore != null)
        {
            if (item.getLink() != null)
            {
                boolean isReadMoreEnabled = !TextUtils.isEmpty(item.getLink()) && App.getInstance().socialReader.isOnline() == SocialReader.ONLINE;
                readMore.setEnabled(isReadMoreEnabled);
                if (!isReadMoreEnabled)
                {
                    readMore.setOnClickListener(null);
                }
                else
                {
                    readMore.setOnClickListener(this);
                }
                readMore.setVisibility(View.VISIBLE);
            }
            else
            {
                readMore.setVisibility(View.GONE);
            }
        }*/
        itemView.setOnClickListener(new View.OnClickListener() {
            private ItemViewModel item;

            View.OnClickListener init(ItemViewModel item) {
                this.item = item;
                return this;
            }

            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onItemSelected(item);
                }
            }
        }.init(itemViewModel));
        if (publicationTime != null) {
            startTimeUpdates();
        }
    }

    private void populateContent(Item item, boolean forceDownload) {
        if (showFullText && App.getInstance().socialReader.hasFullTextForItem(item)) {
            if (webContent != null) {
                itemViewModel.populateWithFullText(content, webContent);
            }
        } else {
            if (webContent != null) {
                webContent.setVisibility(View.GONE);
            }
            if (content != null) {
                itemViewModel.populateWithFormattedContent(content);
                content.setVisibility(View.VISIBLE);
            }

            if (showFullText && (forceDownload || App.getInstance().socialReader.syncSettingsForCurrentNetwork().contains(ModeSettings.Sync.FullText)) &&
                    App.getInstance().socialReader.isOnline() == SocialReader.ONLINE && itemViewModel.fullTextMediaContent != null) {
                App.getInstance().socialReader.loadMediaContent(item, itemViewModel.fullTextMediaContent, new SimpleSyncTaskMediaFetcherCallback() {
                    @Override
                    public void mediaAddedToQueue(MediaContent mediaContent) {
                        super.mediaAddedToQueue(mediaContent);
                        btnFetchFullText.setText(R.string.fetch_full_story_queued);
                    }

                    @Override
                    public void mediaDownloadStarted(MediaContent mediaContent) {
                        super.mediaDownloadStarted(mediaContent);
                        btnFetchFullText.setText(R.string.fetch_full_story_downloading);
                    }

                    @Override
                    public void mediaDownloaded(MediaContent mediaContent, File file) {
                        ViewHolderItem vhi = ViewHolderItem.this;
                        if (webContent != null && vhi.itemViewModel != null &&
                                vhi.itemViewModel.fullTextMediaContent != null &&
                                vhi.itemViewModel.fullTextMediaContent.getDatabaseId() == mediaContent.getDatabaseId()) {
                            showFullText = true;
                            populateContent(itemViewModel.item, false);
                        }
                    }
                }, true, forceDownload);
            }
        }
        if (btnFetchFullText != null && itemViewModel.fullTextMediaContent != null) {
            if (App.getInstance().socialReader.hasFullTextForItem(item)) {
                btnFetchFullText.setVisibility(View.GONE);
            } else if (App.getInstance().socialReader.isMediaContentLoading(item, itemViewModel.fullTextMediaContent)) {
                btnFetchFullText.setText(R.string.fetch_full_story_queued);
                btnFetchFullText.setVisibility(View.VISIBLE);
            } else {
                btnFetchFullText.setText(R.string.fetch_full_story);
                btnFetchFullText.setVisibility(View.VISIBLE);
            }
        }
    }

    private void populateTime()
    {
        if (publicationTime != null && itemViewModel != null) {
            publicationTime.setText(UIHelpers.dateDiffDisplayString(itemViewModel.item.getPublicationTime(), getContext(), R.string.story_item_short_published_never,
                    R.string.story_item_short_published_recently, R.string.story_item_short_published_minutes, R.string.story_item_short_published_minute,
                    R.string.story_item_short_published_hours, R.string.story_item_short_published_hour, R.string.story_item_short_published_days,
                    R.string.story_item_short_published_day));
        }
    }

    private boolean willShowMedia(ItemViewModel itemViewModel, boolean showOnlyDownloadedMedia) {
        List<MediaItemViewModel> mediaItems = itemViewModel.getMediaItems(showOnlyDownloadedMedia);
        if (mediaItems != null) {
            if ((App.getInstance().socialReader.syncSettingsForCurrentNetwork().contains(ModeSettings.Sync.Media)) &&
                    App.getInstance().socialReader.isOnline() == SocialReader.ONLINE &&
                    mediaItems.size() > 0) {
                return true;
            }
            if (!showOnlyDownloadedMedia && mediaItems.size() > 0) {
                // Item contains media and we should show download views => we will show media!
                return true;
            }
            for (MediaItemViewModel media : mediaItems) {
                if (media.isDownloaded()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void unbindModel() {
        // Cleanup!
        if (mediaPager != null && mediaPager.getAdapter() != null) {
            mediaPager.setAdapter(null);
        }
        stopTimeUpdates();
        itemViewModel = null;
    }

    private final Runnable mUpdateTimestamp = new Runnable()
    {
        @Override
        public void run()
        {
            populateTime();
            if (itemView != null) {
                itemView.postDelayed(mUpdateTimestamp, 60000); // Every minute
            }
        }
    };

    private void startTimeUpdates()
    {
        if (itemView != null)
        {
            itemView.removeCallbacks(mUpdateTimestamp);
            itemView.postDelayed(mUpdateTimestamp, 0);
        }
    }

    private void stopTimeUpdates()
    {
        if (itemView != null)
        {
            itemView.removeCallbacks(mUpdateTimestamp);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == readMore) {
            if (!TextUtils.isEmpty(itemViewModel.item.getLink())) {
                Bundle params = new Bundle();
                params.putString("url", itemViewModel.item.getLink());
                App.getInstance().onCommand(getContext(), R.integer.command_read_more, params);
            }
        }
    }

    public boolean showFullText() {
        return showFullText;
    }

    public void setShowFullText(boolean show) {
        if (!show || SocialReader.getInstance(getContext()).hasFullTextForItem(itemViewModel.item)) {
            showFullText = show;
            populateContent(itemViewModel.item, false);
        }
    }
}

