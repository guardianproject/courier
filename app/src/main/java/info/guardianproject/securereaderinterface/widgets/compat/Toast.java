package info.guardianproject.securereaderinterface.widgets.compat;

import info.guardianproject.securereaderinterface.R;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

public class Toast extends android.widget.Toast
{
	public Toast(Context context)
	{
		super(context);
	}
	
	public static Toast makeText(Context context, int resId, int duration)
	{
		Toast toast = new Toast(context);
        toast.setDuration(duration);
        TextView view = new TextView(context);
        view.setText(resId);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundResource(R.drawable.toast_frame);
        toast.setView(view);
		return toast;
	}
}
