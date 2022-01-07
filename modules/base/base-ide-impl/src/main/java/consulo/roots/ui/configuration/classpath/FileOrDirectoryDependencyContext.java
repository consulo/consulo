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
package consulo.roots.ui.configuration.classpath;

import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.impl.FileChooserUtil;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryTableBase;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.DefaultLibraryRootsComponentDescriptor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import consulo.roots.ModifiableModuleRootLayer;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    final FileChooserDescriptor chooserDescriptor;
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

    final List<Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor>> descriptors = new ArrayList<Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor>>();
    for (LibraryRootsComponentDescriptor componentDescriptor : myLibraryTypes.keySet()) {
      descriptors.add(Pair.create(componentDescriptor, componentDescriptor.createAttachFilesChooserDescriptor(null)));
    }

    List<LibraryRootsComponentDescriptor> suitableDescriptors = new ArrayList<LibraryRootsComponentDescriptor>();
    for (Pair<LibraryRootsComponentDescriptor, FileChooserDescriptor> pair : descriptors) {
      if (acceptAll(pair.getSecond(), chosenFiles)) {
        suitableDescriptors.add(pair.getFirst());
      }
    }

    final LibraryRootsComponentDescriptor rootsComponentDescriptor;
    LibraryType libraryType = null;
    if (suitableDescriptors.size() == 1) {
      rootsComponentDescriptor = suitableDescriptors.get(0);
      libraryType = myLibraryTypes.get(rootsComponentDescriptor);
    }
    else {
      rootsComponentDescriptor = myDefaultDescriptor;
    }
    List<OrderRoot> chosenRoots = RootDetectionUtil.detectRoots(chosenFiles, null, layer.getProject(), rootsComponentDescriptor);

    final List<OrderRoot> roots = filterAlreadyAdded(layer, chosenRoots);
    if (roots.isEmpty()) {
      return Collections.emptyList();
    }

    final List<Library> addedLibraries = new ArrayList<Library>();
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

  private Library createLibraryFromRoots(ModifiableModuleRootLayer layer, List<OrderRoot> roots, @Nullable final LibraryType libraryType) {
    final LibraryTable.ModifiableModel moduleLibraryModel = layer.getModuleLibraryTable().getModifiableModel();

    final PersistentLibraryKind kind = libraryType == null ? null : libraryType.getKind();
    final Library library = ((LibraryTableBase.ModifiableModelEx)moduleLibraryModel).createLibrary(null, kind);
    final LibraryEx.ModifiableModelEx libModel = (LibraryEx.ModifiableModelEx)library.getModifiableModel();

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

  private List<OrderRoot> filterAlreadyAdded(ModifiableModuleRootLayer layer, final List<OrderRoot> roots) {
    if (roots == null || roots.isEmpty()) {
      return Collections.emptyList();
    }

    final List<OrderRoot> result = new ArrayList<OrderRoot>();
    final Library[] libraries = layer.getModuleLibraryTable().getLibraries();
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
