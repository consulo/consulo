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
package consulo.component.impl.internal.inject;

import consulo.component.internal.inject.InjectingContainer;
import consulo.logging.Logger;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A drop-in replacement of {@link org.picocontainer.defaults.ConstructorInjectionComponentAdapter}
 * The same code (generified and cleaned up) but without constructor caching (hence taking up less memory).
 */
class ConstructorInjectionComponentAdapter<T> implements ComponentAdapter<T> {
  private static final Logger LOG = Logger.getInstance(ConstructorInjectionComponentAdapter.class);

  private static final ThreadLocal<Set<ConstructorInjectionComponentAdapter>> ourGuard = new ThreadLocal<>();
  @Nonnull
  private final Class<? super T> myComponentKey;
  @Nonnull
  private final Class<T> myComponentImplementation;

  public ConstructorInjectionComponentAdapter(@Nonnull Class<? super T> componentKey, @Nonnull Class<T> componentImplementation) {
    myComponentKey = componentKey;
    myComponentImplementation = componentImplementation;
  }

  @Override
  @Nonnull
  public Class<? super T> getComponentClass() {
    return myComponentKey;
  }

  @Override
  @Nonnull
  public Class<T> getComponentImplClass() {
    return myComponentImplementation;
  }

  @Override
  public T getComponentInstance(InstanceContainer container) throws PicoInitializationException, PicoIntrospectionException {
    return instantiateGuarded(container, getComponentImplClass());
  }

  private T instantiateGuarded(InstanceContainer container, Class stackFrame) {
    Set<ConstructorInjectionComponentAdapter> currentStack = ourGuard.get();
    if (currentStack == null) {
      ourGuard.set(currentStack = Sets.newHashSet(HashingStrategy.identity()));
    }

    if (currentStack.contains(this)) {
      throw new CyclicDependencyException(stackFrame);
    }

    try {
      currentStack.add(this);
      return doGetComponentInstance(container);
    }
    catch (CyclicDependencyException e) {
      e.push(stackFrame);
      throw e;
    }
    finally {
      currentStack.remove(this);
    }
  }

  @Nonnull
  protected T doGetComponentInstance(InstanceContainer guardedContainer) {
    Constructor<T> constructor = getGreediestSatisfiableConstructor(guardedContainer);

    if (InjectingContainer.LOG_INJECTING_PROBLEMS && !isDefaultConstructor(constructor) && !constructor.isAnnotationPresent(Inject.class)) {
      LOG.warn("Missing @Inject at constructor " + constructor);
    }
    try {
      Object[] parameters = getConstructorArguments(guardedContainer, constructor);
      return newInstance(constructor, parameters);
    }
    catch (InvocationTargetException e) {
      ExceptionUtil.rethrowUnchecked(e.getTargetException());
      throw new PicoInvocationTargetInitializationException(e.getTargetException());
    }
    catch (InstantiationException e) {
      throw new PicoInitializationException("Should never get here");
    }
    catch (IllegalAccessException e) {
      throw new PicoInitializationException(e);
    }
  }

  @Nonnull
  private T newInstance(Constructor<T> constructor, Object[] parameters) throws InstantiationException, IllegalAccessException, InvocationTargetException {
    constructor.setAccessible(true);
    return constructor.newInstance(parameters);
  }

