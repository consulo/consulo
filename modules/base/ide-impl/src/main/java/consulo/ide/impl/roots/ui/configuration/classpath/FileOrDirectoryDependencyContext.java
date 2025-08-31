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
package consulo.ide.impl.roots.ui.configuration.classpath;

import consulo.ide.setting.module.AddModuleDependencyContext;
import consulo.language.editor.LangDataKeys;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileChooserUtil;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.content.internal.LibraryEx;
import consulo.application.content.impl.internal.library.LibraryTableBase;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryType;
import consulo.content.library.PersistentLibraryKind;
import consulo.content.library.ui.LibraryRootsComponentDescriptor;
import consulo.content.library.OrderRoot;
import consulo.ide.impl.idea.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import consulo.ide.setting.module.ClasspathPanel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.content.library.ui.DefaultLibraryRootsComponentDescriptor;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.module.content.layer.ModifiableModuleRootLayer;
import consulo.content.base.BinariesOrderRootType;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author VISTALL
 * @since 27.09.14
 */
public class FileOrDirectoryDependencyContext extends AddModuleDependencyContext<VirtualFile[]> {
  private final FileChooserDescriptor myFileChooserDescriptor;

  private final HashMap<LibraryRootsComponentDescriptor, LibraryType> myLibraryTypes;
  private final DefaultLibraryRootsComponentDescriptor myDefaultDescriptor;

  public FileOrDirectoryDependencyContext(ClasspathPanel panel, ModulesConfigurator modulesConfigurator, LibrariesConfigurator librariesConfigurator) {
    super(panel, modulesConfigurator, librariesConfigurator);

    myLibraryTypes = new HashMap<>();
    myDefaultDescriptor = new DefaultLibraryRootsComponentDescriptor();

    for (LibraryType<?> libraryType : LibraryEditingUtil.getSuitableTypes(myClasspathPanel)) {
      LibraryRootsComponentDescriptor descriptor = null;
      if (libraryType != null) {
        descriptor = libraryType.createLibraryRootsComponentDescriptor();
      }
      if (descriptor == null) {
        descriptor = myDefaultDescriptor;
      }
      if (!myLibraryTypes.containsKey(descriptor)) {
        myLibraryTypes.put(descriptor, libraryType);
      }
    }

    myFileChooserDescriptor = createFileChooserDescriptor();
  }

