package consulo.externalSystem.model;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
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
 * @since 2012-02-14
 */
public class ProjectSystemId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Nonnull
    public static final ProjectSystemId IDE = new ProjectSystemId("IDE", LocalizeValue.localizeTODO("IDE"));

    @Nonnull
    private final String myId;

    @Nonnull
    private final String myCapitalizedId;

    @Nonnull
    private final LocalizeValue myDisplayName;

    @Nonnull
    private final Image myIcon;

    @Nonnull
    private final Image myModuleIcon;

    @Deprecated
    public ProjectSystemId(@Nonnull String id) {
        this(id, LocalizeValue.of(StringUtil.capitalize(id.toLowerCase(Locale.ROOT))));
    }

    @Deprecated
    public ProjectSystemId(@Nonnull String id, @Nonnull LocalizeValue displayName) {
        this(id, displayName, PlatformIconGroup.actionsHelp());
    }

    public ProjectSystemId(@Nonnull String id,
                           @Nonnull LocalizeValue displayName,
                           @Nonnull Image icon) {
        this(id, displayName, icon, PlatformIconGroup.nodesModule());
    }

    public ProjectSystemId(@Nonnull String id,
                           @Nonnull LocalizeValue displayName,
                           @Nonnull Image icon,
                           @Nonnull Image moduleIcon) {
        myId = id;
        myCapitalizedId = StringUtil.capitalize(id.toLowerCase(Locale.ROOT));
        myDisplayName = displayName;
        myIcon = icon;
        myModuleIcon = moduleIcon;
    }

    @Nonnull
    public String getId() {
        return myId;
    }

    @Nonnull
    public final Image getIcon() {
        return myIcon;
    }

    @Nonnull
    public final Image getModuleIcon() {
        return myModuleIcon;
    }

    @Nonnull
    @Deprecated
    public LocalizeValue getReadableName() {
        return getDisplayName();
    }

    @Nonnull
    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }

    @Nonnull
    public String getToolWindowId() {
        return myId;
    }

    @Nonnull
    public String getLibraryPrefix() {
        return myCapitalizedId;
    }

    @Nonnull
    public String getRunConfigurationId() {
        return myCapitalizedId + "RunConfiguration";
    }

    @Override
    public String toString() {
        return myId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectSystemId owner = (ProjectSystemId) o;

        return myId.equals(owner.myId);
    }

    @Override
    public int hashCode() {
        return myId.hashCode();
    }
}
