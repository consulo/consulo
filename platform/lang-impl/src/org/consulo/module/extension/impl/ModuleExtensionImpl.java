/*
 * Copyright 2013 must-be.org
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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableModuleRootLayer;
import com.intellij.openapi.roots.ModuleRootLayer;
import org.consulo.module.extension.ModuleExtension;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 12:30/19.05.13
 */
public class ModuleExtensionImpl<T extends ModuleExtension<T>> implements ModuleExtension<T> {
  @NotNull
  protected static JComponent wrapToNorth(@NotNull JComponent component) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(component, BorderLayout.NORTH);
    return panel;
  }

  protected boolean myIsEnabled;
  protected final String myId;
  protected final ModuleRootLayer myModuleRootLayer;

  public ModuleExtensionImpl(@NotNull String id, @NotNull ModuleRootLayer moduleRootLayer) {
    myId = id;
    myModuleRootLayer = moduleRootLayer;
  }

  @NotNull
  @Override
  public ModifiableModuleRootLayer getModuleRootLayer() {
    return (ModifiableModuleRootLayer)myModuleRootLayer;
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
    return myModuleRootLayer.getModule();
  }

  @NotNull
  @Override
  public Project getProject() {
    return getModule().getProject();
  }

  @Override
  public void commit(@NotNull T mutableModuleExtension) {
    myIsEnabled = mutableModuleExtension.isEnabled();
  }

  @Nullable
  @Override
  public final Element getState() {
    if (!isEnabled()) {
      return null;
    }
    Element element = new Element("extension");
    element.setAttribute("id", myId);

    getStateImpl(element);

    return element;
  }

  protected void getStateImpl(@NotNull Element element) {

  }

  @Override
  @RequiredReadAction
  public final void loadState(Element state) {
    myIsEnabled = true;

    loadStateImpl(state);
  }

  @RequiredReadAction
  protected void loadStateImpl(@NotNull Element element) {

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ModuleExtensionImpl that = (ModuleExtensionImpl)o;

    if (!myId.equals(that.myId)) return false;
    if (!getModule().equals(that.getModule())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myId.hashCode();
    result = 31 * result + getModule().hashCode();
    return result;
  }
}
