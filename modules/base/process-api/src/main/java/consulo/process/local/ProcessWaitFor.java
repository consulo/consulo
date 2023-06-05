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
package consulo.process.local;

import consulo.annotation.DeprecationInfo;
import consulo.process.TaskExecutor;
import jakarta.annotation.Nonnull;

@Deprecated
@DeprecationInfo("Use consulo.process.util.ProcessWaitFor")
public class ProcessWaitFor extends consulo.process.util.ProcessWaitFor {
  public ProcessWaitFor(@Nonnull Process process, @Nonnull TaskExecutor executor, @Nonnull String presentableName) {
    super(process, executor, presentableName);
  }
}