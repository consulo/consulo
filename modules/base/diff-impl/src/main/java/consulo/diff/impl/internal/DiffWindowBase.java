/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal;

import consulo.application.Application;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.DiffWindowMode;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import org.jspecify.annotations.Nullable;

public abstract class DiffWindowBase {
    private static final WidthAndHeight DEFAULT_SIZE = WidthAndHeight.ofFont(60, 42);

    protected final @Nullable Project myProject;

    protected final DiffDialogHints myHints;

    private @Nullable DiffRequestProcessor myProcessor;
    private @Nullable Dialog myDialog;
    private DiffWindowMode myMode = DiffWindowMode.FRAME;
    private String myTitle = "Diff";

    public DiffWindowBase(@Nullable Project project, DiffDialogHints hints) {
        myProject = project;
        myHints = hints;
    }

    @RequiredUIAccess
    protected void init() {
        if (myDialog != null) {
            return;
        }

        DiffRequestProcessor processor = createProcessor();
        myProcessor = processor;
        myMode = UnifiedDiffUtil.getWindowMode(myHints);

        String dialogGroupKey = processor.getContextUserData(DiffUserDataKeys.DIALOG_GROUP_KEY);
        String dimensionKey = dialogGroupKey == null ? "DiffContextDialog" : dialogGroupKey;

        DialogDescriptor descriptor = new DialogDescriptor(LocalizeValue.of(myTitle)) {
            @Override
            @RequiredUIAccess
            public Component createCenterComponent(Disposable uiDisposable) {
                Disposer.register(uiDisposable, processor);
                return processor.getComponent();
            }

            @Override
            public AnAction[] createActions(boolean inverseOrder) {
                return AnAction.EMPTY_ARRAY;
            }

            @Override
            public boolean isModal() {
                return myMode == DiffWindowMode.MODAL;
            }

            @Override
            public @Nullable String getDimensionServiceKey() {
                return dimensionKey;
            }

            @Override
            public @Nullable WidthAndHeight getInitialSize() {
                return DEFAULT_SIZE;
            }

            @Override
            public boolean hasDefaultContentBorder() {
                return false;
            }

            @Override
            @RequiredUIAccess
            public @Nullable Component getPreferredFocusedComponent() {
                return processor.getPreferredFocusedComponent();
            }
        };

        DialogService dialogService = Application.get().getInstance(DialogService.class);
        Component parent = myHints.getParent();
        myDialog = parent != null ? dialogService.build(parent, descriptor) : dialogService.build(descriptor);

        new DumbAwareAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
                close();
            }
        }.registerCustomShortcutSet(CommonShortcuts.getCloseActiveWindow(), processor.getComponent());
    }

    @RequiredUIAccess
    public void show() {
        init();

        DiffRequestProcessor processor = myProcessor;
        Dialog dialog = myDialog;
        if (processor == null || dialog == null) {
            return;
        }

        dialog.showAsync();
        UIAccess.current().give(() -> {
            processor.updateRequest();
            processor.requestFocus();
        });
    }

    @RequiredUIAccess
    protected abstract DiffRequestProcessor createProcessor();

    //
    // Delegate
    //

    @RequiredUIAccess
    protected void setWindowTitle(String title) {
        myTitle = title;
        if (myDialog != null) {
            myDialog.getWindow().setTitle(title);
        }
    }

    @RequiredUIAccess
    protected void onAfterNavigate() {
        if (myMode == DiffWindowMode.MODAL) {
            close();
        }
    }

    @RequiredUIAccess
    protected void close() {
        if (myDialog != null) {
            myDialog.doCancelAction();
        }
    }

    //
    // Getters
    //

    protected @Nullable DiffRequestProcessor getProcessor() {
        return myProcessor;
    }
}
