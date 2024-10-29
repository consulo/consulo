/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.fileTemplates.impl;

import com.google.common.annotations.VisibleForTesting;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributesKey;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateManager;
import consulo.fileTemplate.impl.internal.FileTemplateStreamProvider;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeChooser;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.editor.highlight.*;
import consulo.language.editor.template.TemplateColors;
import consulo.language.file.FileTypeManager;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.lexer.Lexer;
import consulo.language.lexer.MergingLexerAdapter;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.util.collection.Lists;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author MYakovlev
 * @since 2022-07-26
 */
public class FileTemplateConfigurable implements Configurable, Configurable.NoScroll {
    private static final Logger LOG = Logger.getInstance(FileTemplateConfigurable.class);
    private static final String EMPTY_HTML = "<html></html>";

    private JPanel myMainPanel;
    private FileTemplate myTemplate;
    private PsiFile myFile;
    private Editor myTemplateEditor;
    private JTextField myNameField;
    private JTextField myExtensionField;
    private JCheckBox myAdjustBox;
    private JCheckBox myLiveTemplateBox;
    private JPanel myTopPanel;
    private JEditorPane myDescriptionComponent;
    private boolean myModified = false;
    private FileTemplateStreamProvider myDefaultDescriptionUrl;
    private final Project myProject;

    private final List<ChangeListener> myChangeListeners = Lists.newLockFreeCopyOnWriteList();
    private Splitter mySplitter;
    private final FileType myVelocityFileType = FileTypeManager.getInstance().getFileTypeByExtension("ft");
    private float myProportion = 0.6f;

    public FileTemplateConfigurable(Project project) {
        myProject = project;
    }

    public FileTemplate getTemplate() {
        return myTemplate;
    }

    @RequiredUIAccess
    public void setTemplate(FileTemplate template, @Nullable FileTemplateStreamProvider defaultDescription) {
        myDefaultDescriptionUrl = defaultDescription;
        myTemplate = template;
        if (myMainPanel != null) {
            reset();
            myNameField.selectAll();
            myExtensionField.selectAll();
        }
    }

    public void setShowInternalMessage(String message) {
        myTopPanel.removeAll();
        if (message == null) {
            myTopPanel.add(
                new JLabel(IdeLocalize.labelName().get()),
                new GridBagConstraints(
                    0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    JBUI.insetsRight(2), 0, 0
                )
            );
            myTopPanel.add(
                myNameField,
                new GridBagConstraints(
                    1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                    GridBagConstraints.HORIZONTAL, JBUI.insets(3, 2), 0, 0
                )
            );
            myTopPanel.add(
                new JLabel(IdeLocalize.labelExtension().get()),
                new GridBagConstraints(
                    2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    JBUI.insets(0, 2), 0, 0
                )
            );
            myTopPanel.add(
                myExtensionField,
                new GridBagConstraints(
                    3, 0, 1, 1, .3, 0.0, GridBagConstraints.CENTER,
                    GridBagConstraints.HORIZONTAL, JBUI.insetsLeft(2), 0, 0
                )
            );
            myExtensionField.setColumns(7);
        }
        myMainPanel.revalidate();
        myTopPanel.repaint();
    }

    public void setShowAdjustCheckBox(boolean show) {
        myAdjustBox.setEnabled(show);
    }

