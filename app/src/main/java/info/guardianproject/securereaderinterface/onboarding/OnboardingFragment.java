package info.guardianproject.securereaderinterface.onboarding;

import android.app.Fragment;
import android.os.Bundle;

public class OnboardingFragment extends Fragment {
    private OnboardingFragmentListener mListener;

    protected OnboardingFragmentListener getListener() {
        return mListener;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof OnboardingFragmentListener) {
            mListener = (OnboardingFragmentListener) getActivity();
        } else {
            throw new RuntimeException(getActivity().toString()
                    + " must implement OnboardingFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
