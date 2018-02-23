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
package com.intellij.packaging.impl.elements;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import consulo.packaging.artifacts.ArtifactPointerUtil;
import com.intellij.packaging.elements.ComplexPackagingElementType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.util.Processor;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.*;

/**
* @author nik
*/
public class ArtifactElementType extends ComplexPackagingElementType<ArtifactPackagingElement> {
  @Nonnull
  public static ArtifactElementType getInstance() {
    return getInstance(ArtifactElementType.class);
  }

  public ArtifactElementType() {
    super("artifact", CompilerBundle.message("element.type.name.artifact"));
  }

  @Nonnull
  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public boolean isAvailableForAdd(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact) {
    return !getAvailableArtifacts(context, artifact, false).isEmpty();
  }

  @Override
  @Nonnull
  public List<? extends ArtifactPackagingElement> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact,
                                                                  @Nonnull CompositePackagingElement<?> parent) {
    final Project project = context.getProject();
    List<Artifact> artifacts = context.chooseArtifacts(getAvailableArtifacts(context, artifact, false), CompilerBundle.message("dialog.title.choose.artifacts"));
    final List<ArtifactPackagingElement> elements = new ArrayList<ArtifactPackagingElement>();
    for (Artifact selected : artifacts) {
      elements.add(new ArtifactPackagingElement(project, ArtifactPointerUtil.getPointerManager(project).create(selected, context.getArtifactModel())));
    }
    return elements;
  }

  @Nonnull
  public static List<? extends Artifact> getAvailableArtifacts(@Nonnull final ArtifactEditorContext context,
                                                               @Nonnull final Artifact artifact,
                                                               final boolean notIncludedOnly) {
    final Set<Artifact> result = new HashSet<Artifact>(Arrays.asList(context.getArtifactModel().getArtifacts()));
    if (notIncludedOnly) {
      ArtifactUtil.processPackagingElements(artifact, getInstance(), new Processor<ArtifactPackagingElement>() {
        @Override
        public boolean process(ArtifactPackagingElement artifactPackagingElement) {
          result.remove(artifactPackagingElement.findArtifact(context));
          return true;
        }
      }, context, true);
    }
    result.remove(artifact);
    final Iterator<Artifact> iterator = result.iterator();
    while (iterator.hasNext()) {
      Artifact another = iterator.next();
      final boolean notContainThis =
          ArtifactUtil.processPackagingElements(another, getInstance(), new Processor<ArtifactPackagingElement>() {
            @Override
            public boolean process(ArtifactPackagingElement element) {
              return !artifact.getName().equals(element.getArtifactName());
            }
          }, context, true);
      if (!notContainThis) {
        iterator.remove();
      }
    }
    final ArrayList<Artifact> list = new ArrayList<Artifact>(result);
    Collections.sort(list, ArtifactManager.ARTIFACT_COMPARATOR);
    return list;
  }

  @Override
  @Nonnull
  public ArtifactPackagingElement createEmpty(@Nonnull Project project) {
    return new ArtifactPackagingElement(project);
  }

  @Override
  public String getShowContentActionText() {
    return "Show Content of Included Artifacts";
  }
}
