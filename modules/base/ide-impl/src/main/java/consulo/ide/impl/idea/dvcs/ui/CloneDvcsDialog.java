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
package consulo.ide.impl.idea.dvcs.ui;

import consulo.application.progress.ProgressManager;
import consulo.application.ui.FrameStateManager;
import consulo.application.ui.event.FrameStateListener;
import consulo.application.util.UserHomeFileUtil;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.language.editor.ui.awt.EditorComboBox;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.DvcsBundle;
import consulo.versionControlSystem.distributed.DvcsRememberedInputs;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
    @NonNls final String ch = "[\\p{ASCII}&&[\\p{Graph}]&&[^@:/]]";
    @NonNls final String host = ch + "+(?:\\." + ch + "+)*";
    @NonNls final String path = "/?" + ch + "+(?:/" + ch + "+)*/?";
    @NonNls final String all = "(?:" + ch + "+@)?" + host + ":" + path;
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
  @Nullable private final String myDefaultRepoUrl;

  public CloneDvcsDialog(@Nonnull Project project, @Nonnull String displayName, @Nonnull String vcsDirectoryName) {
    this(project, displayName, vcsDirectoryName, null);
  }

  public CloneDvcsDialog(@Nonnull Project project, @Nonnull String displayName, @Nonnull String vcsDirectoryName, @Nullable String defaultUrl) {
    super(project, true);
    myDefaultRepoUrl = defaultUrl;
    myProject = project;
    myVcsDirectoryName = vcsDirectoryName;
    init();
    initListeners();
    setTitle(DvcsBundle.message("clone.title"));
    myRepositoryUrlLabel.setText(DvcsBundle.message("clone.repository.url", displayName));
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
            new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(fcd.getTitle(), fcd.getDescription(), myParentDirectory,
                                                                                 myProject, fcd, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
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

    final DocumentListener updateOkButtonListener = new DocumentAdapter() {
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

  @NonNls
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
    setErrorText(null);
    setOKActionEnabled(true);
  }

  /**
   * Check destination directory and set appropriate error text if there are problems
   *
   * @return true if destination components are OK.
   */
  private boolean checkDestination() {
    if (myParentDirectory.getText().length() == 0 || myDirectoryName.getText().length() == 0) {
      setErrorText(null);
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
      setErrorText(null);
      setOKActionEnabled(false);
      return false;
    }
    if (myTestResult != null && repository.equals(myTestURL)) {
      if (!myTestResult.booleanValue()) {
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
    final DvcsRememberedInputs rememberedInputs = getRememberedInputs();
    List<String> urls = new ArrayList<>(rememberedInputs.getVisitedUrls());
    if (myDefaultRepoUrl != null) {
      urls.add(0, myDefaultRepoUrl);
    }
    myRepositoryURL.setHistory(ArrayUtil.toObjectArray(urls, String.class));
    myRepositoryURL.addDocumentListener(new consulo.document.event.DocumentAdapter() {
      @Override
      public void documentChanged(consulo.document.event.DocumentEvent e) {
        // enable test button only if something is entered in repository URL
        final String url = getCurrentUrlText();
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

  public void prependToHistory(@Nonnull final String item) {
    myRepositoryURL.prependItem(item);
  }

  public void rememberSettings() {
    final DvcsRememberedInputs rememberedInputs = getRememberedInputs();
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
  private static String defaultDirectoryName(@Nonnull final String url, @Nonnull final String vcsDirName) {
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
  public JComponent getPreferredFocusedComponent() {
    return myRepositoryURL;
  }

  protected JComponent createCenterPanel() {
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
