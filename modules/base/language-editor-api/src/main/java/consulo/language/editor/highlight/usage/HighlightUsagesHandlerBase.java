/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.language.editor.highlight.usage;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.keymap.util.KeymapUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author yole
 */
public abstract class HighlightUsagesHandlerBase<T extends PsiElement> {
  protected final Editor myEditor;
  protected final PsiFile myFile;

  protected List<TextRange> myReadUsages = new ArrayList<>();
  protected List<TextRange> myWriteUsages = new ArrayList<>();
  protected String myStatusText;
  protected String myHintText;

  protected HighlightUsagesHandlerBase(final Editor editor, final PsiFile file) {
    myEditor = editor;
    myFile = file;
  }

  @RequiredReadAction
  public void highlightUsages(Runnable performHighlighting) {
    List<T> targets = getTargets();
    if (targets == null) {
      return;
    }
    selectTargets(targets, it -> {
      computeUsages(it);
      performHighlighting.run();
    });
  }

  public void buildStatusText(@Nullable String elementName, int refCount) {
    if (refCount > 0) {
      myStatusText = CodeInsightBundle.message(elementName != null ?
                                               "status.bar.highlighted.usages.message" :
                                               "status.bar.highlighted.usages.no.target.message", refCount, elementName,
                                               getShortcutText());
    }
    else {
      myHintText = CodeInsightBundle.message(elementName != null ?
                                             "status.bar.highlighted.usages.not.found.message" :
                                             "status.bar.highlighted.usages.not.found.no.target.message", elementName);
    }
  }

  public static String getShortcutText() {
    final Shortcut[] shortcuts = ActionManager.getInstance().getAction(IdeActions.ACTION_HIGHLIGHT_USAGES_IN_FILE).getShortcutSet().getShortcuts();
    if (shortcuts.length == 0) {
      return "<no key assigned>";
    }
    return KeymapUtil.getShortcutText(shortcuts[0]);
  }

  @Nullable
  @RequiredReadAction
  public abstract List<T> getTargets();

  @Nullable
  public String getFeatureId() {
    return null;
  }

  protected abstract void selectTargets(List<T> targets, Consumer<List<T>> selectionConsumer);

  public abstract void computeUsages(List<T> targets);

  @RequiredReadAction
  public void addOccurrence(@Nonnull PsiElement element) {
    TextRange range = element.getTextRange();
    if (range != null) {
      range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range);
      myReadUsages.add(range);
    }
  }

  public List<TextRange> getReadUsages() {
    return myReadUsages;
  }

  public List<TextRange> getWriteUsages() {
    return myWriteUsages;
  }

  @Nullable
  public String getHintText() {
    return myHintText;
  }

  @Nullable
  public String getStatusText() {
    return myStatusText;
  }

  /**
   * In case of egoistic handler (highlightReferences = true) IdentifierHighlighterPass applies information only from this particular handler.
   * Otherwise additional information would be collected from reference search as well.
   */
  public boolean highlightReferences() {
    return false;
  }
}
