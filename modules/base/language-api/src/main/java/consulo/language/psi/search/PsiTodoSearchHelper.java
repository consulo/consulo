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
package consulo.language.psi.search;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * @author irengrig
 * @since 2011-02-24
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class PsiTodoSearchHelper {
  @Nonnull
  public static PsiTodoSearchHelper getInstance(@Nonnull Project project) {
    return project.getInstance(PsiTodoSearchHelper.class);
  }

  /**
   * Returns the list of all files in the project which have to do items.
   *
   * @return the list of files with to do items.
   */
  @Nonnull
  public abstract PsiFile[] findFilesWithTodoItems();

  /**
   * Searches the specified file for to do items.
   *
   * @param file the file to search for to do items.
   * @return the array of found items.
   */
  @Nonnull
  public abstract TodoItem[] findTodoItems(@Nonnull PsiFile file);

  /**
   * Searches the specified range of text in the specified file for to do items.
   *
   * @param file        the file to search for to do items.
   * @param startOffset the start offset of the text range to search to do items in.
   * @param endOffset   the end offset of the text range to search to do items in.
   * @return the array of found items.
   */
  @Nonnull
  public abstract TodoItem[] findTodoItems(@Nonnull PsiFile file, int startOffset, int endOffset);

  @Nonnull
  public abstract TodoItem[] findTodoItemsLight(@Nonnull PsiFile file);
  @Nonnull
  public abstract TodoItem[] findTodoItemsLight(@Nonnull PsiFile file, int startOffset, int endOffset);

  /**
   * Returns the number of to do items in the specified file.
   *
   * @param file the file to return the to do count for.
   * @return the count of to do items in the file.
   */
  public abstract int getTodoItemsCount(@Nonnull PsiFile file);

  /**
   * Returns the number of to do items matching the specified pattern in the specified file.
   *
   * @param file    the file to return the to do count for.
   * @param pattern the pattern of to do items to find.
   * @return the count of to do items in the file.
   */
  public abstract int getTodoItemsCount(@Nonnull PsiFile file, @Nonnull TodoPattern pattern);


  /**
   * Returns if td items should be highlighted in editor
   *
   * @param file the file to return the to do count for.
   * @return if td items should be highlighted in editor. True by default
   */
  public boolean shouldHighlightInEditor(@Nonnull PsiFile file) {
    return true;
  }
}
