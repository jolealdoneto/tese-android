package br.com.lealdn.offload;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import br.com.lealdn.offload.RelevantParameter;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import com.google.gson.Gson;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


public class ExecutionManager {
	private Map<String, MethodExecution> executions;
	private SharedPreferences preferences;
	private final String EX_MANAGER_KEY = "executionManagerKey";
	private final String CPU_USAGE_KEY = "cpuUsageKey";
	private Gson gson = new Gson();
	
	public ExecutionManager(final SharedPreferences preferences) {
		this.preferences = preferences;
		this.initialize();
	}
	
	private void initialize() {
		if (executions == null) {			
			if ("".equals(this.preferences.getString(EX_MANAGER_KEY, ""))) {
				this.executions = new HashMap<String, MethodExecution>();
			}
			else {
				try {
					this.executions = gson.fromJson(EX_MANAGER_KEY, HashMap.class);
				} catch(Exception ex) {
					this.executions = new HashMap<String, MethodExecution>();
				}
			}
			// TODO MUST SAVE!
		}
	}
	
	public Map<String, MethodExecution> getExecutions() {
		return this.executions;
	}
	
	public void saveExecutions() {
		final Editor editor = this.preferences.edit();
		editor.putString(EX_MANAGER_KEY, gson.toJson(this.executions));
		editor.commit();
	}
	
	public String getMethodSignature(final Method method) {
		return "<"+ method.getDeclaringClass().getName()+ ": " + method.getReturnType().toString() + " " + method.getName() + "(" + parametersAsString(method, true) + ")>";
	}
	
