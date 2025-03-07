/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.ComponentManager;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionListener;

public class TextFieldWithBrowseButton extends ComponentWithBrowseButton<JTextField> implements TextAccessor {
  public TextFieldWithBrowseButton(){
    this((ActionListener)null);
  }

  public TextFieldWithBrowseButton(JTextField field){
    this(field, null);
  }

  public TextFieldWithBrowseButton(JTextField field, @Nullable ActionListener browseActionListener) {
    this(field, browseActionListener, null);
  }

  public TextFieldWithBrowseButton(JTextField field, @Nullable ActionListener browseActionListener, @Nullable Disposable parent) {
    super(field, browseActionListener);
    if (!(field instanceof JBTextField)) {
      UIUtil.addUndoRedoActions(field);
    }
    installPathCompletion(FileChooserDescriptorFactory.createSingleLocalFileDescriptor(), parent);
  }

  public TextFieldWithBrowseButton(ActionListener browseActionListener) {
    this(browseActionListener, null);
  }

  public TextFieldWithBrowseButton(ActionListener browseActionListener, Disposable parent) {
    this(new JBTextField(10/* to prevent field to be infinitely resized in grid-box layouts */), browseActionListener, parent);
  }

  public void addBrowseFolderListener(@Nullable String title, @Nullable String description, @Nullable ComponentManager project, FileChooserDescriptor fileChooserDescriptor) {
    addBrowseFolderListener(title, description, project, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    installPathCompletion(fileChooserDescriptor);
  }

  public void addBrowseFolderListener(@Nonnull TextBrowseFolderListener listener) {
    listener.setOwnerComponent(this);
    addBrowseFolderListener(null, listener, true);
    installPathCompletion(listener.getFileChooserDescriptor());
  }

  protected void installPathCompletion(final FileChooserDescriptor fileChooserDescriptor) {
    installPathCompletion(fileChooserDescriptor, null);
  }

  protected void installPathCompletion(final FileChooserDescriptor fileChooserDescriptor,
                                       @Nullable Disposable parent) {
    final Application application = ApplicationManager.getApplication();
    if (application == null || application.isUnitTestMode() || application.isHeadlessEnvironment()) return;
    FileChooserFactory.getInstance().installFileCompletion(getChildComponent(), fileChooserDescriptor, true, parent);
  }

  public JTextField getTextField() {
    return getChildComponent();
  }

  /**
   * @return trimmed text
   */
  @Override
  public String getText(){
    return getTextField().getText();
  }

  @Override
  public void setText(final String text){
    getTextField().setText(text);
  }

  public boolean isEditable() {
    return getTextField().isEditable();
  }

  public void setEditable(boolean b) {
    getTextField().setEditable(b);
    JButton button = getButton();
    if (button != null) {
      getButton().setFocusable(!b);
    } else {
      setButtonEnabled(false);
    }
  }

  public static class NoPathCompletion extends TextFieldWithBrowseButton {
    public NoPathCompletion() {
    }

    public NoPathCompletion(final JTextField field) {
      super(field);
    }

    public NoPathCompletion(final JTextField field, final ActionListener browseActionListener) {
      super(field, browseActionListener);
    }

    public NoPathCompletion(final ActionListener browseActionListener) {
      super(browseActionListener);
    }

    @Override
    protected void installPathCompletion(final FileChooserDescriptor fileChooserDescriptor) {
    }
  }
}
