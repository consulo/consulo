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
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDateFilterImpl;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
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
  private final @Nullable VcsLogRangeFilter myRangeFilter;

  public VcsLogFilterCollectionImpl(@Nullable VcsLogBranchFilter branchFilter,
                                    @Nullable VcsLogUserFilter userFilter,
                                    @Nullable VcsLogHashFilter hashFilter,
                                    @Nullable VcsLogDateFilter dateFilter,
                                    @Nullable VcsLogTextFilter textFilter,
                                    @Nullable VcsLogStructureFilter structureFilter,
                                    @Nullable VcsLogRootFilter rootFilter,
                                    @Nullable VcsLogRangeFilter rangeFilter) {
    myBranchFilter = branchFilter;
    myUserFilter = userFilter;
    myHashFilter = hashFilter;
    myDateFilter = dateFilter;
    myTextFilter = textFilter;
    myStructureFilter = structureFilter;
    myRootFilter = rootFilter;
    myRangeFilter = rangeFilter;
  }

  @Override
  public @Nullable VcsLogBranchFilter getBranchFilter() {
    return myBranchFilter;
  }

  @Override
  public @Nullable VcsLogHashFilter getHashFilter() {
    return myHashFilter;
  }

  @Override
  public @Nullable VcsLogUserFilter getUserFilter() {
    return myUserFilter;
  }

  @Override
  public @Nullable VcsLogDateFilter getDateFilter() {
    return myDateFilter;
  }

  @Override
  public @Nullable VcsLogTextFilter getTextFilter() {
    return myTextFilter;
  }

  @Override
  public @Nullable VcsLogStructureFilter getStructureFilter() {
    return myStructureFilter;
  }

  @Override
  public @Nullable VcsLogRootFilter getRootFilter() {
    return myRootFilter;
  }

  @Override
  public @Nullable VcsLogRangeFilter getRangeFilter() {
    return myRangeFilter;
  }

  @Override
  public boolean isEmpty() {
    return myBranchFilter == null && myRangeFilter == null && getDetailsFilters().isEmpty();
  }

  
  @Override
  public List<VcsLogDetailsFilter> getDetailsFilters() {
    return ContainerUtil.skipNulls(Arrays.asList(myUserFilter, myDateFilter, myTextFilter, myStructureFilter));
  }

  public static String getPresentation(VcsLogFilterCollection collection, boolean withPrefix) {
    VcsLogHashFilter hashFilter = collection.getHashFilter();
    if (hashFilter != null) {
      return hashFilter.getDisplayText();
    }
    List<VcsLogFilter> filters = ContainerUtil.skipNulls(Arrays.asList(
      collection.getBranchFilter(),
      collection.getRangeFilter(),
      collection.getUserFilter(),
      collection.getDateFilter(),
      collection.getTextFilter(),
      collection.getStructureFilter(),
      collection.getRootFilter()
    ));
    return StringUtil.join(filters, filter -> filters.size() != 1 || withPrefix ? withPrefix(filter) : filter.getDisplayText(), " ");
  }

  private static String withPrefix(VcsLogFilter filter) {
    if (filter instanceof VcsLogTextFilter) {
      return VersionControlSystemLogLocalize.filterPresentationTextWithPrefix(filter.getDisplayText()).get();
    }
    if (filter instanceof VcsLogUserFilter) {
      return VersionControlSystemLogLocalize.filterPresentationUserWithPrefix(filter.getDisplayText()).get();
    }
    if (filter instanceof VcsLogDateFilter dateFilter) {
      return VcsLogDateFilterImpl.getDisplayTextWithPrefix(dateFilter);
    }
    if (filter instanceof VcsLogBranchFilter || filter instanceof VcsLogRangeFilter) {
      return VersionControlSystemLogLocalize.filterPresentationBranchWithPrefix(filter.getDisplayText()).get();
    }
    if (filter instanceof VcsLogRootFilter) {
      return VersionControlSystemLogLocalize.filterPresentationRootWithPrefix(filter.getDisplayText()).get();
    }
    if (filter instanceof VcsLogStructureFilter) {
      return VersionControlSystemLogLocalize.filterPresentationStructureWithPrefix(filter.getDisplayText()).get();
    }
    return filter.getDisplayText();
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
           (myRootFilter != null ? myRootFilter + ", " : "") +
           (myRangeFilter != null ? myRangeFilter : "") + ")";
  }

  public static class VcsLogFilterCollectionBuilder {
    private @Nullable VcsLogBranchFilter myBranchFilter;
    private @Nullable VcsLogUserFilter myUserFilter;
    private @Nullable VcsLogHashFilter myHashFilter;
    private @Nullable VcsLogDateFilter myDateFilter;
    private @Nullable VcsLogTextFilter myTextFilter;
    private @Nullable VcsLogStructureFilter myStructureFilter;
    private @Nullable VcsLogRootFilter myRootFilter;
    private @Nullable VcsLogRangeFilter myRangeFilter;

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
      myRangeFilter = filterCollection.getRangeFilter();
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

    
    public VcsLogFilterCollectionBuilder with(@Nullable VcsLogRangeFilter filter) {
      myRangeFilter = filter;
      return this;
    }

    public VcsLogFilterCollectionBuilder with(@Nullable VcsLogFilter filter) {
      if (filter == null) {
        return this;
      }
      if (filter instanceof VcsLogBranchFilter branchFilter) {
        return with(branchFilter);
      }
      if (filter instanceof VcsLogUserFilter userFilter) {
        return with(userFilter);
      }
      if (filter instanceof VcsLogHashFilter hashFilter) {
        return with(hashFilter);
      }
      if (filter instanceof VcsLogDateFilter dateFilter) {
        return with(dateFilter);
      }
      if (filter instanceof VcsLogTextFilter textFilter) {
        return with(textFilter);
      }
      if (filter instanceof VcsLogStructureFilter structureFilter) {
        return with(structureFilter);
      }
      if (filter instanceof VcsLogRootFilter rootFilter) {
        return with(rootFilter);
      }
      if (filter instanceof VcsLogRangeFilter rangeFilter) {
        return with(rangeFilter);
      }
      throw new IllegalArgumentException("Unknown filter " + filter);
    }

    public VcsLogFilterCollection build() {
      return new VcsLogFilterCollectionImpl(myBranchFilter, myUserFilter, myHashFilter, myDateFilter, myTextFilter, myStructureFilter,
                                            myRootFilter, myRangeFilter);
    }
  }
}
