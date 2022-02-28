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
package consulo.desktop.awt.uiOld.win;

public class RecentTasks {
  /**
   * Com initialization should be invoked once per process.
   * All invocation should be made from the same thread.
   *
   * @param applicationId
   */
  public native static void initialize(String applicationId);

  public native static void addTasksNativeForCategory(String category, Task[] tasks);

  public native static String getShortenPath(String path);

  public native static void clearNative();
}
