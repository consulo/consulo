/*
 * Copyright 2013 Consulo.org
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

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.compiler.ant.Tag;
import com.intellij.compiler.ant.artifacts.ArchiveAntCopyInstructionCreator;
import com.intellij.compiler.ant.taskdefs.Jar;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 16:05/18.06.13
 */
public class JarArchivePackagingElement extends ArchivePackagingElement {
  public JarArchivePackagingElement(@NotNull JarArchiveElementType type) {
    super(type);
  }

  public JarArchivePackagingElement(@NotNull JarArchiveElementType type, @NotNull String archiveFileName) {
    super(type, archiveFileName);
  }

  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull PackagingElementResolvingContext resolvingContext,
                                                          @NotNull AntCopyInstructionCreator creator,
                                                          @NotNull ArtifactAntGenerationContext generationContext,
                                                          @NotNull ArtifactType artifactType) {
    final String tempJarProperty = generationContext.createNewTempFileProperty("temp.jar.path." + myArchiveFileName, myArchiveFileName);
    String jarPath = BuildProperties.propertyRef(tempJarProperty);
    final Tag jar = new Jar(jarPath, "preserve", true);

    for (Generator generator : computeChildrenGenerators(resolvingContext, new ArchiveAntCopyInstructionCreator(""), generationContext,
                                                         artifactType)) {
      jar.add(generator);
    }
    generationContext.runBeforeCurrentArtifact(jar);
    return Collections.singletonList(creator.createFileCopyInstruction(jarPath, myArchiveFileName));
  }
}
