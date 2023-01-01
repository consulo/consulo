/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.language.impl.ast;

import consulo.application.util.RecursionManager;
import consulo.language.ast.*;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.impl.internal.psi.AstSpine;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiInvalidElementAccessException;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.ILightStubFileElementType;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.IStubFileElementType;
import consulo.language.psi.stub.StubBuilder;
import consulo.language.util.CharTable;
import consulo.util.lang.StackOverflowPreventedException;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FileElement extends LazyParseableElement implements FileASTNode, Supplier<FileElement> {
  public static final FileElement[] EMPTY_ARRAY = new FileElement[0];
  private volatile CharTable myCharTable = new CharTableImpl();
  private volatile boolean myDetached;
  private volatile AstSpine myStubbedSpine;

  @Override
  protected PsiElement createPsiNoLock() {
    return myDetached ? null : super.createPsiNoLock();
  }

  public void detachFromFile() {
    myDetached = true;
    clearPsi();
  }

  @Override
  @Nonnull
  public CharTable getCharTable() {
    return myCharTable;
  }

  @Nonnull
  @Override
  public LighterAST getLighterAST() {
    IElementType contentType = getElementType();
    if (!isParsed() && contentType instanceof ILightStubFileElementType) {
      return new FCTSBackedLighterAST(getCharTable(), ((ILightStubFileElementType<?>)contentType).parseContentsLight(this));
    }
    return new TreeBackedLighterAST(this);
  }

  public FileElement(@Nonnull IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override
  public PsiManager getManager() {
    CompositeElement treeParent = getTreeParent();
    if (treeParent != null) return treeParent.getManager();
    PsiElement psi = getPsi();
    if (psi == null) throw PsiInvalidElementAccessException.createByNode(this, null);
    return psi.getManager();
  }

  @Override
  public ASTNode copyElement() {
    PsiFileImpl psiElement = (PsiFileImpl)getPsi();
    PsiFileImpl psiElementCopy = (PsiFileImpl)psiElement.copy();
    return psiElementCopy.getTreeElement();
  }

  public void setCharTable(@Nonnull CharTable table) {
    myCharTable = table;
  }

  @Override
  public FileElement get() {
    return this;
  }

  @Override
  public void clearCaches() {
    super.clearCaches();
    myStubbedSpine = null;
  }

  @Nonnull
  public final AstSpine getStubbedSpine() {
    AstSpine result = myStubbedSpine;
    if (result == null) {
      PsiFileImpl file = (PsiFileImpl)getPsi();
      IStubFileElementType type = file.getElementTypeForStubBuilder();
      if (type == null) return AstSpine.EMPTY_SPINE;

      result = RecursionManager.doPreventingRecursion(file, false, () -> new AstSpine(calcStubbedDescendants(type.getBuilder())));
      if (result == null) {
        throw new StackOverflowPreventedException("Endless recursion prevented");
      }
      myStubbedSpine = result;
    }
    return result;
  }

  private List<CompositeElement> calcStubbedDescendants(StubBuilder builder) {
    List<CompositeElement> result = new ArrayList<>();
    result.add(this);

    acceptTree(new RecursiveTreeElementWalkingVisitor() {
      @Override
      public void visitComposite(CompositeElement node) {
        CompositeElement parent = node.getTreeParent();
        if (parent != null && builder.skipChildProcessingWhenBuildingStubs(parent, node)) {
          return;
        }

        IElementType type = node.getElementType();
        if (type instanceof IStubElementType && ((IStubElementType)type).shouldCreateStub(node)) {
          result.add(node);
        }

        super.visitNode(node);
      }
    });
    return result;
  }
}
