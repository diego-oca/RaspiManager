package com.diegooca.raspimanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.util.Properties;

public class CommandsActivity extends Activity {

    private String TAG = "CommandsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commands);

        //Reset button handler
        Button btnReset = (Button) findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {

                new AlertDialog.Builder(CommandsActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.reset_title)
                        .setMessage(R.string.reset_message)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SingleCommandAsync resetCommand=new SingleCommandAsync(getConnectionInfo(), "reboot");
                                resetCommand.execute(null);
                            }

                        })
                        .setNegativeButton(R.string.no, null)
                        .show();

            }
        });

        //Shutdown button handler
        Button btnShutdown = (Button) findViewById(R.id.btn_shutdown);
        btnShutdown.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {

                new AlertDialog.Builder(CommandsActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.shutdown_title)
                        .setMessage(R.string.shutdown_message)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SingleCommandAsync resetCommand=new SingleCommandAsync(getConnectionInfo(), "halt");
                                resetCommand.execute(null);
                            }

                        })
                        .setNegativeButton(R.string.no, null)
                        .show();

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.commands, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ConnectionInfo getConnectionInfo() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        ConnectionInfo cn = new ConnectionInfo(
                sharedPref.getString("url", ""),
                sharedPref.getString("user", ""),
                sharedPref.getString("password", "")
        );

        return cn;
    }

    private class ConnectionInfo {
        public String _url;
        public String _user;
        public String _password;

        public ConnectionInfo(String url, String user, String password) {
            _url = url;
            _user = user;
            _password = password;
        }

        @Override
        public String toString() {
            return _user + ":" + _password + "@" + _url;
        }
    }

    /* AsyncTask subclass for executing single commands via SSH */
    private class SingleCommandAsync extends AsyncTask<String, Void, Void> {

        private ProgressDialog _dialog;
        private String _command;
        private ConnectionInfo _cn;
        private String _resultMessage = "";

        public SingleCommandAsync(ConnectionInfo cn, String command) {
            _command = command;
            _cn = cn;
        }

        protected void onPreExecute() {
            _dialog = ProgressDialog.show(CommandsActivity.this, getString(R.string.btn_reset), getString(R.string.msg_wait), false);
            _dialog.show();
        }

        protected Void doInBackground(final String... args) {
            _dialog.setMessage(getString(R.string.msg_connecting));

            JSch jsch = new JSch();
            Session session;

            try {
                session = jsch.getSession(_cn._user,_cn._url, 22);
                session.setPassword(_cn._password);

                // Avoid asking for key confirmation
                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);

                Log.i(TAG, "Connecting to '" + _cn.toString() + "'");
                session.connect();

                Log.i(TAG, "Sending command '" + _command + "'");
                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(_command);
                channel.connect();
                channel.disconnect();
                session.disconnect();
            } catch (Exception ex) {
                Log.i(TAG, ex.getMessage());
                _resultMessage = "Error: " + ex.getMessage();
            }

            return null;
        }

        protected void onPostExecute(final Void unused) {
            if (_dialog.isShowing()) {
                _dialog.dismiss();
            }

            if (_resultMessage.length() > 0) {
                new AlertDialog.Builder(CommandsActivity.this)
                        .setTitle(getString(R.string.btn_reset))
                        .setMessage(_resultMessage)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, null)
                        .create().show();
            }
        }
    }

}
