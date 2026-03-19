/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal;

import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.log.*;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class VcsLogFilterCollectionImpl implements VcsLogFilterCollection {
  
  public static final VcsLogFilterCollection EMPTY = new VcsLogFilterCollectionBuilder().build();

  private final @Nullable VcsLogBranchFilter myBranchFilter;
  private final @Nullable VcsLogUserFilter myUserFilter;
  private final @Nullable VcsLogHashFilter myHashFilter;
  private final @Nullable VcsLogDateFilter myDateFilter;
  private final @Nullable VcsLogTextFilter myTextFilter;
  private final @Nullable VcsLogStructureFilter myStructureFilter;
  private final @Nullable VcsLogRootFilter myRootFilter;

  public VcsLogFilterCollectionImpl(@Nullable VcsLogBranchFilter branchFilter,
                                    @Nullable VcsLogUserFilter userFilter,
                                    @Nullable VcsLogHashFilter hashFilter,
                                    @Nullable VcsLogDateFilter dateFilter,
                                    @Nullable VcsLogTextFilter textFilter,
                                    @Nullable VcsLogStructureFilter structureFilter,
                                    @Nullable VcsLogRootFilter rootFilter) {
    myBranchFilter = branchFilter;
    myUserFilter = userFilter;
    myHashFilter = hashFilter;
    myDateFilter = dateFilter;
    myTextFilter = textFilter;
    myStructureFilter = structureFilter;
    myRootFilter = rootFilter;
  }

  @Nullable
  @Override
  public VcsLogBranchFilter getBranchFilter() {
    return myBranchFilter;
  }

  @Override
  public @Nullable VcsLogHashFilter getHashFilter() {
    return myHashFilter;
  }

  @Nullable
  @Override
  public VcsLogUserFilter getUserFilter() {
    return myUserFilter;
  }

  @Nullable
  @Override
  public VcsLogDateFilter getDateFilter() {
    return myDateFilter;
  }

  @Nullable
  @Override
  public VcsLogTextFilter getTextFilter() {
    return myTextFilter;
  }

  @Nullable
  @Override
  public VcsLogStructureFilter getStructureFilter() {
    return myStructureFilter;
  }

  @Nullable
  @Override
  public VcsLogRootFilter getRootFilter() {
    return myRootFilter;
  }


  @Override
  public boolean isEmpty() {
    return myBranchFilter == null && getDetailsFilters().isEmpty();
  }

  
  @Override
  public List<VcsLogDetailsFilter> getDetailsFilters() {
    return ContainerUtil.skipNulls(Arrays.asList(myUserFilter, myDateFilter, myTextFilter, myStructureFilter));
  }

  @Override
  public String toString() {
    return "filters: (" +
           (myBranchFilter != null ? myBranchFilter + ", " : "") +
           (myUserFilter != null ? myUserFilter + ", " : "") +
           (myHashFilter != null ? myHashFilter + ", " : "") +
           (myDateFilter != null ? myDateFilter + ", " : "") +
           (myTextFilter != null ? myTextFilter + ", " : "") +
           (myStructureFilter != null ? myStructureFilter + ", " : "") +
           (myRootFilter != null ? myRootFilter : "") + ")";
  }

  public static class VcsLogFilterCollectionBuilder {
    private @Nullable VcsLogBranchFilter myBranchFilter;
    private @Nullable VcsLogUserFilter myUserFilter;
    private @Nullable VcsLogHashFilter myHashFilter;
    private @Nullable VcsLogDateFilter myDateFilter;
    private @Nullable VcsLogTextFilter myTextFilter;
    private @Nullable VcsLogStructureFilter myStructureFilter;
    private @Nullable VcsLogRootFilter myRootFilter;

    public VcsLogFilterCollectionBuilder() {
    }

    public VcsLogFilterCollectionBuilder(VcsLogFilterCollection filterCollection) {
      myBranchFilter = filterCollection.getBranchFilter();
      myUserFilter = filterCollection.getUserFilter();
      myHashFilter = filterCollection.getHashFilter();
      myDateFilter = filterCollection.getDateFilter();
      myTextFilter = filterCollection.getTextFilter();
      myStructureFilter = filterCollection.getStructureFilter();
      myRootFilter = filterCollection.getRootFilter();
    }

    
    public VcsLogFilterCollectionBuilder with(@Nullable VcsLogBranchFilter filter) {
      myBranchFilter = filter;
      return this;
    }

    
    public VcsLogFilterCollectionBuilder with(@Nullable VcsLogUserFilter filter) {
      myUserFilter = filter;
      return this;
    }

    
    public VcsLogFilterCollectionBuilder with(@Nullable VcsLogHashFilter filter) {
      myHashFilter = filter;
      return this;
    }

    
    public VcsLogFilterCollectionBuilder with(@Nullable VcsLogDateFilter filter) {
      myDateFilter = filter;
      return this;
    }

    
    public VcsLogFilterCollectionBuilder with(@Nullable VcsLogTextFilter filter) {
      myTextFilter = filter;
      return this;
    }

    
    public VcsLogFilterCollectionBuilder with(@Nullable VcsLogStructureFilter filter) {
      myStructureFilter = filter;
      return this;
    }

    
    public VcsLogFilterCollectionBuilder with(@Nullable VcsLogRootFilter filter) {
      myRootFilter = filter;
      return this;
    }

    
    public VcsLogFilterCollection build() {
      return new VcsLogFilterCollectionImpl(myBranchFilter, myUserFilter, myHashFilter, myDateFilter, myTextFilter, myStructureFilter,
                                            myRootFilter);
    }
  }
}
