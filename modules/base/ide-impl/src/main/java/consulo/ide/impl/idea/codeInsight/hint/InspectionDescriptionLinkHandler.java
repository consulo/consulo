/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.TooltipLinkHandler;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.logging.Logger;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

/**
 * Handles tooltip links in format <code>#inspection/inspection_short_name</code>.
 * On a click or expend acton returns more detailed description for given inspection.
 *
 * @author peter
 */
@ExtensionImpl
public class InspectionDescriptionLinkHandler extends TooltipLinkHandler {
  private static final Logger LOG = Logger.getInstance(InspectionDescriptionLinkHandler.class);

  @Nonnull
  @Override
  public String getPrefix() {
    return "#inspection/";
  }

  @Override
  public String getDescription(@Nonnull String refSuffix, @Nonnull Editor editor) {
    Project project = editor.getProject();
    if (project == null) {
      LOG.error(editor);
      return null;
    }

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) {
      return null;
    }

    InspectionProfile profile = (InspectionProfile)InspectionProfileManager.getInstance().getRootProfile();
    InspectionToolWrapper toolWrapper = profile.getInspectionTool(refSuffix, file);
    if (toolWrapper == null) return null;

    String description = toolWrapper.loadDescription();
    if (description == null) {
      LOG.warn("No description for inspection '" + refSuffix + "'");
      description = InspectionsBundle.message("inspection.tool.description.under.construction.text");
    }
    return description;
  }
}
