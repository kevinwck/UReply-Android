package com.ureply.deep.ureply;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity;
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "preferences";
    private static final String PREF_SNAME = "studentname";
    private static final String PREF_SID = "studentid";

    private final String defaultId = "";
    private final String defaultName = "";
    private String studentName;
    private String studentId;
    private String sessionNumber;

    CheckBox checkBox;
    EditText sessionNumberField;
    EditText studentNameField;
    EditText studentIDField;
    ProgressDialog pd;

    private static final String TAG = "ReactiveNetwork";
    private Disposable networkDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBox = (CheckBox)findViewById(R.id.checkBox);

        studentNameField = (EditText) findViewById(R.id.studentname);
        studentIDField = (EditText) findViewById(R.id.studentid);
        sessionNumberField = (EditText) findViewById(R.id.sessionid);

        if (checkBox.isChecked()) {
            savePreferences();
        } else {

        }

        Button loginButton = (Button)findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(MainActivity.this, WebviewActivity.class));
                loginButtonPressed();
            }
        });
    }

    public void loginButtonPressed(){
        studentName = studentNameField.getText().toString();
        studentId = studentIDField.getText().toString();
        sessionNumber = sessionNumberField.getText().toString();

        // Do more checking here
        // Do Checking when edit starts to make Student name or id (Required), and use it here
        if (sessionNumber.matches("")) {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Please Enter a valid Session Number")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
            return;
        }else{
            sessionNumber = sessionNumber.toUpperCase();
            new JsonTask().execute("http://ureplydev5.ureply.mobi/sessionCheck.php");
        }
    }

    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);
                }
                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }

            if(result != null) {
                Log.d("result", result);
                try {
                    JSONObject json = new JSONObject(result);
                    if (json.has(Character.toString(sessionNumber.charAt(0)))) {
                        Log.d("result", "YES");
                        JSONObject jsonPods = json.getJSONObject((Character.toString(sessionNumber.charAt(0))));
                        Log.d("result", jsonPods.getString("url"));
                        //put if L
                        Intent intentBundle = new Intent(MainActivity.this, WebviewActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("url", jsonPods.getString("url"));
                        intentBundle.putExtras(bundle);
                        startActivity(intentBundle);
                    } else {
                        Log.d("result", "else");
                        JSONObject jsonPods = json.getJSONObject("else");
                        Log.d("result", jsonPods.getString("url"));
                        Intent intentBundle = new Intent(MainActivity.this, WebviewActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("url", jsonPods.getString("url"));
                        intentBundle.putExtras(bundle);
                        startActivity(intentBundle);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{
                Log.d("Error", "No JSON Data Found");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //savePreferences();
        safelyDispose(networkDisposable);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPreferences();
        networkDisposable = ReactiveNetwork.observeNetworkConnectivity(getApplicationContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Connectivity>() {
                    @Override public void accept(final Connectivity connectivity) {
                        // do something with connectivity
                        // you can call connectivity.getState();
                        // connectivity.getType(); or connectivity.toString();
                        if(connectivity.getState() == NetworkInfo.State.DISCONNECTED) {
                            Log.d("Connectivity", "Connection Lost");
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("You are not connected to the Internet");
                            builder.setCancelable(false);

                            builder.setPositiveButton(
                                    "Ok",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });

                            AlertDialog alert11 = builder.create();
                            alert11.show();
                        } else if(connectivity.getState() == NetworkInfo.State.CONNECTED) {
                            Log.d("Connectivity", "Connected");
                                if(connectivity.getSubTypeName().equals("LTE")){
                                    Log.d("Connectivity", "LTE");
                                }else if(connectivity.getSubTypeName().equals("WIFI")){
                                Log.d("Connectivity", "WIFI");
                            }
                        }
                        Log.d("Connectivity", connectivity.toString());
                    }
                });
    }

    private void safelyDispose(Disposable... disposables) {
        for (Disposable subscription : disposables) {
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
            }
        }
    }

    private void savePreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        // Edit and commit
        studentName = studentNameField.getText().toString();;
        studentId = studentIDField.getText().toString();;
        System.out.println("onPause save name: " + studentName);
        System.out.println("onPause save password: " + studentId);
        editor.putString(PREF_SNAME, studentName);
        editor.putString(PREF_SID, studentId);
        editor.commit();
    }


    private void loadPreferences() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);

        // Get value
        studentName = settings.getString(PREF_SNAME, defaultName);
        studentId = settings.getString(PREF_SID, defaultId);
        studentNameField.setText(studentName);
        studentIDField.setText(studentId);
        System.out.println("onResume load name: " + studentName);
        System.out.println("onResume load password: " + studentId);
    }
}