	private String parametersAsString ( Method method, boolean longTypeNames ) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		if ( parameterTypes.length == 0 ) return "";
		StringBuilder paramString = new StringBuilder();
		paramString.append(longTypeNames ? parameterTypes[0].getName()
				: parameterTypes[0].getSimpleName());
		for ( int i = 1 ; i < parameterTypes.length ; i++ )
		{
			paramString.append(",").append(
					longTypeNames  ? parameterTypes[i].getName()
							: parameterTypes[i].getSimpleName());
		}
		return paramString.toString();
	}
	
	public int getMethodRuntimeCount(final Method method, final boolean local) {
		MethodExecution executionRounds = this.executions.get(getMethodSignature(method));
		if (executionRounds != null) {
			return local ? executionRounds.localRounds.size() : executionRounds.remoteRounds.size();
		}
		else {
			return 0;
		}
	}
	
	public void updateMethodRuntimeAssessment(final Method method, final boolean local, final long time, final Object[] args, final long rxTxBytes) {
		final String signature = getMethodSignature(method);
		final double assessment = calculateAssessment(method, args);
		
		MethodExecution executionRounds = this.executions.get(signature);
		if (executionRounds == null) {
			executionRounds = new MethodExecution(signature);
		}
		
		executionRounds.addRound(new ExecutionRound(signature, assessment, this.normalizeTimeAccordingToCPULoad(time), local, rxTxBytes));
		this.executions.put(signature, executionRounds);
	}
	
	public long normalizeTimeAccordingToCPULoad(final long time) {
		final double cpuUsage = this.getCPUUsage();
		return Math.round(time*(1-cpuUsage));
//		if (cpuUsage == 0) {
//			return time;
//		}
//		
//		return Math.round(time * ((100-cpuUsage)/(double)100));
	}

	public boolean canInterpolateAssessment(final Method method, final Object[] args, final boolean local) {
		final double assessment = calculateAssessment(method, args);
		final MethodExecution execution = this.executions.get(getMethodSignature(method));
		if (execution != null) {
			return execution.canInterpolate(assessment, local);
		}
		else {
			return false;
		}
	}
	
	public Double[] interpolateAssessment(final Method method, final Object[] args, final boolean local) {
		final double assessment = calculateAssessment(method, args);
		final MethodExecution execution = this.executions.get(getMethodSignature(method));
		if (execution != null) {
			return new Double[] { execution.interpolate(assessment, local), execution.interpolateRxTx(assessment, local) };
		}
		else {
			return null;
		}
	}

	private boolean isCompletelyNull(final RelevantParameter[] relevantParameters) {
		for (RelevantParameter par : relevantParameters) {
			if (par != null) {
				return false;
			}
		}
		return true;
	}

    private double calculateAssessment(final Method method, final Object[] args) {
    	final RelevantParameter[] relevantParameters = new RelevantParameter[args.length];
    	int i = 0;
        for (Annotation[] annotations : method.getParameterAnnotations()) {
        	relevantParameters[i] = null;
        	if (annotations.length > 0) {
        		for (Annotation annotation : annotations) {
        			if (annotation instanceof RelevantParameter) {
        				relevantParameters[i] = (RelevantParameter)annotation;
        			}
        		}
        	}
        	i++;
        }
        
        final double[] weights = new double[args.length];
        if (isCompletelyNull(relevantParameters)) {
        	for (int index = 0; index < weights.length; index++) {
        		weights[index] = 1D/weights.length;
        	}	
        }
        else {
        	for (int index = 0; index < weights.length; index++) {
        		if (relevantParameters[index] != null) {
        			weights[index] = relevantParameters[index].weight();
        		}
        		else {
        			weights[index] = 0;
        		}
        	}
        }
        return assessArgs(weights, args);
    }
    
    private double assessArgs(final double[] weights, final Object[] args) {
    	double calculation = 0;
    	for (int i = 0; i < weights.length; i++) {
    		final double weight = weights[i];
    		final double assessment = assessArg(args[i]);
    		calculation += weight * assessment;
    	}
    	return calculation;
    }
    
    private boolean isClassOrSuperclass(Class<?> clazz, Class<?> superclass) {
    	return clazz == superclass || superclass.isAssignableFrom(clazz) || clazz.isInstance(superclass);
    }
    
    private double assessArg(final Object arg) {
    	Class<?> clazz = arg.getClass();
    	if (clazz == Integer.class || clazz == int.class) {
    		return ((Integer)arg).doubleValue();
    	}
    	if (clazz == Double.class || clazz == double.class) {
    		return (Double)arg;
    	}
    	if (clazz == Long.class || clazz == long.class) {
    		return ((Long)arg).doubleValue();
    	}
    	if (clazz == Short.class || clazz == short.class) {
    		return ((Short)arg).doubleValue();
    	}
    	if (clazz == String.class) {
    		return ((String)arg).length();
    	}
    	if (isClassOrSuperclass(clazz, Collection.class)) {
    		return ((Collection<?>)arg).size();
    	}
    	return 1;
    }
	
    public class MethodExecution {
    	public final String methodSignature;
    	public final Set<ExecutionRound> localRounds;
    	public final Set<ExecutionRound> remoteRounds;
    	public PolynomialSplineFunction localSpline;
    	public PolynomialSplineFunction remoteSpline;
    	public PolynomialSplineFunction localSplineRxTx;

    	public Comparator<ExecutionRound> comparator = new Comparator<ExecutionRound>() {
			@Override
			public int compare(ExecutionRound lhs, ExecutionRound rhs) {
				return ((Double)lhs.assessment).compareTo(rhs.assessment);
			}
		};
    	
    	public MethodExecution(final String methodSignature) {
    		this.methodSignature = methodSignature;
    		this.localRounds = new TreeSet<ExecutionRound>(comparator);
    		this.remoteRounds = new TreeSet<ExecutionRound>(comparator);
    	}
    	
    	public void addRound(final ExecutionRound round) {
    		if (round.local) {
    			this.localRounds.remove(round);
    			this.localRounds.add(round);
    		} else {
    			this.remoteRounds.remove(round);
    			this.remoteRounds.add(round);
    		}
    		updateSpline(round.local);
    	}
    	
    	public boolean canInterpolate(final double assessment, final boolean local) {
    		final PolynomialSplineFunction spline = local ? localSpline : remoteSpline;
    		if (spline != null) {
    			return spline.isValidPoint(assessment);
    		}
    		return false;
    	}
    	public Double interpolate(final double assessment, final boolean local) {
    		final PolynomialSplineFunction spline = local ? localSpline : remoteSpline;
    		if (spline != null) {
    			return spline.value(assessment);
    		}
    		return null;
    	}
    	public Double interpolateRxTx(final double assessment, final boolean local) {
    		if (!local) {
    			return null;
    		}
    		if (localSplineRxTx != null) {
    			return localSplineRxTx.value(assessment);
    		}
    		return null;
    	}
    	
    	private void updateSpline(boolean local) {
    		if (local) {
    			this.localSpline = getSpline(localRounds);
    			this.localSplineRxTx = getSplineRxTx(localRounds);
    		}
    		else {
    			this.remoteSpline = getSpline(remoteRounds);
    		}
    	}
    	
    	private PolynomialSplineFunction getSpline(Set<ExecutionRound> executionRounds) {
    		if (executionRounds.size() >= 5) {
	    		final double[] assessments = new double[executionRounds.size()];
	    		final double[] times = new double[executionRounds.size()];
	    		int i = 0;
	    		for (Iterator<ExecutionRound> it = executionRounds.iterator(); it.hasNext();) {
	    			final ExecutionRound round = it.next();
	    			assessments[i] = round.assessment;
	    			times[i] = round.time;
	    			i++;
	    		}
	    		return new AkimaSplineInterpolator().interpolate(assessments, times);
    		}
    		else {
    			return null;
    		}
    	}

    	private PolynomialSplineFunction getSplineRxTx(Set<ExecutionRound> executionRounds) {
    		if (executionRounds.size() >= 5) {
	    		final double[] assessments = new double[executionRounds.size()];
	    		final double[] times = new double[executionRounds.size()];
	    		int i = 0;
	    		for (Iterator<ExecutionRound> it = executionRounds.iterator(); it.hasNext();) {
	    			final ExecutionRound round = it.next();
	    			assessments[i] = round.assessment;
	    			times[i] = round.rxTxBytes;
	    			i++;
	    		}
	    		return new AkimaSplineInterpolator().interpolate(assessments, times);
    		}
    		else {
    			return null;
    		}
    	}
    }

	class ExecutionRound {
		public final String methodSignature;
		public final double assessment;
		public final long time;
		public final boolean local;
		public final long rxTxBytes;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			long temp;
			temp = Double.doubleToLongBits(assessment);
			result = prime * result + (int) (temp ^ (temp >>> 32));
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
			ExecutionRound other = (ExecutionRound) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (Double.doubleToLongBits(assessment) != Double
					.doubleToLongBits(other.assessment))
				return false;
			return true;
		}

		public ExecutionRound(final String methodSignature, final double assessment, final long time, final boolean local, final long rxTxBytes) {
			this.methodSignature = methodSignature;
			this.assessment = assessment;
			this.time = time;
			this.local = local;
			this.rxTxBytes = rxTxBytes;
		}

		private ExecutionManager getOuterType() {
			return ExecutionManager.this;
		}
		
		public String toString() {
			return this.assessment + " | " + this.time;
		}
	}
	
	public void updateCPUUsage(final double usage) {
		final Editor editor = this.preferences.edit();
		editor.putFloat(CPU_USAGE_KEY, (float)usage);
		editor.commit();
	}
	
	public double getCPUUsage() {
		return this.preferences.getFloat(CPU_USAGE_KEY, 0);
	}
}