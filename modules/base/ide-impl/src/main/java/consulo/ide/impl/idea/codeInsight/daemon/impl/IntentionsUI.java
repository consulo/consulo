// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.impl.idea.codeInsight.intention.impl.CachedIntentions;
import consulo.ide.ServiceManager;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class IntentionsUI {
  private final Project myProject;

  public static IntentionsUI getInstance(Project project) {
    return ServiceManager.getService(project, IntentionsUI.class);
  }

  public IntentionsUI(Project project) {
    myProject = project;
  }

  private final AtomicReference<CachedIntentions> myCachedIntentions = new AtomicReference<>();

  @Nonnull
  public CachedIntentions getCachedIntentions(@Nullable Editor editor, @Nonnull PsiFile file) {
    return myCachedIntentions.updateAndGet(cachedIntentions -> {
      if (cachedIntentions != null && editor == cachedIntentions.getEditor() && file == cachedIntentions.getFile()) {
        return cachedIntentions;
      }
      else {
        return new CachedIntentions(myProject, file, editor);
      }
    });

  }

  public void invalidate() {
    myCachedIntentions.set(null);
    hide();
  }

  public abstract Object getLastIntentionHint();

  public abstract void update(@Nonnull CachedIntentions cachedIntentions, boolean actionsChanged);

  public abstract void hide();
}
