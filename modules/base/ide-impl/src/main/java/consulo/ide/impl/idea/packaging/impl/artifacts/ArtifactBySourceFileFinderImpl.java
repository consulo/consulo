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
package consulo.ide.impl.idea.packaging.impl.artifacts;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.element.ComplexPackagingElementType;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.component.util.ModificationTracker;
import consulo.ide.impl.idea.packaging.impl.elements.FileOrDirectoryCopyPackagingElement;
import consulo.ide.impl.idea.packaging.impl.elements.ModuleOutputPackagingElement;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;
import consulo.util.collection.MultiValuesMap;
import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class ArtifactBySourceFileFinderImpl extends ArtifactBySourceFileFinder {
  private CachedValue<MultiValuesMap<VirtualFile, Artifact>> myFile2Artifacts;
  private final Project myProject;
  private final ArtifactManager myArtifactManager;

  @Inject
  public ArtifactBySourceFileFinderImpl(Project project, ArtifactManager artifactManager) {
    myProject = project;
    myArtifactManager = artifactManager;
  }

  public CachedValue<MultiValuesMap<VirtualFile, Artifact>> getFileToArtifactsMap() {
    if (myFile2Artifacts == null) {
      myFile2Artifacts =
        CachedValuesManager.getManager(myProject).createCachedValue(new CachedValueProvider<MultiValuesMap<VirtualFile, Artifact>>() {
          public Result<MultiValuesMap<VirtualFile, Artifact>> compute() {
            MultiValuesMap<VirtualFile, Artifact> result = computeFileToArtifactsMap();
            List<ModificationTracker> trackers = new ArrayList<ModificationTracker>();
            trackers.add(myArtifactManager.getModificationTracker());
            for (ComplexPackagingElementType<?> type : PackagingElementFactory.getInstance(myProject).getComplexElementTypes()) {
              ContainerUtil.addIfNotNull(trackers, type.getAllSubstitutionsModificationTracker(myProject));
            }
            return Result.create(result, trackers.toArray(new ModificationTracker[trackers.size()]));
          }
        }, false);
    }
    return myFile2Artifacts;
  }

  private MultiValuesMap<VirtualFile, Artifact> computeFileToArtifactsMap() {
    final MultiValuesMap<VirtualFile, Artifact> result = new MultiValuesMap<VirtualFile, Artifact>();
    for (final Artifact artifact : myArtifactManager.getArtifacts()) {
      final PackagingElementResolvingContext context = myArtifactManager.getResolvingContext();
      ArtifactUtil.processPackagingElements(artifact, null, new PackagingElementProcessor<PackagingElement<?>>() {
        @Override
        public boolean process(@Nonnull PackagingElement<?> element, @Nonnull PackagingElementPath path) {
          if (element instanceof FileOrDirectoryCopyPackagingElement<?>) {
            final VirtualFile root = ((FileOrDirectoryCopyPackagingElement)element).findFile();
            if (root != null) {
              result.put(root, artifact);
            }
          }
          else if (element instanceof ModuleOutputPackagingElement) {
            for (VirtualFile sourceRoot : ((ModuleOutputPackagingElement)element).getSourceRoots(context)) {
              result.put(sourceRoot, artifact);
            }
          }
          return true;
        }
      }, context, true);
    }
    return result;
  }

  @Override
  public Collection<? extends Artifact> findArtifacts(@Nonnull VirtualFile sourceFile) {
    final MultiValuesMap<VirtualFile, Artifact> map = getFileToArtifactsMap().getValue();
    if (map.isEmpty()) {
      return Collections.emptyList();
    }

    List<Artifact> result = null;
    VirtualFile file = sourceFile;
    while (file != null) {
      final Collection<Artifact> artifacts = map.get(file);
      if (artifacts != null) {
        if (result == null) {
          result = new SmartList<Artifact>();
        }
        result.addAll(artifacts);
      }
      file = file.getParent();
    }
    return result != null ? result : Collections.<Artifact>emptyList();
  }
}
