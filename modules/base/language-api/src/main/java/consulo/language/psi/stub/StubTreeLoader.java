/*
 * Copyright 2013-2022 consulo.io
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
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.util.PerApplicationInstance;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiFile;
import consulo.language.psi.internal.PsiFileWithStubSupport;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * @author yole
 */
@Service(ComponentScope.APPLICATION)
public abstract class StubTreeLoader {
  private static final PerApplicationInstance<StubTreeLoader> ourInstance = PerApplicationInstance.of(StubTreeLoader.class);

  @Nonnull
  public static StubTreeLoader getInstance() {
    return ourInstance.get();
  }

  @Nullable
  public abstract ObjectStubTree readOrBuild(Project project, final VirtualFile vFile, @Nullable final PsiFile psiFile);

  @Nullable
  public abstract ObjectStubTree readFromVFile(Project project, final VirtualFile vFile);

  public abstract void rebuildStubTree(VirtualFile virtualFile);

  public abstract boolean canHaveStub(VirtualFile file);

  protected boolean hasPsiInManyProjects(@Nonnull VirtualFile virtualFile) {
    return false;
  }

  @Nullable
  protected IndexingStampInfo getIndexingStampInfo(@Nonnull VirtualFile file) {
    return null;
  }

  @Nonnull
  @RequiredReadAction
  public abstract RuntimeException stubTreeAndIndexDoNotMatch(@Nullable ObjectStubTree stubTree, @Nonnull PsiFileWithStubSupport psiFile, @Nullable Throwable cause);

  public static String getFileViewProviderMismatchDiagnostics(@Nonnull FileViewProvider provider) {
    Function<PsiFile, String> fileClassName = file -> file.getClass().getSimpleName();
    Function<Pair<IStubFileElementType, PsiFile>, String> stubRootToString = pair -> "(" + pair.first.toString() + ", " + pair.first.getLanguage() + " -> " + fileClassName.apply(pair.second) + ")";
    List<Pair<IStubFileElementType, PsiFile>> roots = StubTreeBuilder.getStubbedRoots(provider);
    return "path = " +
           provider.getVirtualFile().getPath() +
           ", stubBindingRoot = " +
           fileClassName.apply(provider.getStubBindingRoot()) +
           ", languages = [" +
           StringUtil.join(provider.getLanguages(), Language::getID, ", ") +
           "], fileTypes = [" +
           StringUtil.join(provider.getAllFiles(), file -> file.getFileType().getId(), ", ") +
           "], files = [" +
           StringUtil.join(provider.getAllFiles(), fileClassName, ", ") +
           "], roots = [" +
           StringUtil.join(roots, stubRootToString, ", ") +
           "]";
  }
}
