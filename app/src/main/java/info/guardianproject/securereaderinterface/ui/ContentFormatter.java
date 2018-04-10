package info.guardianproject.securereaderinterface.ui;

import android.content.Context;

import com.tinymission.rss.Item;

/**
 * Created by N-Pex on 2016-12-14.
 */

public interface ContentFormatter {
    CharSequence getFormattedItemContent(Context context, Item item);
}
