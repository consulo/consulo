/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.copyright.impl.internal.ui;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.component.extension.ExtensionPoint;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.document.Document;
import consulo.language.copyright.PredefinedCopyrightProvider;
import consulo.language.copyright.config.CopyrightManager;
import consulo.language.copyright.config.CopyrightProfile;
import consulo.language.copyright.internal.CopyrightEditorUtil;
import consulo.language.copyright.internal.CopyrightGenerator;
import consulo.language.copyright.util.EntityUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class CopyrightConfigurable extends NamedConfigurable<CopyrightProfile> implements Configurable.NoScroll {
    public static class PreviewDialog extends DialogWrapper {
        @Nonnull
        private final String myText;
        private final Dimension mySize;

        private Editor myEditor;

        private final Project myProject;

        public PreviewDialog(@Nullable Project project, @Nonnull String text, @Nonnull Dimension size) {
            super(project);
            myProject = project;
            myText = text;
            mySize = size;
            setTitle("Preview");
            init();
        }

        @Nonnull
        @Override
        protected Action[] createActions() {
            return new Action[]{getOKAction()};
        }

        @Override
        protected void dispose() {
            super.dispose();
            if (myEditor != null) {
                EditorFactory.getInstance().releaseEditor(myEditor);
            }
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            Document document = EditorFactory.getInstance().createDocument(myText);
            myEditor = EditorFactory.getInstance().createViewer(document, myProject);
            CopyrightEditorUtil.setupEditor(myEditor);

            JComponent component = myEditor.getComponent();
            component.setPreferredSize(mySize);
            return component;
        }
    }

    private final CopyrightProfile myCopyrightProfile;
    private final Project myProject;
    private JPanel myWholePanel;
    private boolean myModified;
    private String myDisplayName;
    private JTextField myKeywordField;
    private JTextField myAllowReplaceTextField;

    private Editor myCopyrightEditor;
    private Document myCopyrightDocument;

    public CopyrightConfigurable(final Project project, CopyrightProfile copyrightProfile, Runnable updater) {
        super(true, updater);
        myProject = project;
        myCopyrightProfile = copyrightProfile;
        myDisplayName = myCopyrightProfile.getName();

        myWholePanel = new JPanel(new VerticalFlowLayout());

        EditorFactory editorFactory = EditorFactory.getInstance();
        myCopyrightDocument = editorFactory.createDocument("");
        myCopyrightEditor = editorFactory.createEditor(myCopyrightDocument, project);
        CopyrightEditorUtil.setupEditor(myCopyrightEditor);

        DefaultActionGroup group = new DefaultActionGroup();

        JComponent editorComponent = myCopyrightEditor.getComponent();

        group.add(new AnAction("Preview", null, AllIcons.Actions.Preview) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                try {
                    String evaluate =
                        CopyrightGenerator.getInstance(project).generate(null, null, myCopyrightEditor.getDocument().getText());

                    new PreviewDialog(project, evaluate, editorComponent.getSize()).showAsync();
                }
                catch (Exception e1) {
                    Messages.showErrorDialog(myProject, "Template contains error:\n" + e1.getMessage(), "Preview");
                }
            }
        });

        ExtensionPoint<PredefinedCopyrightProvider> point = Application.get().getExtensionPoint(PredefinedCopyrightProvider.class);
        if (point.hasAnyExtensions()) {
            group.add(new AnAction("Reset To", null, AllIcons.General.Reset) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    ActionGroup.Builder actionGroup = ActionGroup.newImmutableBuilder();
                    for (PredefinedCopyrightProvider provider : point) {
                        for (Map.Entry<LocalizeValue, String> entry : provider.getCopyrightTexts().entrySet()) {
                            actionGroup.add(new AnAction(entry.getKey()) {
                                @RequiredUIAccess
                                @Override
                                public void actionPerformed(@Nonnull AnActionEvent e) {
                                    String text = entry.getValue();
                                    WriteAction.run(() -> myCopyrightDocument.setText(text));
                                }
                            });
                        }
                    }

                    ActionPopupMenu popupMenu =
                        ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, actionGroup.build());

                    popupMenu.getComponent().show(e.getInputEvent().getComponent(), e.getInputEvent().getComponent().getWidth(), 0);
                }
            });
        }

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        actionToolbar.setTargetComponent(myWholePanel);

        JPanel result = new JPanel(new BorderLayout());
        JPanel toolBarPanel = new JPanel(new BorderLayout());
        toolBarPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
        JLabel label = new JBLabel("Velocity Template");
        label.setForeground(JBColor.GRAY);
        label.setBorder(JBUI.Borders.emptyRight(10));
        toolBarPanel.add(label, BorderLayout.EAST);

        result.add(toolBarPanel, BorderLayout.NORTH);
        result.add(editorComponent, BorderLayout.CENTER);

        myWholePanel.add(result);

        myWholePanel.add(LabeledComponent.left(myKeywordField = new JBTextField(), "Keyword to detect copyright in comments"));
        myWholePanel.add(LabeledComponent.left(
            myAllowReplaceTextField = new JBTextField(),
            "Allow replacing copyright if old copyright contains"
        ));
    }

    @Override
    public CopyrightProfile getEditableObject() {
        return myCopyrightProfile;
    }

    @Override
    public String getBannerSlogan() {
        return myCopyrightProfile.getName();
    }

    @Override
    public JComponent createOptionsPanel() {
        return myWholePanel;
    }

    @Override
    @Nls
    public String getDisplayName() {
        return myCopyrightProfile.getName();
    }

    @Override
    public void setDisplayName(String s) {
        myCopyrightProfile.setName(s);
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        return myModified ||
            !Comparing.strEqual(EntityUtil.encode(myCopyrightDocument.getText().trim()), myCopyrightProfile.getNotice()) ||
            !Comparing.strEqual(myKeywordField.getText().trim(), myCopyrightProfile.getKeyword()) ||
            !Comparing.strEqual(myAllowReplaceTextField.getText().trim(), myCopyrightProfile.getAllowReplaceKeyword()) ||
            !Comparing.strEqual(myDisplayName, myCopyrightProfile.getName());
    }

    public void setModified(boolean modified) {
        myModified = modified;
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        myCopyrightProfile.setNotice(EntityUtil.encode(myCopyrightDocument.getText().trim()));
        myCopyrightProfile.setKeyword(myKeywordField.getText());
        myCopyrightProfile.setAllowReplaceKeyword(myAllowReplaceTextField.getText());
        CopyrightManager.getInstance(myProject).replaceCopyright(myDisplayName, myCopyrightProfile);
        myDisplayName = myCopyrightProfile.getName();
        myModified = false;
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        myDisplayName = myCopyrightProfile.getName();
        WriteAction.run(() -> myCopyrightDocument.setText(EntityUtil.decode(myCopyrightProfile.getNotice())));
        myKeywordField.setText(myCopyrightProfile.getKeyword());
        myAllowReplaceTextField.setText(myCopyrightProfile.getAllowReplaceKeyword());
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        EditorFactory.getInstance().releaseEditor(myCopyrightEditor);
    }
}
