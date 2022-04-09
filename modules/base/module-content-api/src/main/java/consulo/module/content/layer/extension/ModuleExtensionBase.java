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
package consulo.module.content.layer.extension;

import consulo.annotation.access.RequiredReadAction;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12:30/19.05.13
 */
public class ModuleExtensionBase<T extends ModuleExtension<T>> implements ModuleExtension<T> {
  public static final String ELEMENT_NAME = "extension";

  protected boolean myIsEnabled;
  protected final String myId;
  protected final ModuleRootLayer myModuleRootLayer;

  public ModuleExtensionBase(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer) {
    myId = id;
    myModuleRootLayer = moduleRootLayer;
  }

  @Nonnull
  public ModuleRootLayer getModuleRootLayer() {
    return myModuleRootLayer;
  }

  @Nonnull
  @Override
  public String getId() {
    return myId;
  }

  @Override
  public boolean isEnabled() {
    return myIsEnabled;
  }

  @Nonnull
  @Override
  public Module getModule() {
    return myModuleRootLayer.getModule();
  }

  @RequiredReadAction
  @Override
  public void commit(@Nonnull T mutableModuleExtension) {
    myIsEnabled = mutableModuleExtension.isEnabled();
  }

  @Nullable
  @Override
  public final Element getState() {
    if (!isEnabled()) {
      return null;
    }
    Element element = new Element(ELEMENT_NAME);
    element.setAttribute("id", myId);

    getStateImpl(element);

    return element;
  }

  protected void getStateImpl(@Nonnull Element element) {

  }

  @Override
  @RequiredReadAction
  public final void loadState(Element state) {
    myIsEnabled = true;

    loadStateImpl(state);
  }

  @RequiredReadAction
  protected void loadStateImpl(@Nonnull Element element) {

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof ModuleExtensionBase)) return false;

    ModuleExtensionBase that = (ModuleExtensionBase)o;

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
