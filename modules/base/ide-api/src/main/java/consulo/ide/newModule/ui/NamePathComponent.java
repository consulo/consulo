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
package consulo.ide.newModule.ui;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.newModule.NewModuleWizardContext;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.FieldPanel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.util.BrowseFilesListener;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * @author Eugene Zhuravlev
 * @since 2003-10-30
 */
@Deprecated(forRemoval = true)
@DeprecationInfo("Desktop component for ProjectOrModuleNameStep, use UnifiedProjectOrModuleNameStep")
public class NamePathComponent extends JPanel {
    private static final Logger LOG = Logger.getInstance(NamePathComponent.class);

    private JTextField myTfName;
    private JTextField myTfPath;
    private boolean myIsNameChangedByUser = false;
    private boolean myIsPathChangedByUser = false;
    private boolean myIsPathNameSyncEnabled = true;
    private boolean myIsNamePathSyncEnabled = true;
    private boolean myIsSyncEnabled = true;

    private FieldPanel myPathPanel;
    private JLabel myNameLabel;
    private JLabel myPathLabel;
    private boolean myForceSync;

    public NamePathComponent(
        String nameLabelText,
        String pathLabelText,
        char nameMnemonic,
        char locationMnemonic,
        final String pathChooserTitle,
        final String pathChooserDescription
    ) {
        this(nameLabelText, pathLabelText, pathChooserTitle, pathChooserDescription, true);
    }

    public NamePathComponent(
        String nameLabelText,
        String pathLabelText,
        final String pathChooserTitle,
        final String pathChooserDescription,
        boolean hideIgnored
    ) {
        this(nameLabelText, pathLabelText, pathChooserTitle, pathChooserDescription, hideIgnored, true);
    }

