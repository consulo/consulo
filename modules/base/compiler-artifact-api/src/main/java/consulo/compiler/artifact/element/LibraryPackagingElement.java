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
package consulo.compiler.artifact.element;

import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.LibraryElementPresentation;
import consulo.compiler.artifact.ui.PackagingElementPresentation;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.content.layer.ModulesProvider;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class LibraryPackagingElement extends ComplexPackagingElement<LibraryPackagingElement> {
  public static final String LIBRARY_NAME_ATTRIBUTE = "name";
  public static final String MODULE_NAME_ATTRIBUTE = "module-name";
  public static final String LIBRARY_LEVEL_ATTRIBUTE = "level";
  
  private String myLevel;
  private String myLibraryName;
  private String myModuleName;

  public LibraryPackagingElement() {
    super(LibraryElementType.getInstance());
  }

  public LibraryPackagingElement(String level, String libraryName, String moduleName) {
    super(LibraryElementType.getInstance());
    myLevel = level;
    myLibraryName = libraryName;
    myModuleName = moduleName;
  }

  @Override
  public List<? extends PackagingElement<?>> getSubstitution(@Nonnull PackagingElementResolvingContext context,
                                                             @Nonnull ArtifactType artifactType) {
    Library library = findLibrary(context);
    if (library != null) {
      VirtualFile[] files = library.getFiles(BinariesOrderRootType.getInstance());
      List<PackagingElement<?>> elements = new ArrayList<PackagingElement<?>>();
      for (VirtualFile file : files) {
        String path = FileUtil.toSystemIndependentName(VirtualFilePathUtil.getLocalPath(file));
        elements.add(file.isDirectory() && file.isInLocalFileSystem() ? new DirectoryCopyPackagingElement(path) : new FileCopyPackagingElement(
          path));
      }
      return elements;
    }
    return null;
  }

  @Nonnull
  @Override
  public PackagingElementOutputKind getFilesKind(PackagingElementResolvingContext context) {
    Library library = findLibrary(context);
    return library != null ? getKindForLibrary(library) : PackagingElementOutputKind.OTHER;
  }

  @Override
  public PackagingElementPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new LibraryElementPresentation(myLibraryName, myLevel, myModuleName, findLibrary(context), context);
  }

  @Override
  public boolean isEqualTo(@Nonnull PackagingElement<?> element) {
    if (!(element instanceof LibraryPackagingElement)) {
      return false;
    }

    LibraryPackagingElement packagingElement = (LibraryPackagingElement)element;
    return myLevel != null && myLibraryName != null && myLevel.equals(packagingElement.getLevel())
      && myLibraryName.equals(packagingElement.getLibraryName())
      && Comparing.equal(myModuleName, packagingElement.getModuleName());
  }

  @Override
  public LibraryPackagingElement getState() {
    return this;
  }

  @Override
  public void loadState(ArtifactManager artifactManager, LibraryPackagingElement state) {
    myLevel = state.getLevel();
    myLibraryName = state.getLibraryName();
    myModuleName = state.getModuleName();
  }

  @Attribute(LIBRARY_LEVEL_ATTRIBUTE)
  public String getLevel() {
    return myLevel;
  }

  public void setLevel(String level) {
    myLevel = level;
  }

  @Attribute(LIBRARY_NAME_ATTRIBUTE)
  public String getLibraryName() {
    return myLibraryName;
  }

  public void setLibraryName(String libraryName) {
    myLibraryName = libraryName;
  }

  @Attribute(MODULE_NAME_ATTRIBUTE)
  public String getModuleName() {
    return myModuleName;
  }

  public void setModuleName(String moduleName) {
    myModuleName = moduleName;
  }

  @Override
  public String toString() {
    return "lib:" + myLibraryName + "(" + (myModuleName != null ? "module " + myModuleName : myLevel) + ")";
  }

  @Nullable
  public Library findLibrary(@Nonnull PackagingElementResolvingContext context) {
    if (myModuleName == null) {
      return context.findLibrary(myLevel, myLibraryName);
    }
    ModulesProvider modulesProvider = context.getModulesProvider();
    Module module = modulesProvider.getModule(myModuleName);
    if (module != null) {
      for (OrderEntry entry : modulesProvider.getRootModel(module).getOrderEntries()) {
        if (entry instanceof LibraryOrderEntry) {
          LibraryOrderEntry libraryEntry = (LibraryOrderEntry)entry;
          if (libraryEntry.isModuleLevel()) {
            String libraryName = libraryEntry.getLibraryName();
            if (libraryName != null && libraryName.equals(myLibraryName)) {
              return libraryEntry.getLibrary();
            }
          }
        }
      }
    }
    return null;
  }

  public static PackagingElementOutputKind getKindForLibrary(Library library) {
    boolean containsDirectories = false;
    boolean containsJars = false;
    for (VirtualFile file : library.getFiles(BinariesOrderRootType.getInstance())) {
      if (file.isInLocalFileSystem()) {
        containsDirectories = true;
      }
      else {
        containsJars = true;
      }
    }
    return new PackagingElementOutputKind(containsDirectories, containsJars);
  }
}
