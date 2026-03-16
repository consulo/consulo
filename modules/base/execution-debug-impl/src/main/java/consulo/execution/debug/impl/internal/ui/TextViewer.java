package consulo.execution.debug.impl.internal.ui;

import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;

public final class TextViewer extends EditorTextField {
  private final boolean myEmbeddedIntoDialogWrapper;
  private final boolean myUseSoftWraps;

  public TextViewer(Project project, boolean embeddedIntoDialogWrapper, boolean useSoftWraps) {
    this(createDocument(""), project, embeddedIntoDialogWrapper, useSoftWraps);
  }

  public TextViewer(String initialText, Project project) {
    this(createDocument(initialText), project, false, false);
  }

  public TextViewer(Document document, Project project, boolean embeddedIntoDialogWrapper, boolean useSoftWraps) {
    super(document, project, PlainTextFileType.INSTANCE, true, false);

    myEmbeddedIntoDialogWrapper = embeddedIntoDialogWrapper;
    myUseSoftWraps = useSoftWraps;
    setFontInheritedFromLAF(false);
  }

  private static Document createDocument(String initialText) {
    Document document = EditorFactory.getInstance().createDocument(initialText);
    if (document instanceof DocumentEx) {
      ((DocumentEx)document).setAcceptSlashR(true);
    }
    return document;
  }

  @Override
  protected EditorEx createEditor() {
    EditorEx editor = super.createEditor();
    editor.setHorizontalScrollbarVisible(true);
    editor.setCaretEnabled(true);
    editor.setVerticalScrollbarVisible(true);
    editor.setEmbeddedIntoDialogWrapper(myEmbeddedIntoDialogWrapper);
    editor.getComponent().setPreferredSize(null);
    editor.getSettings().setUseSoftWraps(myUseSoftWraps);
    return editor;
  }
}