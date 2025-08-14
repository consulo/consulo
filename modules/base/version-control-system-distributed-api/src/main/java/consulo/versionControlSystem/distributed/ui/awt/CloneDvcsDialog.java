/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.ui.awt;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.application.progress.ProgressManager;
import consulo.application.ui.FrameStateManager;
import consulo.application.ui.event.FrameStateListener;
import consulo.application.util.UserHomeFileUtil;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.language.editor.ui.awt.EditorComboBox;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.DvcsBundle;
import consulo.versionControlSystem.distributed.DvcsRememberedInputs;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static consulo.util.lang.ObjectUtil.assertNotNull;

public abstract class CloneDvcsDialog extends DialogWrapper {
    /**
     * The pattern for SSH URL-s in form [user@]host:path
     */
    private static final Pattern SSH_URL_PATTERN;

    static {
        // TODO make real URL pattern
        String ch = "[\\p{ASCII}&&[\\p{Graph}]&&[^@:/]]";
        String host = ch + "+(?:\\." + ch + "+)*";
        String path = "/?" + ch + "+(?:/" + ch + "+)*/?";
        String all = "(?:" + ch + "+@)?" + host + ":" + path;
        SSH_URL_PATTERN = Pattern.compile(all);
    }

    private JPanel myRootPanel;
    private EditorComboBox myRepositoryURL;
    private TextFieldWithBrowseButton myParentDirectory;
    private JButton myTestButton; // test repository
    private JTextField myDirectoryName;
    private JLabel myRepositoryUrlLabel;

    @Nonnull
    private String myTestURL; // the repository URL at the time of the last test
    @Nullable
    private Boolean myTestResult; // the test result of the last test or null if not tested
    @Nonnull
    private String myDefaultDirectoryName = "";
    @Nonnull
    protected final Project myProject;
    @Nonnull
    protected final String myVcsDirectoryName;
    @Nullable
    private final String myDefaultRepoUrl;

    public CloneDvcsDialog(@Nonnull Project project,
                           @Nonnull LocalizeValue displayName,
                           @Nonnull String vcsDirectoryName) {
        this(project, displayName, vcsDirectoryName, null);
    }

    public CloneDvcsDialog(
        @Nonnull Project project,
        @Nonnull LocalizeValue displayName,
        @Nonnull String vcsDirectoryName,
        @Nullable String defaultUrl
    ) {
        super(project, true);
        myDefaultRepoUrl = defaultUrl;
        myProject = project;
        myVcsDirectoryName = vcsDirectoryName;
        $$$setupUI$$$();
        init();
        initListeners();
        setTitle(DvcsBundle.message("clone.title"));
        myRepositoryUrlLabel.setText(DvcsBundle.message("clone.repository.url", displayName.get()));
        myRepositoryUrlLabel.setDisplayedMnemonic('R');
        setOKButtonText(DvcsBundle.message("clone.button"));

        FrameStateManager.getInstance().addListener(new FrameStateListener() {
            @Override
            public void onFrameActivated() {
                updateButtons();
            }
        }, getDisposable());
    }

    @Override
    protected void doOKAction() {
        File parent = new File(getParentDirectory());
        if (parent.exists() && parent.isDirectory() && parent.canWrite() || parent.mkdirs()) {
            super.doOKAction();
            return;
        }
        setErrorText("Couldn't create " + parent + "<br/>Check your access rights");
        setOKActionEnabled(false);
    }

    @Nonnull
    public String getSourceRepositoryURL() {
        return getCurrentUrlText();
    }

    public String getParentDirectory() {
        return myParentDirectory.getText();
    }

    public String getDirectoryName() {
        return myDirectoryName.getText();
    }

    /**
     * Init components
     */
    private void initListeners() {
        FileChooserDescriptor fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fcd.setShowFileSystemRoots(true);
        fcd.setTitle(DvcsBundle.message("clone.destination.directory.title"));
        fcd.setDescription(DvcsBundle.message("clone.destination.directory.description"));
        fcd.setHideIgnored(false);
        myParentDirectory.addActionListener(
            new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
                fcd.getTitleValue(),
                fcd.getDescriptionValue(),
                myParentDirectory,
                myProject,
                fcd,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
            ) {
                @Override
                protected VirtualFile getInitialFile() {
                    // suggest project base directory only if nothing is typed in the component.
                    String text = getComponentText();
                    if (text.length() == 0) {
                        VirtualFile file = myProject.getBaseDir();
                        if (file != null) {
                            return file;
                        }
                    }
                    return super.getInitialFile();
                }
            }
        );

