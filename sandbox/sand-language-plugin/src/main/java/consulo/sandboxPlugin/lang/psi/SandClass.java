/*
 * Copyright 2013-2016 consulo.io
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
import consulo.annotation.access.RequiredWriteAction;
import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.stub.StubBasedPsiElementBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.util.IncorrectOperationException;
import consulo.sandboxPlugin.lang.psi.stub.SandClassStub;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2014-03-19
 */
public class SandClass extends StubBasedPsiElementBase<SandClassStub> implements PsiNamedElement, PsiNameIdentifierOwner {
    public SandClass(ASTNode node) {
        super(node);
    }

    public SandClass(SandClassStub stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    @RequiredReadAction
    public @Nullable String getName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier != null ? nameIdentifier.getText() : null;
    }

    @Override
    @RequiredWriteAction
    public PsiElement setName(String name) throws IncorrectOperationException {
        return null;
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getNameIdentifier() {
        return findChildByType(SandTokens.IDENTIFIER);
    }
}