  private Object[] getConstructorArguments(InstanceContainer container, Constructor ctor) {
    Class[] parameterTypes = ctor.getParameterTypes();
    Object[] result = new Object[parameterTypes.length];
    Parameter[] currentParameters = createParameters(ctor);

    for (int i = 0; i < currentParameters.length; i++) {
      result[i] = currentParameters[i].resolveInstance(container, this, parameterTypes[i]);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private Parameter[] createParameters(Constructor constructor) {
    Type[] genericParameterTypes = constructor.getGenericParameterTypes();

    Parameter[] parameters = new Parameter[genericParameterTypes.length];
    for (int i = 0; i < genericParameterTypes.length; i++) {
      Type genericParameterType = genericParameterTypes[i];

      if (genericParameterType instanceof ParameterizedType) {
        Class<?> rawType = ReflectionUtil.getRawType(genericParameterType);

        if (rawType == Provider.class) {
          Type type = ((ParameterizedType)genericParameterType).getActualTypeArguments()[0];

          if (!(type instanceof Class)) {
            throw new UnsupportedOperationException("Unknown type " + genericParameterType);
          }

          parameters[i] = new ProviderParameter((Class<?>)type);
        }
        else {
          parameters[i] = DefaultComponentParameter.DEFAULT;
        }
      }
      else {
        parameters[i] = DefaultComponentParameter.DEFAULT;
      }
    }

    return parameters;
  }

  private static boolean isDefaultConstructor(@Nonnull Constructor<?> constructor) {
    return Modifier.isPublic(constructor.getModifiers()) && constructor.getParameterCount() == 0 && constructor.getExceptionTypes().length == 0;
  }

  @SuppressWarnings("unchecked")
  private Constructor<T> getGreediestSatisfiableConstructor(InstanceContainer container) throws PicoIntrospectionException {
    Constructor<T>[] constructors = getConstructors();
    // special check for default constructors, return it without any check
    if (constructors.length == 1) {
      Constructor constructor = constructors[0];
      if (isDefaultConstructor(constructor)) {
        return constructor;
      }
    }

    // if we have constructor with Inject annotation - return it, without any dependency check
    for (Constructor sortedMatchingConstructor : constructors) {
      if (sortedMatchingConstructor.isAnnotationPresent(Inject.class)) {
        constructors = new Constructor[]{sortedMatchingConstructor};
        break;
      }
    }

    Set<Constructor> conflicts = new HashSet<>();
    Set<List<Type>> unsatisfiableDependencyTypes = new HashSet<>();
    Constructor greediestConstructor = null;
    int lastSatisfiableConstructorSize = -1;
    Type unsatisfiedDependencyType = null;
    for (Constructor constructor : constructors) {
      boolean failedDependency = false;
      Class[] parameterTypes = constructor.getParameterTypes();
      Type[] genericParameterTypes = constructor.getGenericParameterTypes();
      Parameter[] currentParameters = createParameters(constructor);

      // remember: all constructors with less arguments than the given parameters are filtered out already
      for (int j = 0; j < currentParameters.length; j++) {
        // check whether this constructor is satisfiable
        if (currentParameters[j].isResolvable(container, this, parameterTypes[j])) {
          continue;
        }
        unsatisfiableDependencyTypes.add(Arrays.asList(genericParameterTypes));
        unsatisfiedDependencyType = genericParameterTypes[j];
        failedDependency = true;
        break;
      }

      if (greediestConstructor != null && parameterTypes.length != lastSatisfiableConstructorSize) {
        if (conflicts.isEmpty()) {
          // we found our match [aka. greedy and satisfied]
          return greediestConstructor;
        }
        else {
          // fits although not greedy
          conflicts.add(constructor);
        }
      }
      else if (!failedDependency && lastSatisfiableConstructorSize == parameterTypes.length) {
        // satisfied and same size as previous one?
        conflicts.add(constructor);
        conflicts.add(greediestConstructor);
      }
      else if (!failedDependency) {
        greediestConstructor = constructor;
        lastSatisfiableConstructorSize = parameterTypes.length;
      }
    }
    if (!conflicts.isEmpty()) {
      throw new TooManySatisfiableConstructorsException(getComponentImplClass(), conflicts);
    }
    else if (greediestConstructor == null && !unsatisfiableDependencyTypes.isEmpty()) {
      throw new UnsatisfiableDependenciesException(this, unsatisfiedDependencyType, unsatisfiableDependencyTypes, container);
    }
    else if (greediestConstructor == null) {
      // be nice to the user, show all constructors that were filtered out
      List<Constructor> nonMatching = Arrays.asList(getConstructors());
      throw new PicoInitializationException("Either do the specified parameters not match any of the following constructors: " +
                                            nonMatching.toString() +
                                            " or the constructors were not accessible for '" +
                                            getComponentImplClass() +
                                            "'");
    }
    return greediestConstructor;
  }

  @Nonnull
  private Constructor[] getConstructors() {
    return getComponentImplClass().getDeclaredConstructors();
  }
}