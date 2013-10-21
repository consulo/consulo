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
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.pom.java.LanguageLevel;
import org.consulo.java.module.extension.JavaModuleExtension;
import org.consulo.module.extension.ModuleInheritableNamedPointer;
import org.consulo.module.extension.impl.ModuleExtensionWithSdkImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 10:02/19.05.13
 */
public class JavaModuleExtensionImpl extends ModuleExtensionWithSdkImpl<JavaModuleExtensionImpl>
  implements JavaModuleExtension<JavaModuleExtensionImpl> {
  private static final String SPECIAL_DIR_LOCATION = "special-dir-location";

  protected LanguageLevelModuleInheritableNamedPointerImpl myLanguageLevel;
  protected SpecialDirLocation mySpecialDirLocation = SpecialDirLocation.MODULE_DIR;

  public JavaModuleExtensionImpl(@NotNull String id, @NotNull Module module) {
    super(id, module);
    myLanguageLevel = new LanguageLevelModuleInheritableNamedPointerImpl(module.getProject(), id);
  }

  @Override
  public void commit(@NotNull JavaModuleExtensionImpl mutableModuleExtension) {
    super.commit(mutableModuleExtension);

    myLanguageLevel.set(mutableModuleExtension.getInheritableLanguageLevel());
    mySpecialDirLocation = mutableModuleExtension.getSpecialDirLocation();
  }

  @NotNull
  public LanguageLevel getLanguageLevel() {
    return myLanguageLevel.get();
  }

  @NotNull
  public SpecialDirLocation getSpecialDirLocation() {
    return mySpecialDirLocation;
  }

  @NotNull
  public ModuleInheritableNamedPointer<LanguageLevel> getInheritableLanguageLevel() {
    return myLanguageLevel;
  }

  @Override
  protected Class<? extends SdkType> getSdkTypeClass() {
    return JavaSdk.class;
  }

  @Override
  protected void getStateImpl(@NotNull Element element) {
    super.getStateImpl(element);

    myLanguageLevel.toXml(element);
    element.setAttribute(SPECIAL_DIR_LOCATION, mySpecialDirLocation.name());
  }

  @Override
  protected void loadStateImpl(@NotNull Element element) {
    super.loadStateImpl(element);

    myLanguageLevel.fromXml(element);
    mySpecialDirLocation =
      SpecialDirLocation.valueOf(element.getAttributeValue(SPECIAL_DIR_LOCATION, SpecialDirLocation.MODULE_DIR.name()));
  }
}