/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.navigation;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.util.lang.Pair;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ref.PatchedWeakReference;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Expands {@link AnAction} contract for documentation-related actions that may be called from the IDE tooltip.
 *
 * @author Denis Zhdanov
 * @since 2012-07-26
 */
public abstract class AbstractDocumentationTooltipAction extends AnAction {
    private @Nullable WeakReference<PsiElement> myDocAnchor;
    private @Nullable WeakReference<PsiElement> myOriginalElement;

    public void setDocInfo(PsiElement docAnchor, PsiElement originalElement) {
        myDocAnchor = new PatchedWeakReference<PsiElement>(docAnchor);
        myOriginalElement = new PatchedWeakReference<PsiElement>(originalElement);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setVisible(getDocInfo() != null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Pair<PsiElement, PsiElement> info = getDocInfo();
        if (info == null) {
            return;
        }
        doActionPerformed(e.getDataContext(), info.first, info.second);
        myDocAnchor = null;
        myOriginalElement = null;
    }

    protected abstract void doActionPerformed(
        DataContext context,
        PsiElement docAnchor,
        PsiElement originalElement
    );

    private @Nullable Pair<PsiElement/* doc anchor */, PsiElement /* original element */> getDocInfo() {
        WeakReference<PsiElement> docAnchorRef = myDocAnchor;
        if (docAnchorRef == null) {
            return null;
        }
        PsiElement docAnchor = docAnchorRef.get();
        if (docAnchor == null) {
            return null;
        }
        WeakReference<PsiElement> originalElementRef = myOriginalElement;
        if (originalElementRef == null) {
            return null;
        }
        PsiElement originalElement = originalElementRef.get();
        if (originalElement == null) {
            return null;
        }
        return Pair.create(docAnchor, originalElement);
    }
}
