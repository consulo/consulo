/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.util;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.DifferenceFilter;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.JBTreeTraverser;
import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.*;

public class ReflectionUtil {
  private static final Logger LOG = Logger.getInstance(ReflectionUtil.class);

  private ReflectionUtil() {
  }

  @Nullable
  public static Type resolveVariable(@Nonnull TypeVariable variable, @Nonnull Class classType) {
    return resolveVariable(variable, classType, true);
  }

  @Nullable
  public static Type resolveVariable(@Nonnull TypeVariable variable, @Nonnull Class classType, boolean resolveInInterfacesOnly) {
    final Class aClass = getRawType(classType);
    int index = ArrayUtilRt.find(aClass.getTypeParameters(), variable);
    if (index >= 0) {
      return variable;
    }

    final Class[] classes = aClass.getInterfaces();
    final Type[] genericInterfaces = aClass.getGenericInterfaces();
    for (int i = 0; i <= classes.length; i++) {
      Class anInterface;
      if (i < classes.length) {
        anInterface = classes[i];
      }
      else {
        anInterface = aClass.getSuperclass();
        if (resolveInInterfacesOnly || anInterface == null) {
          continue;
        }
      }
      final Type resolved = resolveVariable(variable, anInterface);
      if (resolved instanceof Class || resolved instanceof ParameterizedType) {
        return resolved;
      }
      if (resolved instanceof TypeVariable) {
        final TypeVariable typeVariable = (TypeVariable)resolved;
        index = ArrayUtilRt.find(anInterface.getTypeParameters(), typeVariable);
        if (index < 0) {
          LOG.error("Cannot resolve type variable:\n" +
                    "typeVariable = " +
                    typeVariable +
                    "\n" +
                    "genericDeclaration = " +
                    declarationToString(typeVariable.getGenericDeclaration()) +
                    "\n" +
                    "searching in " +
                    declarationToString(anInterface));
        }
        final Type type = i < genericInterfaces.length ? genericInterfaces[i] : aClass.getGenericSuperclass();
        if (type instanceof Class) {
          return Object.class;
        }
        if (type instanceof ParameterizedType) {
          return getActualTypeArguments((ParameterizedType)type)[index];
        }
        throw new AssertionError("Invalid type: " + type);
      }
    }
    return null;
  }

  @SuppressWarnings("HardCodedStringLiteral")
  @Nonnull
  public static String declarationToString(@Nonnull GenericDeclaration anInterface) {
    return anInterface.toString() + Arrays.asList(anInterface.getTypeParameters()) + " loaded by " + ((Class)anInterface).getClassLoader();
  }

  @Nonnull
  public static Class<?> boxType(@Nonnull Class<?> type) {
    if (!type.isPrimitive()) return type;
    if (type == boolean.class) return Boolean.class;
    if (type == byte.class) return Byte.class;
    if (type == short.class) return Short.class;
    if (type == int.class) return Integer.class;
    if (type == long.class) return Long.class;
    if (type == float.class) return Float.class;
    if (type == double.class) return Double.class;
    if (type == char.class) return Character.class;
    return type;
  }

  @Nonnull
  public static Class<?> getRawType(@Nonnull Type type) {
    if (type instanceof Class) {
      return (Class)type;
    }
    if (type instanceof ParameterizedType) {
      return getRawType(((ParameterizedType)type).getRawType());
    }
    if (type instanceof GenericArrayType) {
      //todo[peter] don't create new instance each time
      return Array.newInstance(getRawType(((GenericArrayType)type).getGenericComponentType()), 0).getClass();
    }
    assert false : type;
    return null;
  }

  @Nonnull
  public static Type[] getActualTypeArguments(@Nonnull ParameterizedType parameterizedType) {
    return parameterizedType.getActualTypeArguments();
  }

  @Nullable
  public static Class<?> substituteGenericType(@Nonnull Type genericType, @Nonnull Type classType) {
    if (genericType instanceof TypeVariable) {
      final Class<?> aClass = getRawType(classType);
      final Type type = resolveVariable((TypeVariable)genericType, aClass);
      if (type instanceof Class) {
        return (Class)type;
      }
      if (type instanceof ParameterizedType) {
        return (Class<?>)((ParameterizedType)type).getRawType();
      }
      if (type instanceof TypeVariable && classType instanceof ParameterizedType) {
        final int index = ArrayUtilRt.find(aClass.getTypeParameters(), type);
        if (index >= 0) {
          return getRawType(getActualTypeArguments((ParameterizedType)classType)[index]);
        }
      }
    }
    else {
      return getRawType(genericType);
    }
    return null;
  }

