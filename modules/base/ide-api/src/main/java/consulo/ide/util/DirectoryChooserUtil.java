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
package consulo.ide.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.ide.IdeView;
import consulo.ide.internal.DirectoryChooserDialog;
import consulo.ide.internal.DirectoryChooserFactory;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;

public class DirectoryChooserUtil {
  private DirectoryChooserUtil() {
  }

  @Nullable
  public static PsiDirectory getOrChooseDirectory(@Nonnull IdeView view) {
    PsiDirectory[] dirs = view.getDirectories();
    if (dirs.length == 0) return null;
    if (dirs.length == 1) {
      return dirs[0];
    }
    else {
      Project project = dirs[0].getProject();
      return selectDirectory(project, dirs, null, "");
    }
  }

  @Nullable
  public static PsiDirectory selectDirectory(Project project, PsiDirectory[] packageDirectories, PsiDirectory defaultDirectory, String postfixToShow) {
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();

    ArrayList<PsiDirectory> possibleDirs = new ArrayList<>();
    for (PsiDirectory dir : packageDirectories) {
      if (!dir.isValid()) continue;
      if (!dir.isWritable()) continue;
      if (possibleDirs.contains(dir)) continue;
      if (!projectFileIndex.isInContent(dir.getVirtualFile())) continue;
      possibleDirs.add(dir);
    }

    if (possibleDirs.isEmpty()) return null;
    if (possibleDirs.size() == 1) return possibleDirs.get(0);

    if (ApplicationManager.getApplication().isUnitTestMode()) return possibleDirs.get(0);

    DirectoryChooserDialog chooser = Application.get().getInstance(DirectoryChooserFactory.class).create(project);
    chooser.setTitle(IdeLocalize.titleChooseDestinationDirectory().get());
    chooser.fillList(possibleDirs.toArray(new PsiDirectory[possibleDirs.size()]), defaultDirectory, project, postfixToShow);
    chooser.show();
    return chooser.isOK() ? chooser.getSelectedDirectory() : null;
  }

  @Nullable
  public static PsiDirectory chooseDirectory(PsiDirectory[] targetDirectories, @Nullable PsiDirectory initialDirectory, @Nonnull Project project, Map<PsiDirectory, String> relativePathsToCreate) {
    DirectoryChooserDialog chooser = Application.get().getInstance(DirectoryChooserFactory.class).create(project);
    chooser.setTitle(RefactoringLocalize.chooseDestinationDirectory().get());
    chooser.fillList(targetDirectories, initialDirectory, project, relativePathsToCreate);
    chooser.show();
    if (!chooser.isOK()) return null;
    return chooser.getSelectedDirectory();
  }
}
