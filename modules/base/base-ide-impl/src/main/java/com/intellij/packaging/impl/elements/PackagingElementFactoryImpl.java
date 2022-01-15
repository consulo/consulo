/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.packaging.impl.elements;

import com.intellij.icons.AllIcons;
import consulo.logging.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.LibraryImpl;
import com.intellij.openapi.roots.impl.libraries.LibraryTableImplUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.elements.*;
import com.intellij.packaging.impl.elements.moduleContent.ProductionModuleOutputElementType;
import com.intellij.packaging.impl.elements.moduleContent.TestModuleOutputElementType;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.util.PathUtil;
import com.intellij.util.io.URLUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.packaging.impl.elements.ZipArchivePackagingElement;
import consulo.roots.types.BinariesOrderRootType;
import consulo.ui.image.Image;
import consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
@Singleton
public class PackagingElementFactoryImpl extends PackagingElementFactory {
  private static final Logger LOG = Logger.getInstance(PackagingElementFactoryImpl.class);

  public static final PackagingElementType<ArtifactRootElement<?>> ARTIFACT_ROOT_ELEMENT_TYPE = new ArtifactRootElementType();

  private final ArtifactPointerManager myArtifactPointerManager;
  private final ModulePointerManager myModulePointerManager;

  @Inject
  public PackagingElementFactoryImpl(ArtifactPointerManager artifactPointerManager, ModulePointerManager modulePointerManager) {
    myArtifactPointerManager = artifactPointerManager;
    myModulePointerManager = modulePointerManager;
  }

  @Nonnull
  @Override
  public PackagingElementType<?>[] getNonCompositeElementTypes() {
    final List<PackagingElementType> elementTypes = new ArrayList<>();
    for (PackagingElementType elementType : getAllElementTypes()) {
      if (!(elementType instanceof CompositePackagingElementType)) {
        elementTypes.add(elementType);
      }
    }
    return elementTypes.toArray(new PackagingElementType[elementTypes.size()]);
  }

  @Override
  @Nonnull
  public ComplexPackagingElementType<?>[] getComplexElementTypes() {
    List<ComplexPackagingElementType<?>> types = new ArrayList<>();
    for (PackagingElementType type : getAllElementTypes()) {
      if (type instanceof ComplexPackagingElementType) {
        types.add((ComplexPackagingElementType)type);
      }
    }
    return types.toArray(new ComplexPackagingElementType[types.size()]);
  }

  @Nonnull
  @Override
  public CompositePackagingElementType<?>[] getCompositeElementTypes() {
    final List<CompositePackagingElementType> elementTypes = new ArrayList<>();
    for (PackagingElementType elementType : getAllElementTypes()) {
      if (elementType instanceof CompositePackagingElementType) {
        elementTypes.add((CompositePackagingElementType)elementType);
      }
    }
    return elementTypes.toArray(new CompositePackagingElementType[elementTypes.size()]);
  }

  @Override
  public PackagingElementType<?> findElementType(String id) {
    for (PackagingElementType elementType : getAllElementTypes()) {
      if (elementType.getId().equals(id)) {
        return elementType;
      }
    }
    if (id.equals(ARTIFACT_ROOT_ELEMENT_TYPE.getId())) {
      return ARTIFACT_ROOT_ELEMENT_TYPE;
    }
    return null;
  }

  @Nonnull
  @Override
  public PackagingElementType[] getAllElementTypes() {
    return PackagingElementType.EP_NAME.getExtensions();
  }

  @Nonnull
  @Override
  public PackagingElement<?> createArtifactElement(@Nonnull Artifact artifact, @Nonnull Project project) {
    return new ArtifactPackagingElement(myArtifactPointerManager, myArtifactPointerManager.create(artifact));
  }

  @Override
  @Nonnull
  public DirectoryPackagingElement createDirectory(@Nonnull @NonNls String directoryName) {
    return new DirectoryPackagingElement(directoryName);
  }

  @Nonnull
  @Override
  public ArtifactRootElement<?> createArtifactRootElement() {
    return new ArtifactRootElementImpl();
  }

  @Override
  @Nonnull
  public CompositePackagingElement<?> getOrCreateDirectory(@Nonnull CompositePackagingElement<?> parent, @Nonnull String relativePath) {
    return getOrCreateDirectoryOrArchive(parent, relativePath, true);
  }

  @Nonnull
  @Override
  public CompositePackagingElement<?> getOrCreateArchive(@Nonnull CompositePackagingElement<?> parent, @Nonnull String relativePath) {
    return getOrCreateDirectoryOrArchive(parent, relativePath, false);
  }

