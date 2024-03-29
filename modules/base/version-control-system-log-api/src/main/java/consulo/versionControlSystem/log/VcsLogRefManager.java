package consulo.versionControlSystem.log;

import jakarta.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Sorts {@link VcsRef references} of branches and tags according to their type, "expected importance" and other means.
 */
public interface VcsLogRefManager {

  /**
   * Returns the comparator which compares two given references, which represent heads (leafs) of the log graph,
   * by their expected position in the log graph.
   * <p/>
   * The comparison result is used in graph layout to choose which branch should be laid at the left, and which - at the right.
   * This layout order should be kept between log refreshes, when possible.
   * Branches which are laid at the left are considered more important than which are laid at the right.
   * <p/>
   * <ul>
   * <li><b>Negative</b> value is returned if the first branch should be laid out at the left from the second (i.e. "is more important").
   * <li><b>Positive</b> value is returned if the first branch should be laid out at the right from the second (i.e. "is less important").
   * <li>Zero is returned for equal references.
   * </ul>
   * <p/>
   * It is guaranteed that the supplied collection is not empty.
   * <p/>
   * The given collection may contain references from different roots.
   *
   * @see #getLabelsOrderComparator()
   */
  @Nonnull
  Comparator<VcsRef> getBranchLayoutComparator();

  /**
   * Return the comparator which compares two given references, to identify the order of branch labels in the log table and description.
   * References are compared by their position, more important references appear at the left from the less important.
   * <p/>
   * <ul>
   * <li><b>Negative</b> value is returned if the first reference should appear at the left from the second (i.e. "is more important").
   * <li><b>Positive</b> value is returned if the first reference should appear at the right from the second (i.e. "is less important").
   * <li>Zero is returned for equal references.
   * </ul>
   * <p/>
   * Note that this comparator not necessarily should be consistent with {@link #getBranchLayoutComparator()}.
   * <p/>
   *
   * @see #getBranchLayoutComparator()
   */
  @Nonnull
  Comparator<VcsRef> getLabelsOrderComparator();

  /**
   * <p>Groups VCS references to show them in branch filter.</p>
   * <p>Groups containing only one element will be displayed as a single ref. Others will provide a popup menu.</p>
   * <p>Groups must be pre-sorted in the order which they are to be painted on the panel.</p>
   */
  @Nonnull
  List<RefGroup> groupForBranchFilter(@Nonnull Collection<VcsRef> refs);

  /**
   * Groups VCS references to show them in graph table.
   * All references given to this method are from the same commit.
   */
  @Nonnull
  List<RefGroup> groupForTable(@Nonnull Collection<VcsRef> refs, boolean compact, boolean showTagNames);

  /**
   * Writes given reference type to the output.
   *
   * @param out  output to write type into
   * @param type type to serialize
   */
  void serialize(@Nonnull DataOutput out, @Nonnull VcsRefType type) throws IOException;

  /**
   * Reads reference type from given input.
   *
   * @param in input to read type from
   * @return reference type read from the input
   */
  @Nonnull
  VcsRefType deserialize(@Nonnull DataInput in) throws IOException;
}
