package consulo.externalSystem.service.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 2013-07-16
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalSystemConfigLocator {
    ExtensionPointName<ExternalSystemConfigLocator> EP_NAME = ExtensionPointName.create(ExternalSystemConfigLocator.class);

    @Nonnull
    ProjectSystemId getTargetExternalSystemId();

    /**
     * Allows to adjust target external system config file.
     * <p/>
     * Example: 'gradle' external system stores config file parent as config path and we might want to locate exact config file
     * given it's directory file.
     *
     * @param configPath base config file
     * @return config file to use (if any)
     */
    @Nullable
    VirtualFile adjust(@Nonnull VirtualFile configPath);

    /**
     * Returns all configuration files used by external system to build the project.
     *
     * @param externalProjectSettings external system project settings
     * @return external system project config files
     */
    @Nonnull
    List<VirtualFile> findAll(@Nonnull ExternalProjectSettings externalProjectSettings);
}
