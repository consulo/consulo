package consulo.execution.debug.impl.internal.ui;

import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

public final class TextViewer extends EditorTextField {
  private final boolean myEmbeddedIntoDialogWrapper;
  private final boolean myUseSoftWraps;

  public TextViewer(@Nonnull Project project, boolean embeddedIntoDialogWrapper, boolean useSoftWraps) {
    this(createDocument(""), project, embeddedIntoDialogWrapper, useSoftWraps);
  }

  public TextViewer(@Nonnull String initialText, @Nonnull Project project) {
    this(createDocument(initialText), project, false, false);
  }

  public TextViewer(@Nonnull Document document, @Nonnull Project project, boolean embeddedIntoDialogWrapper, boolean useSoftWraps) {
    super(document, project, PlainTextFileType.INSTANCE, true, false);

    myEmbeddedIntoDialogWrapper = embeddedIntoDialogWrapper;
    myUseSoftWraps = useSoftWraps;
    setFontInheritedFromLAF(false);
  }

  private static Document createDocument(@Nonnull String initialText) {
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