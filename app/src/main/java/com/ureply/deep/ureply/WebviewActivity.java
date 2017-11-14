package com.ureply.deep.ureply;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by deepansh on 10/10/2017.
 */

public class WebviewActivity extends AppCompatActivity {

        public WebView mWebView;
        public String url;
        private ProgressDialog progDialog;


    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_webview);
            LayoutInflater m_inflater = LayoutInflater.from(WebviewActivity.this);
            mWebView = (WebView) findViewById(R.id.activity_webview);

        Intent intentExtras = getIntent();
            Bundle extrasBundle = intentExtras.getExtras();
            if(!(extrasBundle.isEmpty())){
                url = extrasBundle.get("url").toString();
                Log.d("URL Exists", url);
                mWebView.setWebViewClient(new WebViewClient());
                mWebView.loadUrl(url);
            }
        }
}
