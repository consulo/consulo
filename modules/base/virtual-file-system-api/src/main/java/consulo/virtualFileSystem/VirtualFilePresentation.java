package consulo.virtualFileSystem;

import consulo.application.AllIcons;
import consulo.application.util.TypePresentationService;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class VirtualFilePresentation {
  @Nonnull
  public static Image getIcon(@Nonnull VirtualFile vFile) {
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
