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

package com.intellij.ui;

import consulo.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import consulo.undoRedo.util.UndoConstants;
import consulo.logging.Logger;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;

import javax.swing.*;

/**
 * ComboBox with Editor and Strings as item
 * @author dsl
 */
public class StringComboboxEditor extends EditorComboBoxEditor {
  public static final Key<JComboBox> COMBO_BOX_KEY = Key.create("COMBO_BOX_KEY");
  public static final Key<Boolean> USE_PLAIN_PREFIX_MATCHER = Key.create("USE_PLAIN_PREFIX_MATCHER");

  private static final Logger LOG = Logger.getInstance(StringComboboxEditor.class);
  private final Project myProject;

  public StringComboboxEditor(final Project project, final FileType fileType, ComboBox comboBox) {
    this(project, fileType, comboBox, false);
  }

  public StringComboboxEditor(final Project project, final FileType fileType, ComboBox comboBox, boolean usePlainMatcher) {
    super(project, fileType);
    myProject = project;
    final PsiFile file = PsiFileFactory.getInstance(project).createFileFromText("a.dummy", PlainTextFileType.INSTANCE, "", 0, true);
    final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    assert document != null;
    document.putUserData(COMBO_BOX_KEY, comboBox);
    if (usePlainMatcher) {
      document.putUserData(USE_PLAIN_PREFIX_MATCHER, true);
    }
    document.putUserData(UndoConstants.DONT_RECORD_UNDO, true);
    super.setItem(document);
  }

  @Override
  public Object getItem() {
    final String text = ((Document) super.getItem()).getText();
    LOG.assertTrue(text != null);
    return text;
  }

  @Override
  public void setItem(Object anObject) {
    if (anObject == null) anObject = "";
    if (anObject.equals(getItem())) return;
    final String s = (String)anObject;
    new WriteCommandAction(myProject) {
      @Override
      protected void run(Result result) throws Throwable {
        getDocument().setText(s);
      }
    }.execute();

    final Editor editor = getEditor();
    if (editor != null) editor.getCaretModel().moveToOffset(s.length());
  }
}
