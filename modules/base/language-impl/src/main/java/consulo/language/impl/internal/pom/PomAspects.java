/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.impl.internal.pom;

import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionWalker;
import consulo.language.pom.PomModelAspect;
import consulo.language.pom.PomModelAspectRegistrator;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author VISTALL
 * @since 18-Jun-24
 */
public class PomAspects implements PomModelAspectRegistrator {
  public static final ExtensionPointCacheKey<PomModelAspect, PomAspects> CACHE_KEY =
    ExtensionPointCacheKey.create("PomAspects", PomAspects::new);

  private final Map<Class<? extends PomModelAspect>, PomModelAspect> myAspects = new HashMap<>();
  private final Map<PomModelAspect, List<PomModelAspect>> myIncidence = new HashMap<>();
  private final Map<PomModelAspect, List<PomModelAspect>> myInvertedIncidence = new HashMap<>();

  public PomAspects(ExtensionWalker<PomModelAspect> walker) {
    walker.walk(it -> it.register(this));
  }

  public List<PomModelAspect> getAllDependencies(PomModelAspect aspect) {
    List<PomModelAspect> pomModelAspects = myIncidence.get(aspect);
    return pomModelAspects != null ? pomModelAspects : Collections.emptyList();
  }

  public List<PomModelAspect> getAllDependants(PomModelAspect aspect) {
    List<PomModelAspect> pomModelAspects = myInvertedIncidence.get(aspect);
    return pomModelAspects != null ? pomModelAspects : Collections.emptyList();
  }

  @Override
  public <P extends PomModelAspect> P getModelAspect(@Nonnull Class<P> clazz) {
    //noinspection unchecked
    return (P)myAspects.get(clazz);
  }

  @Override
  public void register(Class<? extends PomModelAspect> aClass, PomModelAspect aspect, Set<PomModelAspect> dependencies) {
    myAspects.put(aClass, aspect);
    final Iterator<PomModelAspect> iterator = dependencies.iterator();
    final List<PomModelAspect> deps = new ArrayList<>();
    // todo: reorder dependencies
    while (iterator.hasNext()) {
      final PomModelAspect depend = iterator.next();
      deps.addAll(getAllDependencies(depend));
    }
    deps.add(aspect); // add self to block same aspect transactions from event processing and update
    for (final PomModelAspect pomModelAspect : deps) {
      final List<PomModelAspect> pomModelAspects = myInvertedIncidence.get(pomModelAspect);
      if (pomModelAspects != null) {
        pomModelAspects.add(aspect);
      }
      else {
        myInvertedIncidence.put(pomModelAspect, new ArrayList<>(Collections.singletonList(aspect)));
      }
    }
    myIncidence.put(aspect, deps);
  }
}
