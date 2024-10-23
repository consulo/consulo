/*
 * Copyright 2010 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.setting;

import consulo.application.ApplicationManager;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.setting.CodeStyleSettingsCustomizable;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.language.editor.highlight.EditorHighlighterProvider;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.util.ProjectUIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * Base class for code style settings panels supporting multiple programming languages.
 *
 * @author rvishnyakov
 */
public abstract class CustomizableLanguageCodeStylePanel extends CodeStyleAbstractPanel implements CodeStyleSettingsCustomizable {
    private static final Logger LOG = Logger.getInstance(CustomizableLanguageCodeStylePanel.class);

    protected CustomizableLanguageCodeStylePanel(CodeStyleSettings settings) {
        super(settings);
    }

    protected void init() {
        customizeSettings();
    }

    protected void customizeSettings() {
        LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(getDefaultLanguage());
        if (provider != null) {
            provider.customizeSettings(this, getSettingsType());
        }
    }

    public abstract LanguageCodeStyleSettingsProvider.SettingsType getSettingsType();


    protected void resetDefaultNames() {
    }

    @Override
    protected String getPreviewText() {
        if (getDefaultLanguage() == null) {
            return "";
        }
        String sample = LanguageCodeStyleSettingsProvider.getCodeSample(getDefaultLanguage(), getSettingsType());
        if (sample == null) {
            return "";
        }
        return sample;
    }

    @Override
    protected int getRightMargin() {
        if (getDefaultLanguage() == null) {
            return -1;
        }
        return LanguageCodeStyleSettingsProvider.getRightMargin(getDefaultLanguage(), getSettingsType());
    }

    @Override
    protected String getFileExt() {
        String fileExt = LanguageCodeStyleSettingsProvider.getFileExt(getDefaultLanguage());
        if (fileExt != null) {
            return fileExt;
        }
        return super.getFileExt();
    }

    @Nonnull
    @Override
    protected FileType getFileType() {
        if (getDefaultLanguage() != null) {
            FileType assocType = getDefaultLanguage().getAssociatedFileType();
            if (assocType != null) {
                return assocType;
            }
        }
        return PlainTextFileType.INSTANCE;
    }

    @Override
    @Nullable
    protected EditorHighlighter createHighlighter(final EditorColorsScheme scheme) {
        FileType fileType = getFileType();
        return EditorHighlighterProvider.forFileType(fileType)
            .getEditorHighlighter(ProjectUIUtil.guessCurrentProject(getPanel()), fileType, null, scheme);
    }


    @Override
    protected PsiFile doReformat(final Project project, final PsiFile psiFile) {
        final String text = psiFile.getText();
        final PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
        final Document doc = manager.getDocument(psiFile);
        CommandProcessor.getInstance().executeCommand(
            project,
            () -> ApplicationManager.getApplication().runWriteAction(() -> {
                if (doc != null) {
                    doc.replaceString(0, doc.getTextLength(), text);
                    manager.commitDocument(doc);
                }
                try {
                    CodeStyleManager.getInstance(project).reformat(psiFile);
                }
                catch (IncorrectOperationException e) {
                    LOG.error(e);
                }
            }),
            "",
            ""
        );
        if (doc != null) {
            manager.commitDocument(doc);
        }
        return psiFile;
    }

    protected static JPanel createPreviewPanel() {
        return new JPanel(new BorderLayout());
    }

    @Override
    public void moveStandardOption(String fieldName, String newGroup) {
        throw new UnsupportedOperationException();
    }

    protected <T extends OrderedOption> List<T> sortOptions(Collection<T> options) {
        Set<String> names = new HashSet<>(ContainerUtil.map(options, (Function<OrderedOption, String>)option -> option.getOptionName()));

        List<T> order = new ArrayList<>(options.size());
        MultiMap<String, T> afters = new MultiMap<>();
        MultiMap<String, T> befores = new MultiMap<>();

        for (T each : options) {
            String anchorOptionName = each.getAnchorOptionName();
            if (anchorOptionName != null && names.contains(anchorOptionName)) {
                if (each.getAnchor() == OptionAnchor.AFTER) {
                    afters.putValue(anchorOptionName, each);
                    continue;
                }
                else if (each.getAnchor() == OptionAnchor.BEFORE) {
                    befores.putValue(anchorOptionName, each);
                    continue;
                }
            }
            order.add(each);
        }

        List<T> result = new ArrayList<>(options.size());
        for (T each : order) {
            result.addAll(befores.get(each.getOptionName()));
            result.add(each);
            result.addAll(afters.get(each.getOptionName()));
        }

        assert result.size() == options.size();
        return result;
    }

    protected abstract static class OrderedOption {
        @Nonnull
        private final String optionName;
        @Nullable
        private final OptionAnchor anchor;
        @Nullable
        private final String anchorOptionName;

        protected OrderedOption(@Nonnull String optionName, @Nullable OptionAnchor anchor, @Nullable String anchorOptionName) {
            this.optionName = optionName;
            this.anchor = anchor;
            this.anchorOptionName = anchorOptionName;
        }

        @Nonnull
        public String getOptionName() {
            return optionName;
        }

        @Nullable
        public OptionAnchor getAnchor() {
            return anchor;
        }

        @Nullable
        public String getAnchorOptionName() {
            return anchorOptionName;
        }
    }
}
