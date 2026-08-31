/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.navigationToolbar;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.fileEditor.structureView.tree.NodeProvider;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Deprecated
public abstract class StructureAwareNavBarModelExtension extends consulo.language.editor.ui.navigationBar.StructureAwareNavBarModelExtension {
    protected abstract Language getLanguage();

    protected List<NodeProvider<?>> getApplicableNodeProviders() {
        return Collections.emptyList();
    }

    protected boolean acceptParentFromModel(PsiElement psiElement) {
        return true;
    }

    @Override
    @RequiredReadAction
    public PsiElement getLeafElement(DataContext dataContext) {
        return super.getLeafElement(dataContext);
    }

    @Override
    @RequiredReadAction
    public boolean processChildren(Object object, Object rootElement, Predicate<Object> processor) {
        return super.processChildren(object, rootElement, processor);
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getParent(PsiElement psiElement) {
        return super.getParent(psiElement);
    }

    @Override
    public boolean normalizeChildren() {
        return false;
    }
}
