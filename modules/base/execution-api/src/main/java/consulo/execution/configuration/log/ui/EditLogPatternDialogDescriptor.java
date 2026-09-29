/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.execution.configuration.log.ui;

import consulo.disposer.Disposable;
import consulo.execution.localize.ExecutionLocalize;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

final class EditLogPatternDialogDescriptor extends DialogDescriptor {
    private String myName;
    private String myPattern;
    private boolean myShowAll;

    private @Nullable TextBox myNameField;

    EditLogPatternDialogDescriptor(String name, String pattern, boolean showAll) {
        super(ExecutionLocalize.logMonitorEditAliasesTitle());
        myName = name;
        myPattern = pattern;
        myShowAll = showAll;
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        TextBox nameField = TextBox.create(myName);
        nameField.addValueListener(event -> myName = StringUtil.notNullize(event.getValue()));
        myNameField = nameField;

        FileChooserTextBoxBuilder patternBuilder = FileChooserTextBoxBuilder.create(null);
        patternBuilder.dialogTitle(UILocalize.fileChooserDefaultTitle());
        patternBuilder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor());
        FileChooserTextBoxBuilder.Controller patternField = patternBuilder.build();
        patternField.setValue(myPattern, false);
        patternField.getComponent().addValueListener(event -> {
            myPattern = StringUtil.notNullize(event.getValue());
            updateOkButtonState();
        });

        CheckBox showAllBox = CheckBox.create(ExecutionLocalize.logMonitorEditAliasesShowAllCheckboxTitle(), myShowAll);
        showAllBox.addValueListener(event -> myShowAll = Boolean.TRUE.equals(event.getValue()));

        FormBuilder builder = FormBuilder.create();
        builder.addLabeled(ExecutionLocalize.logMonitorEditAliasesName(), nameField);
        builder.addLabeled(ExecutionLocalize.logMonitorEditAliasesLocation(), patternField.getComponent());
        builder.addBottom(showAllBox);
        return builder.build();
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myNameField;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return !myPattern.isEmpty();
    }

    @Override
    public @Nullable String getHelpId() {
        return "reference.run.configuration.edit.logfile.aliases";
    }

    boolean isShowAllFiles() {
        return myShowAll;
    }

    String getName() {
        return myName.isEmpty() ? myPattern : myName;
    }

    String getLogPattern() {
        return myPattern;
    }
}
