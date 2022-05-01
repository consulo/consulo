package consulo.ide.impl.idea.execution.filters;

import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.execution.ui.console.Filter;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

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
    myFileText = FileUtil.loadFile(new File(path));
  }

  @Nullable
  @Override
  public Result applyFilter(String line, int entireLength) {
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
