/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import consulo.language.ast.FileASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.StubBasedPsiElement;
import consulo.logging.Logger;
import consulo.util.collection.BooleanStack;
import consulo.util.collection.Stack;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public class DefaultStubBuilder implements StubBuilder {
  private static final Logger LOG = Logger.getInstance(DefaultStubBuilder.class);

  @RequiredReadAction
  @Override
  public StubElement buildStubTree(@Nonnull PsiFile file) {
    return buildStubTreeFor(file.getNode(), createStubForFile(file));
  }

  @Nonnull
  protected StubElement createStubForFile(@Nonnull PsiFile file) {
    @SuppressWarnings("unchecked") PsiFileStubImpl stub = new PsiFileStubImpl(file);
    return stub;
  }

  /**
   * @deprecated override and invoke {@link #skipChildProcessingWhenBuildingStubs(ASTNode, ASTNode)} (to be removed in IDEA 2017)
   * Note to implementers: always keep in sync with {@linkplain #skipChildProcessingWhenBuildingStubs(ASTNode, ASTNode)}.
   */
  protected boolean skipChildProcessingWhenBuildingStubs(@Nonnull PsiElement parent, @Nonnull PsiElement element) {
    return false;
  }

  @Nonnull
  protected final StubElement buildStubTreeFor(@Nonnull ASTNode root, @Nonnull StubElement parentStub) {
    new StubBuildingWalkingVisitor(root, parentStub).buildStubTree();
    return parentStub;
  }

  /**
   * Note to implementers: always keep in sync with {@linkplain #skipChildProcessingWhenBuildingStubs(PsiElement, PsiElement)}.
   */
  @Override
  public boolean skipChildProcessingWhenBuildingStubs(@Nonnull ASTNode parent, @Nonnull ASTNode node) {
    return false;
  }

  protected class StubBuildingWalkingVisitor {
    private final Stack<StubElement> parentStubs = new Stack<>();
    private final Stack<ASTNode> parentNodes = new Stack<>();
    private final BooleanStack parentNodesStubbed = new BooleanStack();

    protected StubBuildingWalkingVisitor(ASTNode root, StubElement parentStub) {
      parentNodes.push(root);
      parentStubs.push(parentStub);
      parentNodesStubbed.push(true);
    }

    public final void buildStubTree() {
      while (!parentStubs.isEmpty()) {
        visitNode(parentStubs.pop(), parentNodes.pop(), parentNodesStubbed.pop());
      }
    }

    protected void visitNode(StubElement parentStub, ASTNode node, boolean immediateParentStubbed) {
      StubElement stub = createStub(parentStub, node);
      if (stub != null && !immediateParentStubbed) {
        ((ObjectStubBase)stub).markDangling();
      }

      pushChildren(node, node instanceof FileASTNode || stub != null, stub != null ? stub : parentStub);
    }

    @Nullable
    protected final ASTNode peekNextElement() {
      return parentNodes.isEmpty() ? null : parentNodes.peek();
    }

    @Nullable
    private StubElement createStub(StubElement parentStub, ASTNode node) {
      IElementType nodeType = node.getElementType();

      if (nodeType instanceof IStubElementType) {
        IStubElementType type = (IStubElementType)nodeType;

        if (type.shouldCreateStub(node)) {
          PsiElement element = node.getPsi();
          if (!(element instanceof StubBasedPsiElement)) {
            LOG.error("Non-StubBasedPsiElement requests stub creation. Stub type: " + type + ", PSI: " + element);
          }
          @SuppressWarnings("unchecked") StubElement stub = type.createStub(element, parentStub);
          //noinspection ConstantConditions
          LOG.assertTrue(stub != null, element);
          return stub;
        }
      }
      return null;
    }

    private void pushChildren(ASTNode node, boolean hasStub, StubElement stub) {
      for (ASTNode childNode = node.getLastChildNode(); childNode != null; childNode = childNode.getTreePrev()) {
        if (!skipChildProcessingWhenBuildingStubs(node, childNode)) {
          parentNodes.push(childNode);
          parentStubs.push(stub);
          parentNodesStubbed.push(hasStub);
        }
      }
    }
  }
}