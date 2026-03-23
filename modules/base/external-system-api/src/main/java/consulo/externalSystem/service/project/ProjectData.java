package consulo.externalSystem.service.project;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import org.jspecify.annotations.Nullable;

/**
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/1/11 1:30 PM
 */
public class ProjectData extends AbstractNamedData implements ExternalConfigPathAware, Identifiable {

    private static final long serialVersionUID = 1L;

    private final String myLinkedExternalProjectPath;

    private String myIdeProjectFileDirectoryPath;

    private String group;
    private String version;
    private String ideGrouping;

    @Deprecated
    public ProjectData(ProjectSystemId owner, String ideProjectFileDirectoryPath, String linkedExternalProjectPath) {
        super(owner, "unnamed");
        myLinkedExternalProjectPath = ExternalSystemApiUtil.toCanonicalPath(linkedExternalProjectPath);
        myIdeProjectFileDirectoryPath = ExternalSystemApiUtil.toCanonicalPath(ideProjectFileDirectoryPath);
    }

    public ProjectData(ProjectSystemId owner, String externalName, String ideProjectFileDirectoryPath, String linkedExternalProjectPath) {
        super(owner, externalName);
        myLinkedExternalProjectPath = ExternalSystemApiUtil.toCanonicalPath(linkedExternalProjectPath);
        myIdeProjectFileDirectoryPath = ExternalSystemApiUtil.toCanonicalPath(ideProjectFileDirectoryPath);
    }

    @Deprecated
    @Override
    public void setName(String name) {
        super.setExternalName(name);
        super.setInternalName(name);
    }

    public String getIdeProjectFileDirectoryPath() {
        return myIdeProjectFileDirectoryPath;
    }

    public void setIdeProjectFileDirectoryPath(String ideProjectFileDirectoryPath) {
        myIdeProjectFileDirectoryPath = ExternalSystemApiUtil.toCanonicalPath(ideProjectFileDirectoryPath);
    }

    @Override
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ProjectData project = (ProjectData) o;

        if (!myIdeProjectFileDirectoryPath.equals(project.myIdeProjectFileDirectoryPath)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("%s project '%s'", getOwner().toString().toLowerCase(), getExternalName());
    }

    public @Nullable String getIdeGrouping() {
        return ideGrouping;
    }

    public void setIdeGrouping(@Nullable String ideGrouping) {
        this.ideGrouping = ideGrouping;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getId() {
        return "";
    }
}
