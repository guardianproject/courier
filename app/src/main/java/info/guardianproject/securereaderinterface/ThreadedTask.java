package info.guardianproject.securereaderinterface;

import android.os.Handler;

public abstract class ThreadedTask<Params,Progress,Result> {

	public static final String LOGTAG = "ThreadedTask";
	public static final boolean LOGGING = false;

	private boolean cancelled = false;
	private Handler mHandler;
	private Thread mThread;
	private boolean done;

	public ThreadedTask()
	{
		mHandler = new Handler();
	}
	
	protected abstract Result doInBackground(Params... values);

	protected void onPostExecute(Result result)
	{
	
	}

	protected void onCancelled() {}

	public final ThreadedTask<Params, Progress, Result> execute (Params... params)
	{
		mThread = new Thread()
		{
			private Params[] mParams;
			
			Thread init(Params... params)
			{
				mParams = params;
				return this;
			}
			
			@Override
			public void run() {
				final Result r = doInBackground(mParams);
				if (!isInterrupted()) {
					synchronized (ThreadedTask.this) {
						done = true;
					}
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							onPostExecute(r);
						}
					});
				}
			}
		}.init(params);
		mThread.setPriority(Thread.NORM_PRIORITY - 1);
		mThread.start();
		return this;
	}
	
	public void cancel(boolean mayInterruptIfRunning)
	{
		cancelled = true;

		// TODO: heed the mayInterruptIfRunning param.
		if (mThread != null)
			mThread.interrupt();
		mThread = null;
		synchronized (ThreadedTask.this) {
			if (!done) {
				// If done is set we are already set up to call onPostExecute.
				onCancelled();
			}
		}
	}

	public boolean isCancelled() {
		return cancelled;
	}
}
