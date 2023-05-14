// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.template.impl;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * @author peter
 */
public class InvokeTemplateAction extends AnAction {
  private final Template myTemplate;
  private final Editor myEditor;
  private final Project myProject;
  @Nullable
  private final Runnable myCallback;

  public InvokeTemplateAction(Template template, Editor editor, Project project, Set<Character> usedMnemonicsSet) {
    this(template, editor, project, usedMnemonicsSet, null);
  }

  public InvokeTemplateAction(Template template, Editor editor, Project project, Set<Character> usedMnemonicsSet, @Nullable Runnable afterInvocationCallback) {
    super(extractMnemonic(template.getKey(), usedMnemonicsSet) + (StringUtil.isEmptyOrSpaces(template.getDescription()) ? "" : ". " + template.getDescription()));
    myTemplate = template;
    myProject = project;
    myEditor = editor;
    myCallback = afterInvocationCallback;
  }

  public static String extractMnemonic(String caption, Set<? super Character> usedMnemonics) {
    if (StringUtil.isEmpty(caption)) return "";

    for (int i = 0; i < caption.length(); i++) {
      char c = caption.charAt(i);
      if (usedMnemonics.add(Character.toUpperCase(c))) {
        return caption.substring(0, i) + UIUtil.MNEMONIC + caption.substring(i);
      }
    }

    return caption + " ";
  }

  public Template getTemplate() {
    return myTemplate;
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    perform();
  }

  public void perform() {
    final Document document = myEditor.getDocument();
    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null && ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(Collections.singletonList(file)).hasReadonlyFiles()) {
      return;
    }

    CommandProcessor.getInstance().executeCommand(myProject, () -> {
      myEditor.getCaretModel().runForEachCaret(__ -> {
        // adjust the selection so that it starts with a non-whitespace character (to make sure that the template is inserted
        // at a meaningful position rather than at indent 0)
        if (myEditor.getSelectionModel().hasSelection() && myTemplate.isToReformat()) {
          int offset = myEditor.getSelectionModel().getSelectionStart();
          int selectionEnd = myEditor.getSelectionModel().getSelectionEnd();
          int lineEnd = document.getLineEndOffset(document.getLineNumber(offset));
          while (offset < lineEnd && offset < selectionEnd && (document.getCharsSequence().charAt(offset) == ' ' || document.getCharsSequence().charAt(offset) == '\t')) {
            offset++;
          }
          // avoid extra line break after $SELECTION$ in case when selection ends with a complete line
          if (selectionEnd == document.getLineStartOffset(document.getLineNumber(selectionEnd))) {
            selectionEnd--;
          }
          if (offset < lineEnd && offset < selectionEnd) {  // found non-WS character in first line of selection
            myEditor.getSelectionModel().setSelection(offset, selectionEnd);
          }
        }
        String selectionString = myEditor.getSelectionModel().getSelectedText();
        TemplateManager.getInstance(myProject).startTemplate(myEditor, selectionString, myTemplate);
      });
      if (myCallback != null) {
        myCallback.run();
      }
    }, CodeInsightBundle.message("command.wrap.with.template"), "Wrap with template " + myTemplate.getKey());
  }
}
