package consulo.language.editor.ui.awt;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.document.Document;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.ui.internal.TextFieldDocuments;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.fileType.FileType;

import org.jspecify.annotations.Nullable;
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

  public void apply(EditorTextField field, String text) {
    Project project = field.getProject();
    if (project != null) {
      field.setDocument(createDocument(project, text));
    }
  }

  public void apply(EditorTextField field) {
    apply(field, "");
  }

  public Document createDocument(Project project, String text) {
    FileType fileType = PlainTextLanguage.INSTANCE.getAssociatedFileType();
    assert fileType != null;

    return TextFieldDocuments.create(project, fileType, text, this::installTo);
  }

  public void installTo(PsiFile psiFile) {
    psiFile.putUserData(COMPLETING_TEXT_FIELD_KEY, this);
  }

  public boolean isCaseInsensitivity() {
    return myCaseInsensitivity;
  }

  
  public String getPrefix(String currentTextPrefix) {
    return currentTextPrefix;
  }

  public abstract void addCompletionVariants(String text, int offset, String prefix, CompletionResultSet result);

  
  public EditorTextField createEditor(Project project) {
    return createEditor(project, true);
  }

  
  public EditorTextField createEditor(Project project, boolean shouldHaveBorder) {
    return createEditor(project, shouldHaveBorder, null);
  }

  
  public EditorTextField createEditor(Project project, final boolean shouldHaveBorder, final @Nullable Consumer<Editor> editorConstructionCallback) {
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
