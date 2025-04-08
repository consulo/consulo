// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor;

import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class EditorTabPresentationUtil {
    @Nonnull
    public static String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
        for (EditorTabTitleProvider provider : DumbService.getDumbAwareExtensions(project, EditorTabTitleProvider.EP_NAME)) {
            String result = provider.getEditorTabTitle(project, file);
            if (StringUtil.isNotEmpty(result)) {
                return result;
            }
        }

        return file.getPresentableName();
    }

    @Nonnull
    public static String getUniqueEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
        String name = getEditorTabTitle(project, file);
        if (name.equals(file.getPresentableName())) {
            return UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(project, file);
        }
        return name;
    }

    @Nullable
    public static ColorValue getEditorTabBackgroundColor(
        @Nonnull Project project,
        @Nonnull VirtualFile file,
        @Nullable FileEditorWindow editorWindow
    ) {
        for (EditorTabColorProvider provider : DumbService.getDumbAwareExtensions(project, EditorTabColorProvider.EP_NAME)) {
            ColorValue result = provider.getEditorTabColor(project, file, editorWindow);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Nullable
    public static ColorValue getFileBackgroundColor(@Nonnull Project project, @Nonnull VirtualFile file) {
        for (EditorTabColorProvider provider : DumbService.getDumbAwareExtensions(project, EditorTabColorProvider.EP_NAME)) {
            ColorValue result = provider.getProjectViewColor(project, file);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
