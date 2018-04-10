package info.guardianproject.securereaderinterface.installer;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;
import com.tinymission.rss.Reader;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.LockableFragment;
import info.guardianproject.securereaderinterface.MainActivity;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.adapters.ViewHolderItem;
import info.guardianproject.securereaderinterface.models.FeedSelection;
import info.guardianproject.securereaderinterface.models.ItemViewModel;

public class SecureBluetoothReceiverFragment extends DialogFragment implements LockableFragment, OnClickListener, SecureBluetooth.SecureBluetoothEventListener
{
	public static final String LOGTAG = "SBReceiverFragment";
	public static final boolean LOGGING = false;
	public static final int ACCESS_LOCATION_PERMISSION_REQUEST = 0;
	private boolean mHasRequestedPermission;

	private enum UIState
	{
		Listening, Receiving, ReceivedOk
	}

	Button receiveButton;
	TextView receiveText;

	SecureBluetooth sb;
	java.io.File receivedContentBundleFile;
	BufferedOutputStream bos;

	boolean readyToReceive = false;

	private View mLLWait;
	private View mLLReceive;
	private View mLLSharedStory;

	UIState mCurrentState = UIState.Listening;
	private ProgressBar mProgressReceive;
	private ItemViewModel mItemReceived;

	private long bytesReceived = 0;
	private boolean mReceiverRegistered;
	private Dialog mDialog;