  @Override
  public void addFileCopy(@Nonnull CompositePackagingElement<?> root, @Nonnull String outputDirectoryPath, @Nonnull String sourceFilePath) {
    addFileCopy(root, outputDirectoryPath, sourceFilePath, null);
  }

  @Override
  public void addFileCopy(@Nonnull CompositePackagingElement<?> root, @Nonnull String outputDirectoryPath, @Nonnull String sourceFilePath, @Nullable String outputFileName) {
    final String fileName = PathUtil.getFileName(sourceFilePath);
    if (outputFileName != null && outputFileName.equals(fileName)) {
      outputFileName = null;
    }
    getOrCreateDirectory(root, outputDirectoryPath).addOrFindChild(createFileCopy(sourceFilePath, outputFileName));
  }

  @Nonnull
  private CompositePackagingElement<?> getOrCreateDirectoryOrArchive(@Nonnull CompositePackagingElement<?> root, @Nonnull @NonNls String path, final boolean directory) {
    path = StringUtil.trimStart(StringUtil.trimEnd(path, "/"), "/");
    if (path.length() == 0) {
      return root;
    }
    int index = path.lastIndexOf('/');
    String lastName = path.substring(index + 1);
    String parentPath = index != -1 ? path.substring(0, index) : "";

    final CompositePackagingElement<?> parent = getOrCreateDirectoryOrArchive(root, parentPath, true);
    final CompositePackagingElement<?> last = directory ? createDirectory(lastName) : createZipArchive(lastName);
    return parent.addOrFindChild(last);
  }

  @Override
  @Nonnull
  @RequiredReadAction
  public PackagingElement<?> createModuleOutput(@Nonnull String moduleName, @Nonnull Project project) {
    final NamedPointer<Module> pointer = myModulePointerManager.create(moduleName);
    return ProductionModuleOutputElementType.getInstance().createElement(project, pointer);
  }

  @Override
  @Nonnull
  @RequiredReadAction
  public PackagingElement<?> createModuleOutput(@Nonnull Module module) {
    final NamedPointer<Module> modulePointer = myModulePointerManager.create(module);
    return ProductionModuleOutputElementType.getInstance().createElement(module.getProject(), modulePointer);
  }

  @Override
  @Nonnull
  @RequiredReadAction
  public PackagingElement<?> createTestModuleOutput(@Nonnull Module module) {
    NamedPointer<Module> pointer = myModulePointerManager.create(module);
    return TestModuleOutputElementType.getInstance().createElement(module.getProject(), pointer);
  }

  @Nonnull
  @Override
  public List<? extends PackagingElement<?>> createLibraryElements(@Nonnull Library library) {
    final LibraryTable table = library.getTable();
    final String libraryName = library.getName();
    if (table != null) {
      return Collections.singletonList(createLibraryFiles(libraryName, table.getTableLevel(), null));
    }
    if (libraryName != null) {
      final Module module = ((LibraryImpl)library).getModule();
      if (module != null) {
        return Collections.singletonList(createLibraryFiles(libraryName, LibraryTableImplUtil.MODULE_LEVEL, module.getName()));
      }
    }
    final List<PackagingElement<?>> elements = new ArrayList<>();
    for (VirtualFile file : library.getFiles(BinariesOrderRootType.getInstance())) {
      final String path = FileUtil.toSystemIndependentName(PathUtil.getLocalPath(file));
      elements.add(file.isDirectory() && file.isInLocalFileSystem() ? new DirectoryCopyPackagingElement(path) : new FileCopyPackagingElement(path));
    }
    return elements;
  }

  @Nonnull
  @Override
  public PackagingElement<?> createArtifactElement(@Nonnull ArtifactPointer artifactPointer, @Nonnull Project project) {
    return new ArtifactPackagingElement(myArtifactPointerManager, artifactPointer);
  }

  @Nonnull
  @Override
  public PackagingElement<?> createLibraryFiles(@Nonnull String libraryName, @Nonnull String level, String moduleName) {
    return new LibraryPackagingElement(level, libraryName, moduleName);
  }

  @Override
  @Nonnull
  public CompositePackagingElement<?> createZipArchive(@Nonnull @NonNls String archiveFileName) {
    return new ZipArchivePackagingElement(archiveFileName);
  }

