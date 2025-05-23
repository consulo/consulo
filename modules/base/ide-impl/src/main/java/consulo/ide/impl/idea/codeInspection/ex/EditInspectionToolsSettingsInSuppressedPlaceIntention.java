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

package consulo.ide.impl.idea.codeInspection.ex;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.InspectionExtensionsFactory;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author cdr
 */
@IntentionMetaData(ignoreId = "platform.edit.intention.options", fileExtensions = "txt", categories = "General")
public class EditInspectionToolsSettingsInSuppressedPlaceIntention implements IntentionAction {
    private String myId;
    private String myDisplayName;

    @Override
    @Nonnull
    public String getText() {
        return myDisplayName == null
            ? InspectionLocalize.editOptionsOfReporterInspectionFamily().get()
            : InspectionLocalize.editInspectionOptions(myDisplayName).get();
    }

    @Nullable
    @RequiredReadAction
    private static String getSuppressedId(Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        while (element != null && !(element instanceof PsiFile)) {
            for (InspectionExtensionsFactory factory : InspectionExtensionsFactory.EP_NAME.getExtensionList()) {
                String suppressedIds = factory.getSuppressedInspectionIdsIn(element);
                if (suppressedIds != null) {
                    String text = element.getText();
                    List<String> ids = StringUtil.split(suppressedIds, ",");
                    for (String id : ids) {
                        int i = text.indexOf(id);
                        if (i == -1) {
                            continue;
                        }
                        int idOffset = element.getTextRange().getStartOffset() + i;
                        if (TextRange.from(idOffset, id.length()).contains(offset)) {
                            return id;
                        }
                    }
                }
            }
            element = element.getParent();
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        myId = getSuppressedId(editor, file);
        if (myId != null) {
            InspectionToolWrapper toolWrapper = getTool(project, file);
            if (toolWrapper == null) {
                return false;
            }
            myDisplayName = toolWrapper.getDisplayName();
        }
        return myId != null;
    }

    @Nullable
    private InspectionToolWrapper getTool(Project project, PsiFile file) {
        InspectionProjectProfileManager projectProfileManager = InspectionProjectProfileManager.getInstance(project);
        InspectionProfileImpl inspectionProfile = (InspectionProfileImpl)projectProfileManager.getInspectionProfile();
        return inspectionProfile.getToolById(myId, file);
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        InspectionToolWrapper toolWrapper = getTool(project, file);
        if (toolWrapper == null) {
            return;
        }
        InspectionProjectProfileManager projectProfileManager = InspectionProjectProfileManager.getInstance(project);
        InspectionProfileImpl inspectionProfile = (InspectionProfileImpl)projectProfileManager.getInspectionProfile();
        EditInspectionToolsSettingsAction.editToolSettings(project, inspectionProfile, false, toolWrapper.getShortName());
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
