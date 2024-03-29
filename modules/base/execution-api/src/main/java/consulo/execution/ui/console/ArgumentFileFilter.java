package consulo.execution.ui.console;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * from Kotlin
 */
public class ArgumentFileFilter implements Filter {
  private volatile String myFilePath;
  private volatile String myFileText;
  private boolean myTriggred;

  public ArgumentFileFilter() {
  }

  public ArgumentFileFilter(String filePath, String fileText) {
    myFilePath = filePath;
    myFileText = fileText;
  }

  public void setPath(String path) throws IOException {
    myFilePath = path;
    myFileText = Files.readString(Path.of(path));
  }

  @Nullable
  @Override
  public Result applyFilter(@Nonnull String line, int entireLength) {
    if(!myTriggred) {
      String path = myFilePath;
      String text = myFileText;
      if(path == null || text == null) {
        myTriggred = true;
      }
      else {
        int p = line.indexOf(path);
        if(p > 0) {
          myTriggred = true;
          int offset = entireLength - line.length() + p;
          return new Result(offset, offset + path.length(), new ShowTextPopupHyperlinkInfo(path, text));
        }
      }
    }
    return null;
  }
}
