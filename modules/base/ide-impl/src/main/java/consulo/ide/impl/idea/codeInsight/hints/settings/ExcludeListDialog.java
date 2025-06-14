package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.ide.impl.idea.codeInsight.hints.ParameterHintsPassFactory;
import consulo.language.LangBundle;
import consulo.language.Language;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.impl.internal.inlay.param.HintUtils;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.editor.internal.ParameterNameHintsSettings;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Hyperlink;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class ExcludeListDialog extends DialogWrapper {
    private final Language language;
    private final String patternToAdd;
    private EditorTextField myEditor;
    private boolean myPatternsAreValid = true;

    public ExcludeListDialog(Language language) {
        this(language, null);
    }

    public ExcludeListDialog(Language language, String patternToAdd) {
        super((Project) null);
        this.language = language;
        this.patternToAdd = patternToAdd;
        setTitle(CodeInsightBundle.message("settings.inlay.parameter.hints.exclude.list"));
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return createExcludePanel(language);
    }

    private JPanel createExcludePanel(Language language) {
        InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
        if (provider == null || !provider.isBlackListSupported()) {
            return null;
        }
        String baseList = getLanguageExcludeList(language);
        String initialText = (patternToAdd != null)
            ? baseList + "\n" + patternToAdd
            : baseList;
        EditorTextField field = createExcludeListEditorField(initialText);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent e) {
                updateOkEnabled(field);
            }
        });
        updateOkEnabled(field);
        myEditor = field;

        BorderLayoutPanel layoutPanel = new BorderLayoutPanel();
        Hyperlink hyperlink = Hyperlink.create(LangBundle.message("action.link.reset"), (e) -> {
            setLanguageExcludeListToDefault(language);
        });

        layoutPanel.addToTop(TargetAWT.to(hyperlink));
        layoutPanel.addToCenter(field);

        JPanel bottomPanel = new JPanel(new VerticalFlowLayout());
        layoutPanel.addToBottom(bottomPanel);

        String comment = baseLanguageComment(provider);
        if (comment != null) {
            layoutPanel.add(new JLabel(comment));
        }

        LocalizeValue explanationHTML = getExcludeListExplanationHTML(language);
        if (explanationHTML != LocalizeValue.of()) {
            bottomPanel.add(new JLabel(explanationHTML.get()));
        }

        return layoutPanel;
    }

    private String baseLanguageComment(InlayParameterHintsProvider provider) {
        Language dep = provider.getBlackListDependencyLanguage();
        if (dep == null) {
            return null;
        }
        return CodeInsightLocalize.inlayHintsBaseExcludeListDescription(dep.getDisplayName()).get();
    }

    private void setLanguageExcludeListToDefault(Language language) {
        InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
        Set<String> defaults = provider.getDefaultBlackList();
        myEditor.setText(StringUtil.join(defaults, "\n"));
    }

    private void updateOkEnabled(EditorTextField field) {
        List<Integer> invalid = getExcludeListInvalidLineNumbers(field.getText());
        myPatternsAreValid = invalid.isEmpty();
        getOKAction().setEnabled(myPatternsAreValid);
        Editor editor = field.getEditor();
        if (editor != null) {
            highlightErrorLines(invalid, editor);
        }
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        storeExcludeListDiff(language, myEditor.getText());
    }

    private void storeExcludeListDiff(Language language, String text) {
        Set<String> updated = Set.copyOf(
            List.of(text.split("\n")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet())
        );
        InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
        Set<String> defaults = provider.getDefaultBlackList();
        ParameterNameHintsSettings.Diff diff = ParameterNameHintsSettings.Diff.build(defaults, updated);
        ParameterNameHintsSettings.getInstance()
            .setExcludeListDiff(HintUtils.getLanguageForSettingKey(language), diff);
        ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
    }

    private static String getLanguageExcludeList(Language language) {
        InlayParameterHintsProvider prov = InlayParameterHintsProvider.forLanguage(language);
        if (prov == null) {
            return "";
        }
        ParameterNameHintsSettings.Diff d = ParameterNameHintsSettings.getInstance()
            .getExcludeListDiff(HintUtils.getLanguageForSettingKey(language));
        return StringUtil.join(d.applyOn(prov.getDefaultBlackList()), "\n");
    }

    private static EditorTextField createExcludeListEditorField(String text) {
        var doc = EditorFactory.getInstance().createDocument(text);
        EditorTextField etf = new EditorTextField(doc, null, PlainTextFileType.INSTANCE, false, false);
        etf.setPreferredSize(new Dimension(400, 350));
        etf.addSettingsProvider(editor -> {
            editor.setVerticalScrollbarVisible(true);
            editor.setHorizontalScrollbarVisible(true);
            editor.getSettings().setAdditionalLinesCount(2);
            highlightErrorLines(getExcludeListInvalidLineNumbers(text), editor);
        });
        return etf;
    }

    private static void highlightErrorLines(List<Integer> lines, Editor editor) {
        var doc = editor.getDocument();
        int total = doc.getLineCount();
        var model = editor.getMarkupModel();
        model.removeAllHighlighters();
        lines.stream().filter(i -> i < total).forEach(line ->
            model.addLineHighlighter(CodeInsightColors.ERRORS_ATTRIBUTES, line, HighlighterLayer.ERROR)
        );
    }

    private static List<Integer> getExcludeListInvalidLineNumbers(String text) {
        String[] lines = text.split("\n", -1);
        List<Integer> invalid = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                Pattern.compile(trimmed);
            }
            catch (PatternSyntaxException e) {
                invalid.add(i);
            }
        }
        return invalid;
    }

    @Nonnull
    private static LocalizeValue getExcludeListExplanationHTML(Language language) {
        InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
        if (provider == null) {
            return LocalizeValue.of();
        }
        return provider.getBlacklistExplanationHTML();
    }
}
