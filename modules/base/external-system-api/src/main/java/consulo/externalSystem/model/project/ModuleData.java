package consulo.externalSystem.model.project;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.rt.model.ExternalSystemSourceType;
import consulo.externalSystem.service.project.AbstractNamedData;
import consulo.externalSystem.service.project.ExternalConfigPathAware;
import consulo.externalSystem.service.project.Identifiable;
import consulo.externalSystem.service.project.Named;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Zhdanov
 * @since 8/8/11 12:11 PM
 */
public class ModuleData extends AbstractNamedData implements Named, ExternalConfigPathAware, Identifiable {

    private static final long serialVersionUID = 1L;

    private final Map<ExternalSystemSourceType, String> myCompileOutputPaths = new HashMap<>();

    private final String myId;

    private final String myExternalConfigPath;

    private String myModuleDirPath;
    private @Nullable String group;
    private @Nullable String version;

    private List<File> myArtifacts;

    private boolean myInheritProjectCompileOutputPath = true;

    private @Nullable String productionModuleId;
    private String moduleName;

    private String @Nullable [] ideModuleGroup;

    @Deprecated
    public ModuleData(ProjectSystemId owner, String name, String moduleDir, String externalConfigPath) {
        this("", owner, name, moduleDir, externalConfigPath);
    }

    public ModuleData(String id, ProjectSystemId owner, String externalName, String moduleFileDirectoryPath, String externalConfigPath) {
        super(owner, externalName, externalName.replaceAll("(/|\\\\)", "_"));
        myId = id;
        myExternalConfigPath = externalConfigPath;
        myArtifacts = Collections.emptyList();
        setModuleDirPath(moduleFileDirectoryPath);
        this.moduleName = externalName;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public String getLinkedExternalProjectPath() {
        return myExternalConfigPath;
    }

    public String getModuleDirPath() {
        return myModuleDirPath;
    }

    public void setModuleDirPath(String path) {
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
     * @param type target source type
     * @return file system path to use for compile output for the target source type;
     * {@link JavaProjectData#getCompileOutputPath() project compile output path} should be used if current module
     * doesn't provide specific compile output path
     */
    public @Nullable String getCompileOutputPath(ExternalSystemSourceType type) {
        return myCompileOutputPaths.get(type);
    }

    public void setCompileOutputPath(ExternalSystemSourceType type, @Nullable String path) {
        if (path == null) {
            myCompileOutputPaths.remove(type);
            return;
        }
        myCompileOutputPaths.put(type, ExternalSystemApiUtil.toCanonicalPath(path));
    }

    public @Nullable String getGroup() {
        return group;
    }

    public void setGroup(@Nullable String group) {
        this.group = group;
    }

    public @Nullable String getVersion() {
        return version;
    }

    public void setVersion(@Nullable String version) {
        this.version = version;
    }

    public List<File> getArtifacts() {
        return myArtifacts;
    }

    public void setArtifacts(List<File> artifacts) {
        myArtifacts = artifacts;
    }

    public @Nullable String getIdeGrouping() {
        if (ideModuleGroup != null) {
            return StringUtil.join(ideModuleGroup, ".");
        }
        else {
            return getInternalName();
        }
    }

    public String @Nullable [] getIdeModuleGroup() {
        return ideModuleGroup;
    }

    /**
     * Set or remove explicit module group for this module.
     *
     * @deprecated explicit module groups are replaced by automatic module grouping accordingly to qualified names of modules
     * ([IDEA-166061](https://youtrack.jetbrains.com/issue/IDEA-166061) for details), so this method must not be used anymore, group names
     * must be prepended to the module name, separated by dots, instead.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public void setIdeModuleGroup(String @Nullable [] ideModuleGroup) {
        this.ideModuleGroup = ideModuleGroup;
    }

    public @Nullable String getIdeParentGrouping() {
        if (ideModuleGroup != null) {
            return StringUtil.nullize(StringUtil.join(ArrayUtil.remove(ideModuleGroup, ideModuleGroup.length - 1), "."));
        }
        else {
            String name = getInternalName();
            int i = name.lastIndexOf("." + moduleName);
            if (i > -1) {
                return name.substring(0, i);
            }
            else {
                return null;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ModuleData)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ModuleData that = (ModuleData) o;

        if (group != null ? !group.equals(that.group) : that.group != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }

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
        return String.format("module '%s:%s:%s'", group == null ? "" : group, getExternalName(), version == null ? "" : version);
    }
}
