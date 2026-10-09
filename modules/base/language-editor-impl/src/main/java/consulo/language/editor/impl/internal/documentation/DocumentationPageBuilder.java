/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.editor.impl.internal.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.documentation.CompositeDocumentationProvider;
import consulo.language.editor.documentation.DocumentationMarkup;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.DocumentationProviderEx;
import consulo.language.editor.documentation.ExternalDocumentationHandler;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.HtmlView;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class DocumentationPageBuilder {
    private static final Logger LOG = Logger.getInstance(DocumentationPageBuilder.class);

    private static final int ICON_SIZE = 16;

    private static final Pattern ICON_TAG = Pattern.compile("<icon\\s+src\\s*=\\s*['\"]([^'\"]+)['\"]\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_SOURCE = Pattern.compile("(<img\\b[^>]*\\bsrc\\s*=\\s*)(['\"])([^'\"]+)\\2", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTERNAL_LINK = Pattern.compile("(<a\\s*href=[\"']http[^>]*>)([^>]*)(</a>)");

    private final Map<String, Image> myImages = new LinkedHashMap<>();

    private DocumentationPageBuilder() {
    }

    @RequiredReadAction
    public static DocumentationPage build(
        PsiElement element,
        String documentation,
        @Nullable String externalUrl,
        @Nullable String anchor,
        @Nullable DocumentationProvider provider
    ) {
        DocumentationPageBuilder builder = new DocumentationPageBuilder();

        String html = builder.decorate(element, documentation, externalUrl, provider);
        html = builder.resolveImages(element, provider, html);

        SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);

        PsiElement originalElement = DocumentationManagerHelper.getOriginalElement(element);
        boolean externalDocumentation = provider != null && CompositeDocumentationProvider.hasUrlsFor(provider, element, originalElement);
        boolean editableSource = element instanceof Navigatable navigatable && navigatable.canNavigateToSource();

        return new DocumentationPage(
            pointer,
            html,
            Map.copyOf(builder.myImages),
            anchor,
            getTitle(element),
            externalUrl,
            externalDocumentation,
            editableSource
        );
    }

    public static DocumentationPage message(String message) {
        return DocumentationPage.message(DocumentationMarkup.CONTENT_START + message + DocumentationMarkup.CONTENT_END);
    }

    @RequiredReadAction
    public static String getTitle(PsiElement element) {
        String title = SymbolPresentationUtil.getSymbolPresentableText(element);
        return title != null ? title : StringUtil.notNullize(element.getText());
    }

    @RequiredReadAction
    private String decorate(PsiElement element, String documentation, @Nullable String externalUrl, @Nullable DocumentationProvider provider) {
        String text = documentation;
        text = StringUtil.replaceIgnoreCase(text, "</html>", "");
        text = StringUtil.replaceIgnoreCase(text, "</body>", "");
        text = StringUtil.replaceIgnoreCase(text, DocumentationMarkup.SECTIONS_START + DocumentationMarkup.SECTIONS_END, "");
        text = StringUtil.replaceIgnoreCase(text, DocumentationMarkup.SECTIONS_START + "<p>" + DocumentationMarkup.SECTIONS_END, "");

        boolean hasContent = text.contains(DocumentationMarkup.CONTENT_START);
        if (!hasContent) {
            if (!text.contains(DocumentationMarkup.DEFINITION_START)) {
                int bodyStart = findContentStart(text);
                if (bodyStart > 0) {
                    text = text.substring(0, bodyStart) + DocumentationMarkup.CONTENT_START + text.substring(bodyStart)
                        + DocumentationMarkup.CONTENT_END;
                }
                else {
                    text = DocumentationMarkup.CONTENT_START + text + DocumentationMarkup.CONTENT_END;
                }
                hasContent = true;
            }
            else if (!text.contains(DocumentationMarkup.SECTIONS_START)) {
                text = StringUtil.replaceIgnoreCase(text, DocumentationMarkup.DEFINITION_START, "<div class='definition-only'><pre>");
            }
        }

        if (!text.contains(DocumentationMarkup.DEFINITION_START)) {
            text = text.replace("class='content'", "class='content-only'");
        }
        else {
            text = addDefinitionSeparator(text);
        }

        String location = getLocationText(element);
        if (location != null) {
            text = text + getBottom(hasContent) + location + "</div>";
        }

        String links = getExternalText(element, externalUrl, provider);
        if (links != null) {
            text = text + getBottom(location != null) + links + "</div>";
        }

        text = text.replaceAll("<p>\\s*(<(?:[uo]l|h\\d|p))", "$1");
        text = addExternalLinksIcon(text);
        return text;
    }

    @RequiredReadAction
    private @Nullable String getExternalText(PsiElement element, @Nullable String externalUrl, @Nullable DocumentationProvider provider) {
        if (provider == null) {
            return null;
        }

        PsiElement originalElement = DocumentationManagerHelper.getOriginalElement(element);
        if (!shouldShowExternalDocumentationLink(provider, element, originalElement)) {
            return null;
        }

        String title = XmlStringUtil.escapeText(getTitle(element));
        if (externalUrl == null) {
            List<String> urls = provider.getUrlFor(element, originalElement);
            if (urls == null) {
                return null;
            }

            StringBuilder result = new StringBuilder();
            for (String url : urls) {
                String link = getLink(title, url);
                if (link == null) {
                    result = null;
                    break;
                }

                if (!result.isEmpty()) {
                    result.append("<p>");
                }
                result.append(link);
            }

            if (result != null) {
                return result.toString();
            }
        }
        else {
            String link = getLink(title, externalUrl);
            if (link != null) {
                return link;
            }
        }

        return "<a href='" + DocumentationLinks.EXTERNAL_DOC + "'>" + CodeInsightLocalize.javadocExternalDocumentationFor(title).get()
            + iconTag(PlatformIconGroup.ideExternallink()) + "</a>";
    }

    private static @Nullable String getLink(String title, String url) {
        String hostname = getHostname(url);
        if (hostname == null) {
            return null;
        }

        String text = CodeInsightLocalize.javadocExternalElementOnHost(title, hostname).get();
        return "<a href='" + url + "'>" + text + "</a>";
    }

    private static boolean shouldShowExternalDocumentationLink(
        DocumentationProvider provider,
        PsiElement element,
        @Nullable PsiElement originalElement
    ) {
        if (provider instanceof CompositeDocumentationProvider composite) {
            for (DocumentationProvider p : composite.getProviders()) {
                if (p instanceof ExternalDocumentationHandler handler) {
                    return handler.canHandleExternal(element, originalElement);
                }
            }
        }
        else if (provider instanceof ExternalDocumentationHandler handler) {
            return handler.canHandleExternal(element, originalElement);
        }
        return true;
    }

    private static @Nullable String getHostname(String url) {
        try {
            return new URL(url).toURI().getHost();
        }
        catch (URISyntaxException | MalformedURLException ignored) {
            return null;
        }
    }

    private static String addDefinitionSeparator(String text) {
        int definition = text.indexOf(DocumentationMarkup.DEFINITION_START);
        int end = text.indexOf(DocumentationMarkup.DEFINITION_END, definition);
        if (end < 0) {
            return text;
        }

        int afterDefinition = end + DocumentationMarkup.DEFINITION_END.length();
        return text.substring(0, afterDefinition) + "<hr class='definition-separator'>" + text.substring(afterDefinition);
    }

    private static int findContentStart(String text) {
        int index = StringUtil.indexOfIgnoreCase(text, "<body>", 0);
        if (index >= 0) {
            return index + 6;
        }
        index = StringUtil.indexOfIgnoreCase(text, "</head>", 0);
        if (index >= 0) {
            return index + 7;
        }
        index = StringUtil.indexOfIgnoreCase(text, "</style>", 0);
        if (index >= 0) {
            return index + 8;
        }
        index = StringUtil.indexOfIgnoreCase(text, "<html>", 0);
        if (index >= 0) {
            return index + 6;
        }
        return -1;
    }

    private static String getBottom(boolean hasContent) {
        return "<div class='" + (hasContent ? "bottom" : "bottom-no-content") + "'>";
    }

    private String addExternalLinksIcon(String text) {
        Matcher matcher = EXTERNAL_LINK.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = matcher.group(1) + matcher.group(2) + iconTag(PlatformIconGroup.ideExternallink()) + matcher.group(3);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @RequiredReadAction
    private static @Nullable String getLocationText(PsiElement element) {
        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
        Module module = fileIndex.getModuleForFile(virtualFile);
        if (module != null) {
            if (ModuleManager.getInstance(element.getProject()).getModules().length == 1) {
                return null;
            }
            return iconTag(PlatformIconGroup.nodesModule()) + "&nbsp;" + XmlStringUtil.escapeText(module.getName());
        }

        for (OrderEntry entry : fileIndex.getOrderEntriesForFile(virtualFile)) {
            if (entry instanceof OrderEntryWithTracking) {
                return iconTag(PlatformIconGroup.nodesPplib()) + "&nbsp;" + XmlStringUtil.escapeText(entry.getPresentableName());
            }
        }
        return null;
    }

    private static String iconTag(ImageKey imageKey) {
        return "<icon src='" + imageKey.getGroupId() + "@" + imageKey.getImageId() + "'>";
    }

    @RequiredReadAction
    private String resolveImages(PsiElement element, @Nullable DocumentationProvider provider, String html) {
        Matcher icons = ICON_TAG.matcher(html);
        StringBuilder withIcons = new StringBuilder();
        while (icons.find()) {
            ImageKey imageKey = ImageKey.fromString(icons.group(1), ICON_SIZE, ICON_SIZE);
            String replacement = imageKey == null
                ? ""
                : "<img src=\"" + register(imageKey) + "\" width=\"" + ICON_SIZE + "\" height=\"" + ICON_SIZE + "\">";
            icons.appendReplacement(withIcons, Matcher.quoteReplacement(replacement));
        }
        icons.appendTail(withIcons);

        Matcher images = IMAGE_SOURCE.matcher(withIcons);
        StringBuilder result = new StringBuilder();
        while (images.find()) {
            String source = images.group(3);
            Image image = source.startsWith(HtmlView.IMAGE_SRC_PREFIX) ? null : getElementImage(element, provider, source);
            String replacement = image == null ? images.group() : images.group(1) + images.group(2) + register(image) + images.group(2);
            images.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        images.appendTail(result);
        return result.toString();
    }

    private String register(Image image) {
        String id = "documentation-" + myImages.size();
        myImages.put(id, image);
        return HtmlView.IMAGE_SRC_PREFIX + id;
    }

    @RequiredReadAction
    private static @Nullable Image getElementImage(PsiElement element, @Nullable DocumentationProvider provider, String imageSpec) {
        if (!(provider instanceof CompositeDocumentationProvider composite)) {
            return provider instanceof DocumentationProviderEx providerEx ? toImage(providerEx.getLocalImageForElement(element, imageSpec)) : null;
        }

        for (DocumentationProvider p : composite.getAllProviders()) {
            if (p instanceof DocumentationProviderEx providerEx) {
                Image image = toImage(providerEx.getLocalImageForElement(element, imageSpec));
                if (image != null) {
                    return image;
                }
            }
        }
        return null;
    }

    private static @Nullable Image toImage(java.awt.@Nullable Image image) {
        if (image == null) {
            return null;
        }

        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width <= 0 || height <= 0) {
            return null;
        }

        BufferedImage raster;
        if (image instanceof BufferedImage bufferedImage) {
            raster = bufferedImage;
        }
        else {
            raster = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = raster.createGraphics();
            try {
                graphics.drawImage(image, 0, 0, null);
            }
            finally {
                graphics.dispose();
            }
        }

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(raster, "png", stream);
            return Image.fromBytes(Image.ImageType.PNG, stream.toByteArray());
        }
        catch (IOException e) {
            LOG.warn("Failed to convert a documentation image", e);
            return null;
        }
    }
}