    public NamePathComponent(
        String nameLabelText,
        String pathLabelText,
        final String pathChooserTitle,
        final String pathChooserDescription,
        boolean hideIgnored,
        boolean bold
    ) {
        super(new GridBagLayout());

        myTfName = new JTextField();
        myTfName.setDocument(new NameFieldDocument());
        myTfName.setPreferredSize(new Dimension(200, myTfName.getPreferredSize().height));

        myTfPath = new JTextField();
        myTfPath.setDocument(new PathFieldDocument());
        myTfPath.setPreferredSize(new Dimension(200, myTfPath.getPreferredSize().height));

        myNameLabel = new JLabel(nameLabelText);
        if (bold) {
            myNameLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
        }
        myNameLabel.setLabelFor(myTfName);
        Insets insets = JBUI.insets(0, 0, 5, 4);
        this.add(
            myNameLabel,
            new GridBagConstraints(
                0,
                GridBagConstraints.RELATIVE,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                insets,
                0,
                0
            )
        );

        insets = JBUI.insetsBottom(5);
        this.add(
            myTfName,
            new GridBagConstraints(
                1,
                GridBagConstraints.RELATIVE,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                insets,
                0,
                0
            )
        );
        // todo: review texts
        final FileChooserDescriptor chooserDescriptor = (FileChooserDescriptor)BrowseFilesListener.SINGLE_DIRECTORY_DESCRIPTOR.clone();
        chooserDescriptor.setHideIgnored(hideIgnored);
        final BrowseFilesListener browseButtonActionListener =
            new BrowseFilesListener(myTfPath, pathChooserTitle, pathChooserDescription, chooserDescriptor) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    super.actionPerformed(e);
                    myIsPathChangedByUser = true;
                }
            };
        myPathPanel = new FieldPanel(myTfPath, null, null, browseButtonActionListener, null);
        myPathLabel = new JLabel(pathLabelText);
        myPathLabel.setLabelFor(myTfPath);
        if (bold) {
            myPathLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
        }
        insets = JBUI.insets(0, 0, 5, 4);
        this.add(
            myPathLabel,
            new GridBagConstraints(
                0,
                GridBagConstraints.RELATIVE,
                1,
                1,
                0.0,
                0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                insets,
                0,
                0
            )
        );
        insets = JBUI.insetsBottom(5);
        this.add(
            myPathPanel,
            new GridBagConstraints(
                1,
                GridBagConstraints.RELATIVE,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                insets,
                0,
                0
            )
        );
    }

    @RequiredUIAccess
    public boolean validateNameAndPath(@Nonnull NewModuleWizardContext context) throws ConfigurationException {
        final String name = getNameValue();
        if (name.length() == 0) {
            throw new ConfigurationException(IdeLocalize.promptNewProjectFileName(Application.get().getName(), context.getTargetId()));
        }

        final String projectFileDirectory = getPath();
        if (projectFileDirectory.length() == 0) {
            throw new ConfigurationException(IdeLocalize.promptEnterProjectFileLocation(context.getTargetId()));
        }

        final boolean shouldPromptCreation = isPathChangedByUser();
        if (!ProjectWizardUtil.createDirectoryIfNotExists(
            IdeLocalize.directoryProjectFileDirectory(context.getTargetId()).get(),
            projectFileDirectory,
            shouldPromptCreation
        )) {
            return false;
        }

        final File file = new File(projectFileDirectory);
        if (file.exists() && !file.canWrite()) {
            throw new ConfigurationException(IdeLocalize.projectDirectoryIsNotWritable(projectFileDirectory));
        }

        boolean shouldContinue = true;
        final File projectDir = new File(getPath(), Project.DIRECTORY_STORE_FOLDER);
        if (projectDir.exists()) {
            int answer = Messages.showYesNoDialog(
                IdeLocalize.promptOverwriteProjectFolder(projectDir.getAbsolutePath(), context.getTargetId()).get(),
                IdeLocalize.titleFileAlreadyExists().get(),
                UIUtil.getQuestionIcon()
            );
            shouldContinue = (answer == Messages.YES);
        }

        return shouldContinue;
    }

    public String getNameValue() {
        return myTfName.getText().trim();
    }

    public void setNameValue(String name) {
        final boolean isNameChangedByUser = myIsNameChangedByUser;
        setNamePathSyncEnabled(false);
        try {
            myTfName.setText(name);
        }
        finally {
            myIsNameChangedByUser = isNameChangedByUser;
            setNamePathSyncEnabled(true);
        }
    }

    public String getPath() {
        return myTfPath.getText().trim().replace(File.separatorChar, '/');
    }

    public void setPath(String path) {
        final boolean isPathChangedByUser = myIsPathChangedByUser;
        setPathNameSyncEnabled(false);
        try {
            myTfPath.setText(path);
        }
        finally {
            myIsPathChangedByUser = isPathChangedByUser;
            setPathNameSyncEnabled(true);
        }
    }

    public JTextField getNameComponent() {
        return myTfName;
    }

    @Nonnull
    public JLabel getPathLabel() {
        return myPathLabel;
    }

    public JTextField getPathComponent() {
        return myTfPath;
    }

    @Nonnull
    public FieldPanel getPathPanel() {
        return myPathPanel;
    }

    public void setPathComponentVisible(boolean visible) {
        myPathPanel.setVisible(visible);
    }

    public void setNameComponentVisible(boolean visible) {
        myTfName.setVisible(visible);
        myNameLabel.setVisible(visible);
    }

    public boolean isNameChangedByUser() {
        return myIsNameChangedByUser;
    }

    public boolean isPathChangedByUser() {
        return myIsPathChangedByUser;
    }

    public boolean isSyncEnabled() {
        return myIsSyncEnabled;
    }

    public void setSyncEnabled(boolean isSyncEnabled) {
        myIsSyncEnabled = isSyncEnabled;
    }

    private boolean isPathNameSyncEnabled() {
        return isSyncEnabled() && myIsPathNameSyncEnabled;
    }

    private void setPathNameSyncEnabled(boolean isPathNameSyncEnabled) {
        myIsPathNameSyncEnabled = isPathNameSyncEnabled;
    }

    private boolean isNamePathSyncEnabled() {
        return isSyncEnabled() && myIsNamePathSyncEnabled;
    }

    private void setNamePathSyncEnabled(boolean isNamePathSyncEnabled) {
        myIsNamePathSyncEnabled = isNamePathSyncEnabled;
    }

    public void syncNameToPath(boolean b) {
        myForceSync = b;
        if (b) {
            ((PathFieldDocument)myTfPath.getDocument()).syncPathAndName();
        }
    }

    public void addChangeListener(final Runnable callback) {
        DocumentAdapter adapter = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                callback.run();
            }
        };
        myTfName.getDocument().addDocumentListener(adapter);
        myTfPath.getDocument().addDocumentListener(adapter);
    }

    private class NameFieldDocument extends PlainDocument {
        public NameFieldDocument() {
            addDocumentListener(new DocumentAdapter() {
                @Override
                public void textChanged(DocumentEvent event) {
                    myIsNameChangedByUser = true;
                    syncNameAndPath();
                }
            });
        }

        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            boolean ok = true;
            for (int idx = 0; idx < str.length() && ok; idx++) {
                char ch = str.charAt(idx);
                ok = ch != File.separatorChar && ch != '\\' && ch != '/' && ch != '|' && ch != ':';
            }
            if (ok) {
                super.insertString(offs, str, a);
            }
        }

        private void syncNameAndPath() {
            if (isNamePathSyncEnabled() && (myForceSync || !myIsPathChangedByUser)) {
                try {
                    setPathNameSyncEnabled(false);
                    final String name = getText(0, getLength());
                    final String path = myTfPath.getText().trim();
                    final int lastSeparatorIndex = path.lastIndexOf(File.separator);
                    if (lastSeparatorIndex >= 0) {
                        setPath(path.substring(0, lastSeparatorIndex + 1) + name);
                    }
                }
                catch (BadLocationException e) {
                    LOG.error(e);
                }
                finally {
                    setPathNameSyncEnabled(true);
                }
            }
        }
    }

    private class PathFieldDocument extends PlainDocument {
        public PathFieldDocument() {
            addDocumentListener(new DocumentAdapter() {
                @Override
                public void textChanged(DocumentEvent event) {
                    myIsPathChangedByUser = true;
                    syncPathAndName();
                }
            });
        }

        private void syncPathAndName() {
            if (isPathNameSyncEnabled() && (myForceSync || !myIsNameChangedByUser)) {
                try {
                    setNamePathSyncEnabled(false);
                    final String path = getText(0, getLength());
                    final int lastSeparatorIndex = path.lastIndexOf(File.separator);
                    if (lastSeparatorIndex >= 0 && (lastSeparatorIndex + 1) < path.length()) {
                        setNameValue(path.substring(lastSeparatorIndex + 1));
                    }
                }
                catch (BadLocationException e) {
                    LOG.error(e);
                }
                finally {
                    setNamePathSyncEnabled(true);
                }
            }
        }
    }
}
