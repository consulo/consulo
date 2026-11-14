// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.copyright.pattern;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Allows providing language dependant velocity variables
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CopyrightVariablesProvider {
    ExtensionPointCacheKey<CopyrightVariablesProvider, Map<FileType, CopyrightVariablesProvider>> KEY =
        ExtensionPointCacheKey.groupBy("CopyrightVariablesProvider", CopyrightVariablesProvider::getFileType);

    static @Nullable CopyrightVariablesProvider forFileType(FileType fileType) {
        ExtensionPoint<CopyrightVariablesProvider> extensionPoint = Application.get().getExtensionPoint(CopyrightVariablesProvider.class);
        Map<FileType, CopyrightVariablesProvider> map = extensionPoint.getOrBuildCache(KEY);
        return map.get(fileType);
    }

    FileType getFileType();

    /**
     * Fill {@code context} map with variable name/bean pairs, based on current {@code project}, {@code module}, {@code file}
     */
    void collectVariables(Map<String, Object> context, @Nullable Project project, @Nullable Module module, PsiFile file);
}
