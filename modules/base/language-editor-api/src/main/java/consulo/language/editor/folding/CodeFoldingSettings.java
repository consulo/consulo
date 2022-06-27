package consulo.language.editor.folding;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

@ServiceAPI(ComponentScope.APPLICATION)
public class CodeFoldingSettings {
  public boolean COLLAPSE_IMPORTS = true;
  public boolean COLLAPSE_METHODS;
  public boolean COLLAPSE_FILE_HEADER = true;
  public boolean COLLAPSE_DOC_COMMENTS;

  public static CodeFoldingSettings getInstance() {
    return Application.get().getInstance(CodeFoldingSettings.class);
  }
}
