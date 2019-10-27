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
package consulo.injecting.pico;

import com.intellij.util.ExceptionUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;
import org.picocontainer.*;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * A drop-in replacement of {@link org.picocontainer.defaults.ConstructorInjectionComponentAdapter}
 * The same code (generified and cleaned up) but without constructor caching (hence taking up less memory).
 */
class ConstructorInjectionComponentAdapter extends InstantiatingComponentAdapter {
  private static final Logger LOGGER = Logger.getInstance(ConstructorInjectionComponentAdapter.class);

  private static final ThreadLocal<Set<ConstructorInjectionComponentAdapter>> ourGuard = new ThreadLocal<>();

  public ConstructorInjectionComponentAdapter(@Nonnull Object componentKey, @Nonnull Class componentImplementation) throws AssignabilityRegistrationException, NotConcreteRegistrationException {
    super(componentKey, componentImplementation, null, true);
  }

  @Override
  public Object getComponentInstance(PicoContainer container) throws PicoInitializationException, PicoIntrospectionException, AssignabilityRegistrationException, NotConcreteRegistrationException {
    return instantiateGuarded(container, getComponentImplementation());
  }

  private Object instantiateGuarded(PicoContainer container, Class stackFrame) {
    Set<ConstructorInjectionComponentAdapter> currentStack = ourGuard.get();
    if (currentStack == null) {
      ourGuard.set(currentStack = ContainerUtil.newIdentityTroveSet());
    }

    if (currentStack.contains(this)) {
      throw new CyclicDependencyException(stackFrame);
    }

    try {
      currentStack.add(this);
      return doGetComponentInstance(container);
    }
    catch (final CyclicDependencyException e) {
      e.push(stackFrame);
      throw e;
    }
    finally {
      currentStack.remove(this);
    }
  }

  private Object doGetComponentInstance(PicoContainer guardedContainer) {
    final Constructor constructor;
    try {
      constructor = getGreediestSatisfiableConstructor(guardedContainer);

      if (!isDefaultConstructor(constructor) && !constructor.isAnnotationPresent(Inject.class)) {
        LOGGER.warn("Missing @Inject at constructor " + constructor);
      }
    }
    catch (AmbiguousComponentResolutionException e) {
      e.setComponent(getComponentImplementation());
      throw e;
    }
    ComponentMonitor componentMonitor = currentMonitor();
    try {
      Object[] parameters = getConstructorArguments(guardedContainer, constructor);
      componentMonitor.instantiating(constructor);
      long startTime = System.currentTimeMillis();
      Object inst = newInstance(constructor, parameters);
      componentMonitor.instantiated(constructor, System.currentTimeMillis() - startTime);
      return inst;
    }
    catch (InvocationTargetException e) {
      componentMonitor.instantiationFailed(constructor, e);
      ExceptionUtil.rethrowUnchecked(e.getTargetException());
      throw new PicoInvocationTargetInitializationException(e.getTargetException());
    }
    catch (InstantiationException e) {
      componentMonitor.instantiationFailed(constructor, e);
      throw new PicoInitializationException("Should never get here");
    }
    catch (IllegalAccessException e) {
      componentMonitor.instantiationFailed(constructor, e);
      throw new PicoInitializationException(e);
    }
  }

  private Object[] getConstructorArguments(PicoContainer container, Constructor ctor) {
    Class[] parameterTypes = ctor.getParameterTypes();
    Object[] result = new Object[parameterTypes.length];
    Parameter[] currentParameters = createParameters(ctor);

    for (int i = 0; i < currentParameters.length; i++) {
      result[i] = currentParameters[i].resolveInstance(container, this, parameterTypes[i]);
    }
    return result;
  }

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
          parameters[i] = ComponentParameter.DEFAULT;
        }
      }
      else {
        parameters[i] = ComponentParameter.DEFAULT;
      }
    }

    return parameters;
  }

  private static boolean isDefaultConstructor(@Nonnull Constructor<?> constructor) {
    return Modifier.isPublic(constructor.getModifiers()) && constructor.getParameterCount() == 0 && constructor.getExceptionTypes().length == 0;
  }

  @Override
  protected Constructor getGreediestSatisfiableConstructor(PicoContainer container) throws PicoIntrospectionException, AssignabilityRegistrationException, NotConcreteRegistrationException {
    List<Constructor> sortedMatchingConstructors = getSortedMatchingConstructors();
    // special check for default constructors, return it without any check
    if (sortedMatchingConstructors.size() == 1) {
      Constructor constructor = sortedMatchingConstructors.get(0);
      if (isDefaultConstructor(constructor)) {
        return constructor;
      }
    }

    // if we have constructor with Inject annotation - return it, without any dependency check
    for (Constructor sortedMatchingConstructor : sortedMatchingConstructors) {
      if (sortedMatchingConstructor.isAnnotationPresent(Inject.class)) {
        sortedMatchingConstructors = Collections.singletonList(sortedMatchingConstructor);
        break;
      }
    }

    final Set<Constructor> conflicts = new HashSet<>();
    final Set<List<Class>> unsatisfiableDependencyTypes = new HashSet<>();
    Constructor greediestConstructor = null;
    int lastSatisfiableConstructorSize = -1;
    Class unsatisfiedDependencyType = null;
    for (Constructor constructor : sortedMatchingConstructors) {
      boolean failedDependency = false;
      Class[] parameterTypes = constructor.getParameterTypes();
      Parameter[] currentParameters = createParameters(constructor);

      // remember: all constructors with less arguments than the given parameters are filtered out already
      for (int j = 0; j < currentParameters.length; j++) {
        // check whether this constructor is satisfiable
        if (currentParameters[j].isResolvable(container, this, parameterTypes[j])) {
          continue;
        }
        unsatisfiableDependencyTypes.add(Arrays.asList(parameterTypes));
        unsatisfiedDependencyType = parameterTypes[j];
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
      throw new TooManySatisfiableConstructorsException(getComponentImplementation(), conflicts);
    }
    else if (greediestConstructor == null && !unsatisfiableDependencyTypes.isEmpty()) {
      throw new UnsatisfiableDependenciesException(this, unsatisfiedDependencyType, unsatisfiableDependencyTypes, container);
    }
    else if (greediestConstructor == null) {
      // be nice to the user, show all constructors that were filtered out
      final Set<Constructor> nonMatching = ContainerUtil.newHashSet(getConstructors());
      throw new PicoInitializationException("Either do the specified parameters not match any of the following constructors: " +
                                            nonMatching.toString() +
                                            " or the constructors were not accessible for '" +
                                            getComponentImplementation() +
                                            "'");
    }
    return greediestConstructor;
  }

  private List<Constructor> getSortedMatchingConstructors() {
    List<Constructor> matchingConstructors = new ArrayList<>();
    // filter out all constructors that will definitely not match
    for (Constructor constructor : getConstructors()) {
      if ((parameters == null || constructor.getParameterTypes().length == parameters.length) && (allowNonPublicClasses || (constructor.getModifiers() & Modifier.PUBLIC) != 0)) {
        matchingConstructors.add(constructor);
      }
    }
    // optimize list of constructors moving the longest at the beginning
    if (parameters == null) {
      Collections.sort(matchingConstructors, new Comparator<Constructor>() {
        @Override
        public int compare(Constructor arg0, Constructor arg1) {
          return arg1.getParameterTypes().length - arg0.getParameterTypes().length;
        }
      });
    }
    return matchingConstructors;
  }

  private Constructor[] getConstructors() {
    return (Constructor[])AccessController.doPrivileged((PrivilegedAction)() -> getComponentImplementation().getDeclaredConstructors());
  }
}