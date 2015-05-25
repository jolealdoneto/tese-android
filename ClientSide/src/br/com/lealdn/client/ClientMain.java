package br.com.lealdn.client;

import java.util.Map;

import br.com.lealdn.offload.ExecutionManager.MethodExecution;
import br.com.lealdn.offload.OffloadingManager;
import br.com.lealdn.offload.RTTService;
import br.com.lealdn.offload.utils.Utils;
import junit.framework.Test;
import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ClientMain extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	
		OffloadingManager.initialize(this);
		
		Button button = (Button)findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final TestingClass tc = new TestingClass();
				Log.d("OFFLOADING", "Calling testMe..");
				try {
					final EditText number = (EditText)findViewById(R.id.editText1);
					final int fib = Integer.parseInt(number.getText().toString());
					final int res = tc.fibonacciRecusion(fib);
					Log.d("OFFLOADING", "RESULT: " + String.valueOf(res));
					ClientMain.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							final TextView text = (TextView)ClientMain.this.findViewById(R.id.result);
							text.setText("Res is: "+ res);	

							Map<String, MethodExecution> executions = OffloadingManager.getExecutionManager().getExecutions();
							final MethodExecution me = executions.get("<br.com.lealdn.client.TestingClass: int fibonacciRecusion(java.lang.Integer)>");
							if (me != null) {
								final EditText etext = (EditText)findViewById(R.id.editText2);
								etext.setText(me.localRounds.toString());
								final EditText etextRemote = (EditText)findViewById(R.id.editText3);
								etextRemote.setText(me.remoteRounds.toString());
							}

							final TextView tv1 = (TextView)findViewById(R.id.textView1);
							if (OffloadingManager.getLogManager().getLog().iterator().hasNext()) {
								tv1.setText(String.valueOf(OffloadingManager.getLogManager().getLog().iterator().next().shouldOffload));
							}
						}
					});

				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		OffloadingManager.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		OffloadingManager.onPause();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		OffloadingManager.onStop();
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
