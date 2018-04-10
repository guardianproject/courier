package info.guardianproject.securereaderinterface.adapters;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * Created by N-Pex on 2017-06-07.
 */

public class ItemCursor extends CursorWrapper {

    private final int rowIdColumn;
    private int currentIndex;

    public ItemCursor(Cursor cursor) {
        super(cursor);
        rowIdColumn = getColumnIndex("item_id");
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        currentIndex = index;
    }

    public long getCurrentItemId() {
        if (moveToPosition(currentIndex)) {
            return getLong(rowIdColumn);
        }
        return -1;
    }

    public void setCurrentItemId(long itemId) {
        currentIndex = 0;
        if (moveToFirst()) {
            do {
                long rowItemId = getLong(rowIdColumn);
                if (rowItemId == itemId) {
                    currentIndex = getPosition();
                    break;
                }
            }
            while (moveToNext());
        }
    }

    public long getItemId(int position) {
        if (moveToPosition(position)) {
            return getLong(rowIdColumn);
        }
        return -1;
    }
}
