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
package consulo.diff.impl.internal.merge;

import consulo.application.Application;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.merge.MergeRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

public class MergeWindow {
    private static final WidthAndHeight DEFAULT_SIZE = WidthAndHeight.ofFont(60, 42);

    private final @Nullable Project myProject;

    private final MergeRequest myMergeRequest;

    private @Nullable Dialog myDialog;
    private boolean myCloseConfirmed;

    public MergeWindow(@Nullable Project project, MergeRequest mergeRequest) {
        myProject = project;
        myMergeRequest = mergeRequest;
    }

    @RequiredUIAccess
    public void show() {
        MergeRequestProcessor processor = new MergeRequestProcessor(myProject, myMergeRequest) {
            @Override
            @RequiredUIAccess
            public void closeDialog() {
                myCloseConfirmed = true;
                if (myDialog != null) {
                    myDialog.doCancelAction();
                }
            }

            @Override
            protected void setWindowTitle(LocalizeValue title) {
                if (myDialog != null) {
                    myDialog.getWindow().setTitle(title.get());
                }
            }
        };

        String dimensionKey = StringUtil.notNullize(processor.getContextUserData(DiffUserDataKeys.DIALOG_GROUP_KEY), "MergeDialog");

        DialogDescriptor descriptor = new DialogDescriptor(LocalizeValue.localizeTODO("Merge")) {
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
            public @Nullable String getHelpId() {
                return processor.getHelpId();
            }

            @Override
            @RequiredUIAccess
            public @Nullable Component getPreferredFocusedComponent() {
                return processor.getPreferredFocusedComponent();
            }

            @Override
            @RequiredUIAccess
            public boolean canHandle(AnAction action, @Nullable DialogValue value) {
                if (myCloseConfirmed) {
                    return true;
                }

                UIAccess uiAccess = UIAccess.current();
                processor.checkCloseAction().whenCompleteAsync((confirmed, error) -> {
                    if (Boolean.TRUE.equals(confirmed)) {
                        myCloseConfirmed = true;
                        if (myDialog != null) {
                            myDialog.doCancelAction();
                        }
                    }
                }, uiAccess);
                return false;
            }
        };

        DialogService dialogService = Application.get().getInstance(DialogService.class);
        myDialog = dialogService.build(descriptor);

        myDialog.showAsync();
        UIAccess.current().give(() -> {
            processor.init();
            processor.requestFocus();
        });
    }
}
