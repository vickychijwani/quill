package me.vickychijwani.spectre.view.fragments;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.vickychijwani.spectre.R;
import me.vickychijwani.spectre.SpectreApplication;
import me.vickychijwani.spectre.view.BundleKeys;

/**
 * NOTE: Always use the {@link #newInstance} factory method to create an instance of this fragment.
 */
public class WebViewFragment extends BaseFragment {

    @Bind(R.id.web_view)
    WebView mWebView;

    private Object mJsInterface;
    private String mJsInterfaceName;

    /**
     * Returns a new WebViewFragment which will load the desired URL.
     * @param url - URL to load
     * @return A new instance of WebViewFragment
     */
    public static WebViewFragment newInstance(String url) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(BundleKeys.URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public WebViewFragment() {}

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);
        ButterKnife.bind(this, view);
        String mUrl = getArguments().getString(BundleKeys.URL);
        if (TextUtils.isEmpty(mUrl)) {
            throw new IllegalArgumentException("Empty URL passed to WebViewFragment!");
        }

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);

        // enable remote debugging
        if (0 != (getActivity().getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        mWebView.loadUrl(mUrl);

        return view;
    }

    @SuppressLint({"JavascriptInterface", "AddJavascriptInterface"})
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWebView.addJavascriptInterface(mJsInterface, mJsInterfaceName);
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
        // NOTE: super must be called AFTER WebView is destroyed, because super method calls
        // ButterKnife.unbind which sets mWebView to null WITHOUT destroying it!
        super.onDestroyView();
        ((SpectreApplication) getActivity().getApplicationContext()).forceUnregisterComponentCallbacks();
    }

    // our custom methods
    public void evaluateJavascript(@Nullable String javascript) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(javascript, null);
        } else {
            mWebView.loadUrl("javascript:" + javascript);
        }
    }

    @SuppressLint({"JavascriptInterface", "AddJavascriptInterface"})
    public void setJSInterface(@Nullable Object jsInterface, @Nullable String name) {
        if (mWebView == null) {
            mJsInterface = jsInterface;
            mJsInterfaceName = name;
        } else {
            mWebView.addJavascriptInterface(jsInterface, name);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }

}
