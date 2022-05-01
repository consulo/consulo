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

/*
 * @author max
 */
package consulo.language.psi.internal;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.IStubFileElementType;
import consulo.language.psi.stub.StubTree;
import consulo.language.psi.stub.internal.StubbedSpine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A base interface for PSI files that may contain not only text-based syntactic trees as their content,
 * but also a more lightweight representation called stubs.
 *
 * @see consulo.ide.impl.idea.extapi.psi.StubBasedPsiElementBase
 */
public interface PsiFileWithStubSupport extends PsiFile {
  /**
   * @return the stub tree for this file, if it's stub-based at all. Will be null after the AST has been loaded
   * (e.g. by calling {@link PsiElement#getNode()} or {@link PsiElement#getText()}.
   */
  @Nullable
  StubTree getStubTree();

  @Nullable
  default StubTree calcStubTree() {
    return null;
  }

  @Nullable
  default IStubFileElementType getElementTypeForStubBuilder() {
    return null;
  }

  /**
   * @return StubbedSpine for accessing stubbed PSI, which can be backed up by stubs or AST
   */
  @Nonnull
  default StubbedSpine getStubbedSpine() {
    StubTree tree = getStubTree();
    if (tree == null) {
      throw new UnsupportedOperationException("Please implement getStubbedSpine method");
    }
    return tree.getSpine();
  }
}