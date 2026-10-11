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
package consulo.language.editor.impl.internal.intention.setting;

import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.configurable.Settings;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.externalService.plugin.PluginsConfigurable;
import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.language.editor.internal.intention.ResourceTextDescriptor;
import consulo.language.editor.internal.intention.TextDescriptor;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.Hyperlink;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class IntentionDescriptionPanel {
    private static final Logger LOG = Logger.getInstance(IntentionDescriptionPanel.class);

    private static final String SPOT_OPEN = "<spot>";
    private static final String SPOT_CLOSE = "</spot>";

    private final EditorBoxBuilderFactory myEditorBoxBuilderFactory;

    private static final int DESCRIPTION_PROPORTION = 34;
    private static final int EXAMPLES_PROPORTION = 50;

    private final DockLayout myRoot;
    private final HtmlView myDescription;
    private final DockLayout myBefore;
    private final DockLayout myAfter;
    private final DockLayout myPoweredBy;

    private final List<EditorBox> myBeforeBoxes = new ArrayList<>();
    private final List<EditorBox> myAfterBoxes = new ArrayList<>();

    private @Nullable Component myBeforeContent;
    private @Nullable Component myAfterContent;

    @RequiredUIAccess
    public IntentionDescriptionPanel(EditorBoxBuilderFactory editorBoxBuilderFactory) {
        myEditorBoxBuilderFactory = editorBoxBuilderFactory;

        myDescription = HtmlView.create();

        myBefore = DockLayout.create(Space.NONE);
        myAfter = DockLayout.create(Space.NONE);
        myPoweredBy = DockLayout.create(Space.NONE);

        TwoComponentSplitLayout examples = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        examples.setFirstComponent(LabeledLayout.create(CodeInsightLocalize.intentionSettingsBeforeTitle(), myBefore));
        examples.setSecondComponent(LabeledLayout.create(CodeInsightLocalize.intentionSettingsAfterTitle(), myAfter));
        examples.setProportion(EXAMPLES_PROPORTION);

        TwoComponentSplitLayout sections = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        sections.setFirstComponent(LabeledLayout.create(CodeInsightLocalize.dialogIntentionSettingsDescriptionPanelTitle(), myDescription));
        sections.setSecondComponent(examples);
        sections.setProportion(DESCRIPTION_PROPORTION);

        myRoot = DockLayout.create(Space.SMALL);
        myRoot.center(sections);
        myRoot.bottom(myPoweredBy);
        myRoot.paddingBuilder().allSet(Space.MEDIUM).apply();
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void showIntention(IntentionActionMetaData metaData, @Nullable String filter) {
        TextDescriptor description = metaData.getDescription();

        String html;
        try {
            html = description == null
                ? CodeInsightLocalize.underConstructionString().get()
                : SearchHighlightUtil.markupHtml(description.getText(), filter, "<b><font color=\"#d14\">", "</font></b>");
        }
        catch (IOException e) {
            LOG.warn(e);
            html = CodeInsightLocalize.underConstructionString().get();
        }
        myDescription.render(new HtmlView.RenderData(html));

        showPoweredBy(metaData.getPluginId());

        showExamples(myBefore, myBeforeBoxes, metaData.getExampleUsagesBefore());
        showExamples(myAfter, myAfterBoxes, metaData.getExampleUsagesAfter());
    }

    @RequiredUIAccess
    public void showCategory(LocalizeValue category) {
        myDescription.render(new HtmlView.RenderData(CodeInsightLocalize.intentionSettingsCategoryText(category).get()));

        showPoweredBy(null);

        showExamples(myBefore, myBeforeBoxes, templateExample("before"));
        showExamples(myAfter, myAfterBoxes, templateExample("after"));
    }

    private TextDescriptor[] templateExample(String kind) {
        URL url = IntentionDescriptionPanel.class.getClassLoader()
            .getResource("intentionDescriptions/TemplateGroup/" + kind + ".txt.template");
        return url == null ? new TextDescriptor[0] : new TextDescriptor[]{new ResourceTextDescriptor(url)};
    }

    @RequiredUIAccess
    private void showPoweredBy(@Nullable PluginId pluginId) {
        myPoweredBy.removeAll();

        PluginDescriptor plugin = pluginId == null || PluginIds.isPlatformPlugin(pluginId) ? null : PluginManager.findPlugin(pluginId);
        if (plugin == null) {
            myPoweredBy.setVisible(false);
            return;
        }

        Hyperlink link = Hyperlink.create(CodeInsightLocalize.poweredByPlugin(plugin.getName()), event -> {
            Settings settings = DataManager.getInstance().getDataContext(myPoweredBy).getData(Settings.KEY);
            if (settings == null) {
                return;
            }

            settings.select(PluginsConfigurable.class).whenComplete((configurable, error) -> {
                if (configurable != null) {
                    configurable.selectInstalled(pluginId);
                }
            });
        });
        myPoweredBy.left(link);
        myPoweredBy.setVisible(true);
    }

    @RequiredUIAccess
    private void showExamples(DockLayout holder, List<EditorBox> boxes, TextDescriptor[] examples) {
        while (boxes.size() < examples.length) {
            boxes.add(createExampleBox());
        }

        for (int i = 0; i < examples.length; i++) {
            showExample(boxes.get(i), examples[i]);
        }

        Component content;
        if (examples.length == 0) {
            content = null;
        }
        else if (examples.length == 1) {
            content = boxes.get(0);
        }
        else {
            VerticalLayout layout = VerticalLayout.create(Space.SMALL);
            for (int i = 0; i < examples.length; i++) {
                layout.add(boxes.get(i));
            }
            content = ScrollableLayout.create(layout);
        }

        Component shown = holder == myBefore ? myBeforeContent : myAfterContent;
        if (shown == content) {
            return;
        }

        holder.removeAll();
        if (content != null) {
            holder.center(content);
        }

        if (holder == myBefore) {
            myBeforeContent = content;
        }
        else {
            myAfterContent = content;
        }
    }

    @RequiredUIAccess
    private EditorBox createExampleBox() {
        return myEditorBoxBuilderFactory.create(null)
            .viewer()
            .multiline()
            .editorFont()
            .customize(IntentionDescriptionPanel::customizeExampleEditor)
            .build();
    }

    private static void customizeExampleEditor(EditorEx editor) {
        EditorSettings settings = editor.getSettings();
        settings.setWhitespacesShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setIndentGuidesShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setAdditionalColumnsCount(0);
        settings.setAdditionalLinesCount(0);
        settings.setRightMarginShown(true);
        settings.setRightMargin(60);
    }

    @RequiredUIAccess
    private static void showExample(EditorBox box, TextDescriptor example) {
        String text;
        try {
            text = StringUtil.convertLineSeparators(example.getText());
        }
        catch (IOException e) {
            LOG.warn(e);
            text = "";
        }

        List<int[]> spots = new ArrayList<>();
        StringBuilder builder = new StringBuilder(text.length());
        int index = 0;
        while (true) {
            int start = text.indexOf(SPOT_OPEN, index);
            int end = start < 0 ? -1 : text.indexOf(SPOT_CLOSE, start);
            if (start < 0 || end < 0) {
                builder.append(text, index, text.length());
                break;
            }

            builder.append(text, index, start);
            int spotStart = builder.length();
            builder.append(text, start + SPOT_OPEN.length(), end);
            spots.add(new int[]{spotStart, builder.length()});
            index = end + SPOT_CLOSE.length();
        }

        FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(example.getFileName());
        Document document = EditorFactory.getInstance().createDocument(builder.toString());
        box.setDocument(document, fileType);

        Editor editor = box.getEditor();
        if (editor != null) {
            for (int[] spot : spots) {
                editor.getMarkupModel().addRangeHighlighter(
                    CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES,
                    spot[0],
                    spot[1],
                    HighlighterLayer.ADDITIONAL_SYNTAX,
                    HighlighterTargetArea.EXACT_RANGE
                );
            }
        }
    }
}
