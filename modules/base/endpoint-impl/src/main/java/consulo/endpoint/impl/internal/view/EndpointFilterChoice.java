package consulo.endpoint.impl.internal.view;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public record EndpointFilterChoice(String id, LocalizeValue text, @Nullable Image icon) {
}
