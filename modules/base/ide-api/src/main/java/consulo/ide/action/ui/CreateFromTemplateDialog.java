/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.action.ui;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.disposer.Disposer;
import consulo.fileTemplate.*;
import consulo.ide.action.CreateFileAction;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Properties;

public class CreateFromTemplateDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(CreateFromTemplateDialog.class);
    @Nonnull
    private final PsiDirectory myDirectory;
    @Nonnull
    private final Project myProject;
    private PsiElement myCreatedElement;
    private final CreateFromTemplatePanel myAttrPanel;
    private final JComponent myAttrComponent;
    @Nonnull
    private final FileTemplate myTemplate;
    private final Map<String, Object> myDefaultProperties;

    @Deprecated
    @DeprecationInfo("Use constructor with Map parameter instead of Properties")
    @RequiredUIAccess
    public CreateFromTemplateDialog(
        @Nonnull Project project,
        @Nonnull PsiDirectory directory,
        @Nonnull FileTemplate template,
        @Nullable final AttributesDefaults attributesDefaults,
        @Nullable final Properties defaultProperties
    ) {
        this(directory, template, attributesDefaults, defaultProperties == null ? null : FileTemplateUtil.convert2Map(defaultProperties));
    }

    @RequiredUIAccess
    public CreateFromTemplateDialog(
        @Nonnull PsiDirectory directory,
        @Nonnull FileTemplate template,
        @Nullable final AttributesDefaults attributesDefaults,
        @Nullable final Map<String, Object> defaultProperties
    ) {
        super(directory.getProject(), true);
        myDirectory = directory;
        myProject = directory.getProject();
        myTemplate = template;
        setTitle(IdeLocalize.titleNewFromTemplate(template.getName()));

        myDefaultProperties =
            defaultProperties == null ? FileTemplateManager.getInstance(myProject).getDefaultVariables() : defaultProperties;
        FileTemplateUtil.fillDefaultProperties(myDefaultProperties, directory);
        boolean mustEnterName = FileTemplateUtil.findHandler(template).isNameRequired();
        if (attributesDefaults != null && attributesDefaults.isFixedName()) {
            myDefaultProperties.put(FileTemplate.ATTRIBUTE_NAME, attributesDefaults.getDefaultFileName());
            mustEnterName = false;
        }

        String[] unsetAttributes = null;
        try {
            unsetAttributes = myTemplate.getUnsetAttributes(myDefaultProperties, myProject);
        }
        catch (FileTemplateParseException e) {
            showErrorDialog(e);
        }

        if (unsetAttributes != null) {
            myAttrPanel = new CreateFromTemplatePanel(unsetAttributes, mustEnterName, attributesDefaults);
            myAttrComponent = myAttrPanel.getComponent();
            init();
        }
        else {
            myAttrPanel = null;
            myAttrComponent = null;
        }
    }

    @RequiredUIAccess
    public PsiElement create() {
        if (Application.get().isUnitTestMode()) {
            doCreate(myTemplate.getName() + "." + myTemplate.getExtension());
            Disposer.dispose(getDisposable());
            return myCreatedElement;
        }
        if (myAttrPanel != null) {
            if (myAttrPanel.hasSomethingToAsk()) {
                show();
                return myCreatedElement;
            }
            doCreate(null);
        }
        close(DialogWrapper.OK_EXIT_CODE);
        return myCreatedElement;
    }

    @Override
    @RequiredUIAccess
    protected void doOKAction() {
        String fileName = myAttrPanel.getFileName();
        if (fileName != null && fileName.length() == 0) {
            Messages.showMessageDialog(
                myAttrComponent,
                IdeLocalize.errorPleaseEnterAFileName().get(),
                CommonLocalize.titleError().get(),
                UIUtil.getErrorIcon()
            );
            return;
        }
        doCreate(fileName);
        if (myCreatedElement != null) {
            super.doOKAction();
        }
    }

    @RequiredUIAccess
    private void doCreate(@Nullable String fileName) {
        try {
            String newName = fileName;
            PsiDirectory directory = myDirectory;
            if (fileName != null) {
                final String finalFileName = fileName;
                CreateFileAction.MkDirs mkDirs = WriteAction.compute(() -> new CreateFileAction.MkDirs(finalFileName, myDirectory));
                newName = mkDirs.newName;
                directory = mkDirs.directory;
            }
            myCreatedElement =
                FileTemplateUtil.createFromTemplate(myTemplate, newName, myAttrPanel.getVariables(myDefaultProperties), directory);
        }
        catch (Exception e) {
            showErrorDialog(e);
        }
    }

    public Properties getEnteredProperties() {
        return myAttrPanel.getProperties(new Properties());
    }

    @RequiredUIAccess
    private void showErrorDialog(final Exception e) {
        LOG.info(e);
        Messages.showMessageDialog(myProject, filterMessage(e.getMessage()), getErrorMessage().get(), UIUtil.getErrorIcon());
    }

    private LocalizeValue getErrorMessage() {
        return FileTemplateUtil.findHandler(myTemplate).getErrorMessage();
    }

    @Nonnull
    private String filterMessage(String message) {
        if (message == null) {
            message = "unknown error";
        }

        String ioExceptionPrefix = "java.io.IOException:";
        if (message.startsWith(ioExceptionPrefix)) {
            return message.substring(ioExceptionPrefix.length());
        }
        if (message.contains("File already exists")) {
            return message;
        }

        return IdeLocalize.errorUnableToParseTemplateMessage(myTemplate.getName(), message).get();
    }

    @Override
    protected JComponent createCenterPanel() {
        myAttrPanel.ensureFitToScreen(200, 200);
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.add(
            myAttrComponent,
            new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0)
        );
        return centerPanel;
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myAttrComponent);
    }
}
