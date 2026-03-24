// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.annotate;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Eagerly instantiates {@link AnnotationsPreloader} at project startup so its
 * {@code FileEditorManagerListener} is registered before file-selection events are processed.
 *
 * <p>This mirrors the JetBrains pattern where {@code AnnotationsPreloader} registers
 * an inner {@code AnnotationsPreloaderFileEditorManagerListener} via {@code ProjectListeners}
 * in {@code VcsExtensions.xml}, ensuring the preloader is active from project open.
 *
 * <p>We also schedule preloading for files that are already open at startup, because
 * their {@code selectionChanged} events fired before this activity ran.
 */
@ExtensionImpl
public class AnnotationsPreloaderStartupActivity implements PostStartupActivity, DumbAware {
    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        // Instantiate AnnotationsPreloader (lazy service) so its FileEditorManagerListener
        // is registered and future file-switch events trigger preloading automatically.
        AnnotationsPreloader preloader = project.getInstance(AnnotationsPreloader.class);

        // Preload annotations for files that are already open — their selectionChanged
        // events have already fired, so the listener above would have missed them.
        for (VirtualFile file : FileEditorManager.getInstance(project).getOpenFiles()) {
            preloader.schedulePreloading(file);
        }
    }
}
