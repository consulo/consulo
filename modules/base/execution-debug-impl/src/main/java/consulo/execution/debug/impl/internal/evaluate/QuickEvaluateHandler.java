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
package consulo.execution.debug.impl.internal.evaluate;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * @author nik
 */
public abstract class QuickEvaluateHandler {

  public abstract boolean isEnabled(Project project);

  @Nullable
  public abstract AbstractValueHint createValueHint(Project project, Editor editor, Point point, ValueHintType type);

  public abstract boolean canShowHint(Project project);

  public abstract int getValueLookupDelay(Project project);
}
