package info.guardianproject.securereaderinterface.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVGParseException;
import com.tinymission.rss.Feed;

import java.util.ArrayList;
import java.util.List;

import info.guardianproject.securereaderinterface.R;

public class FeedRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    public interface FeedRecyclerViewAdapterListener
    {
        void onFeedFollow(Feed feed);
        void onFeedUnfollow(Feed feed);
    }

    private class CategoryModel
    {
        public final String name;
        CategoryModel(String name)
        {
            this.name = name;
        }
    }
    private final FeedRecyclerViewAdapter.FeedRecyclerViewAdapterListener listener;
    private final Context context;
    private final SortedList<Feed> originalFeeds;
    private List<Object> filteredFeeds;
    private ArrayList<Feed> selectedFeeds;
    private String filterString;

    public FeedRecyclerViewAdapter(Context context, FeedRecyclerViewAdapterListener listener) {
        super();
        setHasStableIds(true);
        this.context = context;
        this.listener = listener;
        selectedFeeds = new ArrayList<>();
        originalFeeds = new SortedList<>(Feed.class, new SortedList.Callback<Feed>() {
            @Override
            public int compare(Feed o1, Feed o2) {

                    String val1 = o1.getCategory();
                    String val2 = o2.getCategory();
                if (val1 == null && val2 == null)
                    return 0;
                else if (val1 == null)
                    return 1;
                else if (val2 == null)
                    return -1;
                return val1.compareToIgnoreCase(val2);
            }

            @Override
            public void onInserted(int position, int count) {

            }

            @Override
            public void onRemoved(int position, int count) {

            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {

            }

            @Override
            public void onChanged(int position, int count) {

            }

            @Override
            public boolean areContentsTheSame(Feed item1, Feed item2) {
                return item1.getFeedURL().equalsIgnoreCase(item2.getFeedURL());
            }

            @Override
            public boolean areItemsTheSame(Feed item1, Feed item2) {
                return item1.getFeedURL().equalsIgnoreCase(item2.getFeedURL());
            }
        });
        filteredFeeds = new ArrayList<>();
        setFilterString(null);
    }

    private Context getContext() {
        return context;
    }

    public ArrayList<Feed> getSelectedFeeds() {
        return selectedFeeds;
    }

    public void setFeeds(List<Feed> feeds) {
        if (feeds == null)
            return;
        synchronized (context) {
            originalFeeds.beginBatchedUpdates();
            originalFeeds.clear();
            originalFeeds.addAll(feeds);
            originalFeeds.endBatchedUpdates();
        }
        update(); // Re-filter it all
    }

    private void update() {
        setFilterString(filterString);
    }

    private void setFilterString(String filterString) {
        this.filterString = filterString;
        if (TextUtils.isEmpty(this.filterString)) {
            ArrayList<Feed> values = new ArrayList<>();
            for (int i = 0; i < originalFeeds.size(); i++) {
                values.add(originalFeeds.get(i));
            }
            updateAndNotify(values);
        } else {
            getFilter().filter(this.filterString);
        }
    }

    private void updateAndNotify(List<Feed> newItems) {
        filteredFeeds = new ArrayList<>();
        String lastCat = null;
        for (Feed item : newItems) {
            String cat = item.getCategory();
            if (TextUtils.isEmpty(cat)) {
                cat = getContext().getString(R.string.feed_category_uncategorized);
            }
            if (!cat.equalsIgnoreCase(lastCat)) {
                lastCat = cat;
                filteredFeeds.add(new CategoryModel(cat));
            }
            filteredFeeds.add(item);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return filteredFeeds.size();
    }

    @Override
    public long getItemId(int position) {
        return filteredFeeds.get(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        if (filteredFeeds.get(position) instanceof CategoryModel) {
            return 1;
        }
        return 0;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                updateAndNotify((List<Feed>) results.values);
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Feed> filteredResults;
                if (TextUtils.isEmpty(constraint)) {
                    filteredResults = new ArrayList<>();
                    for (int i = 0; i < originalFeeds.size(); i++) {
                        filteredResults.add(originalFeeds.get(i));
                    }
                } else {
                    filteredResults = getFilteredResults(constraint.toString().toLowerCase());
                }

                FilterResults results = new FilterResults();
                results.values = filteredResults;

                return results;
            }
        };
    }

    private List<Feed> getFilteredResults(String constraint) {
        List<Feed> results = new ArrayList<>();
        for (int i = 0; i < originalFeeds.size(); i++) {
            Feed item = originalFeeds.get(i);
            if ((item.getTitle() != null && item.getTitle().toLowerCase().contains(constraint)) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(constraint))) {
                results.add(item);
            }
        }
        return results;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 1) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.onboarding_curate_feeds_category, parent, false);
            return new CategoryViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.onboarding_curate_feeds_item, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof CategoryViewHolder) {
            CategoryModel model = (CategoryModel) filteredFeeds.get(position);
            CategoryViewHolder viewHolder = (CategoryViewHolder) holder;
            viewHolder.bindModel(model);
            return;
        }
        FeedViewHolder viewHolder = (FeedViewHolder) holder;
        Feed feed = (Feed) filteredFeeds.get(position);
        viewHolder.bindModel(feed);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof CategoryViewHolder) {
            CategoryViewHolder viewHolder = (CategoryViewHolder) holder;
            viewHolder.unbindModel();
        }
    }

    private class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final ViewGroup imageContainer;

        CategoryViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.tvName);
            imageContainer = (ViewGroup) view.findViewById(R.id.ivIllustration);
        }

        void bindModel(CategoryModel category) {
            name.setText(category.name);
            if (context.getString(R.string.feed_category_world_news).equalsIgnoreCase(category.name))
            {
                populateContainerWithSVG(R.raw.img_cat_worldnews);
            }
            else if (context.getString(R.string.feed_category_national_news).equalsIgnoreCase(category.name))
            {
                populateContainerWithSVG(R.raw.img_cat_nationalnews);
            }
            else if (context.getString(R.string.feed_category_arts_culture).equalsIgnoreCase(category.name))
            {
                populateContainerWithSVG(R.raw.img_cat_artsculture);
            }
            else if (context.getString(R.string.feed_category_business).equalsIgnoreCase(category.name))
            {
                populateContainerWithSVG(R.raw.img_cat_business);
            }
            else if (context.getString(R.string.feed_category_sports).equalsIgnoreCase(category.name))
            {
                populateContainerWithSVG(R.raw.img_cat_sports);
            }
            else if (context.getString(R.string.feed_category_technology).equalsIgnoreCase(category.name))
            {
                populateContainerWithSVG(R.raw.img_cat_technology);
            }
            else if (context.getString(R.string.feed_category_security).equalsIgnoreCase(category.name))
            {
                populateContainerWithSVG(R.raw.img_cat_security);
            }
            else if (context.getString(R.string.feed_category_discussion).equalsIgnoreCase(category.name))
            {
                populateContainerWithSVG(R.raw.img_cat_discussion);
            }
            else
            {
                ViewGroup viewGroup = (ViewGroup) itemView.findViewById(R.id.ivIllustration);
                viewGroup.removeAllViews();
            }
        }

        private void populateContainerWithSVG(int idSVG) {
            try {
                SVG svg = SVG.getFromResource(context, idSVG);

                SVGImageView svgImageView = new SVGImageView(context)
                {
                    boolean hasSetColor = false;

                    @Override
                    protected void onDraw(Canvas canvas) {
                        super.onDraw(canvas);
                        if (!hasSetColor && getWidth() > 0 && getHeight() > 0) {
                            hasSetColor = true;
                            Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvasTemp = new Canvas(bitmap);
                            draw(canvasTemp);
                            int color = bitmap.getPixel(0, 0);
                            bitmap.recycle();
                            setBackgroundColor(color);
                        }
                    }
                };
                svgImageView.setSVG(svg);
                svgImageView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                svgImageView.setScaleType(ImageView.ScaleType.FIT_START);
                imageContainer.addView(svgImageView);
            } catch (SVGParseException e) {
                e.printStackTrace();
            }
        }

        void unbindModel() {
            imageContainer.removeAllViews();
        }
    }

    private class FeedViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView description;
        private final View btnOff;
        private final View btnOn;

        FeedViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.tvFeedName);
            description = (TextView) view.findViewById(R.id.tvFeedDescription);
            btnOff = view.findViewById(R.id.btnOff);
            btnOn = view.findViewById(R.id.btnOn);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + name.getText() + "'";
        }

        void bindModel(final Feed feed) {
            name.setText(feed.getTitle());
            name.setTextColor(ContextCompat.getColor(context, R.color.feed_list_title_normal));
            if (TextUtils.isEmpty(feed.getTitle()) || name.getText().length() == 0)
                name.setText(feed.getFeedURL());
            description.setText(feed.getDescription());
            if (description.getText().length() == 0) {
                description.setText(feed.getFeedURL());
            }

            btnOn.setVisibility(selectedFeeds.contains(feed) ? View.VISIBLE : View.GONE);
            btnOff.setVisibility(selectedFeeds.contains(feed) ? View.GONE : View.VISIBLE);
            btnOff.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (!selectedFeeds.contains(feed)) {
                        selectedFeeds.add(feed);
                        if (listener != null) {
                            listener.onFeedFollow(feed);
                        }
                    }
                    btnOn.setVisibility(View.VISIBLE);
                    btnOff.setVisibility(View.INVISIBLE);
                }
            });
            btnOn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (selectedFeeds.contains(feed)) {
                        selectedFeeds.remove(feed);
                        if (listener != null) {
                            listener.onFeedUnfollow(feed);
                        }
                    }
                    btnOn.setVisibility(View.INVISIBLE);
                    btnOff.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}
