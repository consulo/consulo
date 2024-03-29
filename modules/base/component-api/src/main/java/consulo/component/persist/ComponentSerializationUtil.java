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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.TypeVariable;

/**
 * @author nik
 */
public class ComponentSerializationUtil {
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getStateClass(final Class<? extends PersistentStateComponent> aClass) {
    TypeVariable<Class<PersistentStateComponent>> variable = PersistentStateComponent.class.getTypeParameters()[0];
    return (Class<T>)ReflectionUtil.getRawType(ReflectionUtil.resolveVariableInHierarchy(variable, aClass));
  }

  public static <S> void loadComponentState(@Nonnull PersistentStateComponent<S> configuration, @Nullable Element element) {
    if (element != null) {
      Class<S> stateClass = getStateClass(configuration.getClass());
      configuration.loadState(XmlSerializer.deserialize(element, stateClass));
    }
  }
}
