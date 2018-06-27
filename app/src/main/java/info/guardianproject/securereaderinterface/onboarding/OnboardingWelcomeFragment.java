package info.guardianproject.securereaderinterface.onboarding;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.BuildConfig;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.ui.SupportedLanguage;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

public class OnboardingWelcomeFragment extends OnboardingFragment {
    private View mRootView;

    public OnboardingWelcomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.onboarding_welcome, container, true);

        View btnLanguage = mRootView.findViewById(R.id.btnLanguage);
        btnLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguagesPopup();
            }
        });
        if (SupportedLanguage.supportedLanguages().length < 2) {
            btnLanguage.setVisibility(View.GONE);
        }

        UIHelpers.populateContainerWithSVG(mRootView, R.raw.onboard_welcome, R.id.ivIllustration);

        View btnNext = mRootView.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getListener() != null)
                    getListener().onNextPressed();
            }
        });
        return mRootView;
    }

    private void showLanguagesPopup()
    {
        try
        {
            View anchor = mRootView.findViewById(R.id.btnLanguage);
            if (anchor == null)
                return;

            try
            {
                final ArrayAdapter<SupportedLanguage> adapter = new ArrayAdapter<>(mRootView.getContext(), R.layout.language_picker_popup_item, R.id.tvItem,
                        SupportedLanguage.supportedLanguages());
                adapter.sort(new Comparator<SupportedLanguage>() {
                    @Override
                    public int compare(SupportedLanguage o1, SupportedLanguage o2) {
                        return o1.getCode().compareTo(o2.getCode());
                    }
                });
                ListView lv = new ListView(mRootView.getContext());
                lv.setBackgroundResource(R.drawable.panel_bg_holo_light);
                lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                lv.setAdapter(adapter);
                // lv.setDivider(mDivider);

                Rect rectGlobal = new Rect();
                anchor.getGlobalVisibleRect(rectGlobal);

                Rect rectGlobalParent = new Rect();
                mRootView.getGlobalVisibleRect(rectGlobalParent);

                int maxHeight = rectGlobalParent.bottom - rectGlobal.top;
                lv.measure(View.MeasureSpec.makeMeasureSpec(Math.min(mRootView.getWidth(), UIHelpers.dpToPx(200, mRootView.getContext())), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST));

                final PopupWindow mPopup = new PopupWindow(lv, lv.getMeasuredWidth(), lv.getMeasuredHeight(), true);

                lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                    {
                        SupportedLanguage selectedLang = (SupportedLanguage) parent.getItemAtPosition(position);
                        App.getSettings().setUiLanguage(selectedLang.getCode());
                        mPopup.dismiss();
                    }
                });

                mPopup.setOutsideTouchable(true);
                mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                mPopup.showAtLocation(anchor, Gravity.TOP | Gravity.START, rectGlobal.left, rectGlobal.top);
                mPopup.setOnDismissListener(new PopupWindow.OnDismissListener()
                {
                    @Override
                    public void onDismiss()
                    {
                        // mPopup = null;
                    }
                });

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
