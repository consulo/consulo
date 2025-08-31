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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.module.Module;
import consulo.project.Project;
import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.*;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.PackagingElementPath;
import consulo.compiler.artifact.PackagingElementProcessor;
import consulo.compiler.artifact.element.ArtifactPackagingElement;
import consulo.compiler.artifact.element.LibraryPackagingElement;
import consulo.compiler.artifact.element.ModuleOutputPackagingElement;
import consulo.ide.setting.module.ModulesConfigurator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactProjectStructureElement extends ProjectStructureElement {
  private final ArtifactsStructureConfigurableContext myArtifactsStructureContext;
  private final Artifact myOriginalArtifact;

  ArtifactProjectStructureElement(ArtifactsStructureConfigurableContext artifactsStructureContext, Artifact artifact) {
    myArtifactsStructureContext = artifactsStructureContext;
    myOriginalArtifact = artifactsStructureContext.getOriginalArtifact(artifact);
  }

  @Override
  public void check(Project project, ProjectStructureProblemsHolder problemsHolder) {
    Artifact artifact = myArtifactsStructureContext.getArtifactModel().getArtifactByOriginal(myOriginalArtifact);
    ArtifactProblemsHolderImpl artifactProblemsHolder = new ArtifactProblemsHolderImpl(myArtifactsStructureContext, myOriginalArtifact, problemsHolder);
    artifact.getArtifactType().checkRootElement(myArtifactsStructureContext.getRootElement(myOriginalArtifact), artifact, artifactProblemsHolder);
  }

  public Artifact getOriginalArtifact() {
    return myOriginalArtifact;
  }

  @Override
  public List<ProjectStructureElementUsage> getUsagesInElement() {
    Artifact artifact = myArtifactsStructureContext.getArtifactModel().getArtifactByOriginal(myOriginalArtifact);
    final List<ProjectStructureElementUsage> usages = new ArrayList<ProjectStructureElementUsage>();
    final CompositePackagingElement<?> rootElement = myArtifactsStructureContext.getRootElement(artifact);
    ArtifactUtil.processPackagingElements(rootElement, null, new PackagingElementProcessor<>() {
      @Override
      public boolean process(@Nonnull PackagingElement<?> packagingElement, @Nonnull PackagingElementPath path) {
        ProjectStructureElement element = getProjectStructureElementFor(packagingElement, ArtifactProjectStructureElement.this.myArtifactsStructureContext);
        if (element != null) {
          usages.add(createUsage(packagingElement, element, path.getPathStringFrom("/", rootElement)));
        }
        return true;
      }
    }, myArtifactsStructureContext, false, artifact.getArtifactType());
    return usages;
  }

  @Nullable
  public static ProjectStructureElement getProjectStructureElementFor(PackagingElement<?> packagingElement, ArtifactsStructureConfigurableContext artifactsStructureContext) {
    if (packagingElement instanceof ModuleOutputPackagingElement) {
      Module module = ((ModuleOutputPackagingElement)packagingElement).findModule(artifactsStructureContext);
      if (module != null) {
        return new ModuleProjectStructureElement((ModulesConfigurator)artifactsStructureContext.getModulesProvider(), module);
      }
    }
    else if (packagingElement instanceof LibraryPackagingElement) {
      Library library = ((LibraryPackagingElement)packagingElement).findLibrary(artifactsStructureContext);
      if (library != null) {
        return new LibraryProjectStructureElement(library);
      }
    }
    else if (packagingElement instanceof ArtifactPackagingElement) {
      Artifact usedArtifact = ((ArtifactPackagingElement)packagingElement).findArtifact(artifactsStructureContext);
      if (usedArtifact != null) {
        return artifactsStructureContext.getOrCreateArtifactElement(usedArtifact);
      }
    }
    return null;
  }

  private UsageInArtifact createUsage(PackagingElement<?> packagingElement, ProjectStructureElement element, String parentPath) {
    return new UsageInArtifact(myOriginalArtifact, myArtifactsStructureContext, element, this, parentPath, packagingElement);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArtifactProjectStructureElement)) return false;

    return myOriginalArtifact.equals(((ArtifactProjectStructureElement)o).myOriginalArtifact);

  }

  @Override
  public int hashCode() {
    return myOriginalArtifact.hashCode();
  }

  @Override
  public String getPresentableName() {
    return "Artifact '" + getActualArtifactName() + "'";
  }

  @Override
  public String getTypeName() {
    return "Artifact";
  }

  @Override
  public String getId() {
    return "artifact:" + getActualArtifactName();
  }

  private String getActualArtifactName() {
    return myArtifactsStructureContext.getArtifactModel().getArtifactByOriginal(myOriginalArtifact).getName();
  }
}
