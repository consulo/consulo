/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.components.impl.stores;

import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings({"deprecation"})
public class DefaultStateSerializer {
  private DefaultStateSerializer() {
  }

  @Nullable
  public static Element serializeState(@Nonnull Object state, @Nullable final Storage storage) throws WriteExternalException {
    if (state instanceof Element) {
      return (Element)state;
    }
    else if (state instanceof JDOMExternalizable) {
      Element element = new Element("temp_element");
      ((JDOMExternalizable)state).writeExternal(element);
      return element;
    }
    else {
      return XmlSerializer.serializeIfNotDefault(state, new SkipDefaultValuesSerializationFilters());
    }
  }

  @SuppressWarnings({"unchecked"})
  @Nullable
  public static <T> T deserializeState(@Nullable Element stateElement, Class <T> stateClass) throws StateStorageException {
    if (stateElement == null) return null;

    if (stateClass.equals(Element.class)) {
      //assert mergeInto == null;
      return (T)stateElement;
    }
    else if (JDOMExternalizable.class.isAssignableFrom(stateClass)) {
      final T t = ReflectionUtil.newInstance(stateClass);
      try {
        ((JDOMExternalizable)t).readExternal(stateElement);
        return t;
      }
      catch (InvalidDataException e) {
        throw new StateStorageException(e);
      }
    }
    else {
      return XmlSerializer.deserialize(stateElement, stateClass);
    }
  }
}
