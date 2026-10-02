package consulo.endpoint.impl.internal.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointType;
import consulo.endpoint.FrameworkPresentation;
import consulo.endpoint.presentation.EndpointMethodPresentation;
import consulo.endpoint.presentation.HttpMethodPresentation;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.ui.TextAttribute;
import consulo.ui.TextEffect;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.ColoredItemPresentation;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.util.TextAttributeUtil;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EndpointRowData<G, E> {
    private final EndpointProvider<G, E> myProvider;
    private final G myGroup;
    private final E myEndpoint;
    private final EndpointRowKey myKey;
    private final @Nullable Image myIcon;
    private final String myMethodText;
    private final int myMethodOrder;
    private final String myUrl;
    private final List<EndpointTextFragment> myUrlFragments;
    private final String myLocation;
    private final @Nullable ColorValue myBackground;
    private final String myTypeTag;
    private final LocalizeValue myTypeText;
    private final String myFrameworkTag;
    private final String myFrameworkTitle;
    private final String mySearchText;

    private EndpointRowData(
        EndpointProvider<G, E> provider,
        G group,
        E endpoint,
        EndpointRowKey key,
        @Nullable Image icon,
        String methodText,
        int methodOrder,
        String url,
        List<EndpointTextFragment> urlFragments,
        String location,
        @Nullable ColorValue background,
        EndpointType type,
        FrameworkPresentation framework
    ) {
        myProvider = provider;
        myGroup = group;
        myEndpoint = endpoint;
        myKey = key;
        myIcon = icon;
        myMethodText = methodText;
        myMethodOrder = methodOrder;
        myUrl = url;
        myUrlFragments = urlFragments;
        myLocation = location;
        myBackground = background;
        myTypeTag = type.getQueryTag();
        myTypeText = type.getLocalizedMessage();
        myFrameworkTag = framework.getQueryTag();
        myFrameworkTitle = framework.getTitle();
        mySearchText = (methodText + " " + url + " " + location).toLowerCase(Locale.ROOT);
    }

    @RequiredReadAction
    public static <G, E> EndpointRowData<G, E> create(
        EndpointProvider<G, E> provider,
        G group,
        E endpoint,
        ItemPresentation presentation,
        EndpointType type,
        FrameworkPresentation framework,
        EditorColorsScheme scheme,
        @Nullable ColorValue defaultBackground
    ) {
        String url = notNullize(presentation.getPresentableText());
        String location = notNullize(presentation.getLocationString());

        String methodText = "";
        List<String> methods = List.of();
        int methodOrder = HttpMethodPresentation.getHttpMethodOrder(null);
        if (presentation instanceof EndpointMethodPresentation methodPresentation) {
            methodText = notNullize(methodPresentation.getEndpointMethodPresentation());
            methods = List.copyOf(methodPresentation.getEndpointMethods());
            methodOrder = methodPresentation.getEndpointMethodOrder();
        }

        ColorValue forcedForeground = presentation instanceof PresentationData data ? data.getForcedTextForeground() : null;
        TextAttribute urlAttribute = getTextAttribute(presentation, scheme, forcedForeground);

        List<EndpointTextFragment> fragments = new ArrayList<>();
        if (presentation instanceof PresentationData data && !data.getColoredText().isEmpty()) {
            for (PresentableNodeDescriptor.ColoredFragment fragment : data.getColoredText()) {
                TextAttribute attribute = TextAttributeUtil.toTextAttribute(fragment.getAttributes(), forcedForeground);
                for (TextEffect effect : urlAttribute.getEffects()) {
                    attribute = attribute.withEffect(effect);
                }
                fragments.add(new EndpointTextFragment(fragment.getText(), attribute));
            }
        }
        else {
            fragments.add(new EndpointTextFragment(LocalizeValue.of(url), urlAttribute));
        }

        Image icon = presentation.getIcon();
        if (icon == null) {
            icon = type.getIcon();
        }

        ColorValue background = presentation instanceof PresentationData data ? data.getBackground() : null;
        if (background == null) {
            background = defaultBackground;
        }

        EndpointRowKey key = new EndpointRowKey(null, provider.getClass().getName(), url, methods, location);
        return new EndpointRowData<>(
            provider,
            group,
            endpoint,
            key,
            icon,
            methodText,
            methodOrder,
            url,
            List.copyOf(fragments),
            location,
            background,
            type,
            framework
        );
    }

    private static TextAttribute getTextAttribute(
        ItemPresentation presentation,
        EditorColorsScheme scheme,
        @Nullable ColorValue forcedForeground
    ) {
        TextAttribute attribute = forcedForeground == null ? TextAttribute.REGULAR : new TextAttribute(Font.PLAIN, forcedForeground);

        TextAttributesKey key = presentation instanceof ColoredItemPresentation coloredPresentation
            ? coloredPresentation.getTextAttributesKey()
            : null;
        if (key == null) {
            return attribute;
        }

        TextAttributes attributes = scheme.getAttributes(key);
        if (attributes == null) {
            return CodeInsightColors.DEPRECATED_ATTRIBUTES.equals(key) ? attribute.withEffect(TextEffect.STRIKEOUT) : attribute;
        }

        TextAttribute schemeAttribute = TextAttributeUtil.toTextAttribute(attributes);
        TextAttribute result = new TextAttribute(
            TextAttributeUtil.getFontStyle(schemeAttribute),
            forcedForeground != null ? forcedForeground : schemeAttribute.getForegroundColor()
        );
        for (TextEffect effect : schemeAttribute.getEffects()) {
            result = result.withEffect(effect);
        }
        return result;
    }

    private static String notNullize(@Nullable String text) {
        return text == null ? "" : text;
    }

    public EndpointProvider<G, E> getProvider() {
        return myProvider;
    }

    public G getGroup() {
        return myGroup;
    }

    public E getEndpoint() {
        return myEndpoint;
    }

    public EndpointRowKey getKey() {
        return myKey;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public String getMethodText() {
        return myMethodText;
    }

    public int getMethodOrder() {
        return myMethodOrder;
    }

    public String getUrl() {
        return myUrl;
    }

    public List<EndpointTextFragment> getUrlFragments() {
        return myUrlFragments;
    }

    public String getLocation() {
        return myLocation;
    }

    public @Nullable ColorValue getBackground() {
        return myBackground;
    }

    public String getTypeTag() {
        return myTypeTag;
    }

    public LocalizeValue getTypeText() {
        return myTypeText;
    }

    public String getFrameworkTag() {
        return myFrameworkTag;
    }

    public String getFrameworkTitle() {
        return myFrameworkTitle;
    }

    public String getSearchText() {
        return mySearchText;
    }
}
