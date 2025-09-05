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
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.application.ApplicationManager;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import consulo.versionControlSystem.impl.internal.change.patch.AutoMatchStrategy;
import consulo.versionControlSystem.impl.internal.change.patch.TextFilePatchInProgress;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class AutoMatchIterator {
  private final Project myProject;
  private final List<AutoMatchStrategy> myStrategies;

  public AutoMatchIterator(Project project) {
    myProject = project;
    VirtualFile baseDir = myProject.getBaseDir();
    myStrategies = new LinkedList<>();
    myStrategies.add(new OneBaseStrategy(baseDir));
    myStrategies.add(new IndividualPiecesStrategy(baseDir));
    myStrategies.add(new DefaultPatchStrategy(baseDir));
  }

  public List<TextFilePatchInProgress> execute(List<TextFilePatch> list) {
    List<TextFilePatch> creations = new LinkedList<>();

    final PatchBaseDirectoryDetector directoryDetector = PatchBaseDirectoryDetector.getInstance(myProject);
    for (TextFilePatch patch : list) {
      if (patch.isNewFile() || (patch.getBeforeName() == null)) {
        creations.add(patch);
        continue;
      }
      final String fileName = patch.getBeforeFileName();
      Collection<VirtualFile> files = ApplicationManager.getApplication().runReadAction(new Computable<Collection<VirtualFile>>() {
        public Collection<VirtualFile> compute() {
          return directoryDetector.findFiles(fileName);
        }
      });
      for (AutoMatchStrategy strategy : myStrategies) {
        strategy.acceptPatch(patch, files);
      }
    }

    for (AutoMatchStrategy strategy : myStrategies) {
      strategy.beforeCreations();
    }
    // then try to match creations
    for (TextFilePatch creation : creations) {
      for (AutoMatchStrategy strategy : myStrategies) {
        strategy.processCreation(creation);
      }
    }

    for (AutoMatchStrategy strategy : myStrategies) {
      if (strategy.succeeded()) {
        return strategy.getResult();
      }
    }
    return myStrategies.get(myStrategies.size() - 1).getResult();
  }
}