        DocumentListener updateOkButtonListener = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                updateButtons();
            }
        };
        myParentDirectory.getChildComponent().getDocument().addDocumentListener(updateOkButtonListener);
        String parentDir = getRememberedInputs().getCloneParentDir();
        if (StringUtil.isEmptyOrSpaces(parentDir)) {
            parentDir = ProjectUtil.getProjectsDirectory().toString();
        }
        myParentDirectory.setText(parentDir);

        myDirectoryName.getDocument().addDocumentListener(updateOkButtonListener);

        myTestButton.addActionListener(e -> test());

        setOKActionEnabled(false);
        myTestButton.setEnabled(false);
    }

    @RequiredUIAccess
    private void test() {
        myTestURL = getCurrentUrlText();
        TestResult testResult = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            () -> test(myTestURL),
            DvcsBundle.message("clone.testing", myTestURL),
            true,
            myProject
        );
        if (testResult.isSuccess()) {
            Messages.showInfoMessage(
                myTestButton,
                DvcsBundle.message("clone.test.success.message", myTestURL),
                DvcsBundle.message("clone.test.connection.title")
            );
            myTestResult = Boolean.TRUE;
        }
        else {
            Messages.showErrorDialog(myProject, assertNotNull(testResult.getError()), "Repository Test Failed");
            myTestResult = Boolean.FALSE;
        }
        updateButtons();
    }

    @Nonnull
    protected abstract TestResult test(@Nonnull String url);

    @Nonnull
    protected abstract DvcsRememberedInputs getRememberedInputs();

    /**
     * Check fields and display error in the wrapper if there is a problem
     */
    private void updateButtons() {
        if (!checkRepositoryURL()) {
            return;
        }
        if (!checkDestination()) {
            return;
        }
        clearErrorText();
        setOKActionEnabled(true);
    }

    /**
     * Check destination directory and set appropriate error text if there are problems
     *
     * @return true if destination components are OK.
     */
    private boolean checkDestination() {
        if (myParentDirectory.getText().length() == 0 || myDirectoryName.getText().length() == 0) {
            clearErrorText();
            setOKActionEnabled(false);
            return false;
        }
        File file = new File(myParentDirectory.getText(), myDirectoryName.getText());
        if (file.exists() && (!file.isDirectory()) || !ArrayUtil.isEmpty(file.list())) {
            setErrorText(DvcsBundle.message("clone.destination.exists.error", file));
            setOKActionEnabled(false);
            return false;
        }
        return true;
    }

    /**
     * Check repository URL and set appropriate error text if there are problems
     *
     * @return true if repository URL is OK.
     */
    private boolean checkRepositoryURL() {
        String repository = getCurrentUrlText();
        if (repository.length() == 0) {
            clearErrorText();
            setOKActionEnabled(false);
            return false;
        }
        if (myTestResult != null && repository.equals(myTestURL)) {
            if (!myTestResult) {
                setErrorText(DvcsBundle.message("clone.test.failed.error"));
                setOKActionEnabled(false);
                return false;
            }
            else {
                return true;
            }
        }
        try {
            if (new URI(repository).isAbsolute()) {
                return true;
            }
        }
        catch (URISyntaxException urlExp) {
            // do nothing
        }
        // check if ssh url pattern
        if (SSH_URL_PATTERN.matcher(repository).matches()) {
            return true;
        }
        try {
            File file = new File(repository);
            if (file.exists()) {
                if (!file.isDirectory()) {
                    setErrorText(DvcsBundle.message("clone.url.is.not.directory.error"));
                    setOKActionEnabled(false);
                }
                return true;
            }
        }
        catch (Exception fileExp) {
            // do nothing
        }
        setErrorText(DvcsBundle.message("clone.invalid.url"));
        setOKActionEnabled(false);
        return false;
    }

    @Nonnull
    private String getCurrentUrlText() {
        return UserHomeFileUtil.expandUserHome(myRepositoryURL.getText().trim());
    }

    private void createUIComponents() {
        myRepositoryURL = new EditorComboBox("");
        DvcsRememberedInputs rememberedInputs = getRememberedInputs();
        List<String> urls = new ArrayList<>(rememberedInputs.getVisitedUrls());
        if (myDefaultRepoUrl != null) {
            urls.add(0, myDefaultRepoUrl);
        }
        myRepositoryURL.setHistory(ArrayUtil.toObjectArray(urls, String.class));
        myRepositoryURL.addDocumentListener(new consulo.document.event.DocumentAdapter() {
            @Override
            public void documentChanged(consulo.document.event.DocumentEvent e) {
                // enable test button only if something is entered in repository URL
                String url = getCurrentUrlText();
                myTestButton.setEnabled(url.length() != 0);
                if (myDefaultDirectoryName.equals(myDirectoryName.getText()) || myDirectoryName.getText().length() == 0) {
                    // modify field if it was unmodified or blank
                    myDefaultDirectoryName = defaultDirectoryName(url, myVcsDirectoryName);
                    myDirectoryName.setText(myDefaultDirectoryName);
                }
                updateButtons();
            }
        });
    }

    public void prependToHistory(@Nonnull String item) {
        myRepositoryURL.prependItem(item);
    }

    public void rememberSettings() {
        DvcsRememberedInputs rememberedInputs = getRememberedInputs();
        rememberedInputs.addUrl(getSourceRepositoryURL());
        rememberedInputs.setCloneParentDir(getParentDirectory());
    }

    /**
     * Get default name for checked out directory
     *
     * @param url an URL to checkout
     * @return a default repository name
     */
    @Nonnull
    private static String defaultDirectoryName(@Nonnull String url, @Nonnull String vcsDirName) {
        String nonSystemName;
        if (url.endsWith("/" + vcsDirName) || url.endsWith(File.separator + vcsDirName)) {
            nonSystemName = url.substring(0, url.length() - vcsDirName.length() - 1);
        }
        else {
            if (url.endsWith(vcsDirName)) {
                nonSystemName = url.substring(0, url.length() - vcsDirName.length());
            }
            else {
                nonSystemName = url;
            }
        }
        int i = nonSystemName.lastIndexOf('/');
        if (i == -1 && File.separatorChar != '/') {
            i = nonSystemName.lastIndexOf(File.separatorChar);
        }
        return i >= 0 ? nonSystemName.substring(i + 1) : "";
    }

    @Nullable
    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myRepositoryURL;
    }

    @Override
    protected JComponent createCenterPanel() {
        return myRootPanel;
    }

    /**
     * Method generated by Consulo GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        myRootPanel = new JPanel();
        myRootPanel.setLayout(new GridLayoutManager(4, 4, new Insets(0, 0, 0, 0), -1, -1));
        myRepositoryUrlLabel = new JLabel();
        myRepositoryUrlLabel.setText("Repository URL");
        myRepositoryUrlLabel.setDisplayedMnemonic('R');
        myRepositoryUrlLabel.setDisplayedMnemonicIndex(0);
        myRootPanel.add(myRepositoryUrlLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        myRootPanel.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        myRootPanel.add(spacer2, new GridConstraints(3, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myRootPanel.add(myRepositoryURL, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, DvcsBundle.message("clone.parent.dir"));
        myRootPanel.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myParentDirectory = new TextFieldWithBrowseButton();
        myRootPanel.add(myParentDirectory, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, DvcsBundle.message("clone.dir.name"));
        myRootPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myTestButton = new JButton();
        this.$$$loadButtonText$$$(myTestButton, DvcsBundle.message("clone.test"));
        myRootPanel.add(myTestButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myDirectoryName = new JTextField();
        myRootPanel.add(myDirectoryName, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer3 = new Spacer();
        myRootPanel.add(spacer3, new GridConstraints(2, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        label2.setLabelFor(myDirectoryName);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myRootPanel;
    }

    protected static class TestResult {
        @Nonnull
        public static final TestResult SUCCESS = new TestResult(null);
        @Nullable
        private final String myErrorMessage;

        public TestResult(@Nullable String errorMessage) {
            myErrorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return myErrorMessage == null;
        }

        @Nullable
        public String getError() {
            return myErrorMessage;
        }
    }
}
