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

import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.UserDataHolder;

import org.jspecify.annotations.Nullable;
import java.util.Comparator;

/**
 * This interface describes visualization of attach items
 *
 * @param <T> type of the child items (belonging to this group)
 *            (applicable both for {@link XAttachHost} and {@link ProcessInfo} items)
 */
public interface XAttachPresentationGroup<T> extends Comparator<T> {
  /**
   * Define order among neighboring groups (smaller at first)
   */
  int getOrder();

  
  String getGroupName();

  /**
   * @deprecated Use {@link #getItemIcon(Project, Object, UserDataHolder)} (will be removed in 2020.1)
   */
  @Deprecated
  
  //@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  Image getProcessIcon(Project project, T info, UserDataHolder dataHolder);

  /**
   * @param dataHolder you may put your specific data into the holder at previous step in {@link XAttachDebuggerProvider#getAvailableDebuggers}
   *                   and use it for presentation
   * @return an icon to be shown in popup menu for your item, described by info
   */
  
  default Image getItemIcon(Project project, T info, UserDataHolder dataHolder) {
    return getProcessIcon(project, info, dataHolder);
  }

  /**
   * @deprecated Use {@link #getItemDisplayText(Project, Object, UserDataHolder)} (will be removed in 2020.1)
   */
  @Deprecated
  
  //@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  String getProcessDisplayText(Project project, T info, UserDataHolder dataHolder);

  /**
   * @param dataHolder you may put your specific data into the holder at previous step in {@link XAttachDebuggerProvider#getAvailableDebuggers}
   *                   and use it for presentation
   * @return a text to be shown on your item, described by info
   */
  
  default String getItemDisplayText(Project project, T info, UserDataHolder dataHolder) {
    return getProcessDisplayText(project, info, dataHolder);
  }

  /**
   * @param dataHolder you may put your specific data into the holder at previous step in {@link XAttachDebuggerProvider#getAvailableDebuggers}
   *                   and use it for presentation
   * @return a description of process to be shown in tooltip of your item, described by info
   */
  @Nullable
  default String getItemDescription(Project project, T info, UserDataHolder dataHolder) {
    return null;
  }

  /**
   * @param dataHolder you may put your specific data into the holder at previous step in {@link XAttachDebuggerProvider#getAvailableDebuggers}
   *                   and use it for comparison
   * @deprecated use {@link #compare(Object, Object)} (will be removed in 2020.1)
   * <p>
   * Specifies process order in your group
   */
  @Deprecated
  //@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  default int compare(Project project, T a, T b, UserDataHolder dataHolder) {
    return compare(a, b);
  }
}