  private FileChooserDescriptor createFileChooserDescriptor() {
    FileChooserDescriptor chooserDescriptor;
    final List<Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor>> descriptors = new ArrayList<Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor>>();
    for (LibraryRootsComponentDescriptor componentDescriptor : myLibraryTypes.keySet()) {
      descriptors.add(Pair.create(componentDescriptor, componentDescriptor.createAttachFilesChooserDescriptor(null)));
    }
    if (descriptors.size() == 1) {
      chooserDescriptor = descriptors.get(0).getSecond();
    }
    else {
      chooserDescriptor = new FileChooserDescriptor(true, true, true, false, true, false) {
        @RequiredUIAccess
        @Override
        public boolean isFileSelectable(VirtualFile file) {
          for (Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor> pair : descriptors) {
            if (pair.getSecond().isFileSelectable(file)) {
              return true;
            }
          }
          return false;
        }

        @Override
        public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
          for (Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor> pair : descriptors) {
            if (pair.getSecond().isFileVisible(file, showHiddenFiles)) {
              return true;
            }
          }
          return false;
        }
      };
    }
    chooserDescriptor.putUserData(LangDataKeys.MODULE_CONTEXT, myClasspathPanel.getRootModel().getModule());
    return chooserDescriptor;
  }

  @Nonnull
  @Override
  public List<OrderEntry> createOrderEntries(@Nonnull ModifiableModuleRootLayer layer, @Nonnull VirtualFile[] value) {
    List<VirtualFile> chosenFiles = FileChooserUtil.getChosenFiles(myFileChooserDescriptor, Arrays.asList(value));
    if (chosenFiles.isEmpty()) {
      return Collections.emptyList();
    }

    List<Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor>> descriptors = new ArrayList<Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor>>();
    for (LibraryRootsComponentDescriptor componentDescriptor : myLibraryTypes.keySet()) {
      descriptors.add(Pair.create(componentDescriptor, componentDescriptor.createAttachFilesChooserDescriptor(null)));
    }

    List<LibraryRootsComponentDescriptor> suitableDescriptors = new ArrayList<LibraryRootsComponentDescriptor>();
    for (Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor> pair : descriptors) {
      if (acceptAll(pair.getSecond(), chosenFiles)) {
        suitableDescriptors.add(pair.getFirst());
      }
    }

    LibraryRootsComponentDescriptor rootsComponentDescriptor;
    LibraryType libraryType = null;
    if (suitableDescriptors.size() == 1) {
      rootsComponentDescriptor = suitableDescriptors.get(0);
      libraryType = myLibraryTypes.get(rootsComponentDescriptor);
    }
    else {
      rootsComponentDescriptor = myDefaultDescriptor;
    }
    List<OrderRoot> chosenRoots = RootDetectionUtil.detectRoots(chosenFiles, null, layer.getProject(), rootsComponentDescriptor);

    List<OrderRoot> roots = filterAlreadyAdded(layer, chosenRoots);
    if (roots.isEmpty()) {
      return Collections.emptyList();
    }

    List<Library> addedLibraries = new ArrayList<Library>();
    boolean onlyClasses = true;
    for (OrderRoot root : roots) {
      onlyClasses &= root.getType() == BinariesOrderRootType.getInstance();
    }
    if (onlyClasses) {
      for (OrderRoot root : roots) {
        addedLibraries.add(createLibraryFromRoots(layer, Collections.singletonList(root), libraryType));
      }
    }
    else {
      addedLibraries.add(createLibraryFromRoots(layer, roots, libraryType));
    }


    List<OrderEntry> orderEntries = new ArrayList<OrderEntry>(addedLibraries.size());
    for (Library addedLibrary : addedLibraries) {
      LibraryOrderEntry libraryOrderEntry = layer.findLibraryOrderEntry(addedLibrary);
      if (libraryOrderEntry != null) {
        orderEntries.add(libraryOrderEntry);
      }
    }
    return orderEntries;
  }

  private Library createLibraryFromRoots(ModifiableModuleRootLayer layer, List<OrderRoot> roots, @Nullable LibraryType libraryType) {
    LibraryTable.ModifiableModel moduleLibraryModel = layer.getModuleLibraryTable().getModifiableModel();

    PersistentLibraryKind kind = libraryType == null ? null : libraryType.getKind();
    Library library = ((LibraryTableBase.ModifiableModelEx)moduleLibraryModel).createLibrary(null, kind);
    LibraryEx.ModifiableModelEx libModel = (LibraryEx.ModifiableModelEx)library.getModifiableModel();

    for (OrderRoot root : roots) {
      if (root.isJarDirectory()) {
        libModel.addJarDirectory(root.getFile(), false, root.getType());
      }
      else {
        libModel.addRoot(root.getFile(), root.getType());
      }
    }
    libModel.commit();
    return library;
  }

  private List<OrderRoot> filterAlreadyAdded(ModifiableModuleRootLayer layer, List<OrderRoot> roots) {
    if (roots == null || roots.isEmpty()) {
      return Collections.emptyList();
    }

    List<OrderRoot> result = new ArrayList<OrderRoot>();
    Library[] libraries = layer.getModuleLibraryTable().getLibraries();
    for (OrderRoot root : roots) {
      if (!isIncluded(root, libraries)) {
        result.add(root);
      }
    }
    return result;
  }

  private static boolean isIncluded(OrderRoot root, Library[] libraries) {
    for (Library library : libraries) {
      if (ArrayUtil.contains(root.getFile(), library.getFiles(root.getType()))) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  private static boolean acceptAll(FileChooserDescriptor descriptor, Collection<VirtualFile> files) {
    for (VirtualFile file : files) {
      if (!descriptor.isFileSelectable(file) || !descriptor.isFileVisible(file, true)) {
        return false;
      }
    }
    return true;
  }

  public FileChooserDescriptor getFileChooserDescriptor() {
    return myFileChooserDescriptor;
  }
}
