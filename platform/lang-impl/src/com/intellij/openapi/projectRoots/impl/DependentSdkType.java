/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Ref;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import consulo.annotations.DeprecationInfo;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 *         Date: 12/5/12
 */
@Deprecated
@DeprecationInfo(value = "DependentSdkType is legacy from IDEA, where modules cant hold more than one Sdk", until = "2.0")
public abstract class DependentSdkType extends SdkType {

  public DependentSdkType(@NonNls String name) {
    super(name);
  }

  /**
   * Checks if dependencies satisfied.
   */
  protected boolean checkDependency(SdkModel sdkModel) {
    return ContainerUtil.find(sdkModel.getSdks(), new Condition<Sdk>() {
      @Override
      public boolean value(Sdk sdk) {
        return isValidDependency(sdk);
      }
    }) != null;
  }

  protected abstract boolean isValidDependency(Sdk sdk);

  public abstract String getUnsatisfiedDependencyMessage();

  @Override
  public boolean supportsCustomCreateUI() {
    return true;
  }

  @Override
  public void showCustomCreateUI(final SdkModel sdkModel, JComponent parentComponent, final Consumer<Sdk> sdkCreatedCallback) {
    if (!checkDependency(sdkModel)) {
      if (Messages.showOkCancelDialog(parentComponent, getUnsatisfiedDependencyMessage(), "Cannot Create SDK", Messages.getWarningIcon()) != Messages.OK) {
        return;
      }
      if (fixDependency(sdkModel, sdkCreatedCallback) == null) {
        return;
      }
    }

    createSdkOfType(sdkModel, this, sdkCreatedCallback);
  }

  protected abstract SdkType getDependencyType();

  protected Sdk fixDependency(SdkModel sdkModel, Consumer<Sdk> sdkCreatedCallback) {
    return createSdkOfType(sdkModel, getDependencyType(), sdkCreatedCallback);
  }
  
  protected static Sdk createSdkOfType(final SdkModel sdkModel,
                                  final SdkType sdkType,
                                  final Consumer<Sdk> sdkCreatedCallback) {
    final Ref<Sdk> result = new Ref<Sdk>(null);
    SdkConfigurationUtil.selectSdkHome(sdkType, new Consumer<String>() {
      @Override
      public void consume(final String home) {
        String newSdkName = SdkConfigurationUtil.createUniqueSdkName(sdkType, home, sdkModel.getSdks());
        final SdkImpl newJdk = new SdkImpl(newSdkName, sdkType);
        newJdk.setHomePath(home);

        sdkCreatedCallback.consume(newJdk);
        result.set(newJdk);
      }
    });
    return result.get();
  }
}
