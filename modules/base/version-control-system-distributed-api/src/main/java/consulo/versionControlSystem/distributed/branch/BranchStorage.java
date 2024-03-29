/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.branch;

import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.versionControlSystem.distributed.repository.Repository;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag("branch-storage")
public class BranchStorage {

  @Property(surroundWithTag = false)
  @MapAnnotation(keyAttributeName = "type")
  @Nonnull
  public Map<String, List<DvcsBranchInfo>> myBranches = new HashMap<>();

  public BranchStorage() {
  }

  public boolean contains(@Nonnull String typeName, @Nullable Repository repository, @Nonnull String branchName) {
    List<DvcsBranchInfo> branches = myBranches.get(typeName);
    return branches != null && DvcsBranchUtil.find(branches, repository, branchName) != null;
  }

  public void add(@Nonnull String typeName, @Nullable Repository repository,
                  @Nonnull String branchName) {
    if (contains(typeName, repository, branchName)) return;
    List<DvcsBranchInfo> branchInfos = myBranches.computeIfAbsent(typeName, name -> new ArrayList<>());
    branchInfos.add(new DvcsBranchInfo(DvcsBranchUtil.getPathFor(repository), branchName));
  }

  public void remove(@Nonnull String typeName, @Nullable Repository repository,
                     @Nonnull String branchName) {

    List<DvcsBranchInfo> branches = myBranches.get(typeName);
    DvcsBranchInfo toDelete = DvcsBranchUtil.find(branches, repository, branchName);
    if (toDelete != null) {
      branches.remove(toDelete);
      if (branches.isEmpty()) {
        myBranches.remove(typeName);
      }
    }
  }
}
