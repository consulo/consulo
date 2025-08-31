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
package consulo.language.editor.impl.internal.inspection;

import consulo.component.persist.PersistentStateComponent;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.packageDependency.DependencyValidationManager;
import consulo.language.editor.impl.internal.inspection.scheme.DefaultProjectProfileManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2005-11-30
 */
public abstract class InspectionProjectProfileManager extends DefaultProjectProfileManager
        implements SeverityProvider, PersistentStateComponent<Element>, consulo.language.editor.inspection.scheme.InspectionProjectProfileManager {
  public InspectionProjectProfileManager(@Nonnull Project project, @Nonnull InspectionProfileManager inspectionProfileManager, @Nonnull DependencyValidationManager holder) {
    super(project, inspectionProfileManager, holder);
  }

  public static InspectionProjectProfileManager getInstance(Project project) {
    return (InspectionProjectProfileManager)project.getComponent(consulo.language.editor.inspection.scheme.InspectionProjectProfileManager.class);
  }

  @Override
  public String getProfileName() {
    return getInspectionProfile().getName();
  }

  @Override
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
  public Profile getProfile(@Nonnull String name) {
    return getProfile(name, true);
  }
}
