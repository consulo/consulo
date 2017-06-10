/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.eap;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.HashMap;
import com.intellij.util.containers.hash.LinkedHashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 17:11/15.10.13
 */
@State(name = "EarlyAccessProgramManager", storages = @Storage("eap.xml"))
public class EarlyAccessProgramManager implements PersistentStateComponent<Element> {
  @NotNull
  public static EarlyAccessProgramManager getInstance() {
    return ServiceManager.getService(EarlyAccessProgramManager.class);
  }

  public static boolean is(@NotNull Class<? extends EarlyAccessProgramDescriptor> key) {
    return getInstance().getState(key);
  }

  private static final Logger LOGGER = Logger.getInstance(EarlyAccessProgramManager.class);
  private Map<Class<? extends EarlyAccessProgramDescriptor>, Boolean> myStates = new LinkedHashMap<>();

  public EarlyAccessProgramManager() {
    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensions()) {
      myStates.put(descriptor.getClass(), descriptor.getDefaultState());
    }
  }

  public boolean getState(@NotNull Class<? extends EarlyAccessProgramDescriptor> key) {
    Boolean value = myStates.get(key);
    if (value == null) {
      LOGGER.error("Descriptor is not registered: " + key.getName());
      return false;
    }
    return value;
  }

  public void setState(Class<? extends EarlyAccessProgramDescriptor> key, boolean itemSelected) {
    myStates.put(key, itemSelected);
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("state");
    for (Map.Entry<Class<? extends EarlyAccessProgramDescriptor>, Boolean> entry : myStates.entrySet()) {
      EarlyAccessProgramDescriptor extension = EarlyAccessProgramDescriptor.EP_NAME.findExtension(entry.getKey());
      if (extension.getDefaultState() == entry.getValue()) {
        continue;
      }

      Element child = new Element("state");
      child.setAttribute("class", entry.getKey().getName());
      child.setAttribute("value", String.valueOf(entry.getValue()));

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
      if (descriptor == null) {
        continue;
      }

      Boolean value = Boolean.parseBoolean(element.getAttributeValue("value"));

      myStates.put(descriptor.getClass(), value);
    }
  }

  private static Map<String, EarlyAccessProgramDescriptor> descriptorToMap() {
    Map<String, EarlyAccessProgramDescriptor> map = new HashMap<>();
    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensions()) {
      map.put(descriptor.getClass().getName(), descriptor);
    }
    return map;
  }
}
