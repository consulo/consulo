/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.internal.LibraryKindRegistry;
import consulo.content.library.*;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class LibraryPresentationManagerImpl extends LibraryPresentationManager {
    private Map<LibraryKind, LibraryPresentation<?>> myPresentationProviders;

    @SuppressWarnings("unchecked")
    private <P extends LibraryProperties> LibraryPresentationProvider<P> getPresentationProvider(LibraryKind kind) {
        if (myPresentationProviders == null) {
            Map<LibraryKind, LibraryPresentation<?>> providers = new HashMap<>();
            for (LibraryType<?> type : LibraryType.EP_NAME.getExtensionList()) {
                providers.put(type.getKind(), type);
            }
            for (LibraryPresentationProvider provider : LibraryPresentationProvider.EP_NAME.getExtensionList()) {
                providers.put(provider.getKind(), provider);
            }
            myPresentationProviders = providers;
        }
        return (LibraryPresentationProvider<P>) myPresentationProviders.get(kind);
    }

    @Nonnull
    @Override
    public Image getNamedLibraryIcon(@Nonnull Library library, @Nullable LibrariesConfigurator context) {
        Image icon = getCustomIcon(library, context);
        return icon != null ? icon : PlatformIconGroup.nodesPplib();
    }

    @Override
    public Image getCustomIcon(@Nonnull Library library, LibrariesConfigurator context) {
        LibraryKind kind = library.getKind();
        if (kind != null) {
            LibraryType<?> type = LibraryKindRegistry.getInstance().findLibraryTypeByKindId(kind.getKindId());
            if (type != null) {
                return type.getIcon();
            }
        }
        List<Image> icons = getCustomIcons(library, context);
        if (icons.size() == 1) {
            return icons.get(0);
        }
        return null;
    }

    @Nonnull
    @Override
    public List<Image> getCustomIcons(@Nonnull Library library, LibrariesConfigurator context) {
        VirtualFile[] files = getLibraryFiles(library, context);
        List<Image> icons = new SmartList<>();
        LibraryDetectionManager.getInstance().processProperties(
            Arrays.asList(files),
            new LibraryDetectionManager.LibraryPropertiesProcessor() {
                @Override
                public <P extends LibraryProperties> boolean processProperties(@Nonnull LibraryKind kind, @Nonnull P properties) {
                    LibraryPresentationProvider<P> provider = getPresentationProvider(kind);
                    if (provider != null) {
                        ContainerUtil.addIfNotNull(icons, provider.getIcon());
                    }
                    return true;
                }
            }
        );
        return icons;
    }

    public static List<LibraryKind> getLibraryKinds(@Nonnull Library library, @Nullable LibrariesConfigurator context) {
        List<LibraryKind> result = new SmartList<>();
        LibraryKind kind = library.getKind();
        if (kind != null) {
            result.add(kind);
        }
        VirtualFile[] files = getLibraryFiles(library, context);
        LibraryDetectionManager.getInstance().processProperties(
            Arrays.asList(files),
            new LibraryDetectionManager.LibraryPropertiesProcessor() {
                @Override
                public <P extends LibraryProperties> boolean processProperties(@Nonnull LibraryKind kind, @Nonnull P properties) {
                    result.add(kind);
                    return true;
                }
            }
        );
        return result;
    }

    @Nonnull
    @Override
    public List<String> getDescriptions(@Nonnull Library library, LibrariesConfigurator context) {
        VirtualFile[] files = getLibraryFiles(library, context);
        return getDescriptions(files, Collections.<LibraryKind>emptySet());
    }

    @Nonnull
    private static VirtualFile[] getLibraryFiles(@Nonnull Library library, @Nullable LibrariesConfigurator context) {
        if (library.isDisposed()) {
            return VirtualFile.EMPTY_ARRAY;
        }
        return context != null
            ? context.getLibraryFiles(library, BinariesOrderRootType.getInstance())
            : library.getFiles(BinariesOrderRootType.getInstance());
    }

    @Nonnull
    @Override
    public List<String> getDescriptions(@Nonnull VirtualFile[] classRoots, Set<LibraryKind> excludedKinds) {
        SmartList<String> result = new SmartList<>();
        LibraryDetectionManager.getInstance().processProperties(
            Arrays.asList(classRoots),
            new LibraryDetectionManager.LibraryPropertiesProcessor() {
                @Override
                public <P extends LibraryProperties> boolean processProperties(@Nonnull LibraryKind kind, @Nonnull P properties) {
                    if (!excludedKinds.contains(kind)) {
                        LibraryPresentationProvider<P> provider = getPresentationProvider(kind);
                        if (provider != null) {
                            ContainerUtil.addIfNotNull(result, provider.getDescription(properties));
                        }
                    }
                    return true;
                }
            }
        );
        return result;
    }

    @Override
    public List<Library> getLibraries(@Nonnull Set<LibraryKind> kinds, @Nonnull Project project, @Nullable LibrariesConfigurator context) {
        List<Library> libraries = new ArrayList<>();
        if (context != null) {
            Collections.addAll(libraries, context.getProjectLibrariesProvider().getModifiableModel().getLibraries());
        }
        else {
            LibraryTablesRegistrar registrar = LibraryTablesRegistrar.getInstance();
            Collections.addAll(libraries, registrar.getLibraryTable(project).getLibraries());
        }

        Iterator<Library> iterator = libraries.iterator();
        while (iterator.hasNext()) {
            Library library = iterator.next();
            List<LibraryKind> libraryKinds = getLibraryKinds(library, context);
            if (!ContainerUtil.intersects(libraryKinds, kinds)) {
                iterator.remove();
            }
        }
        return libraries;
    }
}
