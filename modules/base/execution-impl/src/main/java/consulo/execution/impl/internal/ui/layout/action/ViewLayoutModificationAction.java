package consulo.execution.impl.internal.ui.layout.action;

import consulo.ui.ex.content.Content;
import jakarta.annotation.Nonnull;

public interface ViewLayoutModificationAction {
    @Nonnull
    Content getContent();
}
