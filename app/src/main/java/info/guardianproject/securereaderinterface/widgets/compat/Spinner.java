package info.guardianproject.securereaderinterface.widgets.compat;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.adapters.SpinnerWrapperListAdapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SpinnerAdapter;

public class Spinner extends RelativeLayout implements OnItemClickListener
{
	private SpinnerAdapter mAdapter;
	private AdapterView<Adapter> mAdapterViewWrapper;
	private View mCurrentView;
	private PopupWindow mPopup;
	private Drawable mDropDownBackground;
	private Drawable mDivider;
	private int mCurrentSelection;
	private OnItemSelectedListener mOnItemSelectedListener;

	public Spinner(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs);
	}

	public Spinner(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs);
	}

	public Spinner(Context context)
	{
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs)
	{
		if (attrs != null)
		{
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Spinner);
			mDropDownBackground = a.getDrawable(R.styleable.Spinner_android_popupBackground);
			mDivider = a.getDrawable(R.styleable.Spinner_android_divider);
			a.recycle();
		}
		if (mDropDownBackground == null)
			mDropDownBackground = ContextCompat.getDrawable(getContext(), R.drawable.panel_bg_holo_light);

		initAdapterViewWrapper(getContext());
		
		this.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showPopup();
			}
		});
	}

	public SpinnerAdapter getAdapter()
	{
		return mAdapter;
	}
	
	public void setAdapter(SpinnerAdapter adapter)
	{
		mAdapter = adapter;
		setCurrentSelection(0, false);
	}
	
	private void showPopup()
	{
		if (mAdapter != null && mAdapter.getCount() > 0)
		{
			try
			{
				ListView lv = new ListView(getContext());
				lv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				lv.setAdapter(new SpinnerWrapperListAdapter(mAdapter));
				lv.setOnItemClickListener(this);
				if (mDivider != null)
					lv.setDivider(mDivider);
				
				Rect rectGlobal = new Rect();
				this.getGlobalVisibleRect(rectGlobal);
				Rect rectGlobalParent = new Rect();
				
				getWindowVisibleDisplayFrame(rectGlobalParent);
				
				Rect backgroundRect = new Rect();
	            if (mDropDownBackground != null) 
	            {
	            	mDropDownBackground.getPadding(backgroundRect);
	            }
	            		
				final int spinnerPaddingLeft = getPaddingLeft();
	            final int spinnerWidth = getWidth();
	            final int spinnerPaddingRight = getPaddingRight();
	            
	            int contentWidth = measureContentWidth();
                contentWidth += backgroundRect.left + backgroundRect.right;
	            final int contentWidthLimit = rectGlobalParent.width() - backgroundRect.left - backgroundRect.right;
	            if (contentWidth > contentWidthLimit)
	            {
	            	contentWidth = contentWidthLimit;
	            }
	            
	            contentWidth = Math.max(contentWidth, spinnerWidth
	                        - spinnerPaddingLeft - spinnerPaddingRight);
				
				int maxHeightDown = rectGlobalParent.bottom - rectGlobal.bottom;
				int maxHeightUp = rectGlobal.top - rectGlobalParent.top;
	            int maxHeight = Math.max(maxHeightDown, maxHeightUp);
	            maxHeight = maxHeight - backgroundRect.top - backgroundRect.bottom;
	            
				lv.measure(
						MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY),
						MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
				lv.layout(0, 0, lv.getMeasuredWidth(), lv.getMeasuredHeight());
					
				mPopup = new PopupWindow(lv, lv.getMeasuredWidth(), lv.getMeasuredHeight() + backgroundRect.top + backgroundRect.bottom, true);
				mPopup.setOutsideTouchable(true);
				mPopup.setBackgroundDrawable(mDropDownBackground);
				mPopup.showAsDropDown(this, backgroundRect.left + spinnerPaddingLeft, 0);
				mPopup.setOnDismissListener(new PopupWindow.OnDismissListener()
				{
					@Override
					public void onDismiss()
					{
						mPopup = null;
					}
				});
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static final int MAX_ITEMS_TO_MEASURE = 15;
	
    int measureContentWidth()
    {
    	int maxWidth = 0;
    	
    	int nToMeasure = mAdapter.getCount();
    	if (nToMeasure > MAX_ITEMS_TO_MEASURE)
    		nToMeasure = MAX_ITEMS_TO_MEASURE;

        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0,
                MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0,
                MeasureSpec.UNSPECIFIED);

        View convertView = null;
        int convertType = 0;
        
    	for (int i = 0; i < nToMeasure; i++)
    	{
    		int type = mAdapter.getItemViewType(i);
    		if (type != convertType)
    		{
    			convertView = null;
    			convertType = type;
    		}
    		
    		convertView = mAdapter.getDropDownView(i, convertView, this);
            if (convertView.getLayoutParams() == null) 
            {
            	convertView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            convertView.measure(widthMeasureSpec, heightMeasureSpec);
            maxWidth = Math.max(maxWidth, convertView.getMeasuredWidth());    		
    	}
    	return maxWidth;
    }
	
	public int getCurrentSelection()
	{
		return mCurrentSelection;
	}

	public void setCurrentSelection(int position, boolean sendNotification)
	{
		mCurrentSelection = position;
		if (mCurrentView != null)
			this.removeView(mCurrentView);
		if (mAdapter != null && position >= 0 && position < mAdapter.getCount())
			mCurrentView = mAdapter.getView(position, null, this);
		else
			mCurrentView = null;
		if (mCurrentView != null)
			this.addView(mCurrentView);
		if (sendNotification && mOnItemSelectedListener != null)
			mOnItemSelectedListener.onItemSelected(mAdapterViewWrapper, mCurrentView, mCurrentSelection, mCurrentView != null ? mAdapter.getItemId(position) : 0);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
	{
		if (mPopup != null)
		{
			mPopup.dismiss();
		}
		setCurrentSelection(position, true);
	}

	public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener)
	{
		mOnItemSelectedListener = onItemSelectedListener;
	}
	
	private void initAdapterViewWrapper(Context context)
	{
		mAdapterViewWrapper = new AdapterView<Adapter>(context)
		{
			@Override
			public Adapter getAdapter()
			{
				return mAdapter;
			}

			@Override
			public void setAdapter(Adapter adapter)
			{
				Spinner.this.setAdapter((SpinnerAdapter) adapter);
			}

			@Override
			public View getSelectedView()
			{
				return Spinner.this.mCurrentView;
			}

			@Override
			public void setSelection(int position)
			{
				Spinner.this.setCurrentSelection(position, true);
			}
		};
	}
}
