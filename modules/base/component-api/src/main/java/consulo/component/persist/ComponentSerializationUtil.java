/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.component.persist;

import consulo.util.lang.reflect.ReflectionUtil;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;

import org.jspecify.annotations.Nullable;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

/**
 * @author nik
 */
public class ComponentSerializationUtil {
  public static <T> Class<T> getStateClass(Class<? extends PersistentStateComponent> aClass) {
    return getStateClass(aClass, PersistentStateComponent.class);
  }

  /**
   * Resolves the state type argument declared by {@code baseInterface} for the given component class.
   *
   * @param baseInterface single type parameter interface declaring the state type, either
   *                      {@link PersistentStateComponent} or {@link PersistentStateComponentAsync}
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> Class<T> getStateClass(Class<?> aClass, Class<?> baseInterface) {
    TypeVariable variable = baseInterface.getTypeParameters()[0];
    return (Class<T>)ReflectionUtil.getRawType(Objects.requireNonNull(ReflectionUtil.resolveVariableInHierarchy(variable, aClass)));
  }

  public static <S> void loadComponentState(PersistentStateComponent<S> configuration, @Nullable Element element) {
    if (element != null) {
      Class<S> stateClass = getStateClass(configuration.getClass());
      configuration.loadState(Objects.requireNonNull(XmlSerializer.deserialize(element, stateClass)));
    }
  }
}
