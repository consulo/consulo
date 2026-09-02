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
package consulo.sandboxPlugin.ide.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.highlight.usage.HighlightUsagesHandlerBase;
import consulo.language.editor.highlight.usage.HighlightUsagesHandlerFactoryBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.sandboxPlugin.lang.moduleAware.SandViewEnv;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandConditions;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A declaration that is inactive in the current view environment gets no caret-usage
 * highlighting — the identifier pass otherwise presents the dead branch as live code.
 * Active declarations fall through to the default behavior.
 */
@ExtensionImpl
public class SandInactiveUsagesHandlerFactory extends HighlightUsagesHandlerFactoryBase<PsiElement> {
    @Override
    @RequiredReadAction
    public @Nullable HighlightUsagesHandlerBase<PsiElement> createHighlightUsagesHandler(Editor editor, PsiFile file, PsiElement target) {
        SandClass sandClass = PsiTreeUtil.getParentOfType(target, SandClass.class);
        if (sandClass == null) {
            return null;
        }

        String condition = SandConditions.conditionOf(sandClass);
        if (condition.isEmpty()) {
            return null;
        }

        VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        if (SandConditions.matches(condition, SandViewEnv.viewEnv(file.getProject(), virtualFile))) {
            return null;
        }

        return new HighlightUsagesHandlerBase<>(editor, file) {
            @Override
            public @Nullable List<PsiElement> getTargets() {
                return List.of();
            }

            @Override
            protected void selectTargets(List<PsiElement> targets, Consumer<List<PsiElement>> selectionConsumer) {
                selectionConsumer.accept(targets);
            }

            @Override
            public void computeUsages(List<PsiElement> targets) {
            }
        };
    }
}
