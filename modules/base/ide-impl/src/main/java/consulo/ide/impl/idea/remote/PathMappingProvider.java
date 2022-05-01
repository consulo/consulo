package consulo.ide.impl.idea.remote;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.ide.impl.idea.util.PathMappingSettings;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author traff
 */
public abstract class PathMappingProvider {
  public static ExtensionPointName<PathMappingProvider> EP_NAME = ExtensionPointName.create("consulo.remote.pathMappingProvider");

  public static List<PathMappingProvider> getSuitableMappingProviders(final RemoteSdkAdditionalData data) {
    return Lists
      .newArrayList(Iterables.filter(Arrays.asList(EP_NAME.getExtensions()), new Predicate<PathMappingProvider>() {
        @Override
        public boolean apply(PathMappingProvider provider) {
          return provider.accepts(data);
        }
      }));
  }

  @Nonnull
  public abstract String getProviderPresentableName(@Nonnull RemoteSdkAdditionalData data);

  public abstract boolean accepts(@Nullable RemoteSdkAdditionalData data);

  @Nonnull
  public abstract PathMappingSettings getPathMappingSettings(@Nonnull Project project, @Nonnull RemoteSdkAdditionalData data);
}
