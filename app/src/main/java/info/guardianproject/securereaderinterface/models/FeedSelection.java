package info.guardianproject.securereaderinterface.models;

import java.io.Serializable;

public class FeedSelection implements Serializable {
	public static final FeedSelection EMPTY = new FeedSelection(-99);
	public static final FeedSelection ALL_FEEDS = new FeedSelection(-100);
	public static final FeedSelection POPULAR = new FeedSelection(-101);
	public static final FeedSelection FAVORITES = new FeedSelection(-102);
	public static final FeedSelection SHARED = new FeedSelection(-103);

	public long Value;

	public FeedSelection(long value) {
		Value = value;
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof FeedSelection && ((FeedSelection)obj).Value == this.Value;
	}

	public boolean equals(long id) {
		return Value == id;
	}

	public boolean isNormalFeed() {
		return Value >= 0;
	}

	public boolean isSpecialFeed() {
		return Value < 0;
	}
}