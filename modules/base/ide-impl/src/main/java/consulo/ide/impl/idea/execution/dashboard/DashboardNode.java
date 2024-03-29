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
package consulo.ide.impl.idea.execution.dashboard;

import consulo.execution.ui.RunContentDescriptor;
import consulo.project.Project;
import consulo.ui.ex.content.Content;
import jakarta.annotation.Nullable;

/**
 * @author konstantin.aleev
 */
public interface DashboardNode {
  @Nullable
  default RunContentDescriptor getDescriptor() {
    return null;
  }

  @Nullable
  default Content getContent() {
    RunContentDescriptor descriptor = getDescriptor();
    return descriptor == null ? null : descriptor.getAttachedContent();
  }

  Project getProject();
}
