/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.psi.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.component.extension.ExtensionPoint;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.module.content.DirectoryIndex;
import consulo.util.lang.ObjectUtil;
import consulo.application.util.query.Query;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.LowMemoryWatcher;
import consulo.disposer.Disposable;
import consulo.language.psi.*;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.extension.ModuleExtension;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFileMoveEvent;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 2013-05-20
 */
@Singleton
@ServiceImpl
public class PsiPackageManagerImpl extends PsiPackageManager implements Disposable {
    private final Project myProject;
    private final ProjectFileIndex myProjectFileIndex;
    private final PsiManager myPsiManager;
    private final Provider<DirectoryIndex> myDirectoryIndex;

    private final Map<Class<? extends ModuleExtension>, ConcurrentMap<String, Object>> myPackageCache = new ConcurrentHashMap<>();

    @Inject
    public PsiPackageManagerImpl(
        Project project,
        ProjectFileIndex projectFileIndex,
        PsiManager psiManager,
        Provider<DirectoryIndex> directoryIndex
    ) {
        myProject = project;
        myProjectFileIndex = projectFileIndex;
        myPsiManager = psiManager;
        myDirectoryIndex = directoryIndex;

        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void fileCreated(@Nonnull VirtualFileEvent event) {
                myPackageCache.clear();
            }

            @Override
            public void fileDeleted(@Nonnull VirtualFileEvent event) {
                myPackageCache.clear();
            }

            @Override
            public void fileMoved(@Nonnull VirtualFileMoveEvent event) {
                myPackageCache.clear();
            }
        }, this);

        project.getMessageBus().connect().subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                myPackageCache.clear();
            }
        });

        LowMemoryWatcher.register(myPackageCache::clear, this);
    }

    @Override
    public void dropCache(@Nonnull Class<? extends ModuleExtension> extensionClass) {
        myPackageCache.remove(extensionClass);
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiPackage findPackage(@Nonnull String qualifiedName, @Nonnull Class<? extends ModuleExtension> extensionClass) {
        ConcurrentMap<String, Object> map = myPackageCache.get(extensionClass);
        if (map != null) {
            Object value = map.get(qualifiedName);
            // if we processed - but not found package
            if (value == ObjectUtil.NULL) {
                return null;
            }
            else if (value != null) {
                return (PsiPackage)value;
            }
        }

        PsiPackage newPackage = createPackage(qualifiedName, extensionClass);

        Object valueForInsert = ObjectUtil.notNull(newPackage, ObjectUtil.NULL);

        myPackageCache.computeIfAbsent(extensionClass, aClass -> new ConcurrentHashMap<>()).putIfAbsent(qualifiedName, valueForInsert);

        return newPackage;
    }

    @Nullable
    private PsiPackage createPackage(@Nonnull String qualifiedName, @Nonnull Class<? extends ModuleExtension> extensionClass) {
        Query<VirtualFile> dirs = myDirectoryIndex.get().getDirectoriesByPackageName(qualifiedName, true);
        if (dirs.findFirst() == null) {
            return null;
        }

        for (VirtualFile directory : dirs) {
            PsiPackage packageFromProviders = createPackageFromProviders(directory, extensionClass, qualifiedName);
            if (packageFromProviders != null) {
                return packageFromProviders;
            }

            PsiPackage packageFromLibrary = createPackageFromLibrary(directory, extensionClass, qualifiedName);
            if (packageFromLibrary != null) {
                return packageFromLibrary;
            }
        }
        return null;
    }

    @Nullable
    private PsiPackage createPackageFromProviders(
        @Nonnull VirtualFile virtualFile,
        @Nonnull Class<? extends ModuleExtension> extensionClass,
        @Nonnull String qualifiedName
    ) {
        Module moduleForFile = ModuleUtil.findModuleForFile(virtualFile, myProject);
        if (moduleForFile == null) {
            return null;
        }

        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleForFile);

        ModuleExtension extension = moduleRootManager.getExtension(extensionClass);
        if (extension == null) {
            return null;
        }

        return myProject.getExtensionPoint(PsiPackageSupportProvider.class).safeStream()
            .filter(p -> p.isSupported(extension) && p.acceptVirtualFile(moduleForFile, virtualFile))
            .map(p -> p.createPackage(myPsiManager, this, extensionClass, qualifiedName))
            .findFirst()
            .orElse(null);
    }

    private PsiPackage createPackageFromLibrary(
        @Nonnull VirtualFile virtualFile,
        @Nonnull Class<? extends ModuleExtension> extensionClass,
        @Nonnull String qualifiedName
    ) {
        if (myProjectFileIndex.isInLibraryClasses(virtualFile)) {
            List<OrderEntry> orderEntriesForFile = myProjectFileIndex.getOrderEntriesForFile(virtualFile);
            ExtensionPoint<PsiPackageSupportProvider> extensionPoint = myProject.getExtensionPoint(PsiPackageSupportProvider.class);
            for (OrderEntry orderEntry : orderEntriesForFile) {
                Module ownerModule = orderEntry.getOwnerModule();
                ModuleExtension extension = ModuleUtilCore.getExtension(ownerModule, extensionClass);
                if (extension == null) {
                    continue;
                }
                extensionPoint.safeStream()
                    .filter(p -> p.isSupported(extension))
                    .map(p -> p.createPackage(myPsiManager, this, extensionClass, qualifiedName))
                    .findFirst()
                    .orElse(null);
            }
        }
        return null;
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiPackage findPackage(@Nonnull PsiDirectory directory, @Nonnull Class<? extends ModuleExtension> extensionClass) {
        String packageName = myDirectoryIndex.get().getPackageName(directory.getVirtualFile());
        if (packageName == null) {
            return null;
        }
        return findPackage(packageName, extensionClass);
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiPackage findAnyPackage(@Nonnull VirtualFile directory) {
        String packageName = myDirectoryIndex.get().getPackageName(directory);
        if (packageName == null) {
            return null;
        }

        PsiPackage packageFromCache = findAnyPackageFromCache(packageName);
        if (packageFromCache != null) {
            return packageFromCache;
        }

        Module module = ModuleUtilCore.findModuleForFile(directory, myProject);
        if (module != null) {
            PsiPackage aPackage = findForModule(packageName, module);
            if (aPackage != null) {
                return aPackage;
            }
        }
        else {
            List<OrderEntry> orderEntriesForFile = myProjectFileIndex.getOrderEntriesForFile(directory);
            for (OrderEntry orderEntry : orderEntriesForFile) {
                Module ownerModule = orderEntry.getOwnerModule();
                return findForModule(packageName, ownerModule);
            }
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    private PsiPackage findForModule(String packageName, Module module) {
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        for (ModuleExtension<?> moduleExtension : rootManager.getExtensions()) {
            PsiPackage aPackage = findPackage(packageName, moduleExtension.getClass());
            if (aPackage != null) {
                return aPackage;
            }
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public PsiPackage findAnyPackage(@Nonnull String packageName) {
        Query<VirtualFile> dirs = myDirectoryIndex.get().getDirectoriesByPackageName(packageName, true);
        if (dirs.findFirst() == null) {
            return null;
        }

        PsiPackage packageFromCache = findAnyPackageFromCache(packageName);
        if (packageFromCache != null) {
            return packageFromCache;
        }

        for (VirtualFile dir : dirs) {
            PsiPackage psiPackage = findAnyPackage(dir);
            if (psiPackage != null) {
                return psiPackage;
            }
        }
        return null;
    }

    @Nullable
    private PsiPackage findAnyPackageFromCache(@Nonnull String packageName) {
        for (ConcurrentMap<String, Object> map : myPackageCache.values()) {
            Object o = map.get(packageName);
            if (o instanceof PsiPackage) {
                return (PsiPackage)o;
            }
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public boolean isValidPackageName(@Nonnull Module module, @Nonnull String packageName) {
        ExtensionPoint<PsiPackageSupportProvider> extensionPoint = myProject.getExtensionPoint(PsiPackageSupportProvider.class);

        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        for (ModuleExtension<?> moduleExtension : rootManager.getExtensions()) {
            boolean matched = extensionPoint
                .anyMatchSafe(provider -> provider.isSupported(moduleExtension) && provider.isValidPackageName(module, packageName));
            if (matched) {
                return true;
            }
        }
        return true;
    }

    @Override
    public void dispose() {
        myPackageCache.clear();
    }
}
