package consulo.externalSystem.model;

import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Locale;

/**
 * The general idea of 'external system' integration is to provide management facilities for the project structure defined in
 * terms over than IntelliJ (e.g. maven, gradle, eclipse etc).
 * <p/>
 * This class serves as an id of a system which defines project structure, i.e. it might be any external system or the ide itself.
 * 
 * @author Denis Zhdanov
 * @since 2/14/12 12:59 PM
 */
public class ProjectSystemId implements Serializable {

  private static final long serialVersionUID = 1L;
  
  @Nonnull
  public static final ProjectSystemId IDE = new ProjectSystemId("IDE", LocalizeValue.localizeTODO("IDE"));

  @Nonnull
  private final String myId;
  @Nonnull
  private final LocalizeValue myReadableName;

  @Deprecated
  public ProjectSystemId(@Nonnull String id) {
    this(id, LocalizeValue.of(StringUtil.capitalize(id.toLowerCase(Locale.ROOT))));
  }

  public ProjectSystemId(@Nonnull String id, @Nonnull LocalizeValue readableName) {
    myId = id;
    myReadableName = readableName;
  }

  @Nonnull
  public String getId() {
    return myId;
  }

  @Nonnull
  public LocalizeValue getReadableName() {
    return myReadableName;
  }

  @Nonnull
  public String getToolWindowId() {
    return myId;
  }

  @Override
  public String toString() {
    return myId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProjectSystemId owner = (ProjectSystemId)o;

    return myId.equals(owner.myId);
  }

  @Override
  public int hashCode() {
    return myId.hashCode();
  }
}
