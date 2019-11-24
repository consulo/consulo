package consulo.ui.migration;

import consulo.annotation.DeprecationInfo;
import consulo.ui.image.Image;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 18-Jan-17
 */
@Deprecated
@DeprecationInfo("Icon -> Image migration class. Do not use it. Use converter TargetAWT.to() or TargetAWT.from()")
public interface SwingImageRef extends Icon, Image {
}
