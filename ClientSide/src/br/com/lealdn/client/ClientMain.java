package br.com.lealdn.client;

import junit.framework.Test;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ClientMain extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_main);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				final TestingClass tc = new TestingClass();
				Log.d("OFFLOADING", "Calling testMe..");
				try {
					final int res = tc.testMe();
					Log.d("OFFLOADING", "RESULT: " + String.valueOf(res));
					ClientMain.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							final TextView text = (TextView)ClientMain.this.findViewById(R.id.text);
							text.setText("Res is: "+ res);	
						}
					});
					
				} catch(Exception e) {
					Log.e("OFFLOADING", e.getMessage());
				}
			}
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.client_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
