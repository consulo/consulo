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
package consulo.task.impl.internal.action;

import consulo.document.Document;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionListProvider;
import consulo.project.Project;
import consulo.task.Task;
import consulo.task.TaskManager;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author Dmitry Avdeev
 * @since 2013-07-16
 */
public class TaskAutoCompletionListProvider extends TextFieldWithAutoCompletionListProvider<Task> {

  private final Project myProject;

  public TaskAutoCompletionListProvider(Project project) {
    super(null);
    myProject = project;
  }

  @Override
  protected String getQuickDocHotKeyAdvertisementTail(@Nonnull String shortcut) {
    return "task description and comments";
  }

  @Nonnull
  @Override
  public List<Task> getItems(final String prefix, final boolean cached, CompletionParameters parameters) {
    return TaskSearchSupport.getItems(TaskManager.getManager(myProject), prefix, cached, parameters.isAutoPopup());
  }

  @Override
  public void setItems(@Nullable Collection variants) {
    // Do nothing
  }

  @Override
  public LookupElementBuilder createLookupBuilder(@Nonnull final Task task) {
    LookupElementBuilder builder = super.createLookupBuilder(task);

    builder = builder.withLookupString(task.getSummary());
    if (task.isClosed()) {
      builder = builder.strikeout();
    }

    return builder;
  }

  @Override
  protected InsertHandler<LookupElement> createInsertHandler(@Nonnull final Task task) {
    return new InsertHandler<LookupElement>() {
      @Override
      public void handleInsert(InsertionContext context, LookupElement item) {
        Document document = context.getEditor().getDocument();
        String s = ((TaskManagerImpl)TaskManager.getManager(context.getProject())).getChangelistName(task);
        s = StringUtil.convertLineSeparators(s);
        document.replaceString(context.getStartOffset(), context.getTailOffset(), s);
        context.getEditor().getCaretModel().moveToOffset(context.getStartOffset() + s.length());

        TaskAutoCompletionListProvider.this.handleInsert(task);
      }
    };
  }

  protected void handleInsert(@Nonnull final Task task) {
    // Override it for autocompletion insert handler
  }

  @Override
  protected Image getIcon(@Nonnull final Task task) {
    return task.getIcon();
  }

  @Nonnull
  @Override
  protected String getLookupString(@Nonnull final Task task) {
    return task.getId();
  }

  @Override
  protected String getTailText(@Nonnull final Task task) {
    return " " + task.getSummary();
  }

  @Override
  protected String getTypeText(@Nonnull final Task task) {
    return null;
  }

  @Override
  public int compare(@Nonnull final Task task1, @Nonnull final Task task2) {
    // N/A here
    throw new UnsupportedOperationException();
  }
}