	public SecureBluetoothReceiverFragment()
	{
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE;
        int theme = R.style.AppTheme_Dialog;
        setStyle(style, theme);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case ACCESS_LOCATION_PERMISSION_REQUEST: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					startBluetooth();

				} else {

					Toast.makeText(this.getActivity(), "Sorry, we can not use Bluetooth without location permission", Toast.LENGTH_LONG).show();
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					if (mDialog != null)
						mDialog.dismiss();
				}
			}

		}
	}

	private void startBluetooth() {
		sb = new SecureBluetooth(this.getActivity());
		sb.setSecureBluetoothEventListener(this);

		// Start by trying to receive
		if (!sb.isEnabled())
			sb.enableBluetooth(getActivity());
		else if (mDialog != null)
			mDialog.show();

		//sb.enableDiscovery(getActivity());
		receiveButton.performClick();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View root = inflater.inflate(R.layout.activity_secure_blue_tooth_receiver, container, false);

		mLLWait = root.findViewById(R.id.llWait);
		mLLReceive = root.findViewById(R.id.llReceive);
		mLLSharedStory = root.findViewById(R.id.llSharedStory);

		receiveText = (TextView) root.findViewById(R.id.btReceiveText);

		receiveButton = (Button) root.findViewById(R.id.btReceiveButton);
		receiveButton.setOnClickListener(this);

		mProgressReceive = (ProgressBar) root.findViewById(R.id.progressReceive);

		mLLSharedStory.findViewById(R.id.btnClose).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				dismiss();
			}
		});
		mLLSharedStory.findViewById(R.id.btnRead).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mItemReceived != null)
				{
					dismiss();
					Intent intent = new Intent(v.getContext(), MainActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_ITEM, mItemReceived);
			    	intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_FEED_SELECTION, FeedSelection.SHARED);
					startActivity(intent);
				}
			}
		});

		registerReceiver();

		return root;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		mDialog = super.onCreateDialog(savedInstanceState);
		mDialog.setOnShowListener(new OnShowListener()
		{
			@Override
			public void onShow(DialogInterface dialog)
			{
				// If BT not enabled, hide us for now. We will prompt the user to enable
				// BT and handle the result in onUnlockedActivityResult. Based on the
				// user's choice the dialog will either be dismissed or shown there.
				if (sb == null || !sb.isEnabled())
					mDialog.hide();
			}
		});
		return mDialog;
	}

	@Override
	public void onPause()
	{
		if (LOGGING)
		Log.v(LOGTAG,"onPause");
		super.onPause();
	}
	
	@Override
	public void onStop()
	{
		if (LOGGING) 
		Log.v(LOGTAG,"onStop");
		if (sb != null) {
			sb.disconnect();
			sb.setSecureBluetoothEventListener(null);
		}
		unregisterReceiver();
		super.onStop();
	}

	@Override
	public void onResume()
	{
		registerReceiver();
		super.onResume();
		updateUi();

		if (!mHasRequestedPermission) {
			mHasRequestedPermission = true;
			int permissionCheck = ContextCompat.checkSelfPermission(this.getActivity(),
					Manifest.permission.ACCESS_FINE_LOCATION);

			if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
						ACCESS_LOCATION_PERMISSION_REQUEST);
			} else {
				startBluetooth();
			}
		}
	}

	private void updateUi()
	{
		if (mCurrentState == UIState.Receiving)
		{
			mLLReceive.setVisibility(View.VISIBLE);
			mLLWait.setVisibility(View.GONE);
			mLLSharedStory.setVisibility(View.GONE);
		}
		else if (mCurrentState == UIState.Listening)
		{
			receiveText.setText(R.string.bluetooth_receive_info);
			receiveButton.setEnabled(true);
			mLLReceive.setVisibility(View.GONE);
			mLLWait.setVisibility(View.VISIBLE);
			mLLSharedStory.setVisibility(View.GONE);
		}
		else if (mCurrentState == UIState.ReceivedOk)
		{
			mLLReceive.setVisibility(View.GONE);
			mLLWait.setVisibility(View.GONE);

			View itemView = mLLSharedStory.findViewById(R.id.sharedItemView);
			ViewHolderItem itemViewHolder = new ViewHolderItem(itemView);
			itemViewHolder.bindModel(mItemReceived, true, false, null);
			mLLSharedStory.setVisibility(View.VISIBLE);
		}
	}

	private void setUiState(UIState state)
	{
		mCurrentState = state;
		updateUi();
	}

	@Override
	public void onClick(View clickedView)
	{
		if (clickedView == receiveButton && sb != null)
		{
			sb.enableDiscovery(getActivity());
			sb.listen();
			this.updateBasedOnScanMode(sb.btAdapter.getScanMode());
			if (LOGGING)
				Log.v(LOGTAG, "listen called, ready to receive");
			//receiveButton.setEnabled(false);
		}
	}

	private void getReadyToReceive()
	{
		//receivedContentBundleFile = ((App) this.getApplication()).socialReader.vfsTempItemBundle();
		receivedContentBundleFile = App.getInstance().socialReader.nonVfsTempItemBundle();

		receiveText.setText(getString(R.string.bluetooth_receive_connected));

		try
		{
			bos = new BufferedOutputStream(new java.io.FileOutputStream(receivedContentBundleFile));
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		readyToReceive = true;
	}

	@Override
	public void secureBluetoothEvent(int eventType, int dataLength, Object data)
	{
		if (LOGGING)
		Log.v(LOGTAG, "secureBluetoothEvent " + eventType);

		if (eventType == SecureBluetooth.EVENT_CONNECTED)
		{

			if (LOGGING)
			Log.v(LOGTAG, "We have a connection");
			setUiState(UIState.Receiving);
			getReadyToReceive();

		}
		else if (eventType == SecureBluetooth.EVENT_DISCONNECTED)
		{

			if (LOGGING)
			Log.v(LOGTAG, "Got a disconnect, " + bytesReceived + " bytes received");

			if (mCurrentState == UIState.Receiving) {
				try {
					bos.close();
					bos = null;

					ArrayList<File> mediaFiles = new ArrayList<>();

					// Now unzip it
					ZipFile zipFile = new ZipFile(receivedContentBundleFile);

					Item receivedItem = null;

					for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); ) {
						ZipEntry currentEntry = entries.nextElement();

						if (LOGGING)
							Log.v(LOGTAG, "Found: " + currentEntry.getName());

						if (currentEntry.getName().contains(SocialReader.CONTENT_ITEM_EXTENSION)) {
							if (LOGGING)
								Log.v(LOGTAG, "Parsing " + currentEntry.getName());

							InputStream inputStream = zipFile.getInputStream(currentEntry);

							Feed incomingFeed = new Feed(inputStream);
							Reader feedReader = new Reader(App.getInstance().socialReader, incomingFeed);
							incomingFeed = feedReader.fetchFeed();


							// Looping through but ...
							for (int i = 0; i < incomingFeed.getItemCount(); i++) {

								receivedItem = incomingFeed.getItem(i);
								//objectInputStream.close();
								if (LOGGING)
									Log.v(LOGTAG, "We have an Item!!!");
								mItemReceived = new ItemViewModel(receivedItem);
								receivedItem.setShared(true);
								receivedItem.setDatabaseId(Item.DEFAULT_DATABASE_ID);
								receivedItem.setFeedId(Feed.DEFAULT_DATABASE_ID);
								for (MediaContent mc : receivedItem.getMediaContent()) {
									mc.setDatabaseId(MediaContent.DEFAULT_DATABASE_ID);
								}
								if (LOGGING) {
									Log.v(LOGTAG, "getGuid: " + receivedItem.getGuid());
									Log.v(LOGTAG, "getFeedId: " + receivedItem.getFeedId());
								}
								// Add it in..
								App.getInstance().socialReader.setItemData(receivedItem);
							}
						} else { // Ignore for now, we'll loop through again in a second
							if (LOGGING)
								Log.v(LOGTAG, "Ignoring media element for now");
						}
					}

					if (receivedItem != null) {
						int mediaContentCount = 0;
						for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); ) {
							ZipEntry currentEntry = entries.nextElement();
							if (!currentEntry.getName().contains(SocialReader.CONTENT_ITEM_EXTENSION)) {
								// It's Media Content
								// Save the files in the normal place

								InputStream inputStream = zipFile.getInputStream(currentEntry);
								BufferedOutputStream bos;

								MediaContent mediaContent = receivedItem.getMediaContent(mediaContentCount);
								mediaContentCount++;

								File savedFile = new File(App.getInstance().socialReader.getFileSystemDir(), SocialReader.MEDIA_CONTENT_FILE_PREFIX + mediaContent.getDatabaseId());
								bos = new BufferedOutputStream(new FileOutputStream(savedFile));

								byte buffer[] = new byte[1024];
								int count;
								while ((count = inputStream.read(buffer)) != -1) {
									bos.write(buffer, 0, count);
								}
								inputStream.close();
								bos.close();
							}
						}
					} else {
						if (LOGGING)
							Log.e(LOGTAG, "Didn't get an item");
					}


				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				receivedContentBundleFile = null;
				readyToReceive = false;
			}
			if (mItemReceived != null) {
				setUiState(UIState.ReceivedOk);
			}
			else
				setUiState(UIState.Listening);
		}
		else if (eventType == SecureBluetooth.EVENT_DATA_RECEIVED)
		{
			if (!readyToReceive)
			{
				getReadyToReceive();
			}

			if (LOGGING)
			Log.v(LOGTAG, "Reading data: " + dataLength);
			bytesReceived += dataLength;

			try
			{
				bos.write((byte[]) data, 0, dataLength);
				updateProgress(dataLength, 2 * dataLength);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void updateProgress(final long cb, final long max)
	{
		mProgressReceive.post(new Runnable()
		{
			@Override
			public void run()
			{
				mProgressReceive.setMax((int) max);
				mProgressReceive.setProgress((int) cb);
			}
		});
	}

	private void registerReceiver()
	{
		if (!mReceiverRegistered)
		{
			mReceiverRegistered = true;
			if (LOGGING)
			Log.d(LOGTAG, "Register receiver");

			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
			getActivity().registerReceiver(receiver, filter);
		}

		// Update according to current scan mode
		if (sb != null && sb.btAdapter != null)
			updateBasedOnScanMode(sb.btAdapter.getScanMode());
	}

	private void unregisterReceiver()
	{
		if (mReceiverRegistered)
		{
			mReceiverRegistered = false;
			if (LOGGING)
			Log.d(LOGTAG, "Unregister receiver");
			getActivity().unregisterReceiver(receiver);
		}
	}

	private final BroadcastReceiver receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent != null)
			{
				String action = intent.getAction();
				if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action))
				{
					int newScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
					updateBasedOnScanMode(newScanMode);
					if (sb.isEnabled())
						sb.listen();
				}
			}
		}
	};

	private void updateBasedOnScanMode(int scanMode)
	{
		if (scanMode == BluetoothAdapter.SCAN_MODE_NONE || scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE)
		{
			receiveText.setVisibility(View.GONE);
			receiveButton.setVisibility(View.VISIBLE);
		}
		else
		{
			receiveText.setVisibility(View.VISIBLE);
			receiveButton.setVisibility(View.GONE);
		}
	}

	@Override
	public void onUnlockedActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent)
	{
		// If we don't allow BT to be turned on, just quit out of this activity!
		if (requestCode == SecureBluetooth.REQUEST_ENABLE_BT)
		{
			if (resultCode == Activity.RESULT_CANCELED)	
			{
				this.dismiss();
			}
			else if (resultCode == Activity.RESULT_OK)
			{
				mDialog.show();
				receiveButton.performClick();
			}
		}
	}
}
