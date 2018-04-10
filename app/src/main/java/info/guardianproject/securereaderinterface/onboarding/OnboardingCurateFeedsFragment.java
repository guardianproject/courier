package info.guardianproject.securereaderinterface.onboarding;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.adapters.FeedListAdapterCurate;

public class OnboardingCurateFeedsFragment extends OnboardingFragment implements FeedListAdapterCurate.FeedListAdapterCurateListener {

    private View btnNext;

    public OnboardingCurateFeedsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.onboarding_curate_feeds, container, true);

        final FeedListAdapterCurate adapter = new FeedListAdapterCurate(mRootView.getContext());
        adapter.setListener(this);

        btnNext = mRootView.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String processedXML = adapter.getProcessedXML();
                if (processedXML != null)
                    App.getInstance().setOverrideResources(new OPMLOverrideResources(processedXML));
                if (getListener() != null)
                    getListener().onNextPressed();
            }
        });
        btnNext.setEnabled(adapter.getSelectionCount() > 0);

        ListView lv = (ListView) mRootView.findViewById(R.id.lvFeeds);
        lv.setAdapter(adapter);

        return mRootView;
    }

    @Override
    public void onSelectionChanged(int count) {
        btnNext.setEnabled(count > 0);
    }

    public class OPMLOverrideResources extends Resources
    {
        private String mXML;

        public OPMLOverrideResources(String xml) {
            super(App.getInstance().getResources().getAssets(), App.getInstance().getResources().getDisplayMetrics(), App.getInstance().getResources().getConfiguration());
            mXML = xml;
        }

        @Override
        public InputStream openRawResource(int id) throws NotFoundException {
            if (id == R.raw.bigbuffalo_opml) {
                return new ByteArrayInputStream(mXML.getBytes(Charset.forName("UTF-8")));
            }
            return super.openRawResource(id);
        }
    }
}
