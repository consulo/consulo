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

package consulo.language.psi.path;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class StaticPathReferenceProvider extends PathReferenceProviderBase {

  private boolean myEndingSlashNotAllowed;
  private boolean myRelativePathsAllowed;
  private final FileType[] mySuitableFileTypes;

  public StaticPathReferenceProvider(@Nullable FileType[] suitableFileTypes) {
    mySuitableFileTypes = suitableFileTypes;
  }

  @Override
  public boolean createReferences(@Nonnull final PsiElement psiElement,
                                  final int offset,
                                  final String text,
                                  @Nonnull List<PsiReference> references,
                                  final boolean soft) {

    FileReferenceSet set = new FileReferenceSet(text, psiElement, offset, null, true, myEndingSlashNotAllowed, mySuitableFileTypes) {
      @Override
      public boolean isUrlEncoded() {
        return true;
      }

      @Override
      protected boolean isSoft() {
        return soft;
      }
    };
    if (!myRelativePathsAllowed) {
      set.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, FileReferenceSet.ABSOLUTE_TOP_LEVEL);
    }
    Collections.addAll(references, set.getAllReferences());
    return true;
  }

  @Override
  @Nullable
  public PathReference getPathReference(@Nonnull final String path, @Nonnull PsiElement element) {
    List<PsiReference> list = new SmartList<PsiReference>();
    createReferences(element, list, true);
    if (list.isEmpty()) return null;

    final PsiElement target = list.get(list.size() - 1).resolve();
    if (target == null) return null;

    return new PathReference(path, PathReference.ResolveFunction.NULL_RESOLVE_FUNCTION) {
      @Override
      public PsiElement resolve() {
        return target;
      }
    };

  }

  public void setEndingSlashNotAllowed(boolean endingSlashNotAllowed) {
    myEndingSlashNotAllowed = endingSlashNotAllowed;
  }

  public void setRelativePathsAllowed(boolean relativePathsAllowed) {
    myRelativePathsAllowed = relativePathsAllowed;
  }
}
