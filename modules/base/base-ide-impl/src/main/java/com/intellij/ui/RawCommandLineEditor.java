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
package com.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.Function;
import com.intellij.util.execution.ParametersListUtil;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.ValueComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RawCommandLineEditor extends Wrapper implements TextAccessor {
  private final TextBoxWithExpandAction myTextBoxWithExpandAction;

  public RawCommandLineEditor() {
    this(ParametersListUtil.DEFAULT_LINE_PARSER, ParametersListUtil.DEFAULT_LINE_JOINER);
  }

  public RawCommandLineEditor(final Function<String, List<String>> lineParser, final Function<List<String>, String> lineJoiner) {
    super(new BorderLayout());
    myTextBoxWithExpandAction = TextBoxWithExpandAction.create(AllIcons.Actions.ShowViewer, "", lineParser::fun, lineJoiner::fun);
    setContent((JComponent)TargetAWT.to(myTextBoxWithExpandAction));

    setDescriptor(null);
  }

  public void setDescriptor(FileChooserDescriptor descriptor) {
    //InsertPathAction.addTo(myTextField.getTextField(), descriptor);
  }

  public void setDialogCaption(String dialogCaption) {
    myTextBoxWithExpandAction.setDialogTitle(dialogCaption);
  }

  @Override
  public void setText(@Nullable String text) {
    myTextBoxWithExpandAction.setValue(text);
  }

  @Override
  public String getText() {
    return myTextBoxWithExpandAction.getValue();
  }

  public JComponent getTextField() {
    return (JComponent)TargetAWT.to(myTextBoxWithExpandAction);
  }

  //public Document getDocument() {
  //  return myTextField.getTextField().getDocument();
  //}

  @Nonnull
  public Disposable addValueListener(@Nonnull ValueComponent.ValueListener<String> valueComponent) {
    return myTextBoxWithExpandAction.addValueListener(valueComponent);
  }

  public void attachLabel(JLabel label) {
    label.setLabelFor(getTextField());
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    myTextBoxWithExpandAction.setEnabled(enabled);
  }
}
