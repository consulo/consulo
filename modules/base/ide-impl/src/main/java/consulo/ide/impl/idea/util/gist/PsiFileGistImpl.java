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
package consulo.ide.impl.idea.util.gist;

import consulo.application.ApplicationManager;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.component.util.ModificationTracker;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.index.io.data.DataExternalizer;
import consulo.language.file.LanguageFileType;
import consulo.language.impl.internal.psi.stub.FileContentImpl;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.gist.GistManager;
import consulo.language.psi.stub.gist.PsiFileGist;
import consulo.language.psi.stub.gist.VirtualFileGist;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author peter
 */
class PsiFileGistImpl<Data> implements PsiFileGist<Data> {
  private static final ModificationTracker ourReindexTracker = () -> ((GistManagerImpl)GistManager.getInstance()).getReindexCount();
  private final VirtualFileGist<Data> myPersistence;
  private final BiFunction<Project, VirtualFile, Data> myCalculator;
  private final Key<CachedValue<Data>> myCacheKey;

  PsiFileGistImpl(@Nonnull String id, int version, @Nonnull DataExternalizer<Data> externalizer, @Nonnull Function<PsiFile, Data> calculator) {
    myCalculator = (project, file) -> {
      PsiFile psiFile = getPsiFile(project, file);
      return psiFile == null ? null : calculator.apply(psiFile);
    };
    myPersistence = GistManager.getInstance().newVirtualFileGist(id, version, externalizer, myCalculator);
    myCacheKey = Key.create("PsiFileGist " + id);
  }

  @Override
  public Data getFileData(@Nonnull PsiFile file) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    if (shouldUseMemoryStorage(file)) {
      return CachedValuesManager.getManager(file.getProject()).getCachedValue(file, myCacheKey, () -> {
        Data data = myCalculator.apply(file.getProject(), file.getViewProvider().getVirtualFile());
        return CachedValueProvider.Result.create(data, file, ourReindexTracker);
      }, false);
    }

    file.putUserData(myCacheKey, null);
    return myPersistence.getFileData(file.getProject(), file.getVirtualFile());
  }

  private static boolean shouldUseMemoryStorage(PsiFile file) {
    if (!(file.getVirtualFile() instanceof NewVirtualFile)) return true;

    PsiDocumentManager pdm = PsiDocumentManager.getInstance(file.getProject());
    Document document = pdm.getCachedDocument(file);
    return document != null && (pdm.isUncommited(document) || FileDocumentManager.getInstance().isDocumentUnsaved(document));
  }

  private static PsiFile getPsiFile(@Nonnull Project project, @Nonnull VirtualFile file) {
    PsiFile psi = PsiManager.getInstance(project).findFile(file);
    if (!(psi instanceof PsiFileImpl) || ((PsiFileImpl)psi).isContentsLoaded()) {
      return psi;
    }

    FileType fileType = file.getFileType();
    if (!(fileType instanceof LanguageFileType)) return null;

    return FileContentImpl.createFileFromText(project, psi.getViewProvider().getContents(), (LanguageFileType)fileType, file, file.getName());
  }

}
