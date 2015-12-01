package org.xblackcat.sjpu.builder;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
}
