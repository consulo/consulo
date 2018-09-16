package com.intellij.openapi.externalSystem.model.project;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import javax.annotation.Nonnull;

/**
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/1/11 1:30 PM
 */
public class ProjectData extends AbstractNamedData implements ExternalConfigPathAware, Identifiable {

  private static final long serialVersionUID = 1L;

  @Nonnull
  private final String myLinkedExternalProjectPath;

  @Nonnull
  private String myIdeProjectFileDirectoryPath;

  @Deprecated
  public ProjectData(@Nonnull ProjectSystemId owner,
                     @Nonnull String ideProjectFileDirectoryPath,
                     @Nonnull String linkedExternalProjectPath) {
    super(owner, "unnamed");
    myLinkedExternalProjectPath = ExternalSystemApiUtil.toCanonicalPath(linkedExternalProjectPath);
    myIdeProjectFileDirectoryPath = ExternalSystemApiUtil.toCanonicalPath(ideProjectFileDirectoryPath);
  }

  public ProjectData(@Nonnull ProjectSystemId owner,
                     @Nonnull String externalName,
                     @Nonnull String ideProjectFileDirectoryPath,
                     @Nonnull String linkedExternalProjectPath) {
    super(owner, externalName);
    myLinkedExternalProjectPath = ExternalSystemApiUtil.toCanonicalPath(linkedExternalProjectPath);
    myIdeProjectFileDirectoryPath = ExternalSystemApiUtil.toCanonicalPath(ideProjectFileDirectoryPath);
  }

  @Deprecated
  @Override
  public void setName(@Nonnull String name) {
    super.setExternalName(name);
    super.setInternalName(name);
  }

  @Nonnull
  public String getIdeProjectFileDirectoryPath() {
    return myIdeProjectFileDirectoryPath;
  }

  public void setIdeProjectFileDirectoryPath(@Nonnull String ideProjectFileDirectoryPath) {
    myIdeProjectFileDirectoryPath = ExternalSystemApiUtil.toCanonicalPath(ideProjectFileDirectoryPath);
  }

  @Nonnull
  public String getLinkedExternalProjectPath() {
    return myLinkedExternalProjectPath;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myIdeProjectFileDirectoryPath.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    ProjectData project = (ProjectData)o;

    if (!myIdeProjectFileDirectoryPath.equals(project.myIdeProjectFileDirectoryPath)) return false;

    return true;
  }

  @Override
  public String toString() {
    return String.format("%s project '%s'", getOwner().toString().toLowerCase(), getExternalName());
  }

  @Nonnull
  @Override
  public String getId() {
    return "";
  }
}
