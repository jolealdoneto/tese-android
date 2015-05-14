package br.com.lealdn.offload;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;

import android.content.SharedPreferences;

public class LogManager {
	private Set<LogEntry> log = new TreeSet<LogEntry>(new Comparator<LogEntry>() {
		@Override
		public int compare(LogEntry arg0, LogEntry arg1) {
			if (arg1 == null)
				return 0;
			return ((Long)arg1.time).compareTo(arg0.time);
		}
	});
	
	public LogManager() {
	}
	
	public void addToLog(final String methodSignature, final boolean shouldOffload) {
		final LogEntry newLog = new LogEntry(methodSignature, shouldOffload, System.currentTimeMillis());
		this.log.add(newLog);
	}
	
	public Set<LogEntry> getLog() { 
		return this.log;
	}
	
	public class LogEntry {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (time ^ (time >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LogEntry other = (LogEntry) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (time != other.time)
				return false;
			return true;
		}
		public LogEntry(final String methodSignature, final boolean shouldOffload, final long time) {
			this.methodSignature = methodSignature;
			this.shouldOffload = shouldOffload;
			this.time = time;
		}

		public final String methodSignature;
		public final boolean shouldOffload;
		public final long time;
		private LogManager getOuterType() {
			return LogManager.this;
		}
	}

}
