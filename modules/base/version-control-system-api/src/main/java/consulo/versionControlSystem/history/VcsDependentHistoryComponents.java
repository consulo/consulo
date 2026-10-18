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
package consulo.versionControlSystem.history;

import consulo.ui.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class VcsDependentHistoryComponents {
  private final List<VcsHistoryColumn> myColumns;
  private final @Nullable Consumer<VcsFileRevision> myRevisionListener;
  private final @Nullable Component myDetailsComponent;

  public VcsDependentHistoryComponents(List<VcsHistoryColumn> columns,
                                       @Nullable Consumer<VcsFileRevision> revisionListener,
                                       @Nullable Component detailsComponent) {
    myColumns = List.copyOf(columns);
    myRevisionListener = revisionListener;
    myDetailsComponent = detailsComponent;
  }

  public static VcsDependentHistoryComponents createOnlyColumns(List<VcsHistoryColumn> columns) {
    return new VcsDependentHistoryComponents(columns, null, null);
  }

  public static VcsDependentHistoryComponents empty() {
    return createOnlyColumns(List.of());
  }

  public List<VcsHistoryColumn> getColumns() {
    return myColumns;
  }

  public @Nullable Consumer<VcsFileRevision> getRevisionListener() {
    return myRevisionListener;
  }

  public @Nullable Component getDetailsComponent() {
    return myDetailsComponent;
  }
}
