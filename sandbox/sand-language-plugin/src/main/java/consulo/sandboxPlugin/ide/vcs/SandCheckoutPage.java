/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ide.vcs;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.ui.MessageBoxes;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.checkout.CheckoutCallback;
import consulo.versionControlSystem.checkout.CheckoutPage;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-25
 */
public class SandCheckoutPage implements CheckoutPage {
    private final Project myProject;
    private final Disposable myUiDisposable;

    private @Nullable TextBox myUrlBox;
    private FileChooserTextBoxBuilder.@Nullable Controller myDirectoryController;

    public SandCheckoutPage(Project project, Disposable uiDisposable) {
        myProject = project;
        myUiDisposable = uiDisposable;
    }

    @Override
    @RequiredUIAccess
    public Component createComponent(Context context) {
        TextBox urlBox = TextBox.create();

        FileChooserTextBoxBuilder directoryBuilder = FileChooserTextBoxBuilder.create(myProject);
        directoryBuilder.uiDisposable(myUiDisposable);
        directoryBuilder.fileChooserDescriptor(new FileChooserDescriptor(false, true, false, false, false, false));
        directoryBuilder.dialogTitle(LocalizeValue.localizeTODO("Select Target Directory"));

        FileChooserTextBoxBuilder.Controller directoryController = directoryBuilder.build();
        directoryController.setValue(ProjectUtil.getProjectsDirectory().toString());

        myUrlBox = urlBox;
        myDirectoryController = directoryController;

        urlBox.addValueListener(event -> context.setCheckoutEnabled(isFilled()));
        directoryController.getComponent().addValueListener(event -> context.setCheckoutEnabled(isFilled()));

        context.setCheckoutEnabled(isFilled());

        return FormBuilder.create()
            .addLabeled(LocalizeValue.localizeTODO("Repository URL:"), urlBox)
            .addLabeled(LocalizeValue.localizeTODO("Target directory:"), directoryController.getComponent())
            .build();
    }

    @RequiredUIAccess
    private boolean isFilled() {
        return !StringUtil.isEmptyOrSpaces(getUrl()) && !StringUtil.isEmptyOrSpaces(getDirectory());
    }

    @RequiredUIAccess
    private String getUrl() {
        return myUrlBox == null ? "" : StringUtil.notNullize(myUrlBox.getValue());
    }

    @RequiredUIAccess
    private String getDirectory() {
        return myDirectoryController == null ? "" : myDirectoryController.getValue();
    }

    @Override
    @RequiredUIAccess
    public void doCheckout(CheckoutCallback listener) {
        MessageBoxes.okInfo(LocalizeValue.localizeTODO("Sand clone of '" + getUrl() + "' into '" + getDirectory() + "'")).showAsync();

        listener.checkoutCompleted();
    }
}
