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
package com.intellij.codeInsight.actions;

import com.intellij.openapi.components.ServiceManager;
import consulo.logging.Logger;
import consulo.document.Document;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import consulo.util.dataholder.Key;
import consulo.document.util.TextRange;
import consulo.application.util.function.ThrowableComputable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.diff.FilesTooBigForDiffException;
import consulo.application.AccessRule;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public class FormatChangedTextUtil {
  public static final Key<CharSequence> TEST_REVISION_CONTENT = Key.create("test.revision.content");
  protected static final Logger LOG = Logger.getInstance(FormatChangedTextUtil.class);

  protected FormatChangedTextUtil() {
  }

  @Nonnull
  public static FormatChangedTextUtil getInstance() {
    return ServiceManager.getService(FormatChangedTextUtil.class);
  }

  /**
   * Allows to answer if given file has changes in comparison with VCS.
   *
   * @param file target file
   * @return <code>true</code> if given file has changes; <code>false</code> otherwise
   */
  public static boolean hasChanges(@Nonnull PsiFile file) {
    final Project project = file.getProject();
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile != null) {
      final Change change = ChangeListManager.getInstance(project).getChange(virtualFile);
      return change != null;
    }
    return false;
  }

  /**
   * Allows to answer if any file below the given directory (any level of nesting) has changes in comparison with VCS.
   *
   * @param directory target directory to check
   * @return <code>true</code> if any file below the given directory has changes in comparison with VCS;
   * <code>false</code> otherwise
   */
  public static boolean hasChanges(@Nonnull PsiDirectory directory) {
    return hasChanges(directory.getVirtualFile(), directory.getProject());
  }

  /**
   * Allows to answer if given file or any file below the given directory (any level of nesting) has changes in comparison with VCS.
   *
   * @param file    target directory to check
   * @param project target project
   * @return <code>true</code> if given file or any file below the given directory has changes in comparison with VCS;
   * <code>false</code> otherwise
   */
  public static boolean hasChanges(@Nonnull VirtualFile file, @Nonnull Project project) {
    final Collection<Change> changes = ChangeListManager.getInstance(project).getChangesIn(file);
    for (Change change : changes) {
      if (change.getType() == Change.Type.NEW || change.getType() == Change.Type.MODIFICATION) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasChanges(@Nonnull VirtualFile[] files, @Nonnull Project project) {
    for (VirtualFile file : files) {
      if (hasChanges(file, project)) return true;
    }
    return false;
  }

  /**
   * Allows to answer if any file that belongs to the given module has changes in comparison with VCS.
   *
   * @param module target module to check
   * @return <code>true</code> if any file that belongs to the given module has changes in comparison with VCS
   * <code>false</code> otherwise
   */
  public static boolean hasChanges(@Nonnull Module module) {
    final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    for (VirtualFile root : rootManager.getSourceRoots()) {
      if (hasChanges(root, module.getProject())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Allows to answer if any file that belongs to the given project has changes in comparison with VCS.
   *
   * @param project target project to check
   * @return <code>true</code> if any file that belongs to the given project has changes in comparison with VCS
   * <code>false</code> otherwise
   */
  public static boolean hasChanges(@Nonnull final Project project) {
    ThrowableComputable<ModifiableModuleModel,RuntimeException> action = () -> ModuleManager.getInstance(project).getModifiableModel();
    final ModifiableModuleModel moduleModel = AccessRule.read(action);

    try {
      for (Module module : moduleModel.getModules()) {
        if (hasChanges(module)) {
          return true;
        }
      }
      return false;
    }
    finally {
      moduleModel.dispose();
    }
  }

  @Nonnull
  public static List<PsiFile> getChangedFilesFromDirs(@Nonnull Project project, @Nonnull List<PsiDirectory> dirs) {
    ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    Collection<Change> changes = ContainerUtil.newArrayList();

    for (PsiDirectory dir : dirs) {
      changes.addAll(changeListManager.getChangesIn(dir.getVirtualFile()));
    }

    return getChangedFiles(project, changes);
  }

  @Nonnull
  public static List<PsiFile> getChangedFiles(@Nonnull final Project project, @Nonnull Collection<Change> changes) {
    Function<Change, PsiFile> changeToPsiFileMapper = new Function<Change, PsiFile>() {
      private PsiManager myPsiManager = PsiManager.getInstance(project);

      @Override
      public PsiFile fun(Change change) {
        VirtualFile vFile = change.getVirtualFile();
        return vFile != null ? myPsiManager.findFile(vFile) : null;
      }
    };

    return ContainerUtil.mapNotNull(changes, changeToPsiFileMapper);
  }

  @Nonnull
  public List<TextRange> getChangedTextRanges(@Nonnull Project project, @Nonnull PsiFile file) throws FilesTooBigForDiffException {
    return ContainerUtil.emptyList();
  }

  public int calculateChangedLinesNumber(@Nonnull Document document, @Nonnull CharSequence contentFromVcs) {
    return -1;
  }

  public boolean isChangeNotTrackedForFile(@Nonnull Project project, @Nonnull PsiFile file) {
    return false;
  }
}
