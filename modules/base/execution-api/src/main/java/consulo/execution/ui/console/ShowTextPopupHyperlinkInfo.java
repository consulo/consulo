package consulo.execution.ui.console;

import consulo.application.Application;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.StringUtil;

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

  @RequiredUIAccess
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
      if (frame != null) {
        Dimension size = frame.getSize();
        if (size != null) {
          textField.setPreferredSize(new Dimension(size.width / 2, size.height / 2));
        }
      }

      JBPopupFactory.getInstance().createComponentPopupBuilder(textField, textField).setTitle(myTitle).setResizable(true).setMovable(true).setRequestFocus(true).createPopup()
              .showCenteredInCurrentWindow(project);
    });
  }
}
