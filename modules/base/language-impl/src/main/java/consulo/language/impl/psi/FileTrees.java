/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.impl.psi;

import consulo.component.ProcessCanceledException;
import consulo.language.ast.ASTNode;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.internal.psi.SpineRef;
import consulo.language.impl.internal.psi.SubstrateRef;
import consulo.language.impl.internal.psi.SubstrateRefOwner;
import consulo.language.psi.stub.StubTreeLoader;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.PsiFileStubImpl;
import consulo.language.psi.stub.StubBase;
import consulo.language.psi.stub.StubElement;
import consulo.language.psi.stub.StubTree;
import consulo.language.psi.stub.StubbedSpine;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ref.SoftReference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author peter
 */
final class FileTrees {
  private static final Logger LOG = Logger.getInstance(FileTrees.class);
  private static final int firstNonFilePsiIndex = 1;
  private final PsiFileImpl myFile;
  private final Reference<StubTree> myStub;
  private final Supplier<FileElement> myTreeElementPointer; // SoftReference/WeakReference to ASTNode or a strong reference to a tree if the file is a DummyHolder

  /**
   * Keeps references to all alive stubbed PSI (using {@link SpineRef}) to ensure PSI identity is preserved after AST/stubs are gc-ed and reloaded
   */
  @Nullable
  private final Reference<SubstrateRefOwner>[] myRefToPsi;

  private FileTrees(@Nonnull PsiFileImpl file, @Nullable Reference<StubTree> stub, @Nullable Supplier<FileElement> ast, @Nullable Reference<SubstrateRefOwner>[] refToPsi) {
    myFile = file;
    myStub = stub;
    myTreeElementPointer = ast;
    myRefToPsi = refToPsi;
  }

  @Nullable
  public StubTree derefStub() {
    return SoftReference.dereference(myStub);
  }

  @Nullable
  public FileElement derefTreeElement() {
    return SoftReference.deref(myTreeElementPointer);
  }

  public FileTrees switchToStrongRefs() {
    if (myRefToPsi == null) return this;

    forEachCachedPsi(psi -> {
      ASTNode node = psi.getNode();
      LOG.assertTrue(node.getPsi() == psi);
      psi.setSubstrateRef(SubstrateRef.createAstStrongRef(node));
    });

    return new FileTrees(myFile, myStub, myTreeElementPointer, null);
  }

  private void forEachCachedPsi(Consumer<? super SubstrateRefOwner> consumer) {
    ContainerUtil.process(myRefToPsi, ref -> {
      SubstrateRefOwner psi = SoftReference.dereference(ref);
      if (psi != null) {
        consumer.accept(psi);
      }
      return true;
    });
  }

  private boolean hasCachedPsi() {
    Reference<SubstrateRefOwner>[] refToPsi = myRefToPsi;
    return refToPsi != null && ContainerUtil.exists(refToPsi, ref -> SoftReference.dereference(ref) != null);
  }

  boolean useSpineRefs() {
    return myRefToPsi != null;
  }

  FileTrees switchToSpineRefs(@Nonnull List<PsiElement> spine) {
    Reference<SubstrateRefOwner>[] refToPsi = myRefToPsi;
    if (refToPsi == null) {
      //noinspection unchecked
      refToPsi = new Reference[spine.size()];
    }

    try {
      for (int i = firstNonFilePsiIndex; i < refToPsi.length; i++) {
        SubstrateRefOwner psi = (SubstrateRefOwner)Objects.requireNonNull(spine.get(i));
        psi.setSubstrateRef(new SpineRef(myFile, i));
        SubstrateRefOwner existing = SoftReference.dereference(refToPsi[i]);
        if (existing != null) {
          assert existing == psi : "Duplicate PSI found";
        }
        else {
          refToPsi[i] = new WeakReference<>(psi);
        }
      }
      return new FileTrees(myFile, myStub, myTreeElementPointer, refToPsi);
    }
    catch (Throwable e) {
      throw new RuntimeException("Exceptions aren't allowed here", e);
      // otherwise, e.g. in case of PCE, we'd remain with PSI having SpineRef's but not registered in any "myRefToPsi"
      // and so that PSI wouldn't be updated on AST change
    }
  }

  FileTrees clearStub(@Nonnull String reason) {
    StubTree stubHolder = derefStub();
    if (stubHolder != null) {
      ((PsiFileStubImpl<?>)stubHolder.getRoot()).clearPsi(reason);
    }

    if (myRefToPsi != null) {
      DebugUtil.performPsiModification("clearStub", () -> forEachCachedPsi(psi -> {
        DebugUtil.onInvalidated(psi);
        psi.setSubstrateRef(SubstrateRef.createInvalidRef(psi));
      }));
    }

    return new FileTrees(myFile, null, myTreeElementPointer, null);
  }

  FileTrees withAst(@Nonnull Supplier<FileElement> ast) {
    return new FileTrees(myFile, myStub, ast, myRefToPsi).reconcilePsi(derefStub(), ast.get(), true);
  }

