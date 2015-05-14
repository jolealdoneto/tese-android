package br.com.lealdn.client;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import br.com.lealdn.offload.Intercept;
import br.com.lealdn.offload.OffloadCandidate;
import br.com.lealdn.offload.RelevantParameter;

public class TestingClass {

	/*
	public int execute(int n) {
		final Map<Object, Object> sendArgs = new HashMap<Object, Object>();
		sendArgs.put("@this", this);
		sendArgs.put("@arg0", n);
		final boolean should = Intercept.shouldOffload("<br.com.lealdn.client.TestingClass: int fibonacciRecusion(java.lang.Integer)>", sendArgs);
		if (should) {
			final Object result = Intercept.sendAndSerialize("<br.com.lealdn.client.TestingClass: int fibonacciRecusion(java.lang.Integer)>", sendArgs);
			Log.d("RESULT", result.toString());
			return (Integer)result;
		}
		final long startTime = System.currentTimeMillis();
		final int fib = fibonacciRecusion(n);
		Intercept.updateMethodRuntime("<br.com.lealdn.client.TestingClass: int fibonacciRecusion(java.lang.Integer)>", startTime, sendArgs);
		return fib;
	}
	*/
	
	@OffloadCandidate
	public int fibonacciRecusion(@RelevantParameter(weight=1) Integer number){
		return doFib(number);
    }

	public int doFib(Integer number){
        if(number == 1 || number == 2){
            return 1;
        } 
        return doFib(number-1) + doFib(number -2); //tail recursion
    }
}
