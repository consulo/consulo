/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.psi.stub;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

/**
 * @author max
 */
public interface StubBuilder {
  @RequiredReadAction
  @Nullable StubElement buildStubTree(PsiFile file);

  boolean skipChildProcessingWhenBuildingStubs(ASTNode parent, ASTNode node);

  /**
   * Whether {@code node} has a stub in the stub tree this builder builds: by default, when its element type is a stub element
   * type that creates a stub for it. A builder that creates stubs by other means (e.g. element types mapped to their stub
   * types) tells here, so that a loaded AST binds to the stubs of the file.
   */
  default boolean isStubbedNode(ASTNode node) {
    return node.getElementType() instanceof IStubElementType<?, ?> stubElementType && stubElementType.shouldCreateStub(node);
  }
}