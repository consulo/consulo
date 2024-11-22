package consulo.language.editor.ui.awt;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.document.Document;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author sergey.evdokimov
 */
public abstract class TextFieldCompletionProvider {

  public static final Key<TextFieldCompletionProvider> COMPLETING_TEXT_FIELD_KEY = Key.create("COMPLETING_TEXT_FIELD_KEY");

  protected boolean myCaseInsensitivity;

  protected TextFieldCompletionProvider() {
    this(false);
  }

  protected TextFieldCompletionProvider(boolean caseInsensitivity) {
    myCaseInsensitivity = caseInsensitivity;
  }

  public void apply(@Nonnull EditorTextField field, @Nonnull String text) {
    Project project = field.getProject();
    if (project != null) {
      field.setDocument(createDocument(project, text));
    }
  }

  public void apply(@Nonnull EditorTextField field) {
    apply(field, "");
  }

  private Document createDocument(final Project project, @Nonnull String text) {
    final FileType fileType = PlainTextLanguage.INSTANCE.getAssociatedFileType();
    assert fileType != null;

    final long stamp = LocalTimeCounter.currentTime();
    final PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("Dummy." + fileType.getDefaultExtension(), fileType, text, stamp, true, false);

    psiFile.putUserData(COMPLETING_TEXT_FIELD_KEY, this);

    final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
    assert document != null;
    return document;
  }

  public boolean isCaseInsensitivity() {
    return myCaseInsensitivity;
  }

  @Nonnull
  public String getPrefix(@Nonnull String currentTextPrefix) {
    return currentTextPrefix;
  }

  public abstract void addCompletionVariants(@Nonnull String text, int offset, @Nonnull String prefix, @Nonnull CompletionResultSet result);

  @Nonnull
  public EditorTextField createEditor(Project project) {
    return createEditor(project, true);
  }

  @Nonnull
  public EditorTextField createEditor(Project project, final boolean shouldHaveBorder) {
    return createEditor(project, shouldHaveBorder, null);
  }

  @Nonnull
  public EditorTextField createEditor(Project project, final boolean shouldHaveBorder, @Nullable final Consumer<Editor> editorConstructionCallback) {
    return new EditorTextField(createDocument(project, ""), project, PlainTextLanguage.INSTANCE.getAssociatedFileType()) {
      @Override
      protected boolean shouldHaveBorder() {
        return shouldHaveBorder;
      }

      protected EditorEx createEditor() {
        EditorEx result = super.createEditor();
        if (editorConstructionCallback != null) {
          editorConstructionCallback.accept(result);
        }
        return result;
      }
    };
  }
}
