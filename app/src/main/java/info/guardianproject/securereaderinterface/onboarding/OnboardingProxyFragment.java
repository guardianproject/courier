package info.guardianproject.securereaderinterface.onboarding;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Toast;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

public class OnboardingProxyFragment extends OnboardingFragment {
    private View mRootView;
    private Settings.ProxyType mWaitingForProxyConnection;

    public OnboardingProxyFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWaitingForProxyConnection = Settings.ProxyType.None;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.onboarding_proxy, container, true);

        UIHelpers.populateContainerWithSVG(mRootView, R.raw.onboard_psiphon, R.id.ivIllustrationPsiphon);
        UIHelpers.populateContainerWithSVG(mRootView, R.raw.onboard_tor, R.id.ivIllustrationTor);

        View btnNotNow = mRootView.findViewById(R.id.btnNotNow);
        btnNotNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.getSettings().setProxyType(Settings.ProxyType.None);
                if (getListener() != null)
                    getListener().onNextPressed();
            }
        });

        View btnConnectTor = mRootView.findViewById(R.id.btnConnectTor);
        btnConnectTor.setOnClickListener(new ProxyConnectClickListener(Settings.ProxyType.Tor));

        View btnConnectPsiphon = mRootView.findViewById(R.id.btnConnectPsiphon);
        btnConnectPsiphon.setOnClickListener(new ProxyConnectClickListener(Settings.ProxyType.Psiphon));

        return mRootView;
    }

    private class ProxyConnectClickListener implements View.OnClickListener
    {
        private Settings.ProxyType mProxyType;

        public ProxyConnectClickListener(Settings.ProxyType proxyType)
        {
            mProxyType = proxyType;
        }

        @Override
        public void onClick(View v)
        {
            tryToConnect();
        }

        private void tryToConnect()
        {
            // Have net?
			int onlineMode = App.getInstance().socialReader.isOnline();
			if (onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK)
			{
				showNoNetToast();
			}
			else
			{
				mWaitingForProxyConnection = mProxyType;
				App.getSettings().setProxyType(mProxyType);
				App.getInstance().socialReader.connectProxy(getActivity());
				moveToNextIfProxyOnline(mProxyType);
			}
        }

        private void showNoNetToast()
        {
            try
            {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                final PopupWindow mMenuPopup = new PopupWindow(inflater.inflate(R.layout.lock_screen_proxy_select_no_net, (ViewGroup) mRootView, false),
                        mRootView.getWidth(), mRootView.getHeight(), true);
                View viewRetry = mMenuPopup.getContentView().findViewById(R.id.btnRetry);
                viewRetry.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        mMenuPopup.dismiss();
                        tryToConnect();
                    }
                });
                mMenuPopup.setOutsideTouchable(true);
                mMenuPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                mMenuPopup.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
                mMenuPopup.getContentView().setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        mMenuPopup.dismiss();
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mWaitingForProxyConnection != Settings.ProxyType.None)
		{
			mRootView.removeCallbacks(mCheckProxyRunnable);
			Settings.ProxyType proxyType = mWaitingForProxyConnection;
			mWaitingForProxyConnection = Settings.ProxyType.None;
			boolean online = moveToNextIfProxyOnline(proxyType);
			if (!online)
			{
				waitForProxyConnection();
			}
		}
	}

    // POLL for proxy connection
	private void waitForProxyConnection()
	{
		mWaitForProxyTries = 0;
        if (App.getSettings().proxyType() == Settings.ProxyType.Psiphon)
    		App.getInstance().socialReader.checkPsiphonStatus();
        else
            App.getInstance().socialReader.checkTorStatus();
		mRootView.postDelayed(mCheckProxyRunnable, 1000);
	}

	private int mWaitForProxyTries = 0;
	private Runnable mCheckProxyRunnable = new Runnable()
	{
		@Override
		public void run() {
			boolean online = moveToNextIfProxyOnline(App.getSettings().proxyType());
			mWaitForProxyTries++;
			if (!online && mWaitForProxyTries < 10)
				mRootView.postDelayed(mCheckProxyRunnable, 1000);
		}
	};

    private boolean moveToNextIfProxyOnline(Settings.ProxyType type)
	{
		if (App.getSettings().proxyType() == type && App.getInstance().socialReader.isProxyOnline())
		{
			if (type == Settings.ProxyType.Tor)
				Toast.makeText(mRootView.getContext(), R.string.onboarding_proxy_tor_connected, Toast.LENGTH_LONG).show();
			else if (type == Settings.ProxyType.Psiphon)
				Toast.makeText(mRootView.getContext(), R.string.onboarding_proxy_psiphon_connected, Toast.LENGTH_LONG).show();
            if (getListener() != null)
                getListener().onNextPressed();
			return true;
		}
		return false;
	}
}
