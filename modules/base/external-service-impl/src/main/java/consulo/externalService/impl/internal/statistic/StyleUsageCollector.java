package consulo.externalService.impl.internal.statistic;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.statistic.CollectUsagesException;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.externalService.statistic.UsagesCollector;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.style.StyleManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Set;

@ExtensionImpl
public class StyleUsageCollector extends UsagesCollector {
    @Nonnull
    @Override
    public Set<UsageDescriptor> getUsages(@Nullable Project project) throws CollectUsagesException {
        String styleId = StyleManager.get().getCurrentStyle().getId();
        String key = Platform.current().os().name() + " - ";
        boolean darkTheme = Platform.current().user().darkTheme();
        return Collections.singleton(new UsageDescriptor(key + " - " +  styleId + " - " + darkTheme, 1));
    }

    @Nonnull
    @Override
    public String getGroupId() {
        return "consulo:style";
    }
}
