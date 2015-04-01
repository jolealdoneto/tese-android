package br.com.lealdn.client;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import br.com.lealdn.offload.Intercept;
import br.com.lealdn.offload.OffloadCandidate;

public class TestingClass {

	
	/*public int testMe2() {
		final Map<Object, Object> sendArgs = new HashMap<Object, Object>();
		sendArgs.put("@this", this);
		final Object result = Intercept.sendAndSerialize("<br.com.lealdn.client.TestingClass: int testMe()>", sendArgs);
		Log.d("RESULT", result.toString());
		return 1;
	}*/
	
	@OffloadCandidate
	public int testMe() {
		return 1;
	}
}
