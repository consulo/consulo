package consulo.endpoint.impl.internal.diagram;

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public record ServicesDiagramMember(String text, String framework, boolean client, @Nullable Image icon) {
}
