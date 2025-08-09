// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.PowerSaveMode;
import consulo.application.PowerSaveModeListener;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.statusBar.EditorBasedWidget;
import consulo.ide.impl.idea.codeInsight.daemon.impl.HectorComponent;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.highlight.FileHighlightingSettingListener;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.inspection.scheme.event.ProfileChangeAdapter;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class TogglePopupHintsPanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, StatusBarWidget.IconPresentation {
    private Image myCurrentIcon;
    private String myToolTipText;

    public TogglePopupHintsPanel(@Nonnull Project project, @Nonnull StatusBarWidgetFactory factory) {
        super(project, factory);
        myCurrentIcon = ImageEffects.grayed(PlatformIconGroup.ideHectoroff());
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
        return new TogglePopupHintsPanel(getProject(), myFactory);
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
            PsiFile file = getCurrentFile();
            if (file != null) {
                if (!DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file)) {
                    return;
                }
                HectorComponent component = new HectorComponent(file);
                component.showComponent(e.getComponent(), d -> new Point(-d.width, -d.height));
            }
        };
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {
        super.install(statusBar);
        myConnection.subscribe(PowerSaveModeListener.class, this::updateStatus);
        myConnection.subscribe(ProfileChangeAdapter.class, new ProfileChangeAdapter() {
            @Override
            public void profileActivated(@Nullable Profile oldProfile, @Nullable Profile profile) {
                updateStatus();
            }

            @Override
            public void profileChanged(@Nullable Profile profile) {
                updateStatus();
            }
        });

        myConnection.subscribe(FileHighlightingSettingListener.class, (r, s) -> updateStatus());
    }


    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    public void clear() {
        myCurrentIcon = ImageEffects.grayed(PlatformIconGroup.ideHectoroff());
        myToolTipText = null;
        myStatusBar.updateWidget(getId());
    }

    public void updateStatus() {
        UIUtil.invokeLaterIfNeeded(() -> updateStatus(getCurrentFile()));
    }


    private void updateStatus(PsiFile file) {
        if (isDisposed()) {
            return;
        }
        if (isStateChangeable(file)) {
            if (PowerSaveMode.isEnabled()) {
                myCurrentIcon = ImageEffects.grayed(PlatformIconGroup.ideHectoroff());
                myToolTipText = "Code analysis is disabled in power save mode.\n";
            }
            else if (HighlightingLevelManager.getInstance(getProject()).shouldInspect(file)) {
                myCurrentIcon = PlatformIconGroup.ideHectoron();
                InspectionProfileImpl profile = InspectionProjectProfileManager.getInstance(file.getProject()).getCurrentProfile();
                if (profile.wasInitialized()) {
                    myToolTipText = "Current inspection profile: " + profile.getName() + ".\n";
                }
            }
            else if (HighlightingLevelManager.getInstance(getProject()).shouldHighlight(file)) {
                myCurrentIcon = PlatformIconGroup.ideHectorsyntax();
                myToolTipText = "Highlighting level is: Syntax.\n";
            }
            else {
                myCurrentIcon = PlatformIconGroup.ideHectoroff();
                myToolTipText = "Inspections are off.\n";
            }
            myToolTipText += UILocalize.popupHintsPanelClickToConfigureHighlightingTooltipText();
        }
        else {
            myCurrentIcon = file != null ? ImageEffects.grayed(PlatformIconGroup.ideHectoroff()) : null;
            myToolTipText = null;
        }

        if (!myProject.getApplication().isUnitTestMode() && myStatusBar != null) {
            myStatusBar.updateWidget(getId());
        }
    }

    private static boolean isStateChangeable(PsiFile file) {
        return file != null && DaemonCodeAnalyzer.getInstance(file.getProject()).isHighlightingAvailable(file);
    }

    @Nullable
    @RequiredReadAction
    private PsiFile getCurrentFile() {
        VirtualFile virtualFile = getSelectedFile();
        if (virtualFile != null && virtualFile.isValid()) {
            return PsiManager.getInstance(getProject()).findFile(virtualFile);
        }
        return null;
    }
}