  @Nonnull
  public static List<Field> collectFields(@Nonnull Class clazz) {
    List<Field> result = ContainerUtil.newArrayList();
    for (Class c : classTraverser(clazz)) {
      result.addAll(getClassDeclaredFields(c));
    }
    return result;
  }

  @Nonnull
  public static Field findField(@Nonnull Class clazz, @Nullable final Class type, @Nonnull final String name) throws NoSuchFieldException {
    Field result = processFields(clazz, new Condition<Field>() {
      @Override
      public boolean value(Field field) {
        return name.equals(field.getName()) && (type == null || field.getType().equals(type));
      }
    });
    if (result != null) return result;

    throw new NoSuchFieldException("Class: " + clazz + " name: " + name + " type: " + type);
  }

  @Nonnull
  public static Field findAssignableField(@Nonnull Class<?> clazz, @Nullable final Class<?> fieldType, @Nonnull final String fieldName) throws NoSuchFieldException {
    Field result = processFields(clazz, new Condition<Field>() {
      @Override
      public boolean value(Field field) {
        return fieldName.equals(field.getName()) && (fieldType == null || fieldType.isAssignableFrom(field.getType()));
      }
    });
    if (result != null) return result;
    throw new NoSuchFieldException("Class: " + clazz + " fieldName: " + fieldName + " fieldType: " + fieldType);
  }

  private static Field processFields(@Nonnull Class clazz, @Nonnull Condition<Field> checker) {
    for (Class c : classTraverser(clazz)) {
      Field field = JBIterable.of(c.getDeclaredFields()).find(checker);
      if (field != null) {
        field.setAccessible(true);
        return field;
      }
    }
    return null;
  }

  public static void resetField(@Nonnull Class clazz, @Nullable Class type, @Nonnull String name) {
    try {
      resetField(null, findField(clazz, type, name));
    }
    catch (NoSuchFieldException e) {
      LOG.info(e);
    }
  }

  public static void resetField(@Nonnull Object object, @Nullable Class type, @Nonnull String name) {
    try {
      resetField(object, findField(object.getClass(), type, name));
    }
    catch (NoSuchFieldException e) {
      LOG.info(e);
    }
  }

  public static void resetField(@Nonnull Object object, @Nonnull String name) {
    try {
      resetField(object, findField(object.getClass(), null, name));
    }
    catch (NoSuchFieldException e) {
      LOG.info(e);
    }
  }

  public static void resetField(@Nullable final Object object, @Nonnull Field field) {
    field.setAccessible(true);
    Class<?> type = field.getType();
    try {
      if (type.isPrimitive()) {
        if (boolean.class.equals(type)) {
          field.set(object, Boolean.FALSE);
        }
        else if (int.class.equals(type)) {
          field.set(object, Integer.valueOf(0));
        }
        else if (double.class.equals(type)) {
          field.set(object, Double.valueOf(0));
        }
        else if (float.class.equals(type)) {
          field.set(object, Float.valueOf(0));
        }
      }
      else {
        field.set(object, null);
      }
    }
    catch (IllegalAccessException e) {
      LOG.info(e);
    }
  }

  public static void resetStaticField(@Nonnull Class aClass, @Nonnull @NonNls String name) {
    resetField(aClass, null, name);
  }

  @Nullable
  public static Method findMethod(@Nonnull Collection<Method> methods, @NonNls @Nonnull String name, @Nonnull Class... parameters) {
    for (final Method method : methods) {
      if (name.equals(method.getName()) && Arrays.equals(parameters, method.getParameterTypes())) {
        method.setAccessible(true);
        return method;
      }
    }
    return null;
  }

  @Nullable
  public static Method getMethod(@Nonnull Class aClass, @NonNls @Nonnull String name, @Nonnull Class... parameters) {
    return findMethod(getClassPublicMethods(aClass, false), name, parameters);
  }

  @Nullable
  public static Method getDeclaredMethod(@Nonnull Class aClass, @NonNls @Nonnull String name, @Nonnull Class... parameters) {
    return findMethod(getClassDeclaredMethods(aClass, false), name, parameters);
  }

  @Nullable
  public static Field getDeclaredField(@Nonnull Class aClass, @NonNls @Nonnull final String name) {
    return processFields(aClass, field -> name.equals(field.getName()));
  }

  @Nonnull
  public static List<Method> getClassPublicMethods(@Nonnull Class aClass) {
    return getClassPublicMethods(aClass, false);
  }

  @Nonnull
  public static List<Method> getClassPublicMethods(@Nonnull Class aClass, boolean includeSynthetic) {
    Method[] methods = aClass.getMethods();
    return includeSynthetic ? Arrays.asList(methods) : filterRealMethods(methods);
  }

