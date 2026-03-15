package consulo.virtualFileSystem;

import consulo.application.AllIcons;
import consulo.application.presentation.TypePresentationService;
import consulo.ui.image.Image;


/**
 * @author yole
 */
public class VirtualFilePresentation {
  
  public static Image getIcon(VirtualFile vFile) {
    Image icon = TypePresentationService.getInstance().getIcon(vFile);
    if (icon != null) {
      return icon;
    }
    if (vFile.isDirectory())  {
      return AllIcons.Nodes.Folder;
    }
    return vFile.getFileType().getIcon();
  }
}
