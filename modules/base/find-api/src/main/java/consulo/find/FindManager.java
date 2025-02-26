/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.find;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.content.scope.SearchScope;
import consulo.fileEditor.FileEditor;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.PsiElement;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.intellij.lang.annotations.MagicConstant;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Allows to invoke and control Find, Replace and Find Usages operations.
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class FindManager {
  public abstract FindModel createReplaceInFileModel();

  @Nullable
  public abstract FindModel getPreviousFindModel();

  public abstract void setPreviousFindModel(FindModel previousFindModel);

  public abstract void showSettingsAndFindUsages(@Nonnull NavigationItem[] targets);

  /**
   * Returns the find manager instance for the specified project.
   *
   * @param project the project for which the manager is requested.
   * @return the manager instance.
   */
  public static FindManager getInstance(Project project) {
    return project.getInstance(FindManager.class);
  }

  /**
   * Shows the Find, Replace or Find Usages dialog initializing it from the specified
   * model and saves the settings entered by the user into the same model. Does not
   * perform the actual find or replace operation.
   *
   * @param model the model containing the settings of a find or replace operation.
   * @param okHandler Will be executed after doOkAction
   */
  @RequiredUIAccess
  public abstract void showFindDialog(@Nonnull FindModel model, @Nonnull @RequiredUIAccess Runnable okHandler);

  /**
   * Shows a replace prompt dialog for the specified replace operation.
   *
   * @param model the model containing the settings of the replace operation.
   * @param title the title of the dialog to show.
   * @return the exit code of the dialog, as defined by the {@link FindManager.PromptResult}
   * interface.
   */
  @PromptResultValue
  public abstract int showPromptDialog(@Nonnull FindModel model, String title);

  /**
   * Returns the settings of the last performed Find in File operation, or the
   * default Find in File settings if no such operation was performed by the user.
   *
   * @return the last Find in File settings.
   */
  @Nonnull
  public abstract FindModel getFindInFileModel();

  /**
   * Returns the settings of the last performed Find in Project operation, or the
   * default Find in Project settings if no such operation was performed by the user.
   *
   * @return the last Find in Project settings.
   */
  @Nonnull
  public abstract FindModel getFindInProjectModel();

  /**
   * Searches for the specified substring in the specified character sequence,
   * using the specified find settings. Supports case sensitive and insensitive
   * searches, forward and backward searches, regular expression searches and
   * searches for whole words.
   *
   * @param text   the text in which the search is performed.
   * @param offset the start offset for the search.
   * @param model  the settings for the search, including the string to find.
   * @return the result of the search.
   */
  @Nonnull
  public abstract FindResult findString(@Nonnull CharSequence text, int offset, @Nonnull FindModel model);

  /**
   * Searches for the specified substring in the specified character sequence,
   * using the specified find settings. Supports case sensitive and insensitive
   * searches, forward and backward searches, regular expression searches and
   * searches for whole words.
   *
   * @param text   the text in which the search is performed.
   * @param offset the start offset for the search.
   * @param model  the settings for the search, including the string to find.
   * @return the result of the search.
   */
  @Nonnull
  public abstract FindResult findString(@Nonnull CharSequence text, int offset, @Nonnull FindModel model,
                                        @Nullable VirtualFile findContextFile);

  /**
   * Shows a replace prompt dialog for the bad replace operation.
   *
   * @param model the model containing the settings of the replace operation.
   * @param title the title of the dialog to show.
   * @param exception exception from {@link FindManager#getStringToReplace(String, FindModel, int, CharSequence)}
   * @return the exit code of the dialog, as defined by the {@link PromptResult}
   * interface. May be only {@link PromptResult#CANCEL} or {@link PromptResult#SKIP} for bad replace operation
   */
  @PromptResultValue
  public abstract int showMalformedReplacementPrompt(@Nonnull FindModel model, String title, MalformedReplacementStringException exception);

  public static class MalformedReplacementStringException extends Exception {
    public MalformedReplacementStringException(String s) {
      super(s);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public MalformedReplacementStringException(String s, Throwable throwable) {
      super(s, throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }
  }

  /**
   * Gets the string to replace with, given the specified found string and find/replace
   * settings. Supports case-preserving and regular expression replaces.
   *
   * @param foundString the found string.
   * @param model       the search and replace settings, including the replace string.
   * @param startOffset offset in the source text at which the string was found (matters for regex searches)
   * @param documentText source text in which the string was found (matters for regex searches)
   * @return the string to replace the specified found string.
   */
  public abstract String getStringToReplace(@Nonnull String foundString, @Nonnull FindModel model,
                                            int startOffset, @Nonnull CharSequence documentText) throws MalformedReplacementStringException;

  /**
   * Gets the flag indicating whether the "Find Next" and "Find Previous" actions are
   * available to continue a previously started search operation. (The operations are
   * available if at least one search was performed in the current IDEA session.)
   *
   * @return true if the actions are available, false if there is no previous search
   *         operation to continue.
   */
  public abstract boolean findWasPerformed();

  /**
   * Sets the flag indicating that the "Find Next" and "Find Previous" actions are
   * available to continue a previously started search operation.
   */
  public abstract void setFindWasPerformed();

  /**
   * Gets the flag indicating that 'Add Selection for Next Occurrence' action was performed recently,
   * so "Find Next" and "Find Previous" actions should work in its context.
   */
  public abstract boolean selectNextOccurrenceWasPerformed();

  /**
   * Sets the flag indicating that 'Add Selection for Next Occurrence' action was performed recently,
   * so "Find Next" and "Find Previous" actions should work in its context.
   */
  public abstract void setSelectNextOccurrenceWasPerformed();

  /**
   * Explicitly tell FindManager that "Find Next" and "Find Previous" actions should not use
   * find usages previous results.
   */
  public abstract void clearFindingNextUsageInFile();

  /**
   * Sets the model containing the search settings to use for "Find Next" and
   * "Find Previous" operations.
   *
   * @param model the model to use for the operations.
   */
  public abstract void setFindNextModel(FindModel model);

  /**
   * Gets the model containing the search settings to use for "Find Next" and
   * "Find Previous" operations.
   *
   * @return the model to use for the operations.
   */
  public abstract FindModel getFindNextModel();

  /**
   * Gets the model containing the search settings to use for "Find Next" and
   * "Find Previous" operations specific for the editor given. It may be different than {@link #getFindNextModel()}
   * if there is find bar currently shown for the editor.
   *
   * @param editor editor, for which find model shall be retreived for
   * @return the model to use for the operations.
   */
  public abstract FindModel getFindNextModel(@Nonnull Editor editor);

  /**
   * Checks if the Find Usages action is available for the specified element.
   *
   * @param element the element to check the availability for.
   * @return true if Find Usages is available, false otherwise.
   * @see FindUsagesProvider#canFindUsagesFor(PsiElement)
   */
  public abstract boolean canFindUsages(@Nonnull PsiElement element);

  /**
   * Performs the Find Usages operation for the specified element.
   *
   * @param element the element to find the usages for.
   */
  public abstract void findUsages(@Nonnull PsiElement element);
  public abstract void findUsagesInScope(@Nonnull PsiElement element, @Nonnull SearchScope searchScope);

  /**
   * Shows the Find Usages dialog (if {@code showDialog} is true} and performs the Find Usages operation for the
   * specified element.
   *
   * @param element the element to find the usages for.
   * @param showDialog true if find usages settings dialog needs to be shown.
   * @since idea 12
   */
  public abstract void findUsages(@Nonnull PsiElement element, boolean showDialog);

  /**
   * Performs a "Find Usages in File" operation for the specified element.
   *
   * @param element the element for which the find is performed.
   * @param editor  the editor in which the find is performed.
   */
  public abstract void findUsagesInEditor(@Nonnull PsiElement element, @Nonnull FileEditor editor);

  /**
   * Performs a "Find Next" operation after "Find Usages in File" or
   * "Highlight Usages in File".
   *
   * @param editor the editor in which the find is performed.
   * @return true if the operation was performed (not necessarily found anything),
   *         false if an error occurred during the operation.
   */
  public abstract boolean findNextUsageInEditor(@Nonnull FileEditor editor);

  /**
   * Performs a "Find Previous" operation after "Find Usages in File" or
   * "Highlight Usages in File".
   *
   * @param editor the editor in which the find is performed.
   * @return true if the operation was performed (not necessarily found anything),
   *         false if an error occurred during the operation.
   */
  public abstract boolean findPreviousUsageInEditor(@Nonnull FileEditor editor);

  @Nullable
  public abstract FindUsagesHandler getFindUsagesHandler(@Nonnull PsiElement element, final boolean forHighlightUsages);

  @MagicConstant(valuesFromClass = FindManager.PromptResult.class)
  public @interface PromptResultValue {}

  /**
   * Possible return values for the {@link FindManager#showPromptDialog(FindModel, String)} method.
   *
   * @since 5.0.2
   */
  public interface PromptResult {
    int OK = 0;
    int CANCEL = 1;
    int SKIP = 2;
    int ALL = 3;
    int ALL_IN_THIS_FILE = 4;
    int ALL_FILES = 5;
    int SKIP_ALL_IN_THIS_FILE = 6;
  }
}
