// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.wm.impl.status;

import consulo.language.editor.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.HectorComponent;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSettingListener;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import consulo.application.AllIcons;
import consulo.application.PowerSaveMode;
import consulo.application.ApplicationManager;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.inspection.scheme.event.ProfileChangeAdapter;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.ui.ex.UIBundle;
import com.intellij.util.Consumer;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TogglePopupHintsPanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, StatusBarWidget.IconPresentation {
  public static final String ID = "InspectionProfile";

  private Image myCurrentIcon;
  private String myToolTipText;

  public TogglePopupHintsPanel(@Nonnull Project project) {
    super(project);
    myCurrentIcon = ImageEffects.grayed(AllIcons.Ide.HectorOff);
  }

  @Override
  public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
    updateStatus();
  }


  @Override
  public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
    updateStatus();
  }

  @Override
  public StatusBarWidget copy() {
    return new TogglePopupHintsPanel(getProject());
  }

  @Override
  @Nullable
  public Image getIcon() {
    return myCurrentIcon;
  }

  @Override
  public String getTooltipText() {
    return myToolTipText;
  }

  @Override
  public Consumer<MouseEvent> getClickConsumer() {
    return e -> {
      final PsiFile file = getCurrentFile();
      if (file != null) {
        if (!DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file)) return;
        final HectorComponent component = new HectorComponent(file);
        component.showComponent(e.getComponent(), d -> new Point(-d.width, -d.height));
      }
    };
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    super.install(statusBar);
    myConnection.subscribe(PowerSaveMode.TOPIC, this::updateStatus);
    myConnection.subscribe(ProfileChangeAdapter.TOPIC, new ProfileChangeAdapter() {
      @Override
      public void profilesInitialized() {
        updateStatus();
      }

      @Override
      public void profileActivated(@Nullable Profile oldProfile, @Nullable Profile profile) {
        updateStatus();
      }

      @Override
      public void profileChanged(@Nullable Profile profile) {
        updateStatus();
      }
    });

    myConnection.subscribe(FileHighlightingSettingListener.SETTING_CHANGE, (r, s) -> updateStatus());
  }

  @Override
  @Nonnull
  public String ID() {
    return "InspectionProfile";
  }

  @Override
  public WidgetPresentation getPresentation() {
    return this;
  }

  public void clear() {
    myCurrentIcon = ImageEffects.grayed(AllIcons.Ide.HectorOff);
    myToolTipText = null;
    myStatusBar.updateWidget(ID());
  }

  public void updateStatus() {
    UIUtil.invokeLaterIfNeeded(() -> updateStatus(getCurrentFile()));
  }


  private void updateStatus(PsiFile file) {
    if (isDisposed()) return;
    if (isStateChangeable(file)) {
      if (PowerSaveMode.isEnabled()) {
        myCurrentIcon = ImageEffects.grayed(AllIcons.Ide.HectorOff);
        myToolTipText = "Code analysis is disabled in power save mode.\n";
      }
      else if (HighlightingLevelManager.getInstance(getProject()).shouldInspect(file)) {
        myCurrentIcon = AllIcons.Ide.HectorOn;
        InspectionProfileImpl profile = InspectionProjectProfileManager.getInstance(file.getProject()).getCurrentProfile();
        if (profile.wasInitialized()) myToolTipText = "Current inspection profile: " + profile.getName() + ".\n";
      }
      else if (HighlightingLevelManager.getInstance(getProject()).shouldHighlight(file)) {
        myCurrentIcon = AllIcons.Ide.HectorSyntax;
        myToolTipText = "Highlighting level is: Syntax.\n";
      }
      else {
        myCurrentIcon = AllIcons.Ide.HectorOff;
        myToolTipText = "Inspections are off.\n";
      }
      myToolTipText += UIBundle.message("popup.hints.panel.click.to.configure.highlighting.tooltip.text");
    }
    else {
      myCurrentIcon = file != null ? ImageEffects.grayed(AllIcons.Ide.HectorOff) : null;
      myToolTipText = null;
    }

    if (!ApplicationManager.getApplication().isUnitTestMode() && myStatusBar != null) {
      myStatusBar.updateWidget(ID());
    }
  }

  private static boolean isStateChangeable(PsiFile file) {
    return file != null && DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file);
  }

  @Nullable
  private PsiFile getCurrentFile() {
    VirtualFile virtualFile = getSelectedFile();
    if (virtualFile != null && virtualFile.isValid()) {
      return PsiManager.getInstance(getProject()).findFile(virtualFile);
    }
    return null;
  }
}
