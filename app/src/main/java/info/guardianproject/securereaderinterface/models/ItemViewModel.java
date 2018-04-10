package info.guardianproject.securereaderinterface.models;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;

import net.etuldan.sparss.utils.ArticleTextExtractor;
import net.etuldan.sparss.utils.HtmlUtils;
import net.etuldan.sparss.view.EntryView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.ui.ContentFormatter;

/**
 * Created by N-Pex on 2017-01-31.
 */

public class ItemViewModel implements Serializable {
    public final Item item;
    private transient CharSequence formattedContent;
    private transient String formattedFullText;
    private ArrayList<MediaItemViewModel> mediaItems;
    public MediaItemViewModel preferredMediaItem;
    public MediaContent fullTextMediaContent;

    public ItemViewModel(Item item) {
        this.item = item;

        // Full text?
        if (item.getNumberOfMediaContent() > 0) {
            for (MediaContent media : item.getMediaContent()) {
                if (media.getMediaContentType() == MediaContent.MediaContentType.FULLTEXT) {
                    fullTextMediaContent = media;
                    break;
                }
            }
        }
    }

    public List<MediaItemViewModel> getMediaItems(boolean onlyDownloaded) {
        if (mediaItems == null && item.getNumberOfMediaContent() > 0) {
            mediaItems = new ArrayList<>(item.getNumberOfMediaContent());
            for (MediaContent media : item.getMediaContent()) {
                // Don't include full text media
                if (media.getMediaContentType() == MediaContent.MediaContentType.FULLTEXT) {
                    continue;
                }
                mediaItems.add(new MediaItemViewModel(this, media));
            }
            Collections.sort(mediaItems, new Comparator<MediaItemViewModel>() {
                @Override
                public int compare(MediaItemViewModel item1, MediaItemViewModel item2) {
                    if (item1.media.getWidth() == item2.media.getWidth())
                        return 0;
                    else if (item1.media.getWidth() < item2.media.getWidth())
                        return 1;
                    return -1;
                }
            });
        }
        if (mediaItems != null) {
            if (onlyDownloaded) {
                ArrayList<MediaItemViewModel> ret = new ArrayList<>(mediaItems.size());
                for (MediaItemViewModel media : mediaItems) {
                    if (media.isDownloaded()) {
                        ret.add(media);
                    }
                }
                return ret;
            }
            return new ArrayList<>(mediaItems);
        }
        return null;
    }

    public boolean populateWithFormattedContent(TextView textView) {
        if (formattedContent != null) {
            textView.setText(formattedContent);
            return true;
        }
        FormatContentWorkerTask formatContentTask = new FormatContentWorkerTask(textView);
        formatContentTask.execute();
        return false;
    }

    public boolean populateWithFullText(TextView summaryView, EntryView entryView) {
        if (formattedFullText != null) {
            summaryView.setVisibility(View.GONE);
            entryView.setHtml(0, item.getTitle(), "null", formattedFullText, null, item.getAuthor(), 0, true);
            entryView.setVisibility(View.VISIBLE);
            return true;
        }

        // While we work use the summary
        if (formattedContent != null) {
            summaryView.setText(formattedContent);
        }

        FormatFullTextWorkerTask formatFullTextTask = new FormatFullTextWorkerTask(summaryView, entryView);
        formatFullTextTask.execute();
        return false;
    }

    private class FormatContentWorkerTask extends AsyncTask<Void, Void, CharSequence> {
        private final WeakReference<TextView> textViewReference;
        private final Context context;

        FormatContentWorkerTask(TextView textView) {
            // Use a WeakReference to ensure the TextView can be garbage collected
            textViewReference = new WeakReference<>(textView);
            context = textView.getContext();
        }

        // Format content in background.
        @Override
        protected CharSequence doInBackground(Void... params) {
            final ContentFormatter formatter = App.getInstance().getItemContentFormatter();
            if (formatter == null) {
                // No Formatter, just use the clean content
                return item.getCleanMainContent();
            } else {
                return formatter.getFormattedItemContent(context, item);
            }
        }

        // Once complete, see if TextView is still around and set content.
        @Override
        protected void onPostExecute(CharSequence content) {
            formattedContent = content;
            if (textViewReference != null && content != null) {
                final TextView textView = textViewReference.get();
                if (textView != null) {
                    textView.setText(content);
                }
            }
        }
    }

    private class FormatFullTextWorkerTask extends AsyncTask<String, Void, String> {
        private final WeakReference<View> viewReferenceSummary;
        private final WeakReference<EntryView> viewReference;
        private final Context context;

        FormatFullTextWorkerTask(View summaryView, EntryView view) {
            // Use a WeakReference to ensure the TextView can be garbage collected
            viewReferenceSummary = new WeakReference<View>(summaryView);
            viewReference = new WeakReference<>(view);
            context = view.getContext();
        }

        // Format content in background.
        @Override
        protected String doInBackground(String... params) {
            String ret = null;
            try {
                InputStream is = new FileInputStream(App.getInstance().socialReader.getFullTextForItem(item));
                StringBuilder x = new StringBuilder();

                int numRead = 0;
                byte[] bytes = new byte[1000];
                while ((numRead = is.read(bytes)) >= 0) {
                    x.append(new String(bytes, 0, numRead));
                }
                ret = x.toString();
                is.close();
            } catch (Exception ignored) {}
            return ret;
        }

        // Once complete, see if TextView is still around and set content.
        @Override
        protected void onPostExecute(String fullText) {
            formattedFullText = fullText;
            if (viewReferenceSummary != null && fullText != null) {
                final View summaryView = viewReferenceSummary.get();
                if (summaryView != null) {
                    summaryView.setVisibility(View.GONE);
                }
            }
            if (viewReference != null && fullText != null) {
                final EntryView entryView = viewReference.get();
                if (entryView != null) {
                    entryView.setHtml(0, item.getTitle(), "null", formattedFullText, null, item.getAuthor(), 0, true);
                    entryView.setVisibility(View.VISIBLE);
                }
            }
        }
    };
}
