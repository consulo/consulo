package consulo.ide.impl.idea.remote;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointName;
import consulo.ide.impl.idea.util.PathMappingSettings;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author traff
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class PathMappingProvider {
  public static List<PathMappingProvider> getSuitableMappingProviders(final RemoteSdkAdditionalData data) {
    return ContainerUtil.filter(Application.get().getExtensionList(PathMappingProvider.class), provider -> provider.accepts(data));
  }

  @Nonnull
  public abstract String getProviderPresentableName(@Nonnull RemoteSdkAdditionalData data);

  public abstract boolean accepts(@Nullable RemoteSdkAdditionalData data);

  @Nonnull
  public abstract PathMappingSettings getPathMappingSettings(@Nonnull Project project, @Nonnull RemoteSdkAdditionalData data);
}
