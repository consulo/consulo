/*
 * Copyright 2013 Consulo.org
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
package org.consulo.ide.eap;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.containers.HashMap;
import com.intellij.util.containers.hash.LinkedHashMap;
import org.consulo.lombok.annotations.ApplicationService;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 17:11/15.10.13
 */
@ApplicationService
@State(
  name = "EarlyAccessProgramManager",
  storages = {
    @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")
  }
)
public class EarlyAccessProgramManager implements PersistentStateComponent<Element> {
  private Map<Class<? extends EarlyAccessProgramDescriptor>, Boolean> myStates =
    new LinkedHashMap<Class<? extends EarlyAccessProgramDescriptor>, Boolean>();

  public EarlyAccessProgramManager() {
    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensions()) {
      myStates.put(descriptor.getClass(), descriptor.getDefaultState());
    }
  }

  public boolean getState(@NotNull Class<? extends EarlyAccessProgramDescriptor> key) {
    Boolean val = myStates.get(key);
    assert val != null;
    return val;
  }

  public void setState(Class<? extends EarlyAccessProgramDescriptor> key, boolean itemSelected) {
    myStates.put(key, itemSelected);
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("state");
    for (Map.Entry<Class<? extends EarlyAccessProgramDescriptor>, Boolean> k : myStates.entrySet()) {
      EarlyAccessProgramDescriptor extension = EarlyAccessProgramDescriptor.EP_NAME.findExtension(k.getKey());
      if(extension.getDefaultState() == k.getValue()) {
        continue;
      }

      Element child = new Element("state");
      child.setAttribute("class", k.getKey().getName());
      child.setAttribute("value", String.valueOf(k.getValue()));

      element.addContent(child);
    }
    return element;
  }

  @Override
  public void loadState(Element state) {
    Map<String, EarlyAccessProgramDescriptor> map = descriptorToMap();

    for (Element element : state.getChildren()) {
      String aClass = element.getAttributeValue("class");

      EarlyAccessProgramDescriptor descriptor = map.get(aClass);
      if(descriptor == null) {
        continue;
      }

      Boolean value = Boolean.parseBoolean(element.getAttributeValue("value"));

      myStates.put(descriptor.getClass(), value);
    }
  }

  private static Map<String, EarlyAccessProgramDescriptor> descriptorToMap() {
    Map<String, EarlyAccessProgramDescriptor> map = new HashMap<String, EarlyAccessProgramDescriptor>();
    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensions()) {
      map.put(descriptor.getClass().getName(), descriptor);
    }
    return map;
  }
}
