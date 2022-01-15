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
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 17:11/15.10.13
 */
@Singleton
@State(name = "EarlyAccessProgramManager", storages = @Storage("eap.xml"))
public class EarlyAccessProgramManager implements PersistentStateComponent<Element> {
  @Nonnull
  public static EarlyAccessProgramManager getInstance() {
    return ServiceManager.getService(EarlyAccessProgramManager.class);
  }

  public static boolean is(@Nonnull Class<? extends EarlyAccessProgramDescriptor> key) {
    return getInstance().getState(key);
  }

  private static final Logger LOG = Logger.getInstance(EarlyAccessProgramManager.class);

  private final Map<Class<? extends EarlyAccessProgramDescriptor>, Boolean> myStates = new LinkedHashMap<>();

  @Inject
  public EarlyAccessProgramManager() {
  }

  public boolean getState(@Nonnull Class<? extends EarlyAccessProgramDescriptor> key) {
    Boolean value = myStates.get(key);
    if (value == null) {
      EarlyAccessProgramDescriptor extension = EarlyAccessProgramDescriptor.EP_NAME.findExtension(key);
      return extension != null && extension.getDefaultState();
    }
    return value;
  }

  public void setState(Class<? extends EarlyAccessProgramDescriptor> key, boolean itemSelected) {
    EarlyAccessProgramDescriptor extension = EarlyAccessProgramDescriptor.EP_NAME.findExtensionOrFail(key);

    if(extension.getDefaultState() == itemSelected) {
      myStates.remove(key);
    }
    else {
      myStates.put(key, itemSelected);
    }
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("state");
    for (Map.Entry<Class<? extends EarlyAccessProgramDescriptor>, Boolean> entry : myStates.entrySet()) {
      EarlyAccessProgramDescriptor extension = EarlyAccessProgramDescriptor.EP_NAME.findExtension(entry.getKey());
      if (extension == null || extension.getDefaultState() == entry.getValue()) {
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
    myStates.clear();
    
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
    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensionList()) {
      map.put(descriptor.getClass().getName(), descriptor);
    }
    return map;
  }
}
