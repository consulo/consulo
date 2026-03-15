/*
 * Copyright 2013-2023 consulo.io
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
package consulo.sandboxPlugin.lang.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandClass;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-04-03
 */
@ExtensionImpl
public class SandLocalInspection extends LocalInspectionTool {
    private static final String SHORT_NAME = getShortName(SandLocalInspection.class);

    
    @Override
    public String getShortName() {
        return SHORT_NAME;
    }

    
    @Override
    public LocalizeValue getGroupDisplayName() {
        return InspectionLocalize.inspectionGeneralToolsGroupName();
    }

    
    @Override
    public InspectionToolState<?> createStateProvider() {
        return new SandLocalInspectionState();
    }

    
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Test Sand Inspection with Settings");
    }

    
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Nullable
    @Override
    public Language getLanguage() {
        return SandLanguage.INSTANCE;
    }

    
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder,
                                          boolean isOnTheFly,
                                          LocalInspectionToolSession session,
                                          Object state) {
        SandLocalInspectionState sandState = (SandLocalInspectionState) state;
        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof SandClass sandClass) {
                    boolean checkClass = sandState.isCheckClass();

                    if (checkClass) {
                        PsiElement nameIdentifier = sandClass.getNameIdentifier();
                        if (nameIdentifier != null) {
                            holder.newProblem(LocalizeValue.of("Test Error"))
                                .range(nameIdentifier, new TextRange(0, nameIdentifier.getTextLength()))
                                .withFixes(new UpdateInspectionOptionFix<SandLocalInspection, SandLocalInspectionState>(
                                    SandLocalInspection.this,
                                    LocalizeValue.localizeTODO("Disable class check"),
                                    state -> state.setCheckClass(false))
                                )
                                .highlightType(ProblemHighlightType.ERROR)
                                .create();

                        }
                    }
                }
            }
        };
    }
}