    @Override
    public String getDisplayName() {
        return IdeLocalize.titleEditFileTemplate().get();
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent() {
        myMainPanel = new JPanel(new GridBagLayout());
        myNameField = new JTextField();
        myExtensionField = new JTextField();
        mySplitter = new Splitter(true, myProportion);
        myAdjustBox = new JCheckBox(IdeLocalize.checkboxReformatAccordingToStyle().get());
        myLiveTemplateBox = new JCheckBox(IdeLocalize.checkboxEnableLiveTemplates().get());
        myTemplateEditor = createEditor();

        myDescriptionComponent = new JEditorPane();
        myDescriptionComponent.setEditorKit(JBHtmlEditorKit.create());
        myDescriptionComponent.setText(EMPTY_HTML);
        myDescriptionComponent.setEditable(false);
        myDescriptionComponent.addHyperlinkListener(new BrowserHyperlinkListener());

        myTopPanel = new JPanel(new GridBagLayout());

        JPanel descriptionPanel = new JPanel(new GridBagLayout());
        descriptionPanel.add(
            SeparatorFactory.createSeparator(IdeLocalize.labelDescription().get(), null),
            new GridBagConstraints(
                0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                JBUI.insetsBottom(2), 0, 0
            )
        );
        descriptionPanel.add(
            ScrollPaneFactory.createScrollPane(myDescriptionComponent),
            new GridBagConstraints(
                0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                JBUI.insetsTop(2), 0, 0
            )
        );

        myMainPanel.add(
            myTopPanel,
            new GridBagConstraints(
                0, 0, 4, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0
            )
        );
        myMainPanel.add(
            mySplitter,
            new GridBagConstraints(
                0, 2, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                JBUI.emptyInsets(), 0, 0
            )
        );

        mySplitter.setSecondComponent(descriptionPanel);
        setShowInternalMessage(null);

        myNameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(@Nonnull FocusEvent e) {
                onNameChanged();
            }
        });
        myExtensionField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(@Nonnull FocusEvent e) {
                onNameChanged();
            }
        });
        myMainPanel.setPreferredSize(JBUI.size(400, 300));
        return myMainPanel;
    }

    public void setProportion(float proportion) {
        myProportion = proportion;
    }

    private Editor createEditor() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document doc = myFile == null
            ? editorFactory.createDocument(myTemplate == null ? "" : myTemplate.getText())
            : PsiDocumentManager.getInstance(myFile.getProject()).getDocument(myFile);
        assert doc != null;
        Editor editor = editorFactory.createEditor(doc, myProject);

        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setVirtualSpace(false);
        editorSettings.setLineMarkerAreaShown(false);
        editorSettings.setIndentGuidesShown(false);
        editorSettings.setLineNumbersShown(false);
        editorSettings.setFoldingOutlineShown(false);
        editorSettings.setAdditionalColumnsCount(3);
        editorSettings.setAdditionalLinesCount(3);
        editorSettings.setCaretRowShown(false);

        editor.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent e) {
                onTextChanged();
            }
        });

        ((EditorEx)editor).setHighlighter(createHighlighter());

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel southPanel = new JPanel(new HorizontalLayout(40));
        southPanel.add(myAdjustBox);
        southPanel.add(myLiveTemplateBox);

        topPanel.add(southPanel, BorderLayout.SOUTH);
        topPanel.add(editor.getComponent(), BorderLayout.CENTER);
        mySplitter.setFirstComponent(topPanel);
        return editor;
    }

    private void onTextChanged() {
        myModified = true;
    }

    public String getNameValue() {
        return myNameField.getText();
    }

    private void onNameChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener changeListener : myChangeListeners) {
            changeListener.stateChanged(event);
        }
    }

    public void addChangeListener(ChangeListener listener) {
        if (!myChangeListeners.contains(listener)) {
            myChangeListeners.add(listener);
        }
    }

    public void removeChangeListener(ChangeListener listener) {
        myChangeListeners.remove(listener);
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (myModified) {
            return true;
        }
        String name = (myTemplate == null) ? "" : myTemplate.getName();
        String extension = (myTemplate == null) ? "" : myTemplate.getExtension();
        if (!Comparing.equal(name, myNameField.getText())) {
            return true;
        }
        if (!Comparing.equal(extension, myExtensionField.getText())) {
            return true;
        }
        if (myTemplate != null) {
            if (myTemplate.isReformatCode() != myAdjustBox.isSelected() || myTemplate.isLiveTemplateEnabled() != myLiveTemplateBox.isSelected()) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        if (myTemplate != null) {
            myTemplate.setText(myTemplateEditor.getDocument().getText());
            String name = myNameField.getText();
            String extension = myExtensionField.getText();
            String filename = name + "." + extension;
            if (name.length() == 0 || !isValidFilename(filename)) {
                throw new ConfigurationException(IdeLocalize.errorInvalidTemplateFileNameOrExtension());
            }
            FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(filename);
            if (fileType == UnknownFileType.INSTANCE) {
                FileTypeChooser.associateFileType(filename);
            }
            myTemplate.setName(name);
            myTemplate.setExtension(extension);
            myTemplate.setReformatCode(myAdjustBox.isSelected());
            myTemplate.setLiveTemplateEnabled(myLiveTemplateBox.isSelected());
        }
        myModified = false;
    }

    // TODO: needs to be generalized someday for other profiles
    private static boolean isValidFilename(final String filename) {
        if (filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            return false;
        }
        final File tempFile = new File(FileUtil.getTempDirectory() + File.separator + filename);
        return consulo.ide.impl.idea.openapi.util.io.FileUtil.ensureCanCreateFile(tempFile);
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        final String text = (myTemplate == null) ? "" : myTemplate.getText();
        String name = (myTemplate == null) ? "" : myTemplate.getName();
        String extension = (myTemplate == null) ? "" : myTemplate.getExtension();
        String description = (myTemplate == null) ? "" : myTemplate.getDescription();

        if ((description.length() == 0) && (myDefaultDescriptionUrl != null)) {
            try {
                description = myDefaultDescriptionUrl.loadText();
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }

        EditorFactory.getInstance().releaseEditor(myTemplateEditor);
        myFile = createFile(text, name);
        myTemplateEditor = createEditor();

        myNameField.setText(name);
        myExtensionField.setText(extension);
        myAdjustBox.setSelected(myTemplate != null && myTemplate.isReformatCode());
        myLiveTemplateBox.setSelected(myTemplate != null && myTemplate.isLiveTemplateEnabled());

        int i = description.indexOf("<html>");
        if (i > 0) {
            description = description.substring(i);
        }
        description = XmlStringUtil.stripHtml(description);
        description = description.replace("\n", "").replace("\r", "");
        description = XmlStringUtil.stripHtml(description);
        description = description + "<hr> <font face=\"verdana\" size=\"-1\">" +
            "<a href='https://velocity.apache.org/engine/devel/user-guide.html#velocity-template-language-vtl-an-introduction'>\n" +
            "Apache Velocity</a> template language is used</font>";

        myDescriptionComponent.setText(description);
        myDescriptionComponent.setCaretPosition(0);

        myNameField.setEditable((myTemplate != null) && (!myTemplate.isDefault()));
        myExtensionField.setEditable((myTemplate != null) && (!myTemplate.isDefault()));
        myModified = false;
    }

    @Nullable
    private PsiFile createFile(final String text, final String name) {
        if (myTemplate == null) {
            return null;
        }

        final FileType fileType = myVelocityFileType;
        if (fileType == UnknownFileType.INSTANCE) {
            return null;
        }

        final PsiFile file = PsiFileFactory.getInstance(myProject)
            .createFileFromText(name + ".txt.ft", fileType, text, 0, true);
        file.getViewProvider().putUserData(
            FileTemplateManager.DEFAULT_TEMPLATE_PROPERTIES,
            FileTemplateManager.getInstance(myProject).getDefaultProperties()
        );
        return file;
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myMainPanel = null;
        if (myTemplateEditor != null) {
            EditorFactory.getInstance().releaseEditor(myTemplateEditor);
            myTemplateEditor = null;
        }
        myFile = null;
    }

    private EditorHighlighter createHighlighter() {
        if (myTemplate != null && myVelocityFileType != UnknownFileType.INSTANCE) {
            return EditorHighlighterFactory.getInstance()
                .createEditorHighlighter(myProject, new LightVirtualFile("aaa." + myTemplate.getExtension() + ".ft"));
        }

        FileType fileType = null;
        if (myTemplate != null) {
            fileType = FileTypeManager.getInstance().getFileTypeByExtension(myTemplate.getExtension());
        }
        if (fileType == null) {
            fileType = PlainTextFileType.INSTANCE;
        }

        SyntaxHighlighter originalHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, null, null);
        if (originalHighlighter == null) {
            originalHighlighter = new DefaultSyntaxHighlighter();
        }

        final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        LayeredLexerEditorHighlighter highlighter = new LayeredLexerEditorHighlighter(new TemplateHighlighter(), scheme);
        highlighter.registerLayer(FileTemplateTokenType.TEXT, new LayerDescriptor(originalHighlighter, ""));
        return highlighter;
    }

    private static class TemplateHighlighter extends SyntaxHighlighterBase {
        private final Lexer myLexer;

        public TemplateHighlighter() {
            myLexer = createDefaultLexer();
        }

        @Nonnull
        @Override
        public Lexer getHighlightingLexer() {
            return myLexer;
        }

        @Override
        @Nonnull
        public TextAttributesKey[] getTokenHighlights(@Nonnull IElementType tokenType) {
            if (tokenType == FileTemplateTokenType.MACRO || tokenType == FileTemplateTokenType.DIRECTIVE) {
                return pack(TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES);
            }

            return EMPTY;
        }
    }

    @Nonnull
    @VisibleForTesting
    static Lexer createDefaultLexer() {
        return new MergingLexerAdapter(new FileTemplateTextLexer(), TokenSet.create(FileTemplateTokenType.TEXT));
    }


    public void focusToNameField() {
        myNameField.selectAll();
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
            () -> IdeFocusManager.getGlobalInstance().requestFocus(myNameField, true)
        );
    }

    public void focusToExtensionField() {
        myExtensionField.selectAll();
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
            () -> IdeFocusManager.getGlobalInstance().requestFocus(myExtensionField, true)
        );
    }
}
