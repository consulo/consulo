/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.pathMacro.impl.internal.builtin;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.pathMacro.Macro;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;

import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public abstract class EditorMacro extends Macro {
  private final String myName;
  private final String myDescription;

  public EditorMacro(String name, String description) {
    myName = name;
    myDescription = description;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getDescription() {
    return myDescription;
  }

  @Override
  public final String expand(DataContext dataContext) throws ExecutionCancelledException {
    Project project = dataContext.getData(Project.KEY);
    if (project == null) return null;
    if (ToolWindowManager.getInstance(project).isEditorComponentActive()) {
      Editor editor = dataContext.getData(Editor.KEY);
      if (editor != null){
        return expand(editor);
      }
    }
    return null;
  }

  @Nullable
  protected abstract String expand(Editor editor);
}
