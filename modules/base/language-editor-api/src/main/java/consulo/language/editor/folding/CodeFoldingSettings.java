package consulo.language.editor.folding;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.Application;

@Service(ComponentScope.APPLICATION)
public class CodeFoldingSettings {
  public boolean COLLAPSE_IMPORTS = true;
  public boolean COLLAPSE_METHODS;
  public boolean COLLAPSE_FILE_HEADER = true;
  public boolean COLLAPSE_DOC_COMMENTS;

  public static CodeFoldingSettings getInstance() {
    return Application.get().getInstance(CodeFoldingSettings.class);
  }
}