  @Nonnull
  public static List<Method> getClassDeclaredMethods(@Nonnull Class aClass) {
    return getClassDeclaredMethods(aClass, false);
  }

  @Nonnull
  public static List<Method> getClassDeclaredMethods(@Nonnull Class aClass, boolean includeSynthetic) {
    Method[] methods = aClass.getDeclaredMethods();
    return includeSynthetic ? Arrays.asList(methods) : filterRealMethods(methods);
  }

  @Nonnull
  public static List<Field> getClassDeclaredFields(@Nonnull Class aClass) {
    Field[] fields = aClass.getDeclaredFields();
    return Arrays.asList(fields);
  }

  @Nonnull
  private static List<Method> filterRealMethods(@Nonnull Method[] methods) {
    List<Method> result = ContainerUtil.newArrayList();
    for (Method method : methods) {
      if (!method.isSynthetic()) {
        result.add(method);
      }
    }
    return result;
  }

  @Nullable
  public static Class getMethodDeclaringClass(@Nonnull Class<?> instanceClass, @NonNls @Nonnull String methodName, @Nonnull Class... parameters) {
    Method method = getMethod(instanceClass, methodName, parameters);
    return method == null ? null : method.getDeclaringClass();
  }

  public static <T> T getField(@Nonnull Class objectClass, @Nullable Object object, @Nullable Class<T> fieldType, @Nonnull @NonNls String fieldName) {
    try {
      final Field field = findAssignableField(objectClass, fieldType, fieldName);
      return (T)field.get(object);
    }
    catch (NoSuchFieldException e) {
      LOG.debug(e);
      return null;
    }
    catch (IllegalAccessException e) {
      LOG.debug(e);
      return null;
    }
  }

  public static <T> T getStaticFieldValue(@Nonnull Class objectClass, @Nullable Class<T> fieldType, @Nonnull @NonNls String fieldName) {
    try {
      final Field field = findAssignableField(objectClass, fieldType, fieldName);
      if (!Modifier.isStatic(field.getModifiers())) {
        throw new IllegalArgumentException("Field " + objectClass + "." + fieldName + " is not static");
      }
      return (T)field.get(null);
    }
    catch (NoSuchFieldException e) {
      LOG.debug(e);
      return null;
    }
    catch (IllegalAccessException e) {
      LOG.debug(e);
      return null;
    }
  }

  // returns true if value was set
  public static <T> boolean setField(@Nonnull Class objectClass, Object object, @Nullable Class<T> fieldType, @Nonnull @NonNls String fieldName, T value) {
    try {
      final Field field = findAssignableField(objectClass, fieldType, fieldName);
      field.set(object, value);
      return true;
    }
    catch (NoSuchFieldException e) {
      LOG.debug(e);
      // this 'return' was moved into 'catch' block because otherwise reference to common super-class of these exceptions (ReflectiveOperationException)
      // which doesn't exist in JDK 1.6 will be added to class-file during instrumentation
      return false;
    }
    catch (IllegalAccessException e) {
      LOG.debug(e);
      return false;
    }
  }

  public static Type resolveVariableInHierarchy(@Nonnull TypeVariable variable, @Nonnull Class aClass) {
    Type type;
    Class current = aClass;
    while ((type = resolveVariable(variable, current, false)) == null) {
      current = current.getSuperclass();
      if (current == null) {
        return null;
      }
    }
    if (type instanceof TypeVariable) {
      return resolveVariableInHierarchy((TypeVariable)type, aClass);
    }
    return type;
  }

  @Nonnull
  public static <T> Constructor<T> getDefaultConstructor(@Nonnull Class<T> aClass) {
    try {
      final Constructor<T> constructor = aClass.getConstructor();
      constructor.setAccessible(true);
      return constructor;
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException("No default constructor in " + aClass, e);
    }
  }

  /**
   * @deprecated use {@link #newInstance(Class)} instead (this method will fail anyway if non-empty {@code parameterTypes} is passed)
   */
  public static <T> T newInstance(@Nonnull Class<T> aClass, @Nonnull Class... parameterTypes) {
    return newInstance(aClass);
  }

