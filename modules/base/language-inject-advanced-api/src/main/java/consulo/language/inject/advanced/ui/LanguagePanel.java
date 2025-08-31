/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.inject.advanced.ui;

import consulo.codeEditor.EditorEx;
import consulo.language.Language;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.LexerEditorHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.inject.advanced.BaseInjection;
import consulo.language.inject.advanced.InjectedLanguage;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.ComboBox;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LanguagePanel extends AbstractInjectionPanel<BaseInjection> {
    private JPanel myRoot;
    private ComboBox myLanguage;
    private EditorTextField myPrefix;
    private EditorTextField mySuffix;

    public LanguagePanel(Project project, BaseInjection injection) {
        super(injection, project);

        final String[] languageIDs = InjectedLanguage.getAvailableLanguageIDs();
        Arrays.sort(languageIDs);

        myLanguage.setModel(new DefaultComboBoxModel(languageIDs));
        myLanguage.setRenderer(new ColoredListCellRenderer<String>() {
            final Set<String> IDs = new HashSet<String>(Arrays.asList(languageIDs));

            @Override
            protected void customizeCellRenderer(@Nonnull JList<? extends String> jList, String s, int i, boolean b, boolean b1) {
                SimpleTextAttributes attributes = IDs.contains(s) ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.ERROR_ATTRIBUTES;
                append(s, attributes);

                Language language = InjectedLanguage.findLanguageById(s);
                if (language != null) {
                    FileType fileType = language.getAssociatedFileType();
                    if (fileType != null) {
                        setIcon(fileType.getIcon());
                        append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                        append("(" + fileType.getDescription() + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    }
                }
            }
        });
        myLanguage.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    updateHighlighters();
                }
            }
        });

        myRoot.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                updateHighlighters();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });
    }

    private void updateHighlighters() {
        EditorEx editor = ((EditorEx) myPrefix.getEditor());
        if (editor == null) {
            return;
        }

        EditorEx editor2 = ((EditorEx) mySuffix.getEditor());
        assert editor2 != null;

        Language language = InjectedLanguage.findLanguageById(getLanguage());
        if (language == null) {
            editor.setHighlighter(new LexerEditorHighlighter(new DefaultSyntaxHighlighter(), editor.getColorsScheme()));
            editor2.setHighlighter(new LexerEditorHighlighter(new DefaultSyntaxHighlighter(), editor.getColorsScheme()));
        }
        else {
            SyntaxHighlighter s1 = SyntaxHighlighterFactory.getSyntaxHighlighter(language, getProject(), null);
            SyntaxHighlighter s2 = SyntaxHighlighterFactory.getSyntaxHighlighter(language, getProject(), null);
            editor.setHighlighter(new LexerEditorHighlighter(s1, editor.getColorsScheme()));
            editor2.setHighlighter(new LexerEditorHighlighter(s2, editor2.getColorsScheme()));
        }
    }

    @Nonnull
    public String getLanguage() {
        return (String) myLanguage.getSelectedItem();
    }

    public void setLanguage(String id) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) myLanguage.getModel();
        if (model.getIndexOf(id) == -1 && id.length() > 0) {
            model.insertElementAt(id, 0);
        }
        myLanguage.setSelectedItem(id);
        updateHighlighters();
    }

    public String getPrefix() {
        return myPrefix.getText();
    }

    public void setPrefix(String s) {
        if (!myPrefix.getText().equals(s)) {
            myPrefix.setText(s);
        }
    }

    public String getSuffix() {
        return mySuffix.getText();
    }

    public void setSuffix(String s) {
        if (!mySuffix.getText().equals(s)) {
            mySuffix.setText(s);
        }
    }

    @Override
    protected void resetImpl() {
        BaseInjection origInjection = getOrigInjection();
        setLanguage(origInjection.getInjectedLanguageId());
        setPrefix(origInjection.getPrefix());
        setSuffix(origInjection.getSuffix());
    }

    @Override
    protected void apply(BaseInjection i) {
        i.setInjectedLanguageId(getLanguage());
        i.setPrefix(getPrefix());
        i.setSuffix(getSuffix());
    }

    @Override
    public JPanel getComponent() {
        return myRoot;
    }
}
