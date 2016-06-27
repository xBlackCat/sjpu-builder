package org.xblackcat.sjpu.builder;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 30.06.2014 12:45
 *
 * @author xBlackCat
 */
public class BuilderUtils {
    public static final CtClass[] EMPTY_LIST = new CtClass[]{};

    /**
     * Returns full qualified name of the class in java-source form: inner class names separates with dot ('.') instead of dollar sign ('$')
     *
     * @param clazz class to get FQN
     * @return full qualified name of the class in java-source form
     */
    public static String getName(Class<?> clazz) {
        return StringUtils.replaceChars(checkArray(clazz), '$', '.');
    }

    public static String getName(Type type) {
        if (type instanceof Class) {
            return getName((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            StringBuilder name = new StringBuilder();
            final ParameterizedType pt = (ParameterizedType) type;
            name.append(getName(pt.getRawType()));
            if (pt.getActualTypeArguments().length > 0) {
                final String params = Stream.of(pt.getActualTypeArguments())
                        .map(BuilderUtils::getName)
                        .collect(Collectors.joining(",", "<", ">"));
                name.append(params);
            }
            return name.toString();
        } else if (type instanceof GenericArrayType) {
            final GenericArrayType at = (GenericArrayType) type;
            StringBuilder name = new StringBuilder();
            name.append(getName(at.getGenericComponentType()));
            name.append("[]");
            return name.toString();
        } else {
            return type.toString();
        }
    }

    protected static String checkArray(Class<?> clazz) {
        if (!clazz.isArray()) {
            return clazz.getName();
        }

        return checkArray(clazz.getComponentType()) + "[]";
    }

    /**
     * Returns full qualified name of the class in java-source form: inner class names separates with dot ('.') instead of dollar sign ('$')
     *
     * @param clazz class to get FQN
     * @return full qualified name of the class in java-source form
     */
    public static String getName(CtClass clazz) {
        return StringUtils.replaceChars(clazz.getName(), '$', '.');
    }

    public static String getUnwrapMethodName(CtClass returnType) {
        if (!returnType.isPrimitive()) {
            throw new GeneratorException("Can't build unwrap method for non-primitive class.");
        }

        if (CtClass.booleanType.equals(returnType)) {
            return "booleanValue";
        }
        if (CtClass.byteType.equals(returnType)) {
            return "byteValue";
        }
        if (CtClass.doubleType.equals(returnType)) {
            return "doubleValue";
        }
        if (CtClass.floatType.equals(returnType)) {
            return "floatValue";
        }
        if (CtClass.intType.equals(returnType)) {
            return "intValue";
        }
        if (CtClass.longType.equals(returnType)) {
            return "longValue";
        }
        if (CtClass.shortType.equals(returnType)) {
            return "shortValue";
        }

        throw new GeneratorException("Unsupported primitive type: " + returnType);
    }

    public static CtClass[] toCtClasses(ClassPool pool, Class<?>... classes) throws NotFoundException {
        CtClass[] ctClasses = new CtClass[classes.length];

        int i = 0;
        int classesLength = classes.length;

        while (i < classesLength) {
            ctClasses[i] = pool.get(getName(classes[i]));
            i++;
        }

        return ctClasses;
    }

    public static CtClass toCtClass(ClassPool pool, Class<?> clazz) throws NotFoundException {
        return pool.get(getName(clazz));
    }

    public static String asIdentifier(Class<?> typeMap) {
        return StringUtils.replaceChars(getName(typeMap), '.', '_');
    }

    public static String asIdentifier(Method mm) {
        return mm.getName() + "_" + Integer.toHexString(mm.toGenericString().hashCode());
    }

    public static ClassPool getClassPool(ClassPool parent, Class<?> clazz, Class<?>... classes) {
        ClassPool pool = new ClassPool(parent) {
            @Override
            public ClassLoader getClassLoader() {
                return parent.getClassLoader();
            }
        };

        Set<ClassLoader> usedLoaders = new HashSet<>();
        usedLoaders.add(ClassLoader.getSystemClassLoader());
        usedLoaders.add(ClassPool.class.getClassLoader());

        if (usedLoaders.add(clazz.getClassLoader())) {
            pool.appendClassPath(new ClassClassPath(clazz));
        }

        for (Class<?> c : classes) {
            if (usedLoaders.add(c.getClassLoader())) {
                pool.appendClassPath(new ClassClassPath(c));
            }
        }

        return pool;
    }

    public static Class<?> getClass(String fqn, ClassPool pool) throws ClassNotFoundException {
        return Class.forName(fqn, true, pool.getClassLoader());
    }

    public static Class<?> substituteTypeVariables(Map<TypeVariable<?>, Class<?>> map, Type typeToResolve) {
        if (typeToResolve instanceof Class<?>) {
            return (Class<?>) typeToResolve;
        } else if (typeToResolve instanceof ParameterizedType) {
            return substituteTypeVariables(map, ((ParameterizedType) typeToResolve).getRawType());
        } else if (typeToResolve instanceof TypeVariable<?>) {
            final TypeVariable<?> typeVariable = (TypeVariable<?>) typeToResolve;
            final Class<?> aClass = map.get(typeVariable);
            if (aClass != null) {
                return aClass;
            }

            final Type[] bounds = typeVariable.getBounds();
            if (bounds.length > 0) {
                return substituteTypeVariables(map, bounds[0]);
            }

            return Object.class;
        }

        return null;
    }

    /**
     * Method for resolving classes for all available type variables for the given type
     *
     * @param type querying type
     * @return map with existing type variables as keys with {@linkplain Class} object if the target class is resolved.
     */
    public static Map<TypeVariable<?>, Class<?>> resolveTypeVariables(Type type) {
        final HashMap<TypeVariable<?>, Type> result = new HashMap<>();
        collectTypeVariables(result, type);
        final Map<TypeVariable<?>, Class<?>> map = new HashMap<>();

        for (TypeVariable<?> tv : result.keySet()) {
            TypeVariable<?> key = tv;
            Type resolved;
            do {
                resolved = result.get(key);
                if (resolved instanceof TypeVariable<?>) {
                    key = (TypeVariable<?>) resolved;
                }
            } while (resolved instanceof TypeVariable<?>);
            final Class<?> resolvedClass;
            if (resolved instanceof Class<?>) {
                resolvedClass = (Class<?>) resolved;
            } else if (resolved instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) resolved;
                resolvedClass = (Class<?>) pt.getRawType();
            } else {
                continue;
            }
            map.put(tv, resolvedClass);
        }

        return map;
    }

    private static void collectTypeVariables(Map<TypeVariable<?>, Type> result, Type type) {
        if (type == null) {
            return;
        }
        if (type instanceof Class<?>) {
            final Class aClass = (Class) type;
            collectTypeVariables(result, aClass.getGenericSuperclass());
            for (Type i : aClass.getGenericInterfaces()) {
                collectTypeVariables(result, i);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            final Type[] tArgs = pType.getActualTypeArguments();
            final Type rawType = pType.getRawType();
            if (rawType instanceof Class<?>) {
                final TypeVariable[] tVar = ((Class) rawType).getTypeParameters();
                if (tArgs.length == tVar.length) {
                    for (int i = 0; i < tArgs.length; i++) {
                        result.put(tVar[i], tArgs[i]);
                    }
                }
                collectTypeVariables(result, rawType);
            }
        }
    }

    /**
     * Search method in specified class by signature of method.
     *
     * @param root class to search method in
     * @param m    method as signature source.
     * @return method of the specified class with the same signature or null if the class has no method with the signature
     */
    public static Method findDeclaredMethod(Class<?> root, Method m) {
        return BuilderUtils.findDeclaredMethod(root, root, m);
    }

    /**
     * Search method in specified class by signature of method. Search through superclasses till specified super class (exclude).
     *
     * @param root           class to search method in and its superclasses
     * @param tillSuperClass super class of root as bound for search. <code>null</code> value allows search through all
     *                       superclass hierarchy of root class. To search
     * @param m              method as signature source.
     * @return method of the specified class with the same signature or null if the class has no method with the signature
     */
    public static Method findDeclaredMethod(Class<?> root, Class<?> tillSuperClass, Method m) {
        try {
            // Check non-public abstract method for implementation in the root class
            return root.getDeclaredMethod(m.getName(), m.getParameterTypes());
        } catch (NoSuchMethodException e) {
            // Method is not found - check superclass
        }
        if (root.equals(tillSuperClass)) {
            return null;
        }

        final Class<?> superclass = root.getSuperclass();
        if (superclass == null || superclass.equals(tillSuperClass)) {
            return null;
        }

        return findDeclaredMethod(superclass, tillSuperClass, m);
    }

    public static <T extends Enum<T>> T searchForEnum(Class<T> clazz, String name) throws IllegalArgumentException {
        try {
            return Enum.valueOf(clazz, name);
        } catch (IllegalArgumentException e) {
            // Try to search case-insensitive
            for (T c : clazz.getEnumConstants()) {
                if (name.equalsIgnoreCase(c.name())) {
                    return c;
                }
            }

            throw e;
        }
    }

    /**
     * Generate a field name by getter method name: trims 'is' or 'get' at the beginning and convert to lower case the first letter.
     *
     * @param mName getter method name
     * @return field name related to the getter.
     */
    public static String makeFieldName(String mName) {
        if (mName.startsWith("get") && mName.length() > 3) {
            final char[] fn = mName.toCharArray();
            fn[3] = Character.toLowerCase(fn[3]);
            return new String(fn, 3, fn.length - 3);
        }

        if (mName.startsWith("is") && mName.length() > 2) {
            final char[] fn = mName.toCharArray();
            fn[2] = Character.toLowerCase(fn[2]);
            return new String(fn, 2, fn.length - 2);
        }

        return mName;
    }

    public static Method findGetter(Class<?> aClass, String fieldName) {
        try {
            final Method m = aClass.getMethod(fieldName);
            if (isGetter(m)) {
                return m;
            }
        } catch (ReflectiveOperationException e) {
            // Ignore
        }

        try {
            final Method m = aClass.getMethod("get" + StringUtils.capitalize(fieldName));
            if (isGetter(m)) {
                return m;
            }
        } catch (ReflectiveOperationException e) {
            // Ignore
        }

        try {
            final Method m = aClass.getMethod("is" + StringUtils.capitalize(fieldName));
            if (isGetter(m)) {
                return m;
            }
        } catch (ReflectiveOperationException e) {
            // Ignore
        }

        return null;
    }

    public static boolean isGetter(Method m) {
        return Modifier.isPublic(m.getModifiers()) &&
                !m.getReturnType().equals(Void.class) &&
                !m.getReturnType().equals(void.class) &&
                m.getParameterCount() == 0;
    }

    public static Class<?> detectTypeArgClass(Type type) {
        return detectTypeArgsClass(type, 1)[0];
    }

    public static Class<?>[] detectTypeArgsClass(Type type, int amount) {
        Class<?>[] result = new Class[amount];
        if ((type instanceof ParameterizedType)) {
            final Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
            if (typeArguments.length == amount) {
                while (amount-- > 0) {
                    final Type argument = typeArguments[amount];
                    if (argument instanceof Class) {
                        result[amount] = (Class<?>) argument;
                    }
                }
            }
        }
        return result;
    }

    public static String toJavaLiteral(String str) {
        if (str == null) {
            return "null";
        }
        return '"' + StringEscapeUtils.escapeJava(str) + '"';
    }

    @SafeVarargs
    public static <T> String toArrayJavaCode(Function<T, String> argToJava, Class<T> elementClass, T... args) {
        return toArrayJavaCode(argToJava, elementClass, ArrayUtils.isEmpty(args) ? Collections.emptyList() : Arrays.asList(args));
    }

    public static <T> String toArrayJavaCode(Function<T, String> argToJava, Class<T> elementClass, Collection<T> list) {
        StringBuilder javaCode = new StringBuilder("new ");
        javaCode.append(BuilderUtils.getName(elementClass));
        if (list == null || list.isEmpty()) {
            javaCode.append("[0]");
        } else {
            javaCode.append("[]{");
            boolean first = true;
            for (T el : list) {
                if (first) {
                    first = false;
                } else {
                    javaCode.append(", ");
                }

                if (el != null) {
                    javaCode.append(argToJava.apply(el));
                } else {
                    javaCode.append("null");
                }
            }
            javaCode.append("}");
        }

        return javaCode.toString();
    }
}
