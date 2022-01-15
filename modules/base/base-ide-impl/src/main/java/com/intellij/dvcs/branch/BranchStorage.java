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
package com.intellij.dvcs.branch;

import com.intellij.dvcs.repo.Repository;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

import static com.intellij.dvcs.branch.DvcsBranchUtil.find;
import static com.intellij.util.containers.ContainerUtil.newArrayList;

@Tag("branch-storage")
public class BranchStorage {

  @Property(surroundWithTag = false)
  @MapAnnotation(keyAttributeName = "type")
  @Nonnull
  public Map<String, List<DvcsBranchInfo>> myBranches = ContainerUtil.newHashMap();

  public BranchStorage() {
  }

  public boolean contains(@Nonnull String typeName, @javax.annotation.Nullable Repository repository, @Nonnull String branchName) {
    List<DvcsBranchInfo> branches = myBranches.get(typeName);
    return branches != null && find(branches, repository, branchName) != null;
  }

  public void add(@Nonnull String typeName, @javax.annotation.Nullable Repository repository,
                  @Nonnull String branchName) {
    if (contains(typeName, repository, branchName)) return;
    List<DvcsBranchInfo> branchInfos = myBranches.computeIfAbsent(typeName, name -> newArrayList());
    branchInfos.add(new DvcsBranchInfo(DvcsBranchUtil.getPathFor(repository), branchName));
  }

  public void remove(@Nonnull String typeName, @Nullable Repository repository,
                     @Nonnull String branchName) {

    List<DvcsBranchInfo> branches = myBranches.get(typeName);
    DvcsBranchInfo toDelete = find(branches, repository, branchName);
    if (toDelete != null) {
      branches.remove(toDelete);
      if (branches.isEmpty()) {
        myBranches.remove(typeName);
      }
    }
  }
}
