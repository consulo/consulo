package consulo.virtualFileSystem;

import consulo.application.presentation.TypePresentationService;
import consulo.platform.base.icon.PlatformIconGroup;
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
      return PlatformIconGroup.nodesFolder();
    }
    return vFile.getFileType().getIcon();
  }
}
