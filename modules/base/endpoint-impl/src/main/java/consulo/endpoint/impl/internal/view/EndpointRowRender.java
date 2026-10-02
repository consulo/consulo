package consulo.endpoint.impl.internal.view;

import consulo.ui.RenderItem;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TextItemRender;
import consulo.ui.color.ColorValue;

public final class EndpointRowRender implements TextItemRender<EndpointViewRow> {
    private static final String GAP = "  ";

    @Override
    public void render(TextItemPresentation presentation, RenderItem<EndpointViewRow> item) {
        EndpointViewRow value = item.getValue();
        if (value instanceof EndpointModuleRow moduleRow) {
            presentation.withIcon(moduleRow.getIcon());
            presentation.append(moduleRow.getName(), TextAttribute.REGULAR_BOLD);
        }
        else if (value instanceof EndpointRow<?, ?> row) {
            renderEndpoint(presentation, row, item.isSelected());
        }
    }

    private static void renderEndpoint(TextItemPresentation presentation, EndpointRow<?, ?> row, boolean selected) {
        EndpointRowData<?, ?> data = row.getData();
        presentation.withIcon(data.getIcon());

        if (!data.getMethodText().isEmpty()) {
            presentation.append(data.getMethodText(), TextAttribute.REGULAR_BOLD);
            presentation.append(" ");
        }

        for (EndpointTextFragment fragment : data.getUrlFragments()) {
            presentation.append(fragment.text(), fragment.attribute());
        }

        if (!row.isCompact()) {
            if (!data.getLocation().isEmpty()) {
                presentation.append(GAP + data.getLocation(), TextAttribute.GRAY);
            }
            presentation.append(GAP);
            presentation.append(data.getTypeText(), TextAttribute.GRAY);
            presentation.append(GAP + data.getFrameworkTitle(), TextAttribute.GRAY);
        }

        ColorValue background = data.getBackground();
        if (!selected && background != null) {
            presentation.withBackgroundColor(background);
        }
    }
}
