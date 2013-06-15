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
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import org.consulo.module.extension.ModuleInheritableNamedPointer;
import org.consulo.module.extension.MutableModuleInheritableNamedPointer;
import org.consulo.util.pointers.Named;
import org.consulo.util.pointers.NamedPointer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 19:31/15.06.13
 */
public abstract class ModuleInheritableNamedPointerImpl<T extends Named> implements MutableModuleInheritableNamedPointer<T> {
  private NamedPointer<Module> myModulePointer;
  private NamedPointer<T> myTargetPointer;
  private final Project myProject;
  private final String myXmlPrefix;

  protected ModuleInheritableNamedPointerImpl(Project project, String xmlPrefix) {
    myProject = project;
    myXmlPrefix = xmlPrefix;
  }

  @Nullable
  public abstract String getItemNameFromModule(@NotNull Module module);

  @Nullable
  public abstract T getItemFromModule(@NotNull Module module);

  @NotNull
  public abstract NamedPointer<T> getPointer(@NotNull Project project, @NotNull String name);

  @Nullable
  @Override
  public T get() {
    if (myModulePointer != null) {
      final Module module = myModulePointer.get();
      if (module == null) {
        return null;
      }
      return getItemFromModule(module);
    }
    return myTargetPointer == null ? getDefaultValue() : myTargetPointer.get();
  }

  @Nullable
  @Override
  public String getName() {
    if (myModulePointer != null) {
      final Module module = myModulePointer.get();
      if (module == null) {
        return null;
      }
      return getItemNameFromModule(module);
    }
    return myTargetPointer == null ? null : myTargetPointer.getName();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ModuleInheritableNamedPointerImpl) {
      final ModuleInheritableNamedPointerImpl another = (ModuleInheritableNamedPointerImpl)obj;
      if (!Comparing.equal(myModulePointer, another.myModulePointer)) {
        return false;
      }
      if (!Comparing.equal(myTargetPointer, another.myTargetPointer)) {
        return false;
      }
      return true;
    }
    else {
      return false;
    }
  }

  @Override
  public void set(ModuleInheritableNamedPointer<T> anotherItem) {
    if (anotherItem.isNull()) {
      myModulePointer = null;
      myTargetPointer = null;
    }
    else {
      final String moduleName = anotherItem.getModuleName();
      myModulePointer = moduleName == null ? null : ModuleUtilCore.createPointer(myProject, moduleName);

      if (myModulePointer == null) {
        final String targetName = anotherItem.getName();
        myTargetPointer = getPointer(myProject, targetName);
      }
    }
  }

  @Override
  public void set(@Nullable String moduleName, @Nullable String name) {
    myModulePointer = moduleName == null ? null : ModuleUtilCore.createPointer(myProject, moduleName);
    myTargetPointer = name == null ? null : getPointer(myProject, name);
  }

  public void toXml(Element element) {
    element.setAttribute(myXmlPrefix + "-module-name", StringUtil.notNullize(myModulePointer == null ? null : myModulePointer.getName()));
    element.setAttribute(myXmlPrefix + "-name", StringUtil.notNullize(myTargetPointer == null ? null : myTargetPointer.getName()));
  }

  public void fromXml(Element element) {
    final String moduleName = StringUtil.nullize(element.getAttributeValue(myXmlPrefix + "-module-name"));
    if (moduleName != null) {
      myModulePointer = ModuleUtilCore.createPointer(myProject, moduleName);
    }
    final String itemName = StringUtil.nullize(element.getAttributeValue(myXmlPrefix + "-name"));
    if (itemName != null) {
      myTargetPointer = getPointer(myProject, itemName);
    }
  }

  @Nullable
  public T getDefaultValue() {
    return null;
  }

  @Nullable
  @Override
  public Module getModule() {
    if (myModulePointer == null) {
      return null;
    }
    return myModulePointer.get();
  }

  @Nullable
  @Override
  public String getModuleName() {
    if (myModulePointer == null) {
      return null;
    }
    return myModulePointer.getName();
  }

  @Override
  public boolean isNull() {
    return myModulePointer == null && myTargetPointer == null;
  }
}
