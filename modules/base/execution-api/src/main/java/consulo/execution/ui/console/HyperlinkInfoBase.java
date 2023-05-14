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
package consulo.execution.ui.console;

import consulo.execution.ui.console.HyperlinkInfo;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public abstract class HyperlinkInfoBase implements HyperlinkInfo {
  public abstract void navigate(@Nonnull Project project, @Nullable RelativePoint hyperlinkLocationPoint);

  @Override
  public void navigate(Project project) {
    navigate(project, null);
  }
}
