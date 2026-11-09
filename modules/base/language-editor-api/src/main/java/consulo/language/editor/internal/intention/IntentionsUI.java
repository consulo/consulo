// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.internal.intention;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbar;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class IntentionsUI {
  private final Project myProject;

  public static IntentionsUI getInstance(Project project) {
    return project.getInstance(IntentionsUI.class);
  }

  public IntentionsUI(Project project) {
    myProject = project;
  }

  private final AtomicReference<CachedIntentions> myCachedIntentions = new AtomicReference<>();

  
  public CachedIntentions getCachedIntentions(@Nullable Editor editor, PsiFile file) {
    return myCachedIntentions.updateAndGet(cachedIntentions -> {
      if (cachedIntentions != null && editor == cachedIntentions.getEditor() && file == cachedIntentions.getFile()) {
        return cachedIntentions;
      }
      else {
        return new CachedIntentions(myProject, file, editor);
      }
    });

  }

  /**
   * Hides the hint when it belongs to {@code editor}.
   */
  @RequiredUIAccess
  public void hideHintOf(Editor editor) {
    CachedIntentions cachedIntentions = myCachedIntentions.get();
    if (cachedIntentions != null && cachedIntentions.getEditor() == editor) {
      hide();
    }
  }

  public void invalidate() {
    myCachedIntentions.set(null);
    hide();
  }

  public abstract void update(CachedIntentions cachedIntentions, boolean actionsChanged);

  /**
   * The floating code toolbar offers the intentions itself while it is shown, so the hint is left out.
   */
  @RequiredUIAccess
  protected static boolean isHintReplacedByFloatingToolbar(Editor editor) {
    CodeFloatingToolbar toolbar = CodeFloatingToolbar.getToolbar(editor);
    return toolbar != null && toolbar.isShown();
  }

  public abstract void hide();

  /**
   * The hint of the intentions - one button in the editor, which shows them when it is pressed. Only offers them;
   * what it is made of is the frontend's own.
   */
  @RequiredUIAccess
  public abstract void showHint(PsiFile file, Editor editor, CachedIntentions cachedIntentions);

  /**
   * The intentions themselves, listed for the user to choose from - what the hint opens, and what asking for them
   * shows straight away.
   */
  @RequiredUIAccess
  public abstract void showPopup(PsiFile file, Editor editor, CachedIntentions cachedIntentions);
}
