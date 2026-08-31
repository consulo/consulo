/*
 * Copyright 2013-2021 consulo.io
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
package consulo.web.editor.impl.internal.intention;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.internal.intention.IntentionsUI;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.editor.impl.internal.intention.WebIntentionHintComponent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 17/08/2021
 */
@ServiceImpl
@Singleton
public class WebIntentionsUIImpl extends IntentionsUI {
  private volatile @Nullable WebIntentionHintComponent myLastHint;

  @Inject
  public WebIntentionsUIImpl(Project project) {
    super(project);
  }

  @Override
  @RequiredUIAccess
  public void update(CachedIntentions cachedIntentions, boolean actionsChanged) {
    Editor editor = cachedIntentions.getEditor();
    if (editor == null || !actionsChanged) {
      return;
    }

    hide();

    if (editor.getSettings().isShowIntentionBulb()
      && editor.getCaretModel().getCaretCount() == 1
      && cachedIntentions.showBulb()) {
      showHint(cachedIntentions.getFile(), editor, cachedIntentions);
    }
  }

  @Override
  @RequiredUIAccess
  public void hide() {
    WebIntentionHintComponent hint = myLastHint;
    myLastHint = null;

    if (hint != null) {
      hint.dispose();
    }
  }

  @Override
  @RequiredUIAccess
  public void showHint(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
    WebIntentionHintComponent hint = new WebIntentionHintComponent(file, editor, cachedIntentions);
    myLastHint = hint;

    hint.showHint();
  }

  @Override
  @RequiredUIAccess
  public void showPopup(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
    hide();

    WebIntentionHintComponent hint = new WebIntentionHintComponent(file, editor, cachedIntentions);
    myLastHint = hint;

    hint.showPopup();
  }
}
