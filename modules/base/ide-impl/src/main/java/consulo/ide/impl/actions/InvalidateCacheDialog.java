/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.actions;

import consulo.application.Application;
import consulo.application.CachesInvalidator;
import consulo.application.localize.ApplicationLocalize;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.gist.GistManager;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2021-03-20
 */
public class InvalidateCacheDialog extends DialogWrapper {
    @Nonnull
    private final Application myApplication;
    private Map<CachesInvalidator, Boolean> myStates = new LinkedHashMap<>();

    private Action myJustRestartAction;

    public InvalidateCacheDialog(@Nonnull Application application, @Nullable Project project) {
        super(project);
        myApplication = application;

        boolean restartCapable = application.isRestartCapable();

        setTitle(restartCapable ? ApplicationLocalize.dialogTitleInvalidateCachesAndRestart() : ApplicationLocalize.dialogTitleInvalidateCaches());

        setOKButtonText(restartCapable ? ApplicationLocalize.buttonInvalidateAndRestart() : ApplicationLocalize.buttonInvalidate());
        setOKButtonIcon(TargetAWT.to(PlatformIconGroup.generalWarning()));

        if (restartCapable) {
            myJustRestartAction = new DialogWrapperAction(ApplicationLocalize.buttonJustRestart()) {
                @Override
                protected void doAction(ActionEvent e) {
                    close(OK_EXIT_CODE);

                    myApplication.restart(true);
                }
            };
        }

        init();
    }

    @Nullable
    @Override
    @RequiredUIAccess
    protected JComponent createCenterPanel() {
        VerticalLayout root = VerticalLayout.create();
        root.add(HorizontalLayout.create().add(Label.create(ApplicationLocalize.dialogMessageCachesWillBeInvalidated())));

        VerticalLayout optionalLayout = VerticalLayout.create();
        root.add(LabeledLayout.create(ApplicationLocalize.dialogMessageTheFollowingItems(), optionalLayout));

        myApplication.getExtensionPoint(CachesInvalidator.class).forEachExtensionSafe(invalidator -> {
            CheckBox checkBox = CheckBox.create(invalidator.getDescription());
            checkBox.setValue(invalidator.isEnabledByDefault());
            checkBox.addValueListener(event -> myStates.put(invalidator, event.getValue()));

            myStates.put(invalidator, checkBox.getValueOrError());

            optionalLayout.add(checkBox);
        });
        return (JComponent) TargetAWT.to(root);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();

        FileBasedIndex.getInstance().invalidateCaches();
        GistManager.getInstance().invalidateData();

        for (Map.Entry<CachesInvalidator, Boolean> entry : myStates.entrySet()) {
            if (entry.getValue()) {
                entry.getKey().invalidateCaches();
            }
        }

        myApplication.restart(true);
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        Action[] actions = super.createActions();
        if (myJustRestartAction == null) {
            return actions;
        }

        if (Platform.current().os().isMac()) {
            return ArrayUtil.prepend(myJustRestartAction, actions);
        }
        else {
            return ArrayUtil.append(actions, myJustRestartAction);
        }
    }

    @Nullable
    @Override
    protected String getHelpId() {
        return "platform/dialogs/invalidate_caches/";
    }
}
