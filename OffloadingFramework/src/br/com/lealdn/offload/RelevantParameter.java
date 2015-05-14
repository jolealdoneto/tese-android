package br.com.lealdn.offload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RelevantParameter {
	public double weight();
}