  /**
   * Like {@link Class#newInstance()} but also handles private classes
   */
  @Nonnull
  public static <T> T newInstance(@Nonnull Class<T> aClass) {
    try {
      Constructor<T> constructor = aClass.getDeclaredConstructor();
      try {
        constructor.setAccessible(true);
      }
      catch (SecurityException e) {
        return aClass.newInstance();
      }
      return constructor.newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public static <T> T createInstance(@Nonnull Constructor<T> constructor, @Nonnull Object... args) {
    try {
      return constructor.newInstance(args);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void resetThreadLocals() {
    resetField(Thread.currentThread(), null, "threadLocals");
  }

  @Nullable
  public static Class getGrandCallerClass() {
    int stackFrameCount = 3;
    Class callerClass = findCallerClass(stackFrameCount);
    while (callerClass != null && callerClass.getClassLoader() == null) { // looks like a system class
      callerClass = findCallerClass(++stackFrameCount);
    }
    if (callerClass == null) {
      callerClass = findCallerClass(2);
    }
    return callerClass;
  }

  public static void copyFields(@Nonnull Field[] fields, @Nonnull Object from, @Nonnull Object to) {
    copyFields(fields, from, to, null);
  }

  public static boolean copyFields(@Nonnull Field[] fields, @Nonnull Object from, @Nonnull Object to, @Nullable DifferenceFilter diffFilter) {
    Set<Field> sourceFields = new HashSet<Field>(Arrays.asList(from.getClass().getFields()));
    boolean valuesChanged = false;
    for (Field field : fields) {
      if (sourceFields.contains(field)) {
        if (isPublic(field) && !isFinal(field)) {
          try {
            if (diffFilter == null || diffFilter.isAccept(field)) {
              copyFieldValue(from, to, field);
              valuesChanged = true;
            }
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return valuesChanged;
  }

  public static void copyFieldValue(@Nonnull Object from, @Nonnull Object to, @Nonnull Field field) throws IllegalAccessException {
    Class<?> fieldType = field.getType();
    if (fieldType.isPrimitive() || fieldType.equals(String.class) || fieldType.isEnum()) {
      field.set(to, field.get(from));
    }
    else {
      throw new RuntimeException("Field '" + field.getName() + "' not copied: unsupported type: " + field.getType());
    }
  }

  private static boolean isPublic(final Field field) {
    return Modifier.isPublic(field.getModifiers());
  }

  private static boolean isFinal(final Field field) {
    return Modifier.isFinal(field.getModifiers());
  }

  @Nonnull
  public static Class forName(@Nonnull String fqn) {
    try {
      return Class.forName(fqn);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public static Class findClassOrNull(@Nonnull String fqn, @Nonnull ClassLoader classLoader) {
    try {
      return Class.forName(fqn, true, classLoader);
    }
    catch (ClassNotFoundException ignored) {
      return null;
    }
  }

  /**
   * Returns the class this method was called 'framesToSkip' frames up the caller hierarchy.
   * <p/>
   * NOTE:
   * <b>Extremely expensive!
   * Please consider not using it.
   * These aren't the droids you're looking for!</b>
   */
  @Nullable
  @Deprecated
  @DeprecationInfo("Use consulo.util.lang.reflect.ReflectionUtil")
  public static Class findCallerClass(int framesToSkip) {
    StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    StackWalker.StackFrame frame = walker.walk(it -> {
      Optional<StackWalker.StackFrame> first = it.skip(framesToSkip).findFirst();
      return first.get();
    });

    return frame.getDeclaringClass();
  }

  @Deprecated
  public static boolean isAssignable(@Nonnull Class<?> ancestor, @Nonnull Class<?> descendant) {
    return consulo.util.lang.reflect.ReflectionUtil.isAssignable(ancestor, descendant);
  }

  @Nonnull
  public static JBTreeTraverser<Class> classTraverser(@Nullable Class root) {
    return new JBTreeTraverser<Class>(CLASS_STRUCTURE).unique().withRoot(root);
  }

  private static final Function<Class, Iterable<Class>> CLASS_STRUCTURE = new Function<Class, Iterable<Class>>() {
    @Override
    public Iterable<Class> fun(Class aClass) {
      return JBIterable.of(aClass.getSuperclass()).append(aClass.getInterfaces());
    }
  };

  public static boolean comparePublicNonFinalFields(@Nonnull Object first, @Nonnull Object second) {
    Set<Field> firstFields = ContainerUtil.newHashSet(first.getClass().getFields());
    for (Field field : second.getClass().getFields()) {
      if (firstFields.contains(field)) {
        if (isPublic(field) && !isFinal(field)) {
          try {
            if (!Comparing.equal(field.get(first), field.get(second))) {
              return false;
            }
          }
          catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    return true;
  }

  public static void clearOwnFields(@Nullable Object object, @Nonnull Condition<? super Field> selectCondition) {
    if (object == null) return;
    for (Field each : ReflectionUtil.collectFields(object.getClass())) {
      if ((each.getModifiers() & (Modifier.FINAL | Modifier.STATIC)) > 0) continue;
      if (!selectCondition.value(each)) continue;
      try {
        ReflectionUtil.resetField(object, each);
      }
      catch (Exception ignore) {
      }
    }
  }
}
