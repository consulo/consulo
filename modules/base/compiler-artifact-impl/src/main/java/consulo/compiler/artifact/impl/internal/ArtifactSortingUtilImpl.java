/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.compiler.artifact.*;
import consulo.compiler.artifact.element.ArtifactElementType;
import consulo.compiler.artifact.element.ArtifactPackagingElement;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.compiler.artifact.internal.ArtifactSortingUtil;
import consulo.component.util.graph.CachingSemiGraph;
import consulo.component.util.graph.DFSTBuilder;
import consulo.component.util.graph.GraphGenerator;
import consulo.project.Project;
import consulo.util.collection.primitive.ints.IntList;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class ArtifactSortingUtilImpl extends ArtifactSortingUtil {
  private final Project myProject;
  private CachedValue<Map<String, String>> myArtifactToSelfIncludingName;
  private CachedValue<List<String>> mySortedArtifacts;

  @Inject
  public ArtifactSortingUtilImpl(Project project) {
    myProject = project;
  }

  @Override
  public Map<String, String> getArtifactToSelfIncludingNameMap() {
    if (myArtifactToSelfIncludingName == null) {
      myArtifactToSelfIncludingName =
        CachedValuesManager.getManager(myProject)
                           .createCachedValue(() -> CachedValueProvider.Result.create(computeArtifactToSelfIncludingNameMap(),
                                                                                      ArtifactManager.getInstance(myProject)
                                                                                                     .getModificationTracker()), false);
    }
    return myArtifactToSelfIncludingName.getValue();
  }

  @Override
  public List<String> getArtifactsSortedByInclusion() {
    if (mySortedArtifacts == null) {
      mySortedArtifacts = CachedValuesManager.getManager(myProject)
                                             .createCachedValue(() -> CachedValueProvider.Result.create(doGetSortedArtifacts(),
                                                                                                        ArtifactManager.getInstance(
                                                                                                          myProject)
                                                                                                                       .getModificationTracker()),
                                                                false);
    }
    return mySortedArtifacts.getValue();
  }

  private List<String> doGetSortedArtifacts() {
    GraphGenerator<String> graph = createArtifactsGraph();
    DFSTBuilder<String> builder = new DFSTBuilder<>(graph);
    List<String> names = new ArrayList<>();
    names.addAll(graph.getNodes());
    Collections.sort(names, builder.comparator());
    return names;
  }

  private Map<String, String> computeArtifactToSelfIncludingNameMap() {
    final Map<String, String> result = new HashMap<>();
    GraphGenerator<String> graph = createArtifactsGraph();
    for (String artifactName : graph.getNodes()) {
      Iterator<String> in = graph.getIn(artifactName);
      while (in.hasNext()) {
        String next = in.next();
        if (next.equals(artifactName)) {
          result.put(artifactName, artifactName);
          break;
        }
      }
    }

    final DFSTBuilder<String> builder = new DFSTBuilder<>(graph);
    if (builder.isAcyclic() && result.isEmpty()) return Collections.emptyMap();

    IntList sccs = builder.getSCCs();
    sccs.forEach(new IntConsumer() {
      int myTNumber = 0;

      @Override
      public void accept(int size) {
        if (size > 1) {
          for (int j = 0; j < size; j++) {
            String artifactName = builder.getNodeByTNumber(myTNumber + j);
            result.put(artifactName, artifactName);
          }
        }
        myTNumber += size;
      }
    });

    for (int i = 0; i < graph.getNodes().size(); i++) {
      String artifactName = builder.getNodeByTNumber(i);
      if (!result.containsKey(artifactName)) {
        Iterator<String> in = graph.getIn(artifactName);
        while (in.hasNext()) {
          String name = result.get(in.next());
          if (name != null) {
            result.put(artifactName, name);
          }
        }
      }
    }

    return result;
  }

  private GraphGenerator<String> createArtifactsGraph() {
    ArtifactManager artifactManager = ArtifactManager.getInstance(myProject);
    return GraphGenerator.create(CachingSemiGraph.create(new ArtifactsGraph(artifactManager)));
  }

  private class ArtifactsGraph implements GraphGenerator.SemiGraph<String> {
    private final ArtifactManager myArtifactManager;
    private final Set<String> myArtifactNames;

    public ArtifactsGraph(ArtifactManager artifactManager) {
      myArtifactManager = artifactManager;
      myArtifactNames = new LinkedHashSet<>();
      for (Artifact artifact : myArtifactManager.getSortedArtifacts()) {
        myArtifactNames.add(artifact.getName());
      }
    }

    @Override
    public Collection<String> getNodes() {
      return myArtifactNames;
    }

    @Override
    public Iterator<String> getIn(String name) {
      final Set<String> included = new LinkedHashSet<>();
      PackagingElementResolvingContext context = myArtifactManager.getResolvingContext();
      Artifact artifact = context.getArtifactModel().findArtifact(name);
      if (artifact != null) {
        ArtifactUtil.processPackagingElements(artifact,
                                              ArtifactElementType.getInstance(),
                                              new PackagingElementProcessor<>() {
                                                @Override
                                                public boolean process(@Nonnull ArtifactPackagingElement element,
                                                                       @Nonnull PackagingElementPath path) {
                                                  String artifactName = element.getArtifactName();
                                                  if (myArtifactNames.contains(artifactName)) {
                                                    included.add(artifactName);
                                                  }
                                                  return true;
                                                }
                                              },
                                              context,
                                              false);
      }
      return included.iterator();
    }
  }

}
