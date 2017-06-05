/*
 * Copyright 2013-2017 consulo.io
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
package consulo.externalSystem.module.extension.impl;

import consulo.annotations.RequiredReadAction;
import consulo.externalSystem.module.extension.ExternalSystemModuleExtension;
import consulo.module.extension.impl.ModuleExtensionImpl;
import consulo.roots.ModuleRootLayer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 03-Jun-17
 */
public class ExternalSystemModuleExtensionImpl extends ModuleExtensionImpl<ExternalSystemModuleExtensionImpl>
        implements ExternalSystemModuleExtension<ExternalSystemModuleExtensionImpl> {
  protected final Map<String, String> myOptions = new LinkedHashMap<>();

  public ExternalSystemModuleExtensionImpl(@NotNull String id, @NotNull ModuleRootLayer moduleRootLayer) {
    super(id, moduleRootLayer);
  }

  @RequiredReadAction
  @Override
  public void commit(@NotNull ExternalSystemModuleExtensionImpl mutableModuleExtension) {
    super.commit(mutableModuleExtension);
    myOptions.clear();
    myOptions.putAll(mutableModuleExtension.myOptions);
  }

  @RequiredReadAction
  @Override
  protected void loadStateImpl(@NotNull Element element) {
    for (Element option : element.getChildren("option")) {
      String name = option.getAttributeValue("name");
      if (name == null) {
        continue;
      }
      myOptions.put(name, option.getValue());
    }
  }

  @Override
  protected void getStateImpl(@NotNull Element element) {
    for (Map.Entry<String, String> entry : myOptions.entrySet()) {
      Element option = new Element("option");
      option.setAttribute("name", entry.getKey());
      option.setText(entry.getValue());

      element.addContent(option);
    }
  }

  @Nullable
  @Override
  public String getOption(@NotNull String key) {
    return myOptions.get(key);
  }
}
