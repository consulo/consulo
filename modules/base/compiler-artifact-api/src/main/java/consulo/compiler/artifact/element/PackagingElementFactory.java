/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactPointer;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class PackagingElementFactory {
  public static PackagingElementFactory getInstance(Project project) {
    return project.getInstance(PackagingElementFactory.class);
  }

  
  public abstract ArtifactRootElement<?> createArtifactRootElement();

  
  public abstract CompositePackagingElement<?> createDirectory(String directoryName);

  
  public abstract CompositePackagingElement<?> createZipArchive(String archiveFileName);

  public abstract PackagingElement<?> createFileCopy(String filePath, @Nullable String outputFileName);

  
  @Deprecated
  public abstract PackagingElement<?> createModuleOutput(String moduleName, Project project);

  
  @Deprecated
  public abstract PackagingElement<?> createModuleOutput(Module module);

  
  @Deprecated
  public abstract PackagingElement<?> createTestModuleOutput(Module module);

  
  public abstract List<? extends PackagingElement<?>> createLibraryElements(Library library);

  
  public abstract PackagingElement<?> createArtifactElement(ArtifactPointer artifactPointer, Project project);

  
  public abstract PackagingElement<?> createArtifactElement(Artifact artifact, Project project);

  
  public abstract PackagingElement<?> createLibraryFiles(String libraryName, String level, String moduleName);


  
  public abstract PackagingElement<?> createDirectoryCopyWithParentDirectories(String filePath, String relativeOutputPath);

  
  public abstract PackagingElement<?> createExtractedDirectoryWithParentDirectories(String jarPath, String pathInJar,
                                                                                    String relativeOutputPath);

  
  public abstract PackagingElement<?> createExtractedDirectory(VirtualFile jarEntry);

  
  public abstract PackagingElement<?> createFileCopyWithParentDirectories(String filePath, String relativeOutputPath,
                                                                          @Nullable String outputFileName);
  
  
  public abstract PackagingElement<?> createFileCopyWithParentDirectories(String filePath, String relativeOutputPath);
  

  
  public abstract CompositePackagingElement<?> getOrCreateDirectory(CompositePackagingElement<?> parent, String relativePath);

  
  public abstract CompositePackagingElement<?> getOrCreateArchive(CompositePackagingElement<?> parent, String relativePath);

  public abstract void addFileCopy(CompositePackagingElement<?> root, String outputDirectoryPath, String sourceFilePath,
                                   String outputFileName);

  public abstract void addFileCopy(CompositePackagingElement<?> root, String outputDirectoryPath, String sourceFilePath);

  
  public abstract PackagingElement<?> createParentDirectories(String relativeOutputPath, PackagingElement<?> element);


  
  public abstract List<? extends PackagingElement<?>> createParentDirectories(String relativeOutputPath, List<? extends PackagingElement<?>> elements);

  
  
  public abstract CompositePackagingElementType<?>[] getCompositeElementTypes();

  @Nullable
  public abstract PackagingElementType<?> findElementType(String id);

  
  public abstract PackagingElementType<?>[] getNonCompositeElementTypes();

  
  public abstract PackagingElementType[] getAllElementTypes();

  
  public abstract ComplexPackagingElementType<?>[] getComplexElementTypes();
}
