// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl.tooltips;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.impl.internal.hint.TooltipAction;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


/**
 * Provides actions for error tooltips
 *
 * @see consulo.ide.impl.idea.codeInsight.daemon.impl.DaemonTooltipActionProvider
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TooltipActionProvider {
  ExtensionPointName<TooltipActionProvider> EP = ExtensionPointName.create(TooltipActionProvider.class);

  String SHOW_FIXES_KEY = "tooltips.show.actions.in.key";
  boolean SHOW_FIXES_DEFAULT_VALUE = true;

  @Nullable
  TooltipAction getTooltipAction(@Nonnull final HighlightInfo info, @Nonnull Editor editor, @Nonnull PsiFile psiFile);


  @Nullable
  static TooltipAction calcTooltipAction(@Nonnull final HighlightInfo info, @Nonnull Editor editor) {
    if (!Registry.is("ide.tooltip.show.with.actions")) return null;

    Project project = editor.getProject();
    if (project == null) return null;

    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return null;

    for (TooltipActionProvider extension : EP.getExtensionList()) {
      TooltipAction action = extension.getTooltipAction(info, editor, file);
      if (action != null) return action;
    }

    return null;
  }

  static boolean isShowActions() {
    return ApplicationPropertiesComponent.getInstance().getBoolean(SHOW_FIXES_KEY, SHOW_FIXES_DEFAULT_VALUE);
  }

  static void setShowActions(boolean newValue) {
    ApplicationPropertiesComponent.getInstance().setValue(SHOW_FIXES_KEY, newValue, SHOW_FIXES_DEFAULT_VALUE);
  }
}
