// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi.include;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.application.util.function.Processor;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.include.FileIncludeInfo;
import consulo.language.psi.include.FileIncludeManager;
import consulo.language.psi.include.FileIncludeProvider;
import consulo.language.psi.path.FileReferenceSet;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@ServiceImpl
public final class FileIncludeManagerImpl extends FileIncludeManager {
    private final Project myProject;
    private final PsiManager myPsiManager;
    private final PsiFileFactory myPsiFileFactory;

    private final IncludeCacheHolder myIncludedHolder = new IncludeCacheHolder("compile time includes", "runtime includes") {
        @Override
        protected VirtualFile[] computeFiles(final PsiFile file, final boolean compileTimeOnly) {
            final Set<VirtualFile> files = new HashSet<>();
            processIncludes(file, info -> {
                if (compileTimeOnly != info.runtimeOnly) {
                    PsiFileSystemItem item = resolveFileInclude(info, file);
                    if (item != null) {
                        ContainerUtil.addIfNotNull(files, item.getVirtualFile());
                    }
                }
                return true;
            });
            return VirtualFileUtil.toVirtualFileArray(files);
        }
    };
    private final Map<String, FileIncludeProvider> myProviderMap;

    public void processIncludes(PsiFile file, Processor<? super FileIncludeInfo> processor) {
        List<FileIncludeInfo> infoList = FileIncludeIndex.getIncludes(file.getVirtualFile(), myProject);
        for (FileIncludeInfo info : infoList) {
            if (!processor.process(info)) {
                return;
            }
        }
    }

    private final IncludeCacheHolder myIncludingHolder = new IncludeCacheHolder("compile time contexts", "runtime contexts") {
        @Override
        protected VirtualFile[] computeFiles(PsiFile context, boolean compileTimeOnly) {
            final Set<VirtualFile> files = new HashSet<>();
            processIncludingFiles(context, virtualFileFileIncludeInfoPair -> {
                files.add(virtualFileFileIncludeInfoPair.first);
                return true;
            });
            return VirtualFileUtil.toVirtualFileArray(files);
        }
    };

