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
package consulo.language.editor.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.FileEditorManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.proximity.ProximityLocation;
import consulo.language.util.proximity.ProximityWeigher;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.NotNullLazyKey;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

/**
 * @author peter
*/
@ExtensionImpl(id = "openedInEditor")
public class OpenedInEditorWeigher extends ProximityWeigher {
  private static final NotNullLazyKey<VirtualFile[], ProximityLocation> OPENED_EDITORS = NotNullLazyKey.create("openedEditors", new Function<ProximityLocation, VirtualFile[]>() {
    @Nonnull
    @Override
    public VirtualFile[] apply(ProximityLocation location) {
      return FileEditorManager.getInstance(location.getProject()).getOpenFiles();
    }
  });

  @Override
  public Comparable weigh(@Nonnull final PsiElement element, @Nonnull final ProximityLocation location) {
    if (location.getProject() == null){
      return null;
    }
    final PsiFile psiFile = element.getContainingFile();
    if (psiFile == null) return false;

    final VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();
    return virtualFile != null && ArrayUtil.find(OPENED_EDITORS.getValue(location), virtualFile) != -1;
  }
}
