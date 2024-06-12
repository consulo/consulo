/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.application.ApplicationManager;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.localize.UILocalize;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.io.File;

public class InsertPathAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(InsertPathAction.class);
  protected final JTextComponent myTextField;
  protected static final CustomShortcutSet CTRL_F = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
  protected final FileChooserDescriptor myDescriptor;
  private MouseListener myPopupHandler;
  protected static final Key INSERT_PATH_ACTION= Key.create("insertPathAction");

  private InsertPathAction(JTextComponent textField) {
    this(textField, FileChooserDescriptorFactory.createSingleLocalFileDescriptor());
  }

  private InsertPathAction(JTextComponent textField, FileChooserDescriptor descriptor) {
    super(UILocalize.insertFilePathToTextActionName());
    myTextField = textField;
    registerCustomShortcutSet(CTRL_F, myTextField);
    myDescriptor = descriptor;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    String selectedText = myTextField.getSelectedText();
    VirtualFile virtualFile;
    if (selectedText != null ) {
      virtualFile = LocalFileSystem.getInstance().findFileByPath(selectedText.replace(File.separatorChar, '/'));
    }
    else {
      virtualFile = null;
    }
    //TODO use from openapi
    //FeatureUsageTracker.getInstance().triggerFeatureUsed("ui.commandLine.insertPath");
    FileChooser.chooseFile(myDescriptor, myTextField, e.getData(Project.KEY), virtualFile).doWhenDone((f) -> {
      myTextField.replaceSelection(f.getPresentableUrl());
    });
  }

  private void uninstall() {
    uninstallPopupHandler();
    myTextField.putClientProperty(INSERT_PATH_ACTION, null);
  }

  private void savePopupHandler(MouseListener popupHandler) {
    if (myPopupHandler != null) {
      LOG.error("Installed twice");
      uninstallPopupHandler();
    }
    myPopupHandler = popupHandler;
  }

  private void uninstallPopupHandler() {
    if (myPopupHandler == null) return;
    myTextField.removeMouseListener(myPopupHandler);
    myPopupHandler = null;
  }

  public static void addTo(JTextComponent textField) {
    addTo(textField, null);
  }

  public static void addTo(JTextComponent textField, FileChooserDescriptor descriptor) {
    if (ApplicationManager.getApplication() != null) { //NPE fixed when another class loader works
      removeFrom(textField);
      if (textField.getClientProperty(INSERT_PATH_ACTION) != null) return;
      DefaultActionGroup actionGroup = new DefaultActionGroup();
      InsertPathAction action = descriptor != null? new InsertPathAction(textField, descriptor) : new InsertPathAction(textField);
      actionGroup.add(action);
      MouseListener popupHandler = PopupHandler.installUnknownPopupHandler(textField, actionGroup, ActionManager.getInstance());
      action.savePopupHandler(popupHandler);
      textField.putClientProperty(INSERT_PATH_ACTION, action);
    }
  }

  public static void removeFrom(JTextComponent textComponent) {
    InsertPathAction action = getFrom(textComponent);
    if (action == null) return;
    action.uninstall();
  }

  public static void copyFromTo(JTextComponent original, JTextComponent target) {
    InsertPathAction action = getFrom(original);
    if (action == null) return;
    removeFrom(target);
    addTo(target, action.myDescriptor);
  }

  private static InsertPathAction getFrom(JTextComponent textComponent) {
    Object property = textComponent.getClientProperty(INSERT_PATH_ACTION);
    if (!(property instanceof InsertPathAction)) return null;
    return (InsertPathAction)property;
  }

}