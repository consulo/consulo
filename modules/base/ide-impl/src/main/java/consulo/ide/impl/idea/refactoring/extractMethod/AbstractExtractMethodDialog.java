/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.refactoring.extractMethod;

import consulo.ide.impl.idea.codeInsight.codeFragment.CodeFragment;
import consulo.ide.impl.idea.refactoring.ui.MethodSignatureComponent;
import consulo.ide.impl.idea.refactoring.util.SimpleParameterTablePanel;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.*;

public class AbstractExtractMethodDialog extends DialogWrapper implements ExtractMethodSettings {
  private JPanel myContentPane;
  private SimpleParameterTablePanel myParametersPanel;
  private JTextField myMethodNameTextField;
  private MethodSignatureComponent mySignaturePreviewTextArea;
  private JTextArea myOutputVariablesTextArea;
  private final Project myProject;
  private final String myDefaultName;
  private final ExtractMethodValidator myValidator;
  private final ExtractMethodDecorator myDecorator;

  private AbstractVariableData[] myVariableData;
  private Map<String, AbstractVariableData> myVariablesMap;

  private final List<String> myArguments;
  private final ArrayList<String> myOutputVariables;
  private final FileType myFileType;

  public AbstractExtractMethodDialog(final Project project,
                                     final String defaultName,
                                     final CodeFragment fragment,
                                     final ExtractMethodValidator validator,
                                     final ExtractMethodDecorator decorator,
                                     final FileType type) {
    super(project, true);
    myProject = project;
    myDefaultName = defaultName;
    myValidator = validator;
    myDecorator = decorator;
    myFileType = type;
    myArguments = new ArrayList<>(fragment.getInputVariables());
    Collections.sort(myArguments);
    myOutputVariables = new ArrayList<>(fragment.getOutputVariables());
    Collections.sort(myOutputVariables);
    setModal(true);
    setTitle(RefactoringLocalize.extractMethodTitle().get());
    init();
  }

  @Override
  protected void init() {
    super.init();
    // Set default name and select it
    myMethodNameTextField.setText(myDefaultName);
    myMethodNameTextField.setSelectionStart(0);
    myMethodNameTextField.setSelectionStart(myDefaultName.length());
    myMethodNameTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        updateOutputVariables();
        updateSignature();
        updateOkStatus();
      }
    });


    myVariableData = createVariableDataByNames(myArguments);
    myVariablesMap = createVariableMap(myVariableData);
    myParametersPanel.init(myVariableData);

    updateOutputVariables();
    updateSignature();
    updateOkStatus();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myMethodNameTextField;
  }

  public static AbstractVariableData[] createVariableDataByNames(final List<String> args) {
    final AbstractVariableData[] datas = new AbstractVariableData[args.size()];
    for (int i = 0; i < args.size(); i++) {
      final AbstractVariableData data = new AbstractVariableData();
      final String name = args.get(i);
      data.originalName = name;
      data.name = name;
      data.passAsParameter = true;
      datas[i] = data;
    }
    return datas;
  }

  public static Map<String, AbstractVariableData> createVariableMap(final AbstractVariableData[] data) {
    final HashMap<String, AbstractVariableData> map = new HashMap<>();
    for (AbstractVariableData variableData : data) {
      map.put(variableData.getOriginalName(), variableData);
    }
    return map;
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  @RequiredUIAccess
  protected void doOKAction() {
    final String error = myValidator.check(getMethodName());
    if (error != null){
      if (myProject.getApplication().isUnitTestMode()){
        Messages.showInfoMessage(error, RefactoringLocalize.errorTitle().get());
        return;
      }
      if (Messages.showOkCancelDialog(
        error + ". " + RefactoringLocalize.doYouWishToContinue(),
        RefactoringLocalize.warningTitle().get(),
        UIUtil.getWarningIcon()
      ) != Messages.OK) {
        return;
      }
    }
    super.doOKAction();
  }

  @Override
  protected String getHelpId() {
    return "refactoring.extractMethod";
  }

  @Override
  protected JComponent createCenterPanel() {
    return myContentPane;
  }

  private void createUIComponents() {
    myParametersPanel = new SimpleParameterTablePanel(myValidator::isValidName) {
      @Override
      protected void doCancelAction() {
        AbstractExtractMethodDialog.this.doCancelAction();
      }

      @Override
      @RequiredUIAccess
      protected void doEnterAction() {
        doOKAction();
      }

      @Override
      protected void updateSignature() {
        updateOutputVariables();
        AbstractExtractMethodDialog.this.updateSignature();
      }
    };
    mySignaturePreviewTextArea = new MethodSignatureComponent("", myProject, myFileType);
  }

  private void updateOutputVariables() {
    final StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String variable : myOutputVariables) {
      if (myVariablesMap!=null){
        final AbstractVariableData data = myVariablesMap.get(variable);
        final String outputName = data != null ? data.getName() : variable;
        if (first){
          first = false;
        } else {
          builder.append(", ");
        }
        builder.append(outputName);
      }
    }
    myOutputVariablesTextArea.setText(
      builder.length() > 0 ? builder.toString() : RefactoringLocalize.refactoringExtractMethodDialogEmpty().get()
    );
  }

  private void updateSignature() {
    mySignaturePreviewTextArea.setSignature(myDecorator.createMethodSignature(getMethodName(), myVariableData));
  }

  private void updateOkStatus() {
    setOKActionEnabled(myValidator.isValidName(getMethodName()));
  }

  @Override
  public String getMethodName() {
    return myMethodNameTextField.getText().trim();
  }

  @Override
  public AbstractVariableData[] getAbstractVariableData() {
    return myVariableData;
  }

}