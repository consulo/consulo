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
package com.intellij.debugger.ui;

import com.intellij.execution.ui.ExecutionConsole;
import org.jetbrains.annotations.NonNls;

public interface DebuggerContentInfo {

  @NonNls String CONSOLE_CONTENT = ExecutionConsole.CONSOLE_CONTENT_ID;
  @NonNls String THREADS_CONTENT = "ThreadsContent";
  @NonNls String VARIABLES_CONTENT = "VariablesContent";
  @NonNls String FRAME_CONTENT = "FrameContent";
  @NonNls String WATCHES_CONTENT = "WatchesContent";

}
