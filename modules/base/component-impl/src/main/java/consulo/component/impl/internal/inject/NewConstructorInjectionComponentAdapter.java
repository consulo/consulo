/*
 * Copyright 2013-2022 consulo.io
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

import consulo.util.lang.reflect.ReflectionUtil;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 18-Jun-22
 */
class NewConstructorInjectionComponentAdapter<T> extends ConstructorInjectionComponentAdapter<T> {
  private final Type[] myConstructorParameterTypes;
  private final Function<Object[], T> myConstructorFactory;

  NewConstructorInjectionComponentAdapter(@Nonnull Class<? super T> componentKey, @Nonnull Class<T> componentImplementation, Type[] constructorParameterTypes, Function<Object[], T> constructorFactory) {
    super(componentKey, componentImplementation);
    myConstructorParameterTypes = constructorParameterTypes;
    myConstructorFactory = constructorFactory;
  }

  @Nonnull
  @Override
  protected T doGetComponentInstance(InstanceContainer guardedContainer) {
    Object[] args = getConstructorArguments(guardedContainer);
    return myConstructorFactory.apply(args);
  }

  private Object[] getConstructorArguments(InstanceContainer container) {
    Object[] result = new Object[myConstructorParameterTypes.length];
    Parameter[] currentParameters = createParameters();

    for (int i = 0; i < currentParameters.length; i++) {
      Type constructorParameterType = myConstructorParameterTypes[i];

      Class expectedType;
      if (constructorParameterType instanceof ParameterizedType) {
        expectedType = (Class)((ParameterizedType)constructorParameterType).getRawType();
      }
      else {
        expectedType = (Class)constructorParameterType;
      }

      result[i] = currentParameters[i].resolveInstance(container, this, expectedType);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private Parameter[] createParameters() {
    Parameter[] parameters = new Parameter[myConstructorParameterTypes.length];
    for (int i = 0; i < myConstructorParameterTypes.length; i++) {
      Type genericParameterType = myConstructorParameterTypes[i];

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
}
