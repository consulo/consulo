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

/*
 * @author max
 */
package consulo.language.psi.stub;

import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PsiFileStubImpl<T extends PsiFile> extends StubBase<T> implements PsiFileStub<T> {
  public static final IStubFileElementType TYPE = new IStubFileElementType(Language.ANY);
  private volatile T myFile;
  private volatile String myInvalidationReason;
  private volatile PsiFileStub[] myStubRoots;

  public PsiFileStubImpl(final T file) {
    super(null, null);
    myFile = file;
  }

  @Override
  public T getPsi() {
    return myFile;
  }

  @Override
  public void setPsi(@Nonnull final T psi) {
    myFile = psi;
  }

  public void clearPsi(@Nonnull String reason) {
    myInvalidationReason = reason;
    myFile = null;
  }

  @Override
  @Nullable
  public String getInvalidationReason() {
    return myInvalidationReason;
  }

  @Override
  public IStubElementType getStubType() {
    return null;
  }

  @Nonnull
  @Override
  public IStubFileElementType getType() {
    return TYPE;
  }

  /**
   * Don't call this method, it's public for implementation reasons
   */
  @Nonnull
  public PsiFileStub[] getStubRoots() {
    if (myStubRoots != null) return myStubRoots;

    final T psi = getPsi();
    if (psi == null) {
      return new PsiFileStub[]{this};
    }

    final FileViewProvider viewProvider = psi.getViewProvider();
    final PsiFile stubBindingRoot = viewProvider.getStubBindingRoot();

    StubTree baseTree = getOrCalcStubTree(stubBindingRoot);
    if (baseTree != null) {
      final List<PsiFileStub> roots = new SmartList<>(baseTree.getRoot());
      final List<Pair<IStubFileElementType, PsiFile>> stubbedRoots = StubTreeBuilder.getStubbedRoots(viewProvider);
      for (Pair<IStubFileElementType, PsiFile> stubbedRoot : stubbedRoots) {
        if (stubbedRoot.second == stubBindingRoot) continue;
        final StubTree secondaryStubTree = getOrCalcStubTree(stubbedRoot.second);
        if (secondaryStubTree != null) {
          final PsiFileStub root = secondaryStubTree.getRoot();
          roots.add(root);
        }
      }
      final PsiFileStub[] rootsArray = roots.toArray(PsiFileStub.EMPTY_ARRAY);
      for (PsiFileStub root : rootsArray) {
        if (root instanceof PsiFileStubImpl) {
          ((PsiFileStubImpl)root).setStubRoots(rootsArray);
        }
      }

      myStubRoots = rootsArray;
      return rootsArray;
    }
    return PsiFileStub.EMPTY_ARRAY;
  }

  private static StubTree getOrCalcStubTree(PsiFile stubBindingRoot) {
    StubTree result = null;
    if (stubBindingRoot instanceof PsiFileWithStubSupport) {
      result = ((PsiFileWithStubSupport)stubBindingRoot).getStubTree();
      if (result == null) {
        result = ((PsiFileWithStubSupport)stubBindingRoot).calcStubTree();
      }
    }
    return result;
  }

  public void setStubRoots(@Nonnull PsiFileStub[] roots) {
    if (roots.length == 0) {
      Logger.getInstance(getClass()).error("Incorrect psi file stub roots count" + this + "," + getStubType());
    }
    myStubRoots = roots;
  }

  public boolean rootsAreSet() {
    return myStubRoots != null;
  }

  public String getDiagnostics() {
    ObjectStubTree stubTree = ObjectStubTree.getStubTree(this);
    T file = myFile;
    return toString() +
           Map.of("myFile", file, "myInvalidationReason", myInvalidationReason, "myStubRoots", Arrays.toString(myStubRoots), "stubTree", stubTree == null ? "null" : stubTree).toString();
  }
}