package consulo.ide.impl.idea.xdebugger.impl.ui;

import consulo.document.Document;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorEx;
import consulo.document.impl.DocumentImpl;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.language.editor.ui.awt.EditorTextField;
import javax.annotation.Nonnull;

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
    final Document document = EditorFactory.getInstance().createDocument(initialText);
    if (document instanceof DocumentImpl) {
      ((DocumentImpl)document).setAcceptSlashR(true);
    }
    return document;
  }

  @Override
  protected EditorEx createEditor() {
    final EditorEx editor = super.createEditor();
    editor.setHorizontalScrollbarVisible(true);
    editor.setCaretEnabled(true);
    editor.setVerticalScrollbarVisible(true);
    editor.setEmbeddedIntoDialogWrapper(myEmbeddedIntoDialogWrapper);
    editor.getComponent().setPreferredSize(null);
    editor.getSettings().setUseSoftWraps(myUseSoftWraps);
    return editor;
  }
}