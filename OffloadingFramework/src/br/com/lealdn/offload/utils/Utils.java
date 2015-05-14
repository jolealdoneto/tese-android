package br.com.lealdn.offload.utils;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Utils {
	final static Pattern methodSignaturePattern = Pattern.compile("<([a-zA-Z.$#]+): ([a-zA-Z.$#]+) ([a-zA-Z.$#]+)\\(([a-zA-Z.$#,]*)\\)>");
	final static Pattern fieldSignaturePattern = Pattern.compile("<([a-zA-Z.$#-]+): ([a-zA-Z.$#]+) ([a-zA-Z.$#]+)>");

	private static String[] getGroupsFromSignature(final String methodSignature) throws ClassNotFoundException {
		final Matcher matcher = methodSignaturePattern.matcher(methodSignature);
		if (matcher != null && matcher.find()) {
			final List<String> groups = new ArrayList<String>();
			for (int i = 0; i <= matcher.groupCount(); i++) {
				groups.add(matcher.group(i));
			}
			return groups.toArray(new String[groups.size()]);
		}
		return null;
	}

	public static Method getMethodFromSignature(final Class<?> rootClass, final String methodSignature) throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		final String[] matcher = getGroupsFromSignature(methodSignature);
		if (matcher != null) {
			final String methodName = matcher[3];
			final String args = matcher[4];
			final List<Class<?>> argsClassList = new ArrayList<Class<?>>();
			if (args.length() > 0) {
				for (final String argClass : args.split(",")) {
					final Class<?> clazz = getClassForName(argClass);
					argsClassList.add(clazz);
				}
			}

			return rootClass.getMethod(methodName, argsClassList.toArray(new Class[argsClassList.size()]));
		}
		return null;
	}

	public static Class<?> getClassForName(final String name) throws ClassNotFoundException {
		if ("int".equals(name)) {
			return int.class;
        }
		if ("double".equals(name)) {
			return double.class;
        }
		if ("long".equals(name)) {
			return long.class;
        }
		if ("float".equals(name)) {
			return float.class;
        }
		if ("char".equals(name)) {
			return char.class;
        }
		if ("byte".equals(name)) {
			return byte.class;
        }
		if ("short".equals(name)) {
			return short.class;
        }
        return Class.forName(name);
	}

	public static Class getClassFromSignature(final String methodSignature) throws ClassNotFoundException {
		final String[] matcher = getGroupsFromSignature(methodSignature);
		if (matcher != null) {
			final String className = matcher[1];
			final Class<?> clazz = Class.forName(className);
			return clazz;
		}
		return null;
	}

	public static double calculateBandwidth(final long startTime, final long endTime, final long size) {
		System.out.println("s: " + size + " | " + (endTime - startTime));
		return (double)size / (endTime - startTime);
	}
}
