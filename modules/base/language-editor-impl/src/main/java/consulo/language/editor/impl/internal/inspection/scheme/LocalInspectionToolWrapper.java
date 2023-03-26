/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.language.editor.impl.internal.inspection.scheme;

import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.JobDescriptor;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
public class LocalInspectionToolWrapper extends InspectionToolWrapper<LocalInspectionTool> {
  public LocalInspectionToolWrapper(@Nonnull LocalInspectionTool tool) {
    super(tool);
  }

  private LocalInspectionToolWrapper(@Nonnull LocalInspectionToolWrapper other) {
    super(other);
  }

  @Nonnull
  @Override
  public LocalInspectionToolWrapper createCopy() {
    return new LocalInspectionToolWrapper(this);
  }

  @Override
  @Nonnull
  public JobDescriptor[] getJobDescriptors(@Nonnull GlobalInspectionContext context) {
    return context.getStdJobDescriptors().LOCAL_ANALYSIS_ARRAY;
  }

  public boolean isUnfair() {
    return getTool() instanceof UnfairLocalInspectionTool;
  }

  public String getID() {
    return getTool().getID();
  }

  @Nullable
  public String getAlternativeID() {
    return getTool().getAlternativeID();
  }

  public boolean runForWholeFile() {
    return getTool().runForWholeFile();
  }

  public static InspectionToolWrapper findTool2RunInBatch(@Nonnull Project project, @Nullable PsiElement element, @Nonnull String name) {
    final InspectionProfile inspectionProfile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    final InspectionToolWrapper toolWrapper = element == null ? inspectionProfile.getInspectionTool(name, project) : inspectionProfile.getInspectionTool(name, element);
    if (toolWrapper instanceof LocalInspectionToolWrapper && ((LocalInspectionToolWrapper)toolWrapper).isUnfair()) {
      final LocalInspectionTool inspectionTool = ((LocalInspectionToolWrapper)toolWrapper).getTool();
      if (inspectionTool instanceof PairedUnfairLocalInspectionTool) {
        final String oppositeShortName = ((PairedUnfairLocalInspectionTool)inspectionTool).getInspectionForBatchShortName();
        return element == null ? inspectionProfile.getInspectionTool(oppositeShortName, project) : inspectionProfile.getInspectionTool(oppositeShortName, element);
      }
      return null;
    }
    return toolWrapper;
  }
}
