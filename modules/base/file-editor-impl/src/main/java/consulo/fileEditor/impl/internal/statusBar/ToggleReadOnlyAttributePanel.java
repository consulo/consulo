// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal.statusBar;

import consulo.application.WriteAction;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.statusBar.StatusBarUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.BulkVirtualFileListenerAdapter;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;
import consulo.virtualFileSystem.util.ReadOnlyAttributeUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public final class ToggleReadOnlyAttributePanel implements StatusBarWidget.Multiframe, StatusBarWidget.IconPresentation {
    private final StatusBarWidgetFactory myFactory;
    private StatusBar myStatusBar;

    public ToggleReadOnlyAttributePanel(StatusBarWidgetFactory factory) {
        myFactory = factory;
    }

    @Override
    public String getId() {
        return myFactory.getId();
    }

    @Override
    public @Nullable Image getIcon() {
        if (!isReadonlyApplicable()) {
            return null;
        }
        VirtualFile virtualFile = getCurrentFile();
        return virtualFile == null || virtualFile.isWritable() ? PlatformIconGroup.ideReadwrite() : PlatformIconGroup.ideReadonly();
    }

    @Override
    public StatusBarWidget copy() {
        return new ToggleReadOnlyAttributePanel(myFactory);
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void dispose() {
        myStatusBar = null;
    }

    @Override
    public void install(StatusBar statusBar) {
        myStatusBar = statusBar;
        Project project = statusBar.getProject();
        if (project == null) {
            return;
        }

        project.getMessageBus().connect(this).subscribe(
            FileEditorManagerListener.class,
            new FileEditorManagerListener() {
                @Override
                public void selectionChanged(FileEditorManagerEvent event) {
                    if (myStatusBar != null) {
                        myStatusBar.updateWidget(getId());
                    }
                }
            }
        );

        project.getApplication().getMessageBus().connect(this).subscribe(
            BulkFileListener.class,
            new BulkVirtualFileListenerAdapter(new VirtualFileListener() {
                @Override
                public void propertyChanged(VirtualFilePropertyEvent event) {
                    if (VirtualFile.PROP_WRITABLE.equals(event.getPropertyName()) && myStatusBar != null) {
                        myStatusBar.updateWidget(getId());
                    }
                }
            })
        );
    }

    @Override
    public LocalizeValue getTooltipText() {
        VirtualFile virtualFile = getCurrentFile();
        int writable = virtualFile == null || virtualFile.isWritable() ? 1 : 0;
        int readonly = writable == 1 ? 0 : 1;
        return ActionLocalize.actionTogglereadonlyattributeFiles(readonly, 1);
    }

    @Override
    public ComponentEventListener<Component, ClickEvent> getClickListener() {
        return event -> {
            VirtualFile file = getCurrentFile();
            if (!isReadOnlyApplicableForFile(file)) {
                return;
            }
            FileDocumentManager.getInstance().saveAllDocuments();

            try {
                WriteAction.run(() -> ReadOnlyAttributeUtil.setReadOnlyAttribute(file, file.isWritable()));
                myStatusBar.updateWidget(getId());
            }
            catch (IOException e) {
                MessageBoxes.okError(e).title(UILocalize.errorDialogTitle()).showAsync(event.getComponent());
            }
        };
    }

    private boolean isReadonlyApplicable() {
        VirtualFile file = getCurrentFile();
        return isReadOnlyApplicableForFile(file);
    }

    private static boolean isReadOnlyApplicableForFile(@Nullable VirtualFile file) {
        return file != null && !file.getFileSystem().isReadOnly();
    }

    private @Nullable Project getProject() {
        return myStatusBar != null ? myStatusBar.getProject() : null;
    }

    private @Nullable VirtualFile getCurrentFile() {
        FileEditor fileEditor = StatusBarUtil.getCurrentFileEditor(myStatusBar);
        return fileEditor == null ? null : fileEditor.getFile();
    }
}
