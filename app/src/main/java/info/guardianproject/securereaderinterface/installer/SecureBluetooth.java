package info.guardianproject.securereaderinterface.installer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SecureBluetooth
{
	public final static String LOGTAG = "SecureBlth";
	public final static boolean LOGGING = false;
	
	public final static String BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B31337";
	//public final static String BLUETOOTH_UUID = "00001101-0000-1000-8000-00805f9b34fb";
	
	public final static int EVENT_DATA_RECEIVED = 0;
	public final static int EVENT_CONNECTED = 1;
	public final static int EVENT_DISCONNECTED = 2;
	public final static int EVENT_OBJECT_RECEIVED = 3;

	public final static int REQUEST_ENABLE_BT = 10;
	public final static int REQUEST_ENABLE_BT_DISCOVERY = 11;

	/* Bluetooth */
	public BluetoothAdapter btAdapter;
	private BluetoothDevice btDevice;

	// I made this up
	private final UUID uuidSpp = UUID.fromString(BLUETOOTH_UUID);

	private BluetoothSocket socket;
	private ConnectedThread connectedThread;
	private boolean connected = false;

	SecureBluetoothEventListener sbel;
	Handler eventHandler;

	Context context;

	public SecureBluetooth(Context _context)
	{
		context = _context;

		try
		{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
			{
				if (LOGGING)
					Log.v(LOGTAG,"Using new BluetoothManager Method");

				//Use getSystemService(java.lang.String) with BLUETOOTH_SERVICE to create an BluetoothManager, then call getAdapter() to obtain the BluetoothAdapter.

				BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

				btAdapter = bm.getAdapter();

				if (btAdapter == null) {
					if (LOGGING)
						Log.e(LOGTAG, "No Bluetooth Adapter found");
				}
			}
			else
			{

				if (LOGGING)
					Log.v(LOGTAG,"Using old getDefaultAdapter Method");

				btAdapter = BluetoothAdapter.getDefaultAdapter();
				if (btAdapter == null) {
					if (LOGGING)
						Log.e(LOGTAG, "No Bluetooth Adapter found");
				}
			}
		}
		catch (Exception e)
		{
			if (LOGGING)
				Log.e(LOGTAG, "Couldn't get Bluetooth Adapter");
			e.printStackTrace();
		}

		// Handlers let us interact with threads on the UI thread
		// The handleMessage method receives messages from other threads and
		// will act upon it on the UI thread
		// This handler is to receive data from the connected thread and send
		// events
		eventHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				// We got a message which means that we have an
				// EVENT_DATA_RECEIVED to send
				// Or CONNECTED or DISCONNECTED..
				// msg.arg1 will equal one of those
				if (sbel != null)
				{
					if (msg.what == EVENT_DATA_RECEIVED && msg.obj != null)
					{
						sbel.secureBluetoothEvent(msg.what, msg.arg1, msg.obj);
					}
					else if (msg.what == EVENT_OBJECT_RECEIVED && msg.obj != null)
					{
						sbel.secureBluetoothEvent(msg.what, -1, msg.obj);
					}
					else
					{
						if (LOGGING) 
							Log.v(LOGTAG, "propigating event " + msg.what);
						sbel.secureBluetoothEvent(msg.what, -1, null);
					}
				}
			}
		};
	}

	// This is the interface that must be implemented to get the callbacks
	public interface SecureBluetoothEventListener
	{
		void secureBluetoothEvent(int eventType, int dataLength, Object data);
	}

	public void setSecureBluetoothEventListener(SecureBluetoothEventListener _sbel)
	{
		sbel = _sbel;
	}

	/**
	 * Returns whether the Bluetooth adapter is enabled.
	 * 
	 * @return true of false
	 */
	public boolean isEnabled() {
		return btAdapter != null && btAdapter.isEnabled();
	}

	/**
	 * Call to trigger the intent for enabling Bluetooth
	 * 
	 * @return void
	 */
	public void enableBluetooth(Activity activity)
	{
		if (!isEnabled())
		{
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	/**
	 * Returns a list of bonded (paired) devices.
	 * 
	 * @param info
	 *            flag to control display of additional information (device
	 *            names and types)
	 * @return String array
	 */
	public String[] list(boolean info)
	{
		Vector<String> list = new Vector<>();
		Set<BluetoothDevice> devices;

		try
		{

			devices = btAdapter.getBondedDevices();
			// convert the devices 'set' into an array so that we can
			// perform string functions on it
			Object[] deviceArray = devices.toArray();
			// step through it and assign each device in turn to
			// remoteDevice and then print it's name
			for (int i = 0; i < devices.size(); i++)
			{
				BluetoothDevice thisDevice = btAdapter.getRemoteDevice(deviceArray[i].toString());
				String element = thisDevice.getAddress();
				if (info)
				{
					element += "," + thisDevice.getName() + "," + thisDevice.getBluetoothClass().getMajorDeviceClass(); // extended
																														// information
				}
				list.addElement(element);
			}
		}
		catch (UnsatisfiedLinkError | Exception e)
		{
			if (LOGGING) 
				Log.e(LOGTAG, Log.getStackTraceString(e));
		}

		String outgoing[] = new String[list.size()];
		list.copyInto(outgoing);
		return outgoing;
	}

	/**
	 * Returns a list of hardware (MAC) addresses of bonded (paired) devices.
	 * 
	 * @return String array
	 */
	public String[] list()
	{
		return list(false);
	}

	/**
	 * Returns the name of the connected remote device It not connected, returns
	 * "-1"
	 */
	public String getRemoteName()
	{
		if (connected)
		{
			return (btDevice.getName());
		}
		else
		{
			return ("-1");
		}
	}

	/**
	 * Returns the name of the connected remote device It not connected, returns
	 * "-1"
	 */
	public String getRemoteAddress()
	{
		if (connected)
		{
			return (btDevice.getAddress());
		}
		else
		{
			return ("-1");
		}
	}

	/**
	 * Start discovery for bluetooth devices
	 */
	public boolean startDiscovery() {
		if (LOGGING)
			Log.v(LOGTAG, "startDiscovery()");

		return isEnabled() && !btAdapter.isDiscovering() && btAdapter.startDiscovery();
	}

	/**
	 * Enable our bluetooth to be discovered
	 */
	public void enableDiscovery(Activity activity)
	{
		if (isEnabled() && btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
		{
			Intent enableBtDiscoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			activity.startActivityForResult(enableBtDiscoveryIntent, REQUEST_ENABLE_BT_DISCOVERY);
		}
	}

	/**
	 * Returns the name of the currently connected device.
	 * 
	 * @return String
	 */
	public String getName()
	{
		if (btDevice != null)
			return btDevice.getName();
		else
			return "-1";
	}

	/**
	 * Connects to a Bluetooth device.
	 * 
	 * The connect() method will attempt to determine what type of device is
	 * currently specified by mac and will select one of the following Service
	 * Profile UUIDs accordingly.
	 * <p>
	 * Currently only Android-to-serial modem (Arduino) and Android- to-serial
	 * port (computer) connections are supported.
	 * <p>
	 * 
	 * @param mac
	 *            - hardware (MAC) address of the remote device
	 * @return boolean flag for if connection was successful
	 */
	public boolean connect(String mac)
	{
		/* Before we connect, make sure to cancel any discovery! */
		if (btAdapter.isDiscovering())
		{
			btAdapter.cancelDiscovery();
			if (LOGGING) 
				Log.i(LOGTAG, "Cancelled ongoing discovery");
		}

		if (LOGGING)
			Log.v(LOGTAG, "About to connect to " + mac);

		/* Make sure we're using a real bluetooth address to connect with */
		if (BluetoothAdapter.checkBluetoothAddress(mac))
		{
			if (LOGGING)
				Log.v(LOGTAG, "It's a bluetooth device");

			/* Get the remote device we're trying to connect to */
			btDevice = btAdapter.getRemoteDevice(mac);
			if (LOGGING)
				Log.v(LOGTAG, "Have the device");

			/* Create the RFCOMM sockets */
			try
			{

				if (LOGGING)
					Log.v(LOGTAG, "Trying the service");

				socket = btDevice.createRfcommSocketToServiceRecord(uuidSpp);
				if (LOGGING)
					Log.i(LOGTAG, "connecting to service " + uuidSpp);

				socket.connect();
				if (LOGGING)
					Log.v(LOGTAG, "Connected, moving on");

				if (LOGGING)
					Log.i(LOGTAG, "Connected to device " + btDevice.getName() + " [" + btDevice.getAddress() + "]");

				connected = true;

				// Start the thread to manage the connection and perform
				// transmissions
				connectedThread = new ConnectedThread();
				connectedThread.start();

				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}

				Message msg = eventHandler.obtainMessage(SecureBluetooth.EVENT_CONNECTED);
				eventHandler.sendMessage(msg);

			}
			catch (IOException e)
			{
				if (LOGGING)
					Log.e(LOGTAG, "Couldn't get a connection");
				e.printStackTrace();

				Message msg = eventHandler.obtainMessage(SecureBluetooth.EVENT_DISCONNECTED);
				eventHandler.sendMessage(msg);

				connected = false;
			}

		}
		else
		{
			if (LOGGING)
				Log.i(LOGTAG, "Address is not Bluetooth, please verify MAC.");

			Message msg = eventHandler.obtainMessage(SecureBluetooth.EVENT_DISCONNECTED);
			eventHandler.sendMessage(msg);

			connected = false;
		}

		return connected;
	}

	/**
	 * Opens a BluetoothServerSocket to listen for connections Primarily
	 * intended for Android-to-Android connections
	 * 
	 * @return
	 */
	public void listen()
	{
		AcceptThread listenThread = new AcceptThread();
		listenThread.start();
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 * 
	 * Based on the Android BluetoothChat example
	 */
	private class AcceptThread extends Thread
	{
		// The local server socket
		BluetoothServerSocket mServerSocket = null;

		public AcceptThread()
		{
			// Create a new listening server socket
			try
			{
				mServerSocket = btAdapter.listenUsingRfcommWithServiceRecord("SerialPortProfile", uuidSpp);
			}
			catch (IOException e)
			{
				if (LOGGING)
					Log.e(LOGTAG, "Socket listen() failed", e);
			}
		}

		@Override
		public void run()
		{
			if (LOGGING)
				Log.v(LOGTAG, "running AcceptThread");

			socket = null;

			// Listen to the server socket if we're not connected
			while (!connected)
			{
				if (LOGGING)
					Log.v(LOGTAG, "waiting for connection");

				try
				{
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mServerSocket.accept();
					btDevice = socket.getRemoteDevice();

				}
				catch (Exception e)
				{
					if (LOGGING)
						Log.e(LOGTAG, "Socket accept() failed", e);
					break;
				}

				// If a connection was accepted
				if (socket != null)
				{

					if (LOGGING)
						Log.v(LOGTAG, "Socket accept() succeeded, moving on");

					try
					{

						Message msg = eventHandler.obtainMessage(SecureBluetooth.EVENT_CONNECTED);
						eventHandler.sendMessage(msg);

						// Situation normal. Start the connected thread.
						connectedThread = new ConnectedThread();
						connectedThread.start();

						// Set the status
						connected = true;

						if (LOGGING)
							Log.i(LOGTAG, "Connected to device " + btDevice.getName() + " [" + btDevice.getAddress() + "]");

					}
					catch (Exception ex)
					{
						if (LOGGING)
							Log.i(LOGTAG, "Couldn't get a connection");
						ex.printStackTrace();
						connected = false;
					}
				}
			}
		}
	}

	/**
	 * Writes a byte[] buffer to the output stream.
	 * 
	 * @param buffer
	 */
	public void write(byte[] buffer)
	{
		if (LOGGING)
			Log.v(LOGTAG, "Writing buffer " + buffer.length);
		connectedThread.write(buffer);
	}
	
	public void writeLength(long length) {
		if (LOGGING)
			Log.v(LOGTAG, "Writing length " + length);
		connectedThread.writeLength(length);
	}

	/**
	 * Disconnects the Bluetooth socket.
	 * 
	 * This should be called in the pause() and stop() methods inside the sketch
	 * in order to ensure that the socket is properly closed when the sketch is
	 * not running. The connection should be re-established in a resume() method
	 * if the sketch loses and then regains focus.
	 * 
	 * @see connect()
	 */
	public void disconnect()
	{
		if (LOGGING)
			Log.v(LOGTAG, "Disconnect Called");
		if (connected)
		{
			// This should do it..
			if (connectedThread != null)
			{
				if (LOGGING)
					Log.v(LOGTAG, "Calling cancel on connectThread");
				connectedThread.cancel();
			}
			connected = false;

			Message msg = eventHandler.obtainMessage(EVENT_DISCONNECTED);
			eventHandler.sendMessage(msg);
		}
	}

	public class ConnectedThread extends Thread
	{
		final InputStream inStream;
		final OutputStream outStream;
		DataInputStream dataIn;
		DataOutputStream dataOut;
		long readLength = 100;
		long totalRead = 0;
		
		public ConnectedThread()
		{
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try
			{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			}
			catch (IOException e)
			{
				if (LOGGING) {
					Log.e(LOGTAG, "Error getting Input or Output stream");
					e.printStackTrace();
				}
			}

			inStream = tmpIn;
			outStream = tmpOut;
			
			dataIn = new DataInputStream(inStream);
			dataOut = new DataOutputStream(outStream);
		}

		@Override
		public void run()
		{
			if (LOGGING)
				Log.i(LOGTAG, "ConnectedThread running");
			
			// first going to read the first 4 bytes to get length
			try {
				readLength = dataIn.readLong();
				Log.v(LOGTAG, "readLength is " + readLength);
			} catch (IOException e) {
				Log.v(LOGTAG, "Got IOException reading the length");
				e.printStackTrace();
			} 
			
			byte[] buffer = new byte[256]; // buffer store for the stream
			int bytes; // bytes returned from read()

			byte[] transfer;

			// Keep listening to the InputStream until an exception occurs
			while (true)
			{
				try
				{
					// Read from the InputStream
					if (LOGGING)
						Log.v(LOGTAG, "going to read from inStream");
					//bytes = inStream.read(buffer);
					bytes = dataIn.read(buffer);
					totalRead += bytes;
					if (LOGGING)
						Log.v(LOGTAG, "got " + bytes + " bytes, total " + totalRead + " of " + readLength);
					
					transfer = new byte[bytes];
					System.arraycopy(buffer, 0, transfer, 0, bytes);

					// Send the obtained bytes to the UI Activity
					Message msg = eventHandler.obtainMessage(EVENT_DATA_RECEIVED, bytes, -1, transfer);
					eventHandler.sendMessage(msg);
					
					if (totalRead >= readLength) {
						if (LOGGING)
							Log.v(LOGTAG,"totalRead >= readLength, finished reading");
						break;
					}
				}
				catch (IOException e)
				{
					if (LOGGING) {
						Log.v(LOGTAG, "Got IOException, closing things up");
						e.printStackTrace();
					}
					break;
				}
			}
			if (LOGGING)
				Log.v(LOGTAG, "Calling cancel");
			cancel();
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel()
		{
			if (LOGGING)
				Log.v(LOGTAG,"ConnectedThread cancel");
			/* Close the streams */
			try
			{
				if (LOGGING)
					Log.v(LOGTAG, "Closing the streams");
				outStream.flush();
				dataIn.close();
				inStream.close();
				outStream.close();

				if (LOGGING)
					Log.v(LOGTAG, "Closed Streams");
			}
			catch (IOException e)
			{
				if (LOGGING) 
					e.printStackTrace();
			}
			finally
			{
				try
				{
					if (socket != null) {
						socket.close();
						if (LOGGING)
							Log.v(LOGTAG, "Closed socket");

						Message msg = eventHandler.obtainMessage(EVENT_DISCONNECTED);
						eventHandler.sendMessage(msg);
					}
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					if (LOGGING)
						e.printStackTrace();
				}
			}		
		}

		/*
		 * public void writeObject(Object objectToWrite) { try {
		 * ObjectOutputStream oos = new ObjectOutputStream(outStream);
		 * oos.writeObject(objectToWrite); } catch (IOException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); } }
		 */

		/* Call this from the main Activity to send data to the remote device */
		public void write(byte[] bytes)
		{
			try
			{
				//outStream.write(bytes);
				dataOut.write(bytes);
				if (LOGGING)
					Log.v(LOGTAG, "wrote " + bytes.length);
			}
			catch (IOException e)
			{
				if (LOGGING)
					e.printStackTrace();
			}
		}
		
		public void writeLength(long length) {
			try {
				dataOut.writeLong(length);
			} catch (IOException e) {
				if (LOGGING)
					e.printStackTrace();
			}
			
		}
	}
}
