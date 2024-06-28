/*
 * Copyright 2013-2020 consulo.io
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
package consulo.language.editor.impl.internal.daemon;

import consulo.dataContext.DataContext;
import consulo.language.Language;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.FileHighlightingSetting;
import consulo.language.editor.highlight.HighlightLevelUtil;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.impl.internal.markup.InspectionsLevel;
import consulo.language.editor.localize.DaemonLocalize;
import consulo.language.file.FileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiFile;
import consulo.module.content.ProjectFileIndex;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * from kotlin
 */
public class ConfigureHighlightingLevel {
  private static class LevelAction extends ToggleAction {

    private final InspectionsLevel level;
    private final FileViewProvider provider;
    private final Language language;

    private LevelAction(InspectionsLevel level, FileViewProvider provider, Language language) {
      this.level = level;
      this.provider = provider;
      this.language = language;
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      PsiFile file = provider.getPsi(language);
      if (file == null) {
        return false;
      }

      HighlightingLevelManager manager = HighlightingLevelManager.getInstance(file.getProject());

      InspectionsLevel target;

      if (manager.shouldInspect(file)) {
        target = InspectionsLevel.ALL;
      }
      else if (manager.shouldHighlight(file)) {
        target = InspectionsLevel.ERRORS;
      }
      else {
        target = InspectionsLevel.NONE;
      }
      return target == level;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      if (!state) return;
      PsiFile file = provider.getPsi(language);
      if (file == null) {
        return;
      }

      FileHighlightingSetting target;

      switch (level) {
        case NONE:
          target = FileHighlightingSetting.SKIP_HIGHLIGHTING;
          break;
        case ERRORS:
          target = FileHighlightingSetting.SKIP_INSPECTION;
          break;
        case ALL:
          target = FileHighlightingSetting.FORCE_HIGHLIGHTING;
          break;
        default:
          throw new UnsupportedOperationException();
      }

      HighlightLevelUtil.forceRootHighlighting(file, target);

      InjectedLanguageManager.getInstance(file.getProject()).dropFileCaches(file);
      DaemonCodeAnalyzer.getInstance(file.getProject()).restart();
    }
  }

  @Nullable
  public static JBPopup getConfigureHighlightingLevelPopup(DataContext context) {
    PsiFile psi = context.getData(PsiFile.KEY);
    if (psi == null) {
      return null;
    }

    if (!psi.isValid() || psi.getProject().isDisposed()) return null;

    FileViewProvider provider = psi.getViewProvider();

    Set<Language> languages = provider.getLanguages();

    if (languages.isEmpty()) return null;

    VirtualFile file = psi.getVirtualFile();
    if (file == null) {
      return null;
    }

    ProjectFileIndex index = ProjectFileIndex.getInstance(psi.getProject());

    boolean isAllInspectionsEnabled = index.isInContent(file) || !index.isInLibrary(file);
    boolean isSeparatorNeeded = languages.size() > 1;

    DefaultActionGroup group = new DefaultActionGroup();

    languages.stream().sorted((o1, o2) -> o1.getDisplayName().compareTo(o2.getDisplayName())).forEach(it -> {
      if (isSeparatorNeeded) {
        group.add(AnSeparator.create(it.getDisplayName()));
      }
      group.add(new LevelAction(InspectionsLevel.NONE, provider, it));
      group.add(new LevelAction(InspectionsLevel.ERRORS, provider, it));
      if (isAllInspectionsEnabled) {
        group.add(new LevelAction(InspectionsLevel.ALL, provider, it));
      }
    });

    group.add(AnSeparator.create());
    group.add(new ConfigureInspectionsAction());

    return JBPopupFactory.getInstance().createActionGroupPopup(
      DaemonLocalize.popupTitleConfigureHighlightingLevel(psi.getVirtualFile().getPresentableName()).get(),
      group,
      context,
      true,
      null,
      100
    );
  }
}
