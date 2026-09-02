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
package consulo.sandboxPlugin.lang.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.psi.stub.SandClassSearch;

import java.util.Collection;

/**
 * The {@code : Parent} clause of a class — a reference resolving <b>through context</b>:
 * the candidate set comes from the condition-annotated index (all declaration variants),
 * and the referencing file's resolution environment selects the variant whose guard is
 * satisfied. The same name resolves to different declarations from different files.
 */
public class SandExtendsRef extends ASTWrapperPsiElement {
    public SandExtendsRef(ASTNode node) {
        super(node);
    }

    @RequiredReadAction
    @Override
    public PsiReference getReference() {
        return new PsiReferenceBase<SandExtendsRef>(this, TextRange.from(0, getTextLength())) {
            @RequiredReadAction
            @Override
            public PsiElement resolve() {
                String name = getElement().getText();
                Project project = getElement().getProject();

                Collection<SandClass> candidates = SandClassSearch.active(project, name);
                return candidates.isEmpty() ? null : candidates.iterator().next();
            }
        };
    }
}
