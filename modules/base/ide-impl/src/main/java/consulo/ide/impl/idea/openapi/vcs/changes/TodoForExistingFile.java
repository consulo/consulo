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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.application.ApplicationManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import consulo.document.util.TextRange;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.search.PsiTodoSearchHelper;

import java.util.List;

/**
 * @author Irina.Chernushina
 * @since 2012-02-28
 */
public class TodoForExistingFile extends TodoForRanges {
  private final VirtualFile myFile;

  public TodoForExistingFile(Project project,
                              List<TextRange> ranges,
                              int additionalOffset,
                              String name,
                              String text,
                              boolean revision, FileType type, VirtualFile file) {
    super(project, ranges, additionalOffset, name, text, revision, type);
    myFile = file;
  }

  @Override
  protected TodoItemData[] getTodoItems() {
    return ApplicationManager.getApplication().runReadAction(new Computable<TodoItemData[]>() {
      @Override
      public TodoItemData[] compute() {
        final PsiTodoSearchHelper helper = PsiTodoSearchHelper.getInstance(myProject);

        PsiFile psiFile = myFile == null ? null : PsiManager.getInstance(myProject).findFile(myFile);
        if (psiFile != null) {
          return TodoForBaseRevision.convertTodo(helper.findTodoItems(psiFile));
        }

        return TodoForBaseRevision.convertTodo(getTodoForText(helper));
      }
    });
  }
}