  @Nullable
  private static PackagingElement<?> findArchiveOrDirectoryByName(@Nonnull CompositePackagingElement<?> parent, @Nonnull String name) {
    for (PackagingElement<?> element : parent.getChildren()) {
      if (element instanceof ArchivePackagingElement && ((ArchivePackagingElement)element).getArchiveFileName().equals(name) ||
          element instanceof DirectoryPackagingElement && ((DirectoryPackagingElement)element).getDirectoryName().equals(name)) {
        return element;
      }
    }
    return null;
  }

  @Nonnull
  public static String suggestFileName(@Nonnull CompositePackagingElement<?> parent, @NonNls @Nonnull String prefix, @NonNls @Nonnull String suffix) {
    String name = prefix + suffix;
    int i = 2;
    while (findArchiveOrDirectoryByName(parent, name) != null) {
      name = prefix + i++ + suffix;
    }
    return name;
  }

  @Nonnull
  @Override
  public PackagingElement<?> createDirectoryCopyWithParentDirectories(@Nonnull String filePath, @Nonnull String relativeOutputPath) {
    return createParentDirectories(relativeOutputPath, new DirectoryCopyPackagingElement(filePath));
  }

  @Override
  @Nonnull
  public PackagingElement<?> createExtractedDirectoryWithParentDirectories(@Nonnull String jarPath, @Nonnull String pathInJar, @Nonnull String relativeOutputPath) {
    return createParentDirectories(relativeOutputPath, new ExtractedDirectoryPackagingElement(jarPath, pathInJar));
  }

  @Nonnull
  @Override
  public PackagingElement<?> createExtractedDirectory(@Nonnull VirtualFile jarEntry) {
    LOG.assertTrue(jarEntry.getFileSystem() instanceof ArchiveFileSystem, "Expected file from jar but file from " + jarEntry.getFileSystem() + " found");
    final String fullPath = jarEntry.getPath();
    final int jarEnd = fullPath.indexOf(URLUtil.ARCHIVE_SEPARATOR);
    return new ExtractedDirectoryPackagingElement(fullPath.substring(0, jarEnd), fullPath.substring(jarEnd + 1));
  }

  @Nonnull
  @Override
  public PackagingElement<?> createFileCopyWithParentDirectories(@Nonnull String filePath, @Nonnull String relativeOutputPath) {
    return createFileCopyWithParentDirectories(filePath, relativeOutputPath, null);
  }

  @Nonnull
  @Override
  public PackagingElement<?> createFileCopyWithParentDirectories(@Nonnull String filePath, @Nonnull String relativeOutputPath, @Nullable String outputFileName) {
    return createParentDirectories(relativeOutputPath, createFileCopy(filePath, outputFileName));
  }

  @Override
  public PackagingElement<?> createFileCopy(@Nonnull String filePath, String outputFileName) {
    return new FileCopyPackagingElement(filePath, outputFileName);
  }

  @Nonnull
  @Override
  public PackagingElement<?> createParentDirectories(@Nonnull String relativeOutputPath, @Nonnull PackagingElement<?> element) {
    return createParentDirectories(relativeOutputPath, Collections.singletonList(element)).get(0);
  }

  @Nonnull
  @Override
  public List<? extends PackagingElement<?>> createParentDirectories(@Nonnull String relativeOutputPath, @Nonnull List<? extends PackagingElement<?>> elements) {
    relativeOutputPath = StringUtil.trimStart(relativeOutputPath, "/");
    if (relativeOutputPath.length() == 0) {
      return elements;
    }
    int slash = relativeOutputPath.indexOf('/');
    if (slash == -1) slash = relativeOutputPath.length();
    String rootName = relativeOutputPath.substring(0, slash);
    String pathTail = relativeOutputPath.substring(slash);
    final DirectoryPackagingElement root = createDirectory(rootName);
    final CompositePackagingElement<?> last = getOrCreateDirectory(root, pathTail);
    last.addOrFindChildren(elements);
    return Collections.singletonList(root);
  }

  private static class ArtifactRootElementType extends PackagingElementType<ArtifactRootElement<?>> {
    protected ArtifactRootElementType() {
      super("root", "");
    }

    @Nonnull
    @Override
    public Image getIcon() {
      return AllIcons.Nodes.Artifact;
    }

    @Override
    public boolean isAvailableForAdd(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact) {
      return false;
    }

    @Override
    @Nonnull
    public List<? extends ArtifactRootElement<?>> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact, @Nonnull CompositePackagingElement<?> parent) {
      throw new UnsupportedOperationException("'create' not implemented in " + getClass().getName());
    }

    @Override
    @Nonnull
    public ArtifactRootElement<?> createEmpty(@Nonnull Project project) {
      return new ArtifactRootElementImpl();
    }
  }
}
