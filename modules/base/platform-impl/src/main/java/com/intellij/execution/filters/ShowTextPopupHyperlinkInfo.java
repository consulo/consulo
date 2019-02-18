package com.intellij.execution.filters;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.EditorTextField;
import consulo.awt.TargetAWT;

import javax.swing.*;
import java.awt.*;

/**
 * from Kotlin
 */
public class ShowTextPopupHyperlinkInfo implements HyperlinkInfo {
  private String myTitle;
  private String myText;

  public ShowTextPopupHyperlinkInfo(String title, String text) {
    myTitle = title;
    myText = text;
  }

  @Override
  public void navigate(Project project) {
    Application.get().invokeLater(() -> {
      Document document = EditorFactory.getInstance().createDocument(StringUtil.convertLineSeparators(myText));
      EditorTextField textField = new EditorTextField(document, project, PlainTextFileType.INSTANCE, true, false) {
        @Override
        protected EditorEx createEditor() {
          EditorEx editor = super.createEditor();
          editor.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
          editor.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
          editor.getSettings().setUseSoftWraps(true);
          return editor;
        }
      };

      Window frame = TargetAWT.to(WindowManager.getInstance().getWindow(project));
      if(frame != null) {
        Dimension size = frame.getSize();
        if(size != null) {
          textField.setPreferredSize(new Dimension(size.width / 2, size.height / 2));
        }
      }

      JBPopupFactory.getInstance()
              .createComponentPopupBuilder(textField, textField)
              .setTitle(myTitle)
              .setResizable(true)
              .setMovable(true)
              .setRequestFocus(true)
              .createPopup()
              .showCenteredInCurrentWindow(project);
    });
  }
}
