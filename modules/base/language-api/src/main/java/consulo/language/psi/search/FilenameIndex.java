// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.psi.search;

import consulo.application.Application;
import consulo.application.util.function.Processor;
import consulo.application.util.function.Processors;
import consulo.content.scope.SearchScope;
import consulo.index.io.ID;
import consulo.language.internal.FileNameIndexService;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.EverythingGlobalScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author yole
 */
public class FilenameIndex {
    /**
     * @deprecated Not to be used.
     */
    @Deprecated
    public static final ID<String, Void> NAME = ID.create("FilenameIndex");

    @Nonnull
    public static String[] getAllFilenames(@Nullable Project project) {
        Set<String> names = new HashSet<>();
        getService().processAllFileNames(
            (String s) -> {
                names.add(s);
                return true;
            },
            project == null ? new EverythingGlobalScope() : GlobalSearchScope.allScope(project),
            null
        );
        return ArrayUtil.toStringArray(names);
    }

    public static void processAllFileNames(
        @Nonnull Processor<? super String> processor,
        @Nonnull SearchScope scope,
        @Nullable IdFilter filter
    ) {
        getService().processAllFileNames(processor, scope, filter);
    }

    @Nonnull
    public static Collection<VirtualFile> getVirtualFilesByName(final Project project, @Nonnull String name, @Nonnull SearchScope scope) {
        return getService().getVirtualFilesByName(project, name, scope, null);
    }

    @Nonnull
    public static Collection<VirtualFile> getVirtualFilesByName(
        final Project project,
        @Nonnull String name,
        boolean caseSensitively,
        @Nonnull GlobalSearchScope scope
    ) {
        if (caseSensitively) {
            return getVirtualFilesByName(project, name, scope);
        }
        return getVirtualFilesByNameIgnoringCase(name, scope, project, null);
    }

    @Nonnull
    public static PsiFile[] getFilesByName(@Nonnull Project project, @Nonnull String name, @Nonnull SearchScope scope) {
        return (PsiFile[])getFilesByName(project, name, scope, false);
    }

    public static boolean processFilesByName(
        @Nonnull final String name,
        boolean directories,
        @Nonnull Processor<? super PsiFileSystemItem> processor,
        @Nonnull SearchScope scope,
        @Nonnull Project project,
        @Nullable IdFilter idFilter
    ) {
        return processFilesByName(name, directories, true, processor, scope, project, idFilter);
    }

    public static boolean processFilesByName(
        @Nonnull final String name,
        boolean directories,
        boolean caseSensitively,
        @Nonnull Processor<? super PsiFileSystemItem> processor,
        @Nonnull final SearchScope scope,
        @Nonnull final Project project,
        @Nullable IdFilter idFilter
    ) {
        final Collection<VirtualFile> files;

        if (caseSensitively) {
            files = getService().getVirtualFilesByName(project, name, scope, idFilter);
        }
        else {
            files = getVirtualFilesByNameIgnoringCase(name, scope, project, idFilter);
        }

        if (files.isEmpty()) {
            return false;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        int processedFiles = 0;

        for (VirtualFile file : files) {
            if (!file.isValid()) {
                continue;
            }
            if (!directories && !file.isDirectory()) {
                PsiFile psiFile = psiManager.findFile(file);
                if (psiFile != null) {
                    if (!processor.process(psiFile)) {
                        return true;
                    }
                    ++processedFiles;
                }
            }
            else if (directories && file.isDirectory()) {
                PsiDirectory dir = psiManager.findDirectory(file);
                if (dir != null) {
                    if (!processor.process(dir)) {
                        return true;
                    }
                    ++processedFiles;
                }
            }
        }
        return processedFiles > 0;
    }

    @Nonnull
    private static Set<VirtualFile> getVirtualFilesByNameIgnoringCase(
        @Nonnull final String name,
        @Nonnull final SearchScope scope,
        @Nonnull Project project,
        @Nullable final IdFilter idFilter
    ) {
        final Set<String> keys = new HashSet<>();
        FileNameIndexService fileNameIndexService = getService();
        fileNameIndexService.processAllFileNames(value -> {
            if (name.equalsIgnoreCase(value)) {
                keys.add(value);
            }
            return true;
        }, scope, idFilter);

        // values accessed outside of processAllKeys
        final Set<VirtualFile> files = new HashSet<>();
        for (String each : keys) {
            files.addAll(fileNameIndexService.getVirtualFilesByName(project, each, scope, idFilter));
        }
        return files;
    }

    @Nonnull
    public static PsiFileSystemItem[] getFilesByName(
        @Nonnull Project project,
        @Nonnull String name,
        @Nonnull final SearchScope scope,
        boolean directories
    ) {
        SmartList<PsiFileSystemItem> result = new SmartList<>();
        Processor<PsiFileSystemItem> processor = Processors.cancelableCollectProcessor(result);
        processFilesByName(name, directories, processor, scope, project, null);

        if (directories) {
            return result.toArray(new PsiFileSystemItem[0]);
        }
        //noinspection SuspiciousToArrayCall
        return result.toArray(PsiFile.EMPTY_ARRAY);
    }

    /**
     * Returns all files in the project by extension
     *
     * @param project current project
     * @param ext     file extension without leading dot e.q. "txt", "wsdl"
     * @return all files with provided extension
     * @author Konstantin Bulenkov
     */
    @Nonnull
    public static Collection<VirtualFile> getAllFilesByExt(@Nonnull Project project, @Nonnull String ext) {
        return getAllFilesByExt(project, ext, GlobalSearchScope.allScope(project));
    }

    @Nonnull
    public static Collection<VirtualFile> getAllFilesByExt(
        @Nonnull Project project,
        @Nonnull String ext,
        @Nonnull GlobalSearchScope searchScope
    ) {
        int len = ext.length();

        if (len == 0) {
            return Collections.emptyList();
        }

        ext = "." + ext;
        len++;

        final List<VirtualFile> files = new ArrayList<>();
        for (String name : getAllFilenames(project)) {
            final int length = name.length();
            if (length > len && name.substring(length - len).equalsIgnoreCase(ext)) {
                files.addAll(getVirtualFilesByName(project, name, searchScope));
            }
        }
        return files;
    }

    static FileNameIndexService getService() {
        return Application.get().getInstance(FileNameIndexService.class);
    }
}
