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

package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.projectRoots.ui.BaseSdkEditor;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.SdkProjectStructureElement;
import consulo.bundle.SdkUtil;
import consulo.bundle.ui.SdkEditor;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * User: anna
 * Date: 05-Jun-2006
 */
public class SdkConfigurable extends ProjectStructureElementConfigurable<Sdk> {
  private final SdkImpl mySdk;
  private final BaseSdkEditor mySdkEditor;
  private final SdkProjectStructureElement myProjectStructureElement;

  public SdkConfigurable(@Nonnull final SdkImpl sdk, final SdkModel sdksModel, final Runnable updateTree) {
    super(!sdk.isPredefined(), updateTree);
    mySdk = sdk;
    mySdkEditor = createSdkEditor(sdksModel, mySdk);
    myProjectStructureElement = new SdkProjectStructureElement(mySdk);
  }

  protected BaseSdkEditor createSdkEditor(SdkModel sdksModel, SdkImpl projectJdk) {
    return new SdkEditor(sdksModel, projectJdk);
  }

  @Override
  public ProjectStructureElement getProjectStructureElement() {
    return myProjectStructureElement;
  }

  @Override
  public void setDisplayName(final String name) {
    mySdk.setName(name);
  }

  @Override
  public Sdk getEditableObject() {
    return mySdk;
  }

  @Override
  public String getBannerSlogan() {
    return ProjectBundle.message("sdk.banner.text", ((SdkType)mySdk.getSdkType()).getPresentableName(), mySdk.getName());
  }

  @Override
  public String getDisplayName() {
    return mySdk.getName();
  }

  @Override
  public Image getIcon(boolean open) {
    return SdkUtil.getIcon(mySdk);
  }

  @Override
  @Nullable
  public String getHelpTopic() {
    return ((SdkType)mySdk.getSdkType()).getHelpTopic();
  }

  @RequiredUIAccess
  @Override
  public JComponent createOptionsPanel(@Nonnull Disposable parentDisposable) {
    return mySdkEditor.createComponent(parentDisposable);
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return mySdkEditor.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    mySdkEditor.apply();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    mySdkEditor.reset();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    mySdkEditor.disposeUIResources();
  }

  public Sdk getSdk() {
    return mySdk;
  }
}
