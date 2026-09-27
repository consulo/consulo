/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.lang.annotation;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandFile;

/**
 * @author VISTALL
 */
public class SandAnnotator implements Annotator {
    @Override
    @RequiredReadAction
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (element instanceof SandFile sandFile && PsiTreeUtil.findChildOfType(sandFile, SandClass.class) == null) {
            holder.newAnnotation(HighlightSeverity.WARNING, LocalizeValue.localizeTODO("Sand file declares no classes"))
                .fileLevel()
                .withFix(new SandAddClassFix())
                .create();
        }
    }
}
