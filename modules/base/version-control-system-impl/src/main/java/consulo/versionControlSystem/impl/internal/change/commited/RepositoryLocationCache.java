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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.component.messagebus.MessageBusConnection;
import consulo.versionControlSystem.*;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RepositoryLocationCache {
  private final Project myProject;
  private final Map<Pair<String, String>, RepositoryLocation> myMap;

  public RepositoryLocationCache(Project project) {
    myProject = project;
    myMap = Collections.synchronizedMap(new HashMap<Pair<String, String>, RepositoryLocation>());
    MessageBusConnection connection = myProject.getMessageBus().connect();
    VcsListener listener = () -> reset();
    connection.subscribe(VcsMappingListener.class, listener);
    connection.subscribe(PluginVcsMappingListener.class, listener);
  }

  public RepositoryLocation getLocation(AbstractVcs vcs, FilePath filePath, boolean silent) {
    Pair<String, String> key = new Pair<String, String>(vcs.getName(), filePath.getIOFile().getAbsolutePath());
    RepositoryLocation location = myMap.get(key);
    if (location != null) {
      return location;
    }
    location = getUnderProgress(vcs, filePath, silent);
    myMap.put(key, location);
    return location;
  }

  private RepositoryLocation getUnderProgress(AbstractVcs vcs, FilePath filePath, boolean silent) {
    MyLoader loader = new MyLoader(vcs, filePath);
    if ((! silent) && ApplicationManager.getApplication().isDispatchThread()) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(loader, "Discovering location of " + filePath.getPresentableUrl(), true, myProject);
    } else {
      loader.run();
    }
    return loader.getLocation();
  }

  public void reset() {
    myMap.clear();
  }

  private class MyLoader implements Runnable {
    private final AbstractVcs myVcs;
    private final FilePath myFilePath;
    private RepositoryLocation myLocation;

    private MyLoader(@Nonnull AbstractVcs vcs, @Nonnull FilePath filePath) {
      myVcs = vcs;
      myFilePath = filePath;
    }

    @Override
    public void run() {
      CommittedChangesProvider committedChangesProvider = myVcs.getCommittedChangesProvider();
      if (committedChangesProvider != null) {
        myLocation = committedChangesProvider.getLocationFor(myFilePath);
      }
    }

    public RepositoryLocation getLocation() {
      return myLocation;
    }
  }
}
