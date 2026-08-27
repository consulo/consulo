// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.codeVision;

import consulo.codeEditor.Editor;
import consulo.language.editor.codeVision.PlatformCodeVisionIds;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.ui.RelativePoint2D;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.ProgrammaticInputDetails;

public abstract class ReferencesCodeVisionProvider extends CodeVisionProviderBase {
    @Override
    public final void handleClick(Editor editor, PsiElement element, ComponentEvent<?> event) {
        LanguageEditorInternalHelper helper = LanguageEditorInternalHelper.getInstance();
        RelativePoint2D point = event.getInputDetails() instanceof ProgrammaticInputDetails ? null : RelativePoint2D.of(event);
        helper.startFindUsages(editor, element.getProject(), element, point);
    }

    @Override
    public LocalizeValue getName() {
        return CodeInsightLocalize.codeVisionUsagesName();
    }

    @Override
    public String getGroupId() {
        return PlatformCodeVisionIds.USAGES.getKey();
    }
}
