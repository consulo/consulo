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
package com.intellij.packaging.elements;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author nik
 */
public abstract class PackagingElementFactory {
  public static PackagingElementFactory getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, PackagingElementFactory.class);
  }

  @Nonnull
  public abstract ArtifactRootElement<?> createArtifactRootElement();

  @Nonnull
  public abstract CompositePackagingElement<?> createDirectory(@Nonnull @NonNls String directoryName);

  @Nonnull
  public abstract CompositePackagingElement<?> createZipArchive(@Nonnull @NonNls String archiveFileName);

  public abstract PackagingElement<?> createFileCopy(@Nonnull String filePath, @javax.annotation.Nullable String outputFileName);

  @Nonnull
  @Deprecated
  public abstract PackagingElement<?> createModuleOutput(@Nonnull String moduleName, @Nonnull Project project);

  @Nonnull
  @Deprecated
  public abstract PackagingElement<?> createModuleOutput(@Nonnull Module module);

  @Nonnull
  @Deprecated
  public abstract PackagingElement<?> createTestModuleOutput(@Nonnull Module module);

  @Nonnull
  public abstract List<? extends PackagingElement<?>> createLibraryElements(@Nonnull Library library);

  @Nonnull
  public abstract PackagingElement<?> createArtifactElement(@Nonnull ArtifactPointer artifactPointer, @Nonnull Project project);

  @Nonnull
  public abstract PackagingElement<?> createArtifactElement(@Nonnull Artifact artifact, @Nonnull Project project);

  @Nonnull
  public abstract PackagingElement<?> createLibraryFiles(@Nonnull String libraryName, @Nonnull String level, String moduleName);


  @Nonnull
  public abstract PackagingElement<?> createDirectoryCopyWithParentDirectories(@Nonnull String filePath, @Nonnull String relativeOutputPath);

  @Nonnull
  public abstract PackagingElement<?> createExtractedDirectoryWithParentDirectories(@Nonnull String jarPath, @Nonnull String pathInJar,
                                                                                    @Nonnull String relativeOutputPath);

  @Nonnull
  public abstract PackagingElement<?> createExtractedDirectory(@Nonnull VirtualFile jarEntry);

  @Nonnull
  public abstract PackagingElement<?> createFileCopyWithParentDirectories(@Nonnull String filePath, @Nonnull String relativeOutputPath,
                                                                          @Nullable String outputFileName);
  
  @Nonnull
  public abstract PackagingElement<?> createFileCopyWithParentDirectories(@Nonnull String filePath, @Nonnull String relativeOutputPath);
  

  @Nonnull
  public abstract CompositePackagingElement<?> getOrCreateDirectory(@Nonnull CompositePackagingElement<?> parent, @Nonnull String relativePath);

  @Nonnull
  public abstract CompositePackagingElement<?> getOrCreateArchive(@Nonnull CompositePackagingElement<?> parent, @Nonnull String relativePath);

  public abstract void addFileCopy(@Nonnull CompositePackagingElement<?> root, @Nonnull String outputDirectoryPath, @Nonnull String sourceFilePath,
                                   final String outputFileName);

  public abstract void addFileCopy(@Nonnull CompositePackagingElement<?> root, @Nonnull String outputDirectoryPath, @Nonnull String sourceFilePath);

  @Nonnull
  public abstract PackagingElement<?> createParentDirectories(@Nonnull String relativeOutputPath, @Nonnull PackagingElement<?> element);


  @Nonnull
  public abstract List<? extends PackagingElement<?>> createParentDirectories(@Nonnull String relativeOutputPath, @Nonnull List<? extends PackagingElement<?>> elements);

  @Nonnull
  
  public abstract CompositePackagingElementType<?>[] getCompositeElementTypes();

  @javax.annotation.Nullable
  public abstract PackagingElementType<?> findElementType(String id);

  @Nonnull
  public abstract PackagingElementType<?>[] getNonCompositeElementTypes();

  @Nonnull
  public abstract PackagingElementType[] getAllElementTypes();

  @Nonnull
  public abstract ComplexPackagingElementType<?>[] getComplexElementTypes();
}
