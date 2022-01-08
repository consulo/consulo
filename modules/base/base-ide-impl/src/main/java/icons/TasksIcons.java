/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package icons;

import com.intellij.icons.AllIcons;
import consulo.task.impl.icon.TaskImplIconGroup;
import consulo.ui.image.Image;

public class TasksIcons {
  public static final Image Bug = TaskImplIconGroup.bug();
  public static final Image Clock = TaskImplIconGroup.clock();
  public static final Image Exception = TaskImplIconGroup.exception();
  @Deprecated
  public static final Image SavedContext = TaskImplIconGroup.savedContext();
  public static final Image StartTimer = TaskImplIconGroup.startTimer();
  public static final Image StopTimer = TaskImplIconGroup.stopTimer();
  @Deprecated
  public static final Image Unknown = AllIcons.FileTypes.Unknown;
}
