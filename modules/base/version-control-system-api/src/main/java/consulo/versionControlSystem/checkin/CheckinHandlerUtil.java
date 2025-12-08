/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.checkin;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author oleg
 */
public class CheckinHandlerUtil {
    @RequiredReadAction
    public static List<VirtualFile> filterOutGeneratedAndExcludedFiles(@Nonnull Collection<VirtualFile> files, @Nonnull Project project) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        List<VirtualFile> result = new ArrayList<>(files.size());
        for (VirtualFile file : files) {
            if (!fileIndex.isExcluded(file) && !GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(file, project)) {
                result.add(file);
            }
        }
        return result;
    }

    @RequiredReadAction
    public static PsiFile[] getPsiFiles(Project project, Collection<VirtualFile> selectedFiles) {
        ArrayList<PsiFile> result = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);

        VirtualFile projectFileDir = null;
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null) {
            projectFileDir = baseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
        }

        for (VirtualFile file : selectedFiles) {
            if (file.isValid()) {
                if (isUnderProjectFileDir(projectFileDir, file) || !isFileUnderSourceRoot(project, file)) {
                    continue;
                }
                PsiFile psiFile = psiManager.findFile(file);
                if (psiFile != null) {
                    result.add(psiFile);
                }
            }
        }
        return PsiUtilCore.toPsiFileArray(result);
    }

    private static boolean isUnderProjectFileDir(@Nullable VirtualFile projectFileDir, @Nonnull VirtualFile file) {
        return projectFileDir != null && VirtualFileUtil.isAncestor(projectFileDir, file, false);
    }

    private static boolean isFileUnderSourceRoot(@Nonnull Project project, @Nonnull VirtualFile file) {
        ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        return index.isInContent(file) && !index.isInLibrarySource(file);
    }

    @Deprecated
    @DeprecationInfo("Use #disableWhenDumb() with new UI")
    public static void disableWhenDumb(@Nonnull Project project, @Nonnull JCheckBox checkBox, @Nonnull String tooltip) {
        boolean dumb = DumbService.isDumb(project);
        checkBox.setEnabled(!dumb);
        checkBox.setToolTipText(dumb ? tooltip : "");
    }

    @RequiredUIAccess
    public static void disableWhenDumb(@Nonnull Project project, @Nonnull CheckBox checkBox, @Nonnull LocalizeValue tooltip) {
        boolean dumb = DumbService.isDumb(project);
        checkBox.setEnabled(!dumb);
        checkBox.setToolTipText(dumb ? tooltip : LocalizeValue.absent());
    }
}
