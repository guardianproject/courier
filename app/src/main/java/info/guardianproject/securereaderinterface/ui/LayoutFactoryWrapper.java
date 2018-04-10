package info.guardianproject.securereaderinterface.ui;

import info.guardianproject.securereaderinterface.App;
import android.content.Context;
import android.support.v4.view.LayoutInflaterFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;


public class LayoutFactoryWrapper implements LayoutInflaterFactory
{
	public interface Callback {
		void onViewCreated(View view, String name, Context context, AttributeSet attrs);
	}

	private android.view.LayoutInflater.Factory mParent;
	private Callback mCallback;

	public LayoutFactoryWrapper(LayoutInflater.Factory parent)
	{
		mParent = parent;
	}

	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	@Override
	public View onCreateView(View view, String name, Context context, AttributeSet attrs)
	{
		View ret = App.createView(name, context, attrs);
		if (ret == null && mParent != null)
		{
			ret = mParent.onCreateView(name, context, attrs);
		}
		if (mCallback != null)
			mCallback.onViewCreated(ret, name, context, attrs);
		return ret;
	}	
}
