package info.guardianproject.securereaderinterface.adapters;

import info.guardianproject.securereaderinterface.R;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVGParseException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class FeedListAdapterCurate extends BaseAdapter
{
	public interface FeedListAdapterCurateListener {
		void onSelectionChanged(int count);
	}

	public final static String LOGTAG = "FeedListAdapterCurate";
	public static final boolean LOGGING = false;

	private final Context mContext;
	private final LayoutInflater mInflater;
	private Document mDocument;
	private FeedListAdapterCurateListener mListener;

	private class CategoryModel
	{
		public String name;
		public CategoryModel(String name)
		{
			this.name = name;
		}
	}

	private class FeedModel
	{
		public String name;
		public String description;
		public Drawable icon;
		public Element element;
		public FeedModel(String name, String description, Drawable icon, Element element)
		{
			this.name = name;
			this.description = description;
			this.icon = icon;
			this.element = element;
		}
	}
	
	private final ArrayList<Object> mItems;
	private final HashMap<String, ArrayList<FeedModel>> mItemsMap;
	private final ArrayList<String> mItemsMapKeysSortedByUsage;
	private ArrayList<FeedModel> mSelectedItems;
	
	public FeedListAdapterCurate(Context context)
	{
		super();
		mContext = context;
		
		mItems = new ArrayList<>();
		mItemsMap = new HashMap<>();
		mSelectedItems = new ArrayList<>();
		mItemsMapKeysSortedByUsage = new ArrayList<>();
		
		try {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xPath = factory.newXPath();
			NodeList outlines = (NodeList) xPath.evaluate("//outline[not(parent::outline)]",
					new InputSource(mContext.getResources().openRawResource(R.raw.bigbuffalo_opml)),
					XPathConstants.NODESET);
			for (int i = 0; i < outlines.getLength(); i++) {
				Element outline = (Element) outlines.item(i);

				if (mDocument == null)
					mDocument = outline.getOwnerDocument();
				
				if (!TextUtils.isEmpty(outline.getAttribute("xmlUrl")))
				{
					parseOutlineNode(outline, null);
				}
				else
				{
					String category = outline.getAttribute("text");
					NodeList categoryOutlines = (NodeList) xPath.evaluate(".//outline[@xmlUrl]", outline, XPathConstants.NODESET);
					for (int j = 0; j < categoryOutlines.getLength(); j++) {
						parseOutlineNode((Element)categoryOutlines.item(j), category);
					}
				}
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		// Flatten out the map
		//
		for (String key : mItemsMapKeysSortedByUsage)
		{
			mItems.add(new CategoryModel(key));
			mItems.addAll(mItemsMap.get(key));
		}
	}

	public int getSelectionCount() {
		return mSelectedItems.size();
	}

	public void setListener(FeedListAdapterCurateListener listener) {
		mListener = listener;
	}

	private void parseOutlineNode(Element outline, String category)
	{
		if (TextUtils.isEmpty(category))
			category = mContext.getString(R.string.feed_category_uncategorized);
		
		if (!mItemsMap.containsKey(category))
		{
			mItemsMapKeysSortedByUsage.add(category);
			mItemsMap.put(category, new ArrayList<FeedModel>());
		}
		
		String feedName = outline.getAttribute("text");
		String feedDescription = outline.getAttribute("description");
		if (TextUtils.isEmpty(feedDescription))
			feedDescription = outline.getAttribute("xmlUrl");
		Drawable feedIcon = null;
		String feedIconString = outline.getAttribute("site_icon");
		if (!TextUtils.isEmpty(feedIconString)) {
			try {
				if (feedIconString.startsWith("data:") && feedIconString.indexOf(",") > 0) {
					String imageDataBytes = feedIconString.substring(feedIconString.indexOf(",") + 1);
					byte[] encodeByte = Base64.decode(imageDataBytes, Base64.DEFAULT);
					Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
					feedIcon = new BitmapDrawable(mContext.getResources(), bitmap);
				}
			} catch (Exception ignored) {}
		}

		FeedModel model = new FeedModel(feedName, feedDescription, feedIcon, outline);
		mItemsMap.get(category).add(model);
		
		String subscribe = outline.getAttribute("subscribe");
		if (TextUtils.isEmpty(subscribe) || "true".equalsIgnoreCase(subscribe))
			mSelectedItems.add(model);
	}
	
	@Override
	public int getCount()
	{
		return mItems.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}
	
	@Override
	public int getItemViewType(int position) {
		if (mItems.get(position) instanceof FeedModel)
			return 1;
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		int type = this.getItemViewType(position);
		
		View view;
		if (convertView == null)
		{
			if (type == 0)
				view = mInflater.inflate(R.layout.onboarding_curate_feeds_category, parent, false);
			else
				view = mInflater.inflate(R.layout.onboarding_curate_feeds_item, parent, false);
		}
		else
		{
			view = convertView;
		}

		
		if (type == 0)
		{
			// Category
			CategoryModel category = (CategoryModel)getItem(position);
			
			TextView tv = (TextView) view.findViewById(R.id.tvName);
			tv.setText(category.name);
			
		    if (mContext.getString(R.string.feed_category_world_news).equalsIgnoreCase(category.name))
		    {
				populateContainerWithSVG(view, R.raw.img_cat_worldnews, R.id.ivIllustration);
		    }
		    else if (mContext.getString(R.string.feed_category_national_news).equalsIgnoreCase(category.name))
		    {
				populateContainerWithSVG(view, R.raw.img_cat_nationalnews, R.id.ivIllustration);
		    }
		    else if (mContext.getString(R.string.feed_category_arts_culture).equalsIgnoreCase(category.name))
		    {
				populateContainerWithSVG(view, R.raw.img_cat_artsculture, R.id.ivIllustration);
		    }
		    else if (mContext.getString(R.string.feed_category_business).equalsIgnoreCase(category.name))
		    {
				populateContainerWithSVG(view, R.raw.img_cat_business, R.id.ivIllustration);
		    }
		    else if (mContext.getString(R.string.feed_category_sports).equalsIgnoreCase(category.name))
		    {
				populateContainerWithSVG(view, R.raw.img_cat_sports, R.id.ivIllustration);
		    }
		    else if (mContext.getString(R.string.feed_category_technology).equalsIgnoreCase(category.name))
		    {
				populateContainerWithSVG(view, R.raw.img_cat_technology, R.id.ivIllustration);
		    }
		    else if (mContext.getString(R.string.feed_category_security).equalsIgnoreCase(category.name))
		    {
				populateContainerWithSVG(view, R.raw.img_cat_security, R.id.ivIllustration);
		    }
			else if (mContext.getString(R.string.feed_category_discussion).equalsIgnoreCase(category.name))
			{
				populateContainerWithSVG(view, R.raw.img_cat_discussion, R.id.ivIllustration);
			}
		    else
		    {
		    	ViewGroup viewGroup = (ViewGroup) view.findViewById(R.id.ivIllustration);
		    	viewGroup.removeAllViews();
		        // Default
		        // [cell.categoryImage setArtworkPath:@"onboard-category"];
		    }
		}
		else {
			// ImageView iv = (ImageView) view.findViewById(R.id.ivFeedIcon);
			// if (feedModel.feed.getImageManager() != null)
			// feedModel.feed.getImageManager().download(feedModel.feed.,
			// imageView)
			// App.getInstance().socialReader.loadDisplayImageMediaContent(feedModel.feed.getImageManager(),
			// iv);

			final FeedModel feed = (FeedModel) getItem(position);

			// Name
			TextView tv = (TextView) view.findViewById(R.id.tvFeedName);
			tv.setText(feed.name);
			tv.setTextColor(ContextCompat.getColor(mContext, R.color.feed_list_title_normal));

			// Description
			tv = (TextView) view.findViewById(R.id.tvFeedDescription);
			tv.setText(feed.description);
//		if (TextUtils.isEmpty(feed.getTitle()) || tv.getText().length() == 0)
//			tv.setText(feed.getFeedURL());

			// Icon
			ImageView iv = (ImageView) view.findViewById(R.id.ivFeedIcon);
			if (feed.icon != null) {
				iv.setScaleType(ScaleType.FIT_CENTER);
				iv.setImageDrawable(feed.icon);
			}

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (mSelectedItems.contains(feed)) {
						mSelectedItems.remove(feed);
						feed.element.setAttribute("subscribe", "false");
					} else {
						mSelectedItems.add(feed);
						feed.element.setAttribute("subscribe", "true");
					}
					updateFeedUI(feed, view);
					if (mListener != null) {
						mListener.onSelectionChanged(getSelectionCount());
					}
				}
			});
			updateFeedUI(feed, view);
		}
		return view;
	}

	private void updateFeedUI(FeedModel feed, View view) {
		View btnOff = view.findViewById(R.id.btnOff);
		View btnOn = view.findViewById(R.id.btnOn);
		btnOn.setVisibility(mSelectedItems.contains(feed) ? View.VISIBLE : View.GONE);
		btnOff.setVisibility(mSelectedItems.contains(feed) ? View.GONE : View.VISIBLE);
	}

	private void populateContainerWithSVG(View parent, int idSVG, int idContainer) {
		try {
			SVG svg = SVG.getFromResource(mContext, idSVG);

			SVGImageView svgImageView = new SVGImageView(mContext)
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
		svgImageView.setScaleType(ScaleType.FIT_START);
		ViewGroup layout = (ViewGroup) parent.findViewById(idContainer);
			layout.addView(svgImageView);			
		} catch (SVGParseException e) {
			e.printStackTrace();
		}
	}
	
	public String getProcessedXML()
	{
		if (mDocument == null)
			return null;
		
		try
		{
			DOMSource domSource = new DOMSource(mDocument);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			return writer.toString();
		}
		catch (TransformerException ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
}
