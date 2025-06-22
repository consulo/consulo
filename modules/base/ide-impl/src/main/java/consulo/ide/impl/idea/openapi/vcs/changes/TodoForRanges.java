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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.application.ApplicationManager;
import consulo.colorScheme.TextAttributes;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import consulo.ide.impl.idea.openapi.util.Getter;
import consulo.util.lang.Pair;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.vcs.checkin.StepIntersection;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.search.PsiTodoSearchHelper;
import consulo.language.editor.impl.internal.highlight.TodoAttributesUtil;
import consulo.language.psi.search.TodoItem;
import consulo.ide.impl.idea.util.containers.Convertor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Irina.Chernushina
 * @since 2011-09-09
 */
public abstract class TodoForRanges {
  protected final Project myProject;
  private final List<TextRange> myRanges;
  private final int myAdditionalOffset;
  protected final String myFileName;
  protected final String myText;
  protected final boolean myOldRevision;
  protected final FileType myFileType;

  protected TodoForRanges(final Project project,
                       final List<TextRange> ranges,
                       int additionalOffset,
                       String name,
                       String text,
                       boolean revision, FileType type) {
    myProject = project;
    myRanges = ranges;
    myAdditionalOffset = additionalOffset;
    myFileName = name;
    myText = text;
    myOldRevision = revision;
    myFileType = type;
  }

  public List<Pair<TextRange, TextAttributes>> execute() {
    final TodoItemData[] todoItems = getTodoItems();
    
    final StepIntersection<TodoItemData, TextRange> stepIntersection =
      new StepIntersection<TodoItemData, TextRange>(new Convertor<TodoItemData, TextRange>() {
        @Override
        public TextRange convert(TodoItemData o) {
          return o.getTextRange();
        }
      }, Convertor.SELF, myRanges, new Getter<String>() {
        @Override
        public String get() {
          return "";
        }
      }
      );
    final List<TodoItemData> filtered = stepIntersection.process(Arrays.asList(todoItems));
    final List<Pair<TextRange, TextAttributes>> result = new ArrayList<Pair<TextRange, TextAttributes>>(filtered.size());
    int offset = 0;
    for (TextRange range : myRanges) {
      Iterator<TodoItemData> iterator = filtered.iterator();
      while (iterator.hasNext()) {
        TodoItemData item = iterator.next();
        if (range.contains(item.getTextRange())) {
          TextRange todoRange = new TextRange(offset - range.getStartOffset() + item.getTextRange().getStartOffset(),
                                              offset - range.getStartOffset() + item.getTextRange().getEndOffset());
          result.add(Pair.create(todoRange, TodoAttributesUtil.getTextAttributes(item.getPattern().getAttributes())));
          iterator.remove();
        } else {
          break;
        }
      }
      offset += range.getLength() + 1 + myAdditionalOffset;
    }
    return result;
  }

  protected abstract TodoItemData[] getTodoItems();

  protected TodoItem[] getTodoForText(PsiTodoSearchHelper helper) {
    final PsiFile psiFile = ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
      @Override
      public PsiFile compute() {
        return PsiFileFactory.getInstance(myProject).createFileFromText((myOldRevision ? "old" : "") + myFileName, myFileType, myText);
      }
    });
    return helper.findTodoItemsLight(psiFile);
  }
}
