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
package consulo.ide.impl.psi.impl.source.resolve.reference.impl.providers;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.codeInsight.lookup.CharFilter;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.path.FileReference;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiReference;

/**
 * @author peter
 */
@ExtensionImpl(id = "fileRef", order = "before completion")
public class FileReferenceCharFilter extends CharFilter{
  @Override
  public Result acceptChar(char c, int prefixLength, Lookup lookup) {
    final LookupElement item = lookup.getCurrentItem();
    if ('.' == c && item != null && item.getObject() instanceof PsiFileSystemItem) {
      PsiReference referenceAtCaret = lookup.getPsiFile().findReferenceAt(lookup.getLookupStart());
      if (referenceAtCaret != null && FileReference.findFileReference(referenceAtCaret) != null) {
        return Result.ADD_TO_PREFIX;
      }
    }

    return null;
  }
}
