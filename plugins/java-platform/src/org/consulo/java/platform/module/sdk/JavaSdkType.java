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
package org.consulo.java.platform.module.sdk;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.projectRoots.*;
import org.consulo.java.platform.JavaPlatformIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 21:13/19.05.13
 */
public class JavaSdkType extends SdkType {
  public JavaSdkType() {
    super("JRE/JDK");
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return JavaPlatformIcons.Java;
  }

  @Nullable
  @Override
  public Icon getGroupIcon() {
    return AllIcons.Nodes.PpJdk;
  }

  @Nullable
  @Override
  public String suggestHomePath() {
    return null;
  }

  @Override
  public boolean isValidSdkHome(String path) {
    return true;
  }

  @Nullable
  @Override
  public String getVersionString(String sdkHome) {
    return "1.0";
  }

  @Override
  public String suggestSdkName(String currentSdkName, String sdkHome) {
    return sdkHome;
  }

  @Nullable
  @Override
  public AdditionalDataConfigurable createAdditionalDataConfigurable(SdkModel sdkModel, SdkModificator sdkModificator) {
    return null;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return getName();
  }

  @Override
  public void saveAdditionalData(SdkAdditionalData additionalData, Element additional) {

  }
}
