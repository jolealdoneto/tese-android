package br.com.lealdn.server;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class ServerActivity extends ActionBarActivity {
	public static final String LOGPREFIX = "SERVER";
	
	public static void debug(String msg) {
		Log.d(LOGPREFIX, msg);
	}
	public static void error(String msg) {
		Log.e(LOGPREFIX, msg);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Log.d("OFFLOADING", "HIHUIDAFHUIAIU");
				try {
					ServerService ss = new ServerService();
					ss.start();
				} catch(Exception ex) {
					Log.e("OFFLOADING", ex.getMessage());
				}
			}
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.server, menu);
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
