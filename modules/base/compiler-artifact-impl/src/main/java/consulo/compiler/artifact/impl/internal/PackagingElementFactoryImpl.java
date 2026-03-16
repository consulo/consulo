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
package consulo.compiler.artifact.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactPointer;
import consulo.compiler.artifact.ArtifactPointerManager;
import consulo.compiler.artifact.element.*;
import consulo.component.util.pointer.NamedPointer;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModulePointerManager;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class PackagingElementFactoryImpl extends PackagingElementFactory {
    private static final Logger LOG = Logger.getInstance(PackagingElementFactoryImpl.class);

    public static final PackagingElementType<ArtifactRootElement<?>> ARTIFACT_ROOT_ELEMENT_TYPE = ArtifactRootElementType.INSTANCE;

    private final Application myApplication;
    private final ArtifactPointerManager myArtifactPointerManager;
    private final ModulePointerManager myModulePointerManager;

    @Inject
    public PackagingElementFactoryImpl(
        Application application,
        ArtifactPointerManager artifactPointerManager,
        ModulePointerManager modulePointerManager
    ) {
        myApplication = application;
        myArtifactPointerManager = artifactPointerManager;
        myModulePointerManager = modulePointerManager;
    }

    
    @Override
    public PackagingElementType<?>[] getNonCompositeElementTypes() {
        List<PackagingElementType> elementTypes = new ArrayList<>();
        for (PackagingElementType elementType : getAllElementTypes()) {
            if (!(elementType instanceof CompositePackagingElementType)) {
                elementTypes.add(elementType);
            }
        }
        return elementTypes.toArray(new PackagingElementType[elementTypes.size()]);
    }

    @Override
    
    public ComplexPackagingElementType<?>[] getComplexElementTypes() {
        List<ComplexPackagingElementType<?>> types = new ArrayList<>();
        for (PackagingElementType type : getAllElementTypes()) {
            if (type instanceof ComplexPackagingElementType complexPackagingElementType) {
                types.add(complexPackagingElementType);
            }
        }
        return types.toArray(new ComplexPackagingElementType[types.size()]);
    }

    
    @Override
    public CompositePackagingElementType<?>[] getCompositeElementTypes() {
        List<CompositePackagingElementType> elementTypes = new ArrayList<>();
        for (PackagingElementType elementType : getAllElementTypes()) {
            if (elementType instanceof CompositePackagingElementType compositePackagingElementType) {
                elementTypes.add(compositePackagingElementType);
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

    
    @Override
    public PackagingElementType[] getAllElementTypes() {
        return myApplication.getExtensionPoint(PackagingElementType.class).getExtensions();
    }

    
    @Override
    public PackagingElement<?> createArtifactElement(Artifact artifact, Project project) {
        return new ArtifactPackagingElement(myArtifactPointerManager, myArtifactPointerManager.create(artifact));
    }

    @Override
    
    public DirectoryPackagingElement createDirectory(String directoryName) {
        return new DirectoryPackagingElement(directoryName);
    }

    
    @Override
    public ArtifactRootElement<?> createArtifactRootElement() {
        return new ArtifactRootElementImpl();
    }

    @Override
    
    public CompositePackagingElement<?> getOrCreateDirectory(CompositePackagingElement<?> parent, String relativePath) {
        return getOrCreateDirectoryOrArchive(parent, relativePath, true);
    }

    
    @Override
    public CompositePackagingElement<?> getOrCreateArchive(CompositePackagingElement<?> parent, String relativePath) {
        return getOrCreateDirectoryOrArchive(parent, relativePath, false);
    }

    @Override
    public void addFileCopy(
        CompositePackagingElement<?> root,
        String outputDirectoryPath,
        String sourceFilePath
    ) {
        addFileCopy(root, outputDirectoryPath, sourceFilePath, null);
    }

    @Override
    public void addFileCopy(
        CompositePackagingElement<?> root,
        String outputDirectoryPath,
        String sourceFilePath,
        @Nullable String outputFileName
    ) {
        String fileName = PathUtil.getFileName(sourceFilePath);
        if (outputFileName != null && outputFileName.equals(fileName)) {
            outputFileName = null;
        }
        getOrCreateDirectory(root, outputDirectoryPath).addOrFindChild(createFileCopy(sourceFilePath, outputFileName));
    }

    
    private CompositePackagingElement<?> getOrCreateDirectoryOrArchive(
        CompositePackagingElement<?> root,
        String path,
        boolean directory
    ) {
        path = StringUtil.trimStart(StringUtil.trimEnd(path, "/"), "/");
        if (path.length() == 0) {
            return root;
        }
        int index = path.lastIndexOf('/');
        String lastName = path.substring(index + 1);
        String parentPath = index != -1 ? path.substring(0, index) : "";

        CompositePackagingElement<?> parent = getOrCreateDirectoryOrArchive(root, parentPath, true);
        CompositePackagingElement<?> last = directory ? createDirectory(lastName) : createZipArchive(lastName);
        return parent.addOrFindChild(last);
    }

    @Override
    
    @RequiredReadAction
    public PackagingElement<?> createModuleOutput(String moduleName, Project project) {
        NamedPointer<Module> pointer = myModulePointerManager.create(moduleName);
        return ProductionModuleOutputElementType.getInstance().createElement(project, pointer);
    }

    @Override
    
    @RequiredReadAction
    public PackagingElement<?> createModuleOutput(Module module) {
        NamedPointer<Module> modulePointer = myModulePointerManager.create(module);
        return ProductionModuleOutputElementType.getInstance().createElement(module.getProject(), modulePointer);
    }

    @Override
    
    @RequiredReadAction
    public PackagingElement<?> createTestModuleOutput(Module module) {
        NamedPointer<Module> pointer = myModulePointerManager.create(module);
        return TestModuleOutputElementType.getInstance().createElement(module.getProject(), pointer);
    }

    
    @Override
    public List<? extends PackagingElement<?>> createLibraryElements(Library library) {
        LibraryTable table = library.getTable();
        String libraryName = library.getName();
        if (table != null) {
            return Collections.singletonList(createLibraryFiles(libraryName, table.getTableLevel(), null));
        }
        if (libraryName != null) {
            Module module = (Module)((LibraryEx)library).getModule();
            if (module != null) {
                return Collections.singletonList(createLibraryFiles(libraryName, LibraryEx.MODULE_LEVEL, module.getName()));
            }
        }
        List<PackagingElement<?>> elements = new ArrayList<>();
        for (VirtualFile file : library.getFiles(BinariesOrderRootType.getInstance())) {
            String path = FileUtil.toSystemIndependentName(VirtualFilePathUtil.getLocalPath(file));
            elements.add(
                file.isDirectory() && file.isInLocalFileSystem()
                    ? new DirectoryCopyPackagingElement(path)
                    : new FileCopyPackagingElement(path)
            );
        }
        return elements;
    }

    
    @Override
    public PackagingElement<?> createArtifactElement(ArtifactPointer artifactPointer, Project project) {
        return new ArtifactPackagingElement(myArtifactPointerManager, artifactPointer);
    }

    
    @Override
    public PackagingElement<?> createLibraryFiles(String libraryName, String level, String moduleName) {
        return new LibraryPackagingElement(level, libraryName, moduleName);
    }

    @Override
    
    public CompositePackagingElement<?> createZipArchive(String archiveFileName) {
        return new ZipArchivePackagingElement(archiveFileName);
    }

    
    @Override
    public PackagingElement<?> createDirectoryCopyWithParentDirectories(String filePath, String relativeOutputPath) {
        return createParentDirectories(relativeOutputPath, new DirectoryCopyPackagingElement(filePath));
    }

    @Override
    
    public PackagingElement<?> createExtractedDirectoryWithParentDirectories(
        String jarPath,
        String pathInJar,
        String relativeOutputPath
    ) {
        return createParentDirectories(relativeOutputPath, new ExtractedDirectoryPackagingElement(jarPath, pathInJar));
    }

    
    @Override
    public PackagingElement<?> createExtractedDirectory(VirtualFile jarEntry) {
        LOG.assertTrue(
            jarEntry.getFileSystem() instanceof ArchiveFileSystem,
            "Expected file from jar but file from " + jarEntry.getFileSystem() + " found"
        );
        String fullPath = jarEntry.getPath();
        int jarEnd = fullPath.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        return new ExtractedDirectoryPackagingElement(fullPath.substring(0, jarEnd), fullPath.substring(jarEnd + 1));
    }

    
    @Override
    public PackagingElement<?> createFileCopyWithParentDirectories(String filePath, String relativeOutputPath) {
        return createFileCopyWithParentDirectories(filePath, relativeOutputPath, null);
    }

    
    @Override
    public PackagingElement<?> createFileCopyWithParentDirectories(
        String filePath,
        String relativeOutputPath,
        @Nullable String outputFileName
    ) {
        return createParentDirectories(relativeOutputPath, createFileCopy(filePath, outputFileName));
    }

    @Override
    public PackagingElement<?> createFileCopy(String filePath, String outputFileName) {
        return new FileCopyPackagingElement(filePath, outputFileName);
    }

    
    @Override
    public PackagingElement<?> createParentDirectories(String relativeOutputPath, PackagingElement<?> element) {
        return createParentDirectories(relativeOutputPath, Collections.singletonList(element)).get(0);
    }

    
    @Override
    public List<? extends PackagingElement<?>> createParentDirectories(
        String relativeOutputPath,
        List<? extends PackagingElement<?>> elements
    ) {
        relativeOutputPath = StringUtil.trimStart(relativeOutputPath, "/");
        if (relativeOutputPath.length() == 0) {
            return elements;
        }
        int slash = relativeOutputPath.indexOf('/');
        if (slash == -1) {
            slash = relativeOutputPath.length();
        }
        String rootName = relativeOutputPath.substring(0, slash);
        String pathTail = relativeOutputPath.substring(slash);
        DirectoryPackagingElement root = createDirectory(rootName);
        CompositePackagingElement<?> last = getOrCreateDirectory(root, pathTail);
        last.addOrFindChildren(elements);
        return Collections.singletonList(root);
    }
}