  FileTrees withStub(@Nonnull StubTree stub, @Nullable FileElement ast) {
    assert derefTreeElement() == ast;
    return new FileTrees(myFile, new SoftReference<>(stub), myTreeElementPointer, myRefToPsi).reconcilePsi(stub, ast, false);
  }

  static FileTrees noStub(@Nullable FileElement ast, @Nonnull PsiFileImpl file) {
    return new FileTrees(file, null, ast, null);
  }

  /**
   * Ensures {@link #myRefToPsi}, stubs and AST all have the same PSI at corresponding indices.
   * In case several sources already have PSI (e.g. created during AST parsing), overwrites them with the "correct" one,
   * which is taken from {@link #myRefToPsi} if exists, otherwise from either stubs or AST depending on {@code takePsiFromStubs}.
   */
  private FileTrees reconcilePsi(@Nullable StubTree stubTree, @Nullable FileElement astRoot, boolean takePsiFromStubs) {
    assert stubTree != null || astRoot != null;

    if ((stubTree == null || astRoot == null) && !hasCachedPsi()) {
      // there's only one source of PSI, nothing to reconcile
      return new FileTrees(myFile, myStub, myTreeElementPointer, null);
    }

    List<StubElement<?>> stubList = stubTree == null ? null : stubTree.getPlainList();
    List<CompositeElement> nodeList = astRoot == null ? null : astRoot.getStubbedSpine().getSpineNodes();
    List<PsiElement> srcSpine = stubList == null || nodeList == null ? null : getAllSpinePsi(takePsiFromStubs ? stubTree.getSpine() : astRoot.getStubbedSpine());

    try {
      return DebugUtil.performPsiModification("reconcilePsi", () -> {
        if (myRefToPsi != null) {
          assert myRefToPsi.length == (stubList != null ? stubList.size() : nodeList.size()) : "Cached PSI count doesn't match actual one";
          bindSubstratesToCachedPsi(stubList, nodeList);
        }

        if (stubList != null && nodeList != null) {
          assert stubList.size() == nodeList.size() : "Stub count doesn't match stubbed node length";

          FileTrees result = switchToSpineRefs(srcSpine);
          bindStubsWithAst(srcSpine, stubList, nodeList, takePsiFromStubs);
          return result;
        }
        return this;
      });
    }
    catch (Throwable e) {
      myFile.clearContent(PsiFileImpl.STUB_PSI_MISMATCH);
      myFile.rebuildStub();
      throw StubTreeLoader.getInstance().stubTreeAndIndexDoNotMatch(stubTree, myFile, e);
    }
  }

  /**
   * {@link StubbedSpine#getStubPsi(int)} may throw {@link ProcessCanceledException},
   * so shouldn't be invoked in the middle of a mutating operation to avoid leaving inconsistent state.
   * So we obtain PSI all at once in advance.
   */
  static List<PsiElement> getAllSpinePsi(@Nonnull StubbedSpine spine) {
    return IntStream.range(0, spine.getStubCount()).mapToObj(spine::getStubPsi).collect(Collectors.toList());
  }

  private void bindSubstratesToCachedPsi(List<StubElement<?>> stubList, List<CompositeElement> nodeList) {
    assert myRefToPsi != null;
    for (int i = firstNonFilePsiIndex; i < myRefToPsi.length; i++) {
      SubstrateRefOwner cachedPsi = SoftReference.dereference(myRefToPsi[i]);
      if (cachedPsi != null) {
        if (stubList != null) {
          //noinspection unchecked
          ((StubBase)stubList.get(i)).setPsi(cachedPsi);
        }
        if (nodeList != null) {
          nodeList.get(i).setPsi(cachedPsi);
        }
      }
    }
  }

  private static void bindStubsWithAst(@Nonnull List<PsiElement> srcSpine, List<StubElement<?>> stubList, List<CompositeElement> nodeList, boolean takePsiFromStubs) {
    for (int i = firstNonFilePsiIndex; i < stubList.size(); i++) {
      StubElement<?> stub = stubList.get(i);
      CompositeElement node = nodeList.get(i);
      assert stub.getStubType() == node.getElementType() : "Stub type mismatch: " + stub.getStubType() + "!=" + node.getElementType() + " in #" + node.getElementType().getLanguage();

      PsiElement psi = Objects.requireNonNull(srcSpine.get(i));
      if (takePsiFromStubs) {
        node.setPsi(psi);
      }
      else {
        //noinspection unchecked
        ((StubBase)stub).setPsi(psi);
      }
    }
  }

  @Override
  public String toString() {
    return "FileTrees{" + "stub=" + (myStub == null ? "noRef" : derefStub()) + ", AST=" + (myTreeElementPointer == null ? "noRef" : derefTreeElement()) + ", useSpineRefs=" + useSpineRefs() + '}';
  }
}
