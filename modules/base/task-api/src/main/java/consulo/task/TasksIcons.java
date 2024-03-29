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
package consulo.task;

import consulo.annotation.DeprecationInfo;
import consulo.application.AllIcons;
import consulo.task.icon.TaskIconGroup;
import consulo.ui.image.Image;

@Deprecated
@DeprecationInfo("Use TaskIconGroup")
public class TasksIcons {
  public static final Image Bug = TaskIconGroup.bug();
  public static final Image Clock = TaskIconGroup.clock();
  public static final Image Exception = TaskIconGroup.exception();
  @Deprecated
  public static final Image SavedContext = TaskIconGroup.savedcontext();
  public static final Image StartTimer = TaskIconGroup.starttimer();
  public static final Image StopTimer = TaskIconGroup.stoptimer();
  @Deprecated
  public static final Image Unknown = AllIcons.FileTypes.Unknown;
}
