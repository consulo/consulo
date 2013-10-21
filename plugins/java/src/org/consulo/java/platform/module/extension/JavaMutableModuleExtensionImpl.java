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
package org.consulo.java.platform.module.extension;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.pom.java.LanguageLevel;
import lombok.NonNull;
import org.consulo.java.platform.module.extension.ui.JavaModuleExtensionPanel;
import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import org.consulo.module.extension.MutableModuleInheritableNamedPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12:39/19.05.13
 */
public class JavaMutableModuleExtensionImpl extends JavaModuleExtensionImpl
  implements MutableModuleExtensionWithSdk<JavaModuleExtensionImpl> {
  @NotNull
  private final JavaModuleExtensionImpl myModuleExtension;

  public JavaMutableModuleExtensionImpl(@NotNull String id, @NotNull Module module, @NotNull JavaModuleExtensionImpl moduleExtension) {
    super(id, module);
    myModuleExtension = moduleExtension;
  }

  @Nullable
  @Override
  public JComponent createConfigurablePanel(@NonNull ModifiableRootModel rootModel, @Nullable Runnable updateOnCheck) {
    return new JavaModuleExtensionPanel(this, updateOnCheck);
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  @NotNull
  public MutableModuleInheritableNamedPointer<LanguageLevel> getInheritableLanguageLevel() {
    return myLanguageLevel;
  }

  public void setSpecialDirLocation(SpecialDirLocation specialDirLocation) {
    mySpecialDirLocation = specialDirLocation;
  }

  @Override
  public boolean isModified() {
    if(isModifiedImpl(myModuleExtension)) {
      return true;
    }

    if(!myLanguageLevel.equals(myModuleExtension.getInheritableLanguageLevel())) {
      return true;
    }

    if(!mySpecialDirLocation.equals(myModuleExtension.getSpecialDirLocation())) {
      return true;
    }
    return false;
  }

  @Override
  public void commit() {
    myModuleExtension.commit(this);
  }

  @NotNull
  @Override
  public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk() {
    return (MutableModuleInheritableNamedPointer<Sdk>)super.getInheritableSdk();
  }
}
