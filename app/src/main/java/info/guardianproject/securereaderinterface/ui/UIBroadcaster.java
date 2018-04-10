package info.guardianproject.securereaderinterface.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.tinymission.rss.Item;

import java.util.ArrayList;

import info.guardianproject.securereaderinterface.installer.SecureBluetooth;
import info.guardianproject.securereaderinterface.models.FeedSelection;

public class UIBroadcaster {
    public static final String LOGTAG = "UIBroadcaster";
    public static final boolean LOGGING = false;

    public static final String BROADCAST_ACTION_FEED_SELECTED = "UIBroadcaster.feed_selected";
    public static final String BROADCAST_ACTION_FEED_RESYNC_REQUESTED = "UIBroadcaster.feed_resync_requested";
    public static final String BROADCAST_ACTION_ITEM_FAVORITE_CHANGED = "UIBroadcaster.item_favorite_changed";
    public static final String BROADCAST_ACTION_COMMAND = "UIBroadcaster.command";
    public static final String EXTRAS_FEED_SELECTION = "UIBroadcaster.feed_selection";
    public static final String EXTRAS_ITEM = "UIBroadcaster.item";
    public static final String EXTRAS_COMMAND = "UIBroadcaster.command_id";
    public static final String EXTRAS_COMMAND_OPTIONS = "UIBroadcaster.command_options";

    public enum RequestCode {
        BT_ENABLE(SecureBluetooth.REQUEST_ENABLE_BT),
        BT_DISCOVERABLE(SecureBluetooth.REQUEST_ENABLE_BT_DISCOVERY),
        CREATE_CHAT_ACCOUNT(20);

        /**
         * Value for this RequestCode
         */
        public final int Value;

        RequestCode(int value) {
            Value = value;
        }

        static void checkUniqueness() {
            ArrayList<Integer> intvals = new ArrayList<>();
            for (RequestCode code : RequestCode.values()) {
                if (intvals.contains(code.Value))
                    throw new RuntimeException("RequestCode array is invalid (not usnique numbers), check the values!");
                intvals.add(code.Value);
            }
        }
    }

    public static void setFeedFilter(Context context, FeedSelection feedSelection) {
        Intent intent = new Intent(BROADCAST_ACTION_FEED_SELECTED);
        intent.putExtra(EXTRAS_FEED_SELECTION, feedSelection);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void requestResync(Context context, FeedSelection feedSelection) {
        Intent intent = new Intent(BROADCAST_ACTION_FEED_RESYNC_REQUESTED);
        intent.putExtra(EXTRAS_FEED_SELECTION, feedSelection);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void itemFavoriteStatusChanged(Context context, Item item) {
        Intent intent = new Intent(BROADCAST_ACTION_ITEM_FAVORITE_CHANGED);
        intent.putExtra(EXTRAS_ITEM, item);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void onCommand(Context context, int command, Bundle commandParameters) {
        Intent intent = new Intent(BROADCAST_ACTION_COMMAND);
        intent.putExtra(EXTRAS_COMMAND, command);
        intent.putExtra(EXTRAS_COMMAND_OPTIONS, commandParameters);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
