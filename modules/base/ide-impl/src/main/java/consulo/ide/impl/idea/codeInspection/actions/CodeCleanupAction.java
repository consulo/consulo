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
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.impl.inspection.GlobalInspectionContextBase;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.ide.impl.idea.profile.codeInspection.ui.IDEInspectionToolsConfigurable;

public class CodeCleanupAction extends CodeInspectionAction {
    public static final String CODE_CLEANUP_INSPECTIONS_DISPLAY_NAME = "Code Cleanup Inspections";

    public CodeCleanupAction() {
        super(
            LocalizeValue.localizeTODO("Code Cleanup"),
            LocalizeValue.localizeTODO("Code Cleanup"),
            LocalizeValue.localizeTODO("Code Cleanup"),
            LocalizeValue.localizeTODO("Code Cleanup")
        );
    }

    @Override
    protected void runInspections(Project project, AnalysisScope scope) {
        InspectionProfile profile =
            myExternalProfile != null ? myExternalProfile : InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
        InspectionManager managerEx = InspectionManager.getInstance(project);
        GlobalInspectionContextBase globalContext = (GlobalInspectionContextBase) managerEx.createNewGlobalContext(false);
        globalContext.codeCleanup(project, scope, profile, getTemplatePresentation().getText(), null, false);
    }

    @Override
    protected IDEInspectionToolsConfigurable createConfigurable(
        InspectionProjectProfileManager projectProfileManager,
        InspectionProfileManager profileManager
    ) {
        return new IDEInspectionToolsConfigurable(projectProfileManager, profileManager) {
            @Override
            protected boolean acceptTool(InspectionToolWrapper entry) {
                return super.acceptTool(entry) && entry.isCleanupTool();
            }

            @Override
            public String getDisplayName() {
                return CODE_CLEANUP_INSPECTIONS_DISPLAY_NAME;
            }
        };
    }
}
