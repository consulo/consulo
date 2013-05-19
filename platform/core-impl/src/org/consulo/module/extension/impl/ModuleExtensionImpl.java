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
package org.consulo.module.extension.impl;

import com.intellij.openapi.module.Module;
import org.consulo.module.extension.ModuleExtension;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12:30/19.05.13
 */
public class ModuleExtensionImpl<T extends ModuleExtension<T>> implements ModuleExtension<T> {
  protected boolean myIsEnabled;
  private final String myId;
  private final Module myModule;

  public ModuleExtensionImpl(@NotNull String id, @NotNull Module module) {
    myId = id;
    myModule = module;
  }

  @NotNull
  @Override
  public String getId() {
    return myId;
  }

  @Override
  public boolean isEnabled() {
    return myIsEnabled;
  }

  @NotNull
  @Override
  public Module getModule() {
    return myModule;
  }

  @Override
  public void commit(@NotNull T mutableModuleExtension) {
    myIsEnabled = mutableModuleExtension.isEnabled();
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("extension");
    element.setAttribute("id", myId);
    element.setAttribute("enabled", String.valueOf(myIsEnabled));

    getStateImpl(element);

    return element;
  }

  protected void getStateImpl(@NotNull Element element) {

  }

  @Override
  public void loadState(Element state) {
    for(Element element : state.getChildren("extension")) {
      final String id = element.getAttributeValue("id");
      if(myId.equals(id)) {
        myIsEnabled = Boolean.valueOf(element.getAttributeValue("enabled"));

        loadStateImpl(element);
        break;
      }
    }
  }

  protected void loadStateImpl(@NotNull Element element) {

  }
}
