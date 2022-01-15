package consulo.fileEditor.impl.text;

import com.intellij.openapi.editor.Editor;
import consulo.ui.Component;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 07/08/2021
 */
public interface TextEditorComponentContainer {
  void startLoading();

  void loadingFinished();

  JComponent getComponent();

  Component getUIComponent();

  void hideContent();

  @Nullable
  default Editor validateEditor(Editor editor) {
    return editor;
  }
}
