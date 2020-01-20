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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkImpl;
import com.intellij.openapi.projectRoots.ui.BaseSdkEditor;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.SdkProjectStructureElement;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.ui.navigation.History;
import com.intellij.ui.navigation.Place;
import consulo.bundle.SdkUtil;
import consulo.bundle.ui.SdkEditor;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * User: anna
 * Date: 05-Jun-2006
 */
public class SdkConfigurable extends ProjectStructureElementConfigurable<Sdk> implements Place.Navigator {
  private final SdkImpl mySdk;
  private final BaseSdkEditor mySdkEditor;
  private final SdkProjectStructureElement myProjectStructureElement;

  public SdkConfigurable(@Nonnull final SdkImpl sdk,
                         final SdkModel sdksModel,
                         final Runnable updateTree,
                         @Nonnull History history,
                         Project project) {
    super(!sdk.isPredefined(), updateTree);
    mySdk = sdk;
    mySdkEditor = createSdkEditor(sdksModel, history, mySdk);
    final StructureConfigurableContext context = ModuleStructureConfigurable.getInstance(project).getContext();
    myProjectStructureElement = new SdkProjectStructureElement(context, mySdk);
  }

  public SdkConfigurable(@Nonnull final SdkImpl sdk, final SdkModel sdksModel, final Runnable updateTree, @Nonnull History history, boolean noContext) {
    super(!sdk.isPredefined(), updateTree);
    mySdk = sdk;
    mySdkEditor = createSdkEditor(sdksModel, history, mySdk);
    myProjectStructureElement = new SdkProjectStructureElement(null, sdk);
  }

  protected BaseSdkEditor createSdkEditor(SdkModel sdksModel, History history, SdkImpl projectJdk) {
    return new SdkEditor(sdksModel, history, projectJdk);
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
  @NonNls
  public String getHelpTopic() {
    return ((SdkType) mySdk.getSdkType()).getHelpTopic();
  }

  @Override
  public JComponent createOptionsPanel() {
    return mySdkEditor.createComponent();
  }

  @Override
  public boolean isModified() {
    return mySdkEditor.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    mySdkEditor.apply();
  }

  @Override
  public void reset() {
    mySdkEditor.reset();
  }

  @Override
  public void disposeUIResources() {
    mySdkEditor.disposeUIResources();
  }

  @Override
  public void setHistory(final History history) {
  }

  @Override
  public AsyncResult<Void> navigateTo(@Nullable final Place place, final boolean requestFocus) {
    return mySdkEditor.navigateTo(place, requestFocus);
  }

  @Override
  public void queryPlace(@Nonnull final Place place) {
    mySdkEditor.queryPlace(place);
  }

  public Sdk getSdk() {
    return mySdk;
  }
}
