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
package consulo.ide.impl.idea.openapi.vcs.update;

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.*;

/**
 * author: lesya
 */
public class GroupByPackages {
  private final Map<File, Collection<File>> myParentToChildrenMap = new HashMap<File, Collection<File>>();
  private final Collection<File> myRoots = new HashSet<File>();

  public GroupByPackages(@Nonnull Collection<File> files) {
    for (File file : files) {
      process(file);
    }
    splitRoots();
  }

  private void splitRoots() {
    for (File oldRoot : new ArrayList<File>(myRoots)) {
      File newRoot = splitRoot(oldRoot);
      if (!oldRoot.equals(newRoot)) replaceRoot(oldRoot, newRoot);
    }
  }

  private void replaceRoot(File oldRoot, File newRoot) {
    myRoots.remove(oldRoot);
    myRoots.add(newRoot);
  }

  private File splitRoot(@Nonnull File oldRoot) {
    List<File> children = getChildren(oldRoot);
    if (children.size() == 1) {
      return splitRoot(children.get(0));
    }
    return oldRoot;
  }

  private void process(@Nonnull final File file) {
    File f;
    File parent = file.getParentFile();
    for (f = file; parent != null; f = parent, parent = parent.getParentFile()) {
      Collection<File> files = myParentToChildrenMap.get(parent);
      if (files == null) {
        myParentToChildrenMap.put(parent, files = new HashSet<File>());
      }
      files.add(f);
    }
    myRoots.add(f);
  }

  @Nonnull
  public List<File> getRoots() {
    return new ArrayList<File>(myRoots);
  }

  @Nonnull
  public List<File> getChildren(File file) {
    Collection<File> collection = myParentToChildrenMap.get(file);
    if (collection == null) {
      return ContainerUtil.emptyList();
    }
    return new ArrayList<File>(collection);
  }
}
