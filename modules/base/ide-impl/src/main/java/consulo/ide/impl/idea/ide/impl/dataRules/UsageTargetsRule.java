package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.dataContext.DataSnapshot;
import consulo.usage.UsageTarget;
import consulo.usage.UsageTargetUtil;
import consulo.usage.UsageView;
import org.jspecify.annotations.Nullable;

public final class UsageTargetsRule {
    static @Nullable UsageTarget[] getData(DataSnapshot dataProvider) {
        return UsageTargetUtil.findUsageTargets(dataProvider);
    }
}
