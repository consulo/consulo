package com.intellij.openapi.externalSystem.model.project;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Zhdanov
 * @since 8/8/11 12:11 PM
 */
public class ModuleData extends AbstractNamedData implements Named, ExternalConfigPathAware, Identifiable {

  private static final long serialVersionUID = 1L;

  @Nonnull
  private final Map<ExternalSystemSourceType, String> myCompileOutputPaths = ContainerUtil.newHashMap();
  @Nonnull
  private final String myId;
  @Nonnull
  private final String myExternalConfigPath;
  @Nonnull
  private String myModuleDirPath;
  @Nullable private String group;
  @Nullable private String version;
  @Nonnull
  private List<File> myArtifacts;

  private boolean myInheritProjectCompileOutputPath = true;

  @Deprecated
  public ModuleData(@Nonnull ProjectSystemId owner,
                    @Nonnull String name,
                    @Nonnull String moduleDir,
                    @Nonnull String externalConfigPath) {
    this("", owner, name, moduleDir, externalConfigPath);
  }

  public ModuleData(@Nonnull String id,
                    @Nonnull ProjectSystemId owner,
                    @Nonnull String name,
                    @Nonnull String moduleFileDirectoryPath,
                    @Nonnull String externalConfigPath) {
    super(owner, name, name.replaceAll("(/|\\\\)", "_"));
    myId = id;
    myExternalConfigPath = externalConfigPath;
    myArtifacts = Collections.emptyList();
    setModuleDirPath(moduleFileDirectoryPath);
  }

  @Nonnull
  @Override
  public String getId() {
    return myId;
  }

  @Nonnull
  @Override
  public String getLinkedExternalProjectPath() {
    return myExternalConfigPath;
  }

  @Nonnull
  public String getModuleDirPath() {
    return myModuleDirPath;
  }

  public void setModuleDirPath(@Nonnull String path) {
    myModuleDirPath = path;
  }

  public boolean isInheritProjectCompileOutputPath() {
    return myInheritProjectCompileOutputPath;
  }

  public void setInheritProjectCompileOutputPath(boolean inheritProjectCompileOutputPath) {
    myInheritProjectCompileOutputPath = inheritProjectCompileOutputPath;
  }

  /**
   * Allows to get file system path of the compile output of the source of the target type.
   *
   * @param type  target source type
   * @return      file system path to use for compile output for the target source type;
   *              {@link JavaProjectData#getCompileOutputPath() project compile output path} should be used if current module
   *              doesn't provide specific compile output path
   */
  @javax.annotation.Nullable
  public String getCompileOutputPath(@Nonnull ExternalSystemSourceType type) {
    return myCompileOutputPaths.get(type);
  }

  public void setCompileOutputPath(@Nonnull ExternalSystemSourceType type, @javax.annotation.Nullable String path) {
    if (path == null) {
      myCompileOutputPaths.remove(type);
      return;
    }
    myCompileOutputPaths.put(type, ExternalSystemApiUtil.toCanonicalPath(path));
  }

  @javax.annotation.Nullable
  public String getGroup() {
    return group;
  }

  public void setGroup(@javax.annotation.Nullable String group) {
    this.group = group;
  }

  @Nullable
  public String getVersion() {
    return version;
  }

  public void setVersion(@javax.annotation.Nullable String version) {
    this.version = version;
  }

  @Nonnull
  public List<File> getArtifacts() {
    return myArtifacts;
  }

  public void setArtifacts(@Nonnull List<File> artifacts) {
    myArtifacts = artifacts;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ModuleData)) return false;
    if (!super.equals(o)) return false;

    ModuleData that = (ModuleData)o;

    if (group != null ? !group.equals(that.group) : that.group != null) return false;
    if (version != null ? !version.equals(that.version) : that.version != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (group != null ? group.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return String.format("module '%s:%s:%s'",
                         group == null ? "" : group,
                         getExternalName(),
                         version == null ? "" : version);
  }
}
