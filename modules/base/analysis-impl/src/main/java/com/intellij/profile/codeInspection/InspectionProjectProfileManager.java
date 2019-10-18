/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.profile.codeInspection;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.profile.DefaultProjectProfileManager;
import com.intellij.profile.Profile;
import com.intellij.psi.PsiElement;
import org.jdom.Element;

import javax.annotation.Nonnull;

/**
 * User: anna
 * Date: 30-Nov-2005
 */
public abstract class InspectionProjectProfileManager extends DefaultProjectProfileManager
        implements ProjectComponent, SeverityProvider, PersistentStateComponent<Element> {
  public InspectionProjectProfileManager(@Nonnull Project project,
                                         @Nonnull InspectionProfileManager inspectionProfileManager,
                                         @Nonnull DependencyValidationManager holder) {
    super(project, inspectionProfileManager, holder);
  }

  public static InspectionProjectProfileManager getInstance(Project project) {
    return project.getComponent(InspectionProjectProfileManager.class);
  }

  @Override
  public String getProfileName() {
    return getInspectionProfile().getName();
  }

  public InspectionProfileImpl getCurrentProfile() {
    return (InspectionProfileImpl)getInspectionProfile();
  }

  @Nonnull
  public InspectionProfile getInspectionProfile() {
    return (InspectionProfile)getProjectProfileImpl();
  }

  /**
   * @deprecated use {@link #getInspectionProfile()} instead
   */
  @SuppressWarnings({"UnusedDeclaration"})
  @Nonnull
  public InspectionProfile getInspectionProfile(PsiElement element) {
    return getInspectionProfile();
  }

  public abstract boolean isProfileLoaded();

  public abstract void initProfileWrapper(@Nonnull Profile profile);

  @Override
  public Profile getProfile(@Nonnull final String name) {
    return getProfile(name, true);
  }

  public static boolean isInformationLevel(String shortName, @Nonnull PsiElement element) {
    final HighlightDisplayKey key = HighlightDisplayKey.find(shortName);
    if (key != null) {
      final HighlightDisplayLevel errorLevel = getInstance(element.getProject()).getInspectionProfile().getErrorLevel(key, element);
      return HighlightDisplayLevel.DO_NOT_SHOW.equals(errorLevel);
    }
    return false;
  }
}