    @Override
    public void processIncludingFiles(PsiFile context, Processor<? super Pair<VirtualFile, FileIncludeInfo>> processor) {
        context = context.getOriginalFile();
        VirtualFile contextFile = context.getVirtualFile();
        if (contextFile == null) {
            return;
        }

        String originalName = context.getName();
        Collection<String> names = getPossibleIncludeNames(context, originalName);

        GlobalSearchScope scope = GlobalSearchScope.allScope(myProject);
        for (String name : names) {
            MultiMap<VirtualFile, FileIncludeInfoImpl> infoList = FileIncludeIndex.getIncludingFileCandidates(name, scope);
            for (VirtualFile candidate : infoList.keySet()) {
                PsiFile psiFile = myPsiManager.findFile(candidate);
                if (psiFile == null || context.equals(psiFile)) {
                    continue;
                }
                for (FileIncludeInfo info : infoList.get(candidate)) {
                    PsiFileSystemItem item = resolveFileInclude(info, psiFile);
                    if (item != null && contextFile.equals(item.getVirtualFile())) {
                        if (!processor.process(Pair.create(candidate, info))) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @Nonnull
    private static Collection<String> getPossibleIncludeNames(@Nonnull PsiFile context, @Nonnull String originalName) {
        Collection<String> names = new HashSet<>();
        names.add(originalName);
        for (FileIncludeProvider provider : FileIncludeProvider.EP_NAME.getExtensionList()) {
            String newName = provider.getIncludeName(context, originalName);
            if (newName != originalName) {
                names.add(newName);
            }
        }
        return names;
    }

    @Inject
    public FileIncludeManagerImpl(Project project) {
        myProject = project;
        myPsiManager = PsiManager.getInstance(project);
        myPsiFileFactory = PsiFileFactory.getInstance(myProject);

        List<FileIncludeProvider> providers = FileIncludeProvider.EP_NAME.getExtensionList();
        myProviderMap = new HashMap<>(providers.size());
        for (FileIncludeProvider provider : providers) {
            FileIncludeProvider old = myProviderMap.put(provider.getId(), provider);
            assert old == null;
        }
    }

    @Override
    public VirtualFile[] getIncludedFiles(@Nonnull VirtualFile file, boolean compileTimeOnly) {
        return getIncludedFiles(file, compileTimeOnly, false);
    }

    @Override
    public VirtualFile[] getIncludedFiles(@Nonnull VirtualFile file, boolean compileTimeOnly, boolean recursively) {
        if (file instanceof VirtualFileWithId) {
            return myIncludedHolder.getAllFiles(file, compileTimeOnly, recursively);
        }
        else {
            return VirtualFile.EMPTY_ARRAY;
        }
    }

    @Override
    public VirtualFile[] getIncludingFiles(@Nonnull VirtualFile file, boolean compileTimeOnly) {
        return myIncludingHolder.getAllFiles(file, compileTimeOnly, false);
    }

    @Override
    public PsiFileSystemItem resolveFileInclude(@Nonnull final FileIncludeInfo info, @Nonnull final PsiFile context) {
        return doResolve(info, context);
    }

    @Nullable
    private PsiFileSystemItem doResolve(@Nonnull final FileIncludeInfo info, @Nonnull final PsiFile context) {
        if (info instanceof FileIncludeInfoImpl) {
            String id = ((FileIncludeInfoImpl)info).providerId;
            FileIncludeProvider provider = id == null ? null : myProviderMap.get(id);
            final PsiFileSystemItem resolvedByProvider = provider == null ? null : provider.resolveIncludedFile(info, context);
            if (resolvedByProvider != null) {
                return resolvedByProvider;
            }
        }

        PsiFileImpl psiFile = (PsiFileImpl)myPsiFileFactory.createFileFromText("dummy.txt", PlainTextFileType.INSTANCE, info.path);
        psiFile.setOriginalFile(context);
        return new FileReferenceSet(psiFile) {
            @Override
            protected boolean useIncludingFileAsContext() {
                return false;
            }
        }.resolve();
    }

    private abstract class IncludeCacheHolder {

        private final Key<ParameterizedCachedValue<VirtualFile[], PsiFile>> COMPILE_TIME_KEY;
        private final Key<ParameterizedCachedValue<VirtualFile[], PsiFile>> RUNTIME_KEY;

        private final ParameterizedCachedValueProvider<VirtualFile[], PsiFile> COMPILE_TIME_PROVIDER = new IncludedFilesProvider(true) {
            @Override
            protected VirtualFile[] computeFiles(PsiFile file, boolean compileTimeOnly) {
                return IncludeCacheHolder.this.computeFiles(file, compileTimeOnly);
            }
        };

        private final ParameterizedCachedValueProvider<VirtualFile[], PsiFile> RUNTIME_PROVIDER = new IncludedFilesProvider(false) {
            @Override
            protected VirtualFile[] computeFiles(PsiFile file, boolean compileTimeOnly) {
                return IncludeCacheHolder.this.computeFiles(file, compileTimeOnly);
            }
        };

        private IncludeCacheHolder(String compileTimeKey, String runtimeKey) {
            COMPILE_TIME_KEY = Key.create(compileTimeKey);
            RUNTIME_KEY = Key.create(runtimeKey);
        }

        @Nonnull
        private VirtualFile[] getAllFiles(@Nonnull VirtualFile file, boolean compileTimeOnly, boolean recursively) {
            if (recursively) {
                Set<VirtualFile> result = new HashSet<>();
                getAllFilesRecursively(file, compileTimeOnly, result);
                return VirtualFileUtil.toVirtualFileArray(result);
            }
            return getFiles(file, compileTimeOnly);
        }

        private void getAllFilesRecursively(@Nonnull VirtualFile file, boolean compileTimeOnly, Set<? super VirtualFile> result) {
            if (!result.add(file)) {
                return;
            }
            VirtualFile[] includes = getFiles(file, compileTimeOnly);
            if (includes.length != 0) {
                for (VirtualFile include : includes) {
                    getAllFilesRecursively(include, compileTimeOnly, result);
                }
            }
        }

        private VirtualFile[] getFiles(@Nonnull VirtualFile file, boolean compileTimeOnly) {
            PsiFile psiFile = myPsiManager.findFile(file);
            if (psiFile == null) {
                return VirtualFile.EMPTY_ARRAY;
            }
            if (compileTimeOnly) {
                return CachedValuesManager.getManager(myProject)
                    .getParameterizedCachedValue(psiFile, COMPILE_TIME_KEY, COMPILE_TIME_PROVIDER, false, psiFile);
            }
            return CachedValuesManager.getManager(myProject)
                .getParameterizedCachedValue(psiFile, RUNTIME_KEY, RUNTIME_PROVIDER, false, psiFile);
        }

        protected abstract VirtualFile[] computeFiles(PsiFile file, boolean compileTimeOnly);

    }

    private abstract static class IncludedFilesProvider implements ParameterizedCachedValueProvider<VirtualFile[], PsiFile> {
        private final boolean myRuntimeOnly;

        IncludedFilesProvider(boolean runtimeOnly) {
            myRuntimeOnly = runtimeOnly;
        }

        protected abstract VirtualFile[] computeFiles(PsiFile file, boolean compileTimeOnly);

        @Override
        public CachedValueProvider.Result<VirtualFile[]> compute(PsiFile psiFile) {
            VirtualFile[] value = computeFiles(psiFile, myRuntimeOnly);
            // todo: we need "url modification tracker" for VirtualFile
            List<Object> deps = new ArrayList<>(Arrays.asList(value));
            deps.add(psiFile);
            deps.add(VirtualFileManager.getInstance());

            return CachedValueProvider.Result.create(value, deps);
        }
    }
}
