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
package consulo.execution.debug.attach;

import consulo.platform.ProcessInfo;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;

/**
 * @deprecated Use {@link XAttachProcessPresentationGroup} (will be removed in 2020.1)
 */
//@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
@Deprecated
public interface XLocalAttachGroup extends XAttachProcessPresentationGroup {
  /**
   * @deprecated will be removed in 2020.1
   */
  //@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  @Nonnull
  XLocalAttachGroup DEFAULT = new XDefaultLocalAttachGroup();

  /**
   * @deprecated use {@link #compare(ProcessInfo, ProcessInfo)} (will be removed in 2020.1)
   */
  //@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Override
  @Deprecated
  default int compare(@Nonnull Project project, @Nonnull ProcessInfo a, @Nonnull ProcessInfo b, @Nonnull UserDataHolder dataHolder) {
    return compare(a, b);
  }
}
