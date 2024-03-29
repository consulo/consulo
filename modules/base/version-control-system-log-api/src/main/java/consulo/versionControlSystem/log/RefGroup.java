package consulo.versionControlSystem.log;

import jakarta.annotation.Nonnull;
import java.awt.*;
import java.util.List;

/**
 * Lets group {@link VcsRef references} to show them accordingly in the UI, for example on the branches panel.
 * Grouping decision is made by the concrete {@link VcsLogRefManager}.
 */
public interface RefGroup {

  /**
   * If a group is not-expanded, its references won't be displayed until
   * Otherwise, if a group is expanded, its references will be displayed immediately,
   * but they may possibly be somehow visually united to indicated that they are from similar structure.
   */
  boolean isExpanded();

  /**
   * Returns the name of the reference group. This reference will be displayed on the branches panel.
   */
  @Nonnull
  String getName();

  /**
   * Returns references inside this group.
   */
  @Nonnull
  List<VcsRef> getRefs();

  /**
   * Returns the colors of this ref group, which will be used to paint it in the table.
   */
  @Nonnull
  List<Color> getColors();
}
