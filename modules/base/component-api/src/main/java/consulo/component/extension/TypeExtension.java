package consulo.component.extension;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * A kind of something - an SDK type, a run configuration type, a file type - known by a stable id, shown by its name and icon.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
public interface TypeExtension {
    /**
     * Stable identifier, it is persisted - must not change between releases.
     */
    String getId();

    LocalizeValue getDisplayName();

    @Nullable Image getIcon();
}
