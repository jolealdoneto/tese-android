package br.com.lealdn.server;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class ExecuteMethod {
    final static Pattern methodSignaturePattern = Pattern.compile("<([a-zA-Z.$#]+): ([a-zA-Z.$#]+) ([a-zA-Z.$#]+)\\(([a-zA-Z.$#,]*)\\)>");
    final static Pattern fieldSignaturePattern = Pattern.compile("<([a-zA-Z.$#-]+): ([a-zA-Z.$#]+) ([a-zA-Z.$#]+)>");
    final static Kryo kryo = new Kryo();

    public static Object executeMethod(final byte[] bytes) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final Input input = new Input(bytes);
        final String methodSignature = kryo.readObject(input, String.class);
        final Map<Object, Object> vars = kryo.readObject(input, HashMap.class);
        input.close();

        setStaticFieldsPublic(vars);
        return invokeMethod(methodSignature, vars);
    }
    
    public static ByteArrayOutputStream serializeResult(final Object result) {
    	final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        final Output output = new Output(baos, 1024);
        final Map<Object, Object> mapResult = new HashMap<Object, Object>();
        mapResult.put("r", result);
        kryo.writeObject(output, mapResult);
        output.close();
        
        return baos;
    }

    private static void setStaticFieldsPublic(final Map<Object, Object> vars)
            throws ClassNotFoundException, NoSuchFieldException,
            SecurityException, IllegalArgumentException, IllegalAccessException {
        for (final Object keyObj : vars.keySet()) {
            final String keyName = (String)keyObj;
            if (keyName.startsWith("field-")) {
                final String fieldSig = keyName.split("-")[1];
                final Matcher matcher = fieldSignaturePattern.matcher(fieldSig);
                matcher.find();
                final Class<?> clazz = Class.forName(matcher.group(1));
                final Field field = clazz.getDeclaredField(matcher.group(3));
                field.setAccessible(true);
                field.set(null, vars.get(keyObj));
            }
        }
    }

    private static Object invokeMethod(final String methodSignature, final Map<Object, Object> vars) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final Class<?> clazz = getClassFromSignature(methodSignature);
        final Method method = getMethodFromSignature(clazz, methodSignature);
        final List<Object> argumentList = new ArrayList<>();

        for (int i = 0; i < method.getParameterTypes().length; i++) {
            argumentList.add(vars.get("@arg" + i));
        }
        return method.invoke(vars.get("@this"), argumentList.toArray(new Object[argumentList.size()]));
    }

    private static String[] getGroupsFromSignature(final String methodSignature) throws ClassNotFoundException {
        final Matcher matcher = methodSignaturePattern.matcher(methodSignature);
        if (matcher != null && matcher.find()) {
            final List<String> groups = new ArrayList<>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                groups.add(matcher.group(i));
            }
            return groups.toArray(new String[groups.size()]);
        }
        return null;
    }

    private static Method getMethodFromSignature(final Class<?> rootClass, final String methodSignature) throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        final String[] matcher = getGroupsFromSignature(methodSignature);
        if (matcher != null) {
            final String methodName = matcher[3];
            final String args = matcher[4];
            final List<Class<?>> argsClassList = new ArrayList<>();
            if (args.length() > 0) {
	            for (final String argClass : args.split(",")) {
	                final Class<?> clazz = Class.forName(argClass);
	                argsClassList.add(clazz);
	            }
            }

            return rootClass.getMethod(methodName, argsClassList.toArray(new Class[argsClassList.size()]));
        }
        return null;
    }

    private static Class getClassFromSignature(final String methodSignature) throws ClassNotFoundException {
        final String[] matcher = getGroupsFromSignature(methodSignature);
        if (matcher != null) {
            final String className = matcher[1];
            final Class<?> clazz = Class.forName(className);
            return clazz;
        }
        return null;
    }
}
