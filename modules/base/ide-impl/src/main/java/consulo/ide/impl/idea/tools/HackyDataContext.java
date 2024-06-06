/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.tools;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores main keys from DataContext.
 *
 * Normally nobody needs this. This handles specific case when after action is invoked a dialog can appear, but
 * we need the DataContext from the action.
 *
 * @author Konstantin Bulenkov
 */
class HackyDataContext implements DataContext {
  private static Key[] keys = {
    Project.KEY,
    Project.PROJECT_FILE_DIRECTORY,
    Editor.KEY,
    VirtualFile.KEY,
    Module.KEY,
    PsiFile.KEY
  };


  private final Map<Key, Object> values = new HashMap<>();
  private AnActionEvent myActionEvent;

  @SuppressWarnings("unchecked")
  public HackyDataContext(DataContext context, AnActionEvent e) {
    myActionEvent = e;
    for (Key key : keys) {
      values.put(key, context.getData(key));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getData(@Nonnull Key<T> dataId) {
    if (values.keySet().contains(dataId)) {
      return (T)values.get(dataId);
    }
    //noinspection UseOfSystemOutOrSystemErr
    System.out.println("Please add " + dataId + " key in " + getClass().getName());
    return null;
  }

  AnActionEvent getActionEvent() {
    return myActionEvent;
  }
}
