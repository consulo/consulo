// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.breadcrumbs;

import com.intellij.ide.ui.UISettings;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

abstract class ToggleBreadcrumbsAction extends ToggleAction implements DumbAware {

  static final class ShowHide extends ToggleBreadcrumbsAction {
    @Override
    boolean isEnabled(AnActionEvent event) {
      return findEditor(event) != null && super.isEnabled(event);
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent event, boolean selected) {
      Editor editor = findEditor(event);
      if (editor != null && BreadcrumbsForceShownSettings.setForcedShown(selected, editor)) {
        UISettings.getInstance().fireUISettingsChanged();
      }
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent event) {
    super.update(event);
    boolean enabled = isEnabled(event);
    event.getPresentation().setEnabledAndVisible(enabled);
  }

  boolean isEnabled(AnActionEvent event) {
    PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
    if (psiFile == null) return true;
    FileViewProvider provider = psiFile.getViewProvider();
    return BreadcrumbsUtilEx.findProvider(false, provider) != null;
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent event) {
    EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
    boolean shown = settings.isBreadcrumbsShown();
    Editor editor = findEditor(event);
    if (editor == null) return shown;

    Boolean forcedShown = BreadcrumbsForceShownSettings.getForcedShown(editor);
    if (forcedShown != null) return forcedShown;
    if (!shown) return false;

    String languageID = findLanguageID(event);
    return languageID == null || settings.isBreadcrumbsShownFor(languageID);
  }

  @Contract("null -> null")
  @Nullable
  static Editor findEditor(@Nullable AnActionEvent event) {
    return event == null ? null : event.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE);
  }

  @Contract("null -> null")
  @Nullable
  static String findLanguageID(@Nullable AnActionEvent event) {
    if (event == null) return null;

    PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
    if (psiFile == null) return null;

    Language baseLanguage = psiFile.getViewProvider().getBaseLanguage();
    return BreadcrumbsUtilEx.findLanguageWithBreadcrumbSettings(baseLanguage);
  }
}
