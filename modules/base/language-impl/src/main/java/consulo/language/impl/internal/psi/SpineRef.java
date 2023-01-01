// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi;

import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.Stub;
import consulo.language.psi.stub.StubTree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public class SpineRef extends SubstrateRef {
  private final PsiFileImpl myFile;
  private final int myIndex;

  public SpineRef(@Nonnull PsiFileImpl file, int index) {
    myFile = file;
    myIndex = index;
  }

  @Nonnull
  @Override
  public ASTNode getNode() {
    return myFile.calcTreeElement().getStubbedSpine().getSpineNodes().get(myIndex);
  }

  @Nullable
  @Override
  public Stub getStub() {
    StubTree tree = myFile.getStubTree();
    return tree == null ? null : tree.getPlainList().get(myIndex);
  }

  @Nullable
  @Override
  public Stub getGreenStub() {
    StubTree tree = myFile.getGreenStubTree();
    return tree == null ? null : tree.getPlainList().get(myIndex);
  }

  @Override
  public boolean isValid() {
    return myFile.isValid();
  }

  @Nonnull
  @Override
  public PsiFile getContainingFile() {
    return myFile;
  }
}
