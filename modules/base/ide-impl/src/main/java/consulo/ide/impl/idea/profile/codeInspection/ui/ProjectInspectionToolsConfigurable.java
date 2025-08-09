/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.profile.codeInspection.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.ide.impl.idea.profile.codeInspection.ui.header.InspectionToolsConfigurable;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.localize.LocalizeValue;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author anna
 * @since 2009-04-17
 */
@ExtensionImpl
public class ProjectInspectionToolsConfigurable extends InspectionToolsConfigurable implements ProjectConfigurable {
  @Inject
  public ProjectInspectionToolsConfigurable(InspectionProfileManager profileManager, InspectionProjectProfileManager projectProfileManager) {
    super((consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager)projectProfileManager, profileManager);
  }

  @Nonnull
  @Override
  public String getId() {
    return "editor.code.inspections";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Code Inspections");
  }

  @Override
  protected InspectionProfileImpl getCurrentProfile() {
    return (InspectionProfileImpl)myProjectProfileManager.getProjectProfileImpl();
  }

  @Override
  protected void applyRootProfile(String name, boolean isShared) {
    if (isShared) {
      myProjectProfileManager.setProjectProfile(name);
    }
    else {
      myProfileManager.setRootProfile(name);
      myProjectProfileManager.setProjectProfile(null);
    }
  }
}