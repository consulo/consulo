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
package com.intellij.openapi.roots.ui.configuration.libraries.impl;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.*;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author nik
 */
@Singleton
public class LibraryPresentationManagerImpl extends LibraryPresentationManager {
  private Map<LibraryKind, LibraryPresentationProvider<?>> myPresentationProviders;

  @SuppressWarnings("unchecked")
  private <P extends LibraryProperties> LibraryPresentationProvider<P> getPresentationProvider(LibraryKind kind) {
    if (myPresentationProviders == null) {
      final Map<LibraryKind, LibraryPresentationProvider<?>> providers = new HashMap<>();
      for (LibraryType<?> type : LibraryType.EP_NAME.getExtensionList()) {
        providers.put(type.getKind(), type);
      }
      for (LibraryPresentationProvider provider : LibraryPresentationProvider.EP_NAME.getExtensionList()) {
        providers.put(provider.getKind(), provider);
      }
      myPresentationProviders = providers;
    }
    return (LibraryPresentationProvider<P>)myPresentationProviders.get(kind);
  }

  @Nonnull
  @Override
  public Image getNamedLibraryIcon(@Nonnull Library library, @Nullable LibrariesConfigurator context) {
    final Image icon = getCustomIcon(library, context);
    return icon != null ? icon : AllIcons.Nodes.PpLib;
  }

  @Override
  public Image getCustomIcon(@Nonnull Library library, LibrariesConfigurator context) {
    final LibraryKind kind = ((LibraryEx)library).getKind();
    if (kind != null) {
      return LibraryType.findByKind(kind).getIcon();
    }
    final List<Image> icons = getCustomIcons(library, context);
    if (icons.size() == 1) {
      return icons.get(0);
    }
    return null;
  }

  @Nonnull
  @Override
  public List<Image> getCustomIcons(@Nonnull Library library, LibrariesConfigurator context) {
    final VirtualFile[] files = getLibraryFiles(library, context);
    final List<Image> icons = new SmartList<>();
    LibraryDetectionManager.getInstance().processProperties(Arrays.asList(files), new LibraryDetectionManager.LibraryPropertiesProcessor() {
      @Override
      public <P extends LibraryProperties> boolean processProperties(@Nonnull LibraryKind kind, @Nonnull P properties) {
        final LibraryPresentationProvider<P> provider = getPresentationProvider(kind);
        if (provider != null) {
          ContainerUtil.addIfNotNull(icons, provider.getIcon());
        }
        return true;
      }
    });
    return icons;
  }

  @Override
  public boolean isLibraryOfKind(@Nonnull List<VirtualFile> files, @Nonnull final LibraryKind kind) {
    return !LibraryDetectionManager.getInstance().processProperties(files, new LibraryDetectionManager.LibraryPropertiesProcessor() {
      @Override
      public <P extends LibraryProperties> boolean processProperties(@Nonnull LibraryKind processedKind, @Nonnull P properties) {
        return !kind.equals(processedKind);
      }
    });
  }

  @Override
  public boolean isLibraryOfKind(@Nonnull Library library,
                                 @Nonnull LibrariesContainer librariesContainer,
                                 @Nonnull final Set<? extends LibraryKind> acceptedKinds) {
    final LibraryKind type = ((LibraryEx)library).getKind();
    if (type != null && acceptedKinds.contains(type)) return true;

    final VirtualFile[] files = librariesContainer.getLibraryFiles(library, BinariesOrderRootType.getInstance());
    return !LibraryDetectionManager.getInstance().processProperties(Arrays.asList(files), new LibraryDetectionManager.LibraryPropertiesProcessor() {
      @Override
      public <P extends LibraryProperties> boolean processProperties(@Nonnull LibraryKind processedKind, @Nonnull P properties) {
        return !acceptedKinds.contains(processedKind);
      }
    });
  }

  public static List<LibraryKind> getLibraryKinds(@Nonnull Library library, @Nullable LibrariesConfigurator context) {
    final List<LibraryKind> result = new SmartList<>();
    final LibraryKind kind = ((LibraryEx)library).getKind();
    if (kind != null) {
      result.add(kind);
    }
    final VirtualFile[] files = getLibraryFiles(library, context);
    LibraryDetectionManager.getInstance().processProperties(Arrays.asList(files), new LibraryDetectionManager.LibraryPropertiesProcessor() {
      @Override
      public <P extends LibraryProperties> boolean processProperties(@Nonnull LibraryKind kind, @Nonnull P properties) {
        result.add(kind);
        return true;
      }
    });
    return result;
  }

  @Nonnull
  @Override
  public List<String> getDescriptions(@Nonnull Library library, LibrariesConfigurator context) {
    final VirtualFile[] files = getLibraryFiles(library, context);
    return getDescriptions(files, Collections.<LibraryKind>emptySet());
  }

  @Nonnull
  private static VirtualFile[] getLibraryFiles(@Nonnull Library library, @Nullable LibrariesConfigurator context) {
    if (((LibraryEx)library).isDisposed()) {
      return VirtualFile.EMPTY_ARRAY;
    }
    return context != null ? context.getLibraryFiles(library, BinariesOrderRootType.getInstance()) : library.getFiles(BinariesOrderRootType.getInstance());
  }

  @Nonnull
  @Override
  public List<String> getDescriptions(@Nonnull VirtualFile[] classRoots, final Set<LibraryKind> excludedKinds) {
    final SmartList<String> result = new SmartList<>();
    LibraryDetectionManager.getInstance().processProperties(Arrays.asList(classRoots), new LibraryDetectionManager.LibraryPropertiesProcessor() {
      @Override
      public <P extends LibraryProperties> boolean processProperties(@Nonnull LibraryKind kind, @Nonnull P properties) {
        if (!excludedKinds.contains(kind)) {
          final LibraryPresentationProvider<P> provider = getPresentationProvider(kind);
          if (provider != null) {
            ContainerUtil.addIfNotNull(result, provider.getDescription(properties));
          }
        }
        return true;
      }
    });
    return result;
  }

  @Override
  public List<Library> getLibraries(@Nonnull Set<LibraryKind> kinds, @Nonnull Project project, @Nullable LibrariesConfigurator context) {
    List<Library> libraries = new ArrayList<>();
    if (context != null) {
      Collections.addAll(libraries, context.getProjectLibrariesProvider().getModifiableModel().getLibraries());
    }
    else {
      final LibraryTablesRegistrar registrar = LibraryTablesRegistrar.getInstance();
      Collections.addAll(libraries, registrar.getLibraryTable(project).getLibraries());
    }

    final Iterator<Library> iterator = libraries.iterator();
    while (iterator.hasNext()) {
      Library library = iterator.next();
      final List<LibraryKind> libraryKinds = getLibraryKinds(library, context);
      if (!ContainerUtil.intersects(libraryKinds, kinds)) {
        iterator.remove();
      }
    }
    return libraries;
  }
}
