// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.webcore.packaging;

import com.intellij.execution.ExecutionException;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.CatchingConsumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.logging.Logger;
import consulo.packagesView.SearchablePackageManagementService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ObjectUtil;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * User: catherine
 * <p/>
 * UI for installing python packages
 */
public class ManagePackagesDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(ManagePackagesDialog.class);
  private static final RepoPackage MORE = new RepoPackage("more", "more", "more");

  @Nonnull
  private final Project myProject;
  private final PackageManagementService myController;

  private JPanel myFilter;
  private JPanel myMainPanel;
  private JEditorPane myDescriptionTextArea;
  private final JBList myPackages;
  private JButton myInstallButton;
  private JCheckBox myOptionsCheckBox;
  private JTextField myOptionsField;
  private JCheckBox myInstallToUser;
  private JComboBox myVersionComboBox;
  private JCheckBox myVersionCheckBox;
  private JButton myManageButton;
  private final PackagesNotificationPanel myNotificationArea;
  private JSplitPane mySplitPane;
  private JPanel myNotificationsAreaPlaceholder;
  private PackagesModel myPackagesModel;
  private String mySelectedPackageName;
  private final Set<String> myInstalledPackages;
  @Nullable
  private final PackageManagementService.Listener myPackageListener;

  private final Set<String> myCurrentlyInstalling = new HashSet<>();

  private Future<?> myGetPackagesFuture = CompletableFuture.completedFuture(null);

  @Nullable
  protected ListSpeedSearch myListSpeedSearch;

  private final SearchablePackageManagementService mySearchablePackageManagement;

  public ManagePackagesDialog(@Nonnull Project project, final PackageManagementService packageManagementService, @Nullable final PackageManagementService.Listener packageListener) {
    super(project, true);
    myProject = project;
    myController = packageManagementService;

    myPackageListener = packageListener;
    init();
    setTitle(IdeBundle.message("available.packages.dialog.title"));
    myPackages = new JBList();
    myNotificationArea = new PackagesNotificationPanel();
    myNotificationsAreaPlaceholder.add(myNotificationArea.getComponent(), BorderLayout.CENTER);

    mySearchablePackageManagement = ObjectUtil.tryCast(packageManagementService, SearchablePackageManagementService.class);

    final AnActionButton reloadButton = new AnActionButton(IdeBundle.message("action.AnActionButton.text.reload.list.of.packages"), AllIcons.Actions.Refresh) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        myPackages.setPaintBusy(true);
        final Application application = Application.get();
        application.executeOnPooledThread(() -> {
          try {
            myController.reloadAllPackages();
            initModel("");
            myPackages.setPaintBusy(false);
          }
          catch (final IOException e1) {
            application.invokeLater(() -> {
              Messages.showErrorDialog(myMainPanel, IdeBundle.message("error.updating.package.list", e1.getMessage()), IdeBundle.message("action.AnActionButton.text.reload.list.of.packages"));
              LOG.info("Error updating list of repository packages", e1);
              myPackages.setPaintBusy(false);
            }, ModalityState.any());
          }
        });
      }
    };

    if (mySearchablePackageManagement != null) {
      myListSpeedSearch = new ListSpeedSearch(myPackages, o -> {
        if (o instanceof RepoPackage) return ((RepoPackage)o).getName();
        return "";
      });
    }

    JPanel packagesPanel = ToolbarDecorator.createDecorator(myPackages).disableAddAction().disableUpDownActions().disableRemoveAction().addExtraAction(reloadButton).createPanel();
    packagesPanel.setPreferredSize(new Dimension(JBUIScale.scale(400), -1));
    packagesPanel.setMinimumSize(new Dimension(JBUIScale.scale(100), -1));
    myPackages.setFixedCellWidth(0);
    myPackages.setFixedCellHeight(JBUIScale.scale(22));
    myPackages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mySplitPane.setLeftComponent(packagesPanel);

    myPackages.addListSelectionListener(new MyPackageSelectionListener());
    myInstallToUser.addActionListener(event -> myController.installToUserChanged(myInstallToUser.isSelected()));
    myOptionsCheckBox.setEnabled(false);
    myVersionCheckBox.setEnabled(false);
    myVersionCheckBox.addActionListener(event -> myVersionComboBox.setEnabled(myVersionCheckBox.isSelected()));

    UiNotifyConnector.doWhenFirstShown(myPackages, () -> initModel(""));
    myOptionsCheckBox.addActionListener(event -> myOptionsField.setEnabled(myOptionsCheckBox.isSelected()));
    myInstallButton.setEnabled(false);
    myDescriptionTextArea.addHyperlinkListener(new PluginManagerMain.MyHyperlinkListener());
    addInstallAction();
    myInstalledPackages = new HashSet<>();
    updateInstalledPackages();
    addManageAction();
    myPackages.setCellRenderer(new MyTableRenderer());

    if (myController.canInstallToUser()) {
      myInstallToUser.setVisible(true);
      myInstallToUser.setSelected(myController.isInstallToUserSelected());
      myInstallToUser.setText(myController.getInstallToUserText());
    }
    else {
      myInstallToUser.setVisible(false);
    }
    myMainPanel.setPreferredSize(new Dimension(JBUIScale.scale(900), JBUIScale.scale(700)));

    GuiUtils.replaceJSplitPaneWithIDEASplitter(myMainPanel);
  }

  public void selectPackage(@Nonnull InstalledPackage pkg) {
    mySelectedPackageName = pkg.getName();
    doSelectPackage(mySelectedPackageName);
  }

  @RequiredUIAccess
  private void addManageAction() {
    if (myController.canManageRepositories()) {
      myManageButton.addActionListener(event -> {
        ManageRepoDialog dialog = new ManageRepoDialog(myProject, myController);
        dialog.showAsync();
      });
    }
    else {
      myManageButton.setVisible(false);
    }
  }

  private void addInstallAction() {
    myInstallButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        final Object pyPackage = myPackages.getSelectedValue();
        if (pyPackage instanceof RepoPackage) {
          RepoPackage repoPackage = (RepoPackage)pyPackage;

          String extraOptions = null;
          if (myOptionsCheckBox.isEnabled() && myOptionsCheckBox.isSelected()) {
            extraOptions = myOptionsField.getText();
          }

          String version = null;
          if (myVersionCheckBox.isEnabled() && myVersionCheckBox.isSelected()) {
            version = (String)myVersionComboBox.getSelectedItem();
          }

          final PackageManagementService.Listener listener = new PackageManagementService.Listener() {
            @Override
            public void operationStarted(final String packageName) {
              if (!ApplicationManager.getApplication().isDispatchThread()) {
                ApplicationManager.getApplication().invokeLater(() -> handleInstallationStarted(packageName), ModalityState.stateForComponent(myMainPanel));
              }
              else {
                handleInstallationStarted(packageName);
              }
            }

            @Override
            public void operationFinished(final String packageName, @Nullable final PackageManagementService.ErrorDescription errorDescription) {
              if (!ApplicationManager.getApplication().isDispatchThread()) {
                ApplicationManager.getApplication().invokeLater(() -> handleInstallationFinished(packageName, errorDescription), ModalityState.stateForComponent(myMainPanel));
              }
              else {
                handleInstallationFinished(packageName, errorDescription);
              }
            }
          };
          myController.installPackage(repoPackage, version, false, extraOptions, listener, myInstallToUser.isSelected());
          myInstallButton.setEnabled(false);
        }
        //PackageManagementUsageCollector.triggerInstallPerformed(myProject, myController);
      }
    });
  }

  private void handleInstallationStarted(String packageName) {
    myNotificationArea.hide();
    setDownloadStatus(true);
    myCurrentlyInstalling.add(packageName);
    if (myPackageListener != null) {
      myPackageListener.operationStarted(packageName);
    }
    myPackages.repaint();
  }

  private void handleInstallationFinished(String packageName, PackageManagementService.ErrorDescription errorDescription) {
    if (myPackageListener != null) {
      myPackageListener.operationFinished(packageName, errorDescription);
    }
    setDownloadStatus(false);
    myNotificationArea.showResult(packageName, errorDescription);

    updateInstalledPackages();

    myCurrentlyInstalling.remove(packageName);
    myPackages.repaint();
  }

  private void updateInstalledPackages() {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        List<String> installedPackages = ContainerUtil.map(myController.getInstalledPackagesList(), InstalledPackage::getName);
        UIUtil.invokeLaterIfNeeded(() -> {
          myInstalledPackages.clear();
          myInstalledPackages.addAll(installedPackages);
        });
      }
      catch (ExecutionException e) {
        LOG.info("Error updating list of installed packages", e);
      }
    });
  }

  public void initModel(@Nonnull String filter) {
    myGetPackagesFuture.cancel(true);
    
    setDownloadStatus(true);
    final Application application = Application.get();
    myGetPackagesFuture = application.executeOnPooledThread(() -> {
      try {

        List<RepoPackage> allPackages;

        if (mySearchablePackageManagement != null) {
          allPackages = mySearchablePackageManagement.getPackages(filter, 0, mySearchablePackageManagement.getPageSize());
        }
        else {
          allPackages = myController.getAllPackages();
        }

        myPackagesModel = new PackagesModel(allPackages);

        //if(mySearchablePackageManagement != null) {
        //  myPackagesModel.add(MORE);
        //}

        application.invokeLater(() -> {
          myPackages.setModel(myPackagesModel);
          if(mySearchablePackageManagement == null) {
            ((MyPackageFilter)myFilter).filter();
          }
          doSelectPackage(mySelectedPackageName);
          setDownloadStatus(false);
        }, ModalityState.any());
      }
      catch (final IOException e) {
        application.invokeLater(() -> {
          if (myMainPanel.isShowing()) {
            Messages.showErrorDialog(myMainPanel, IdeBundle.message("error.loading.package.list", e.getMessage()), IdeBundle.message("packages.title"));
          }
          LOG.info("Error initializing model", e);
          setDownloadStatus(false);
        }, ModalityState.any());
      }
    });
  }

  private void doSelectPackage(@Nullable String packageName) {
    PackagesModel packagesModel = ObjectUtil.tryCast(myPackages.getModel(), PackagesModel.class);
    if (packageName == null || packagesModel == null) {
      return;
    }
    for (int i = 0; i < packagesModel.getSize(); i++) {
      RepoPackage repoPackage = packagesModel.getElementAt(i);
      if (packageName.equals(repoPackage.getName())) {
        myPackages.setSelectedIndex(i);
        myPackages.ensureIndexIsVisible(i);
        break;
      }
    }
  }

  protected void setDownloadStatus(boolean status) {
    myPackages.setPaintBusy(status);
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  private void createUIComponents() {
    myFilter = new MyPackageFilter();
    myDescriptionTextArea = SwingHelper.createHtmlViewer(true, null, null, null);
  }

  public void setOptionsText(@Nonnull String optionsText) {
    myOptionsField.setText(optionsText);
  }

  private class MyPackageFilter extends FilterComponent {
    MyPackageFilter() {
      super("PACKAGE_FILTER", 5);
      getTextEditor().addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            e.consume();
            filter();
            IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myPackages, true));
          }
          else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            onEscape(e);
          }
        }
      });
    }

    @Override
    public void filter() {
      if (myPackagesModel == null) {
        return;
      }

      if (mySearchablePackageManagement != null) {
        initModel(getFilter());
      }
      else {
        myPackagesModel.filter(getFilter());
      }
    }
  }

  private class PackagesModel extends CollectionListModel<RepoPackage> {
    protected final List<RepoPackage> myFilteredOut = new ArrayList<>();
    protected List<RepoPackage> myView = new ArrayList<>();

    PackagesModel(List<RepoPackage> packages) {
      super(packages);
      myView = packages;
    }

    public void add(String urlResource, String element) {
      super.add(new RepoPackage(element, urlResource));
    }

    protected void filter(final String filter) {
      final Collection<RepoPackage> toProcess = toProcess();

      toProcess.addAll(myFilteredOut);
      myFilteredOut.clear();

      final ArrayList<RepoPackage> filtered = new ArrayList<>();

      RepoPackage toSelect = null;
      for (RepoPackage repoPackage : toProcess) {
        final String packageName = repoPackage.getName();
        if (StringUtil.containsIgnoreCase(packageName, filter)) {
          filtered.add(repoPackage);
        }
        else {
          myFilteredOut.add(repoPackage);
        }
        if (StringUtil.equalsIgnoreCase(packageName, filter)) toSelect = repoPackage;
      }
      filter(filtered, toSelect);
    }

    public void filter(List<? extends RepoPackage> filtered, @Nullable final RepoPackage toSelect) {
      myView.clear();
      myPackages.clearSelection();
      myView.addAll(filtered);
      if (toSelect != null) myPackages.setSelectedValue(toSelect, true);
      Collections.sort(myView);
      fireContentsChanged(this, 0, myView.size());
    }

    @Override
    public RepoPackage getElementAt(int index) {
      return myView.get(index);
    }

    protected ArrayList<RepoPackage> toProcess() {
      return new ArrayList<>(myView);
    }

    @Override
    public int getSize() {
      return myView.size();
    }
  }

  @Override
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return ((FilterComponent)myFilter).getTextEditor();
  }

  private class MyPackageSelectionListener implements ListSelectionListener {
    @Override
    public void valueChanged(ListSelectionEvent event) {
      myOptionsCheckBox.setEnabled(myPackages.getSelectedIndex() >= 0);
      myVersionCheckBox.setEnabled(myPackages.getSelectedIndex() >= 0);
      myOptionsCheckBox.setSelected(false);
      myVersionCheckBox.setSelected(false);
      myVersionComboBox.setEnabled(false);
      myOptionsField.setEnabled(false);
      myDescriptionTextArea.setText(IdeBundle.message("loading.in.progress"));
      final Object pyPackage = myPackages.getSelectedValue();
      if (pyPackage instanceof RepoPackage) {
        final String packageName = ((RepoPackage)pyPackage).getName();
        mySelectedPackageName = packageName;
        myVersionComboBox.removeAllItems();
        if (myVersionCheckBox.isEnabled()) {
          myController.fetchPackageVersions(packageName, new CatchingConsumer<>() {
            @Override
            public void consume(final List<String> releases) {
              ApplicationManager.getApplication().invokeLater(() -> {
                if (myPackages.getSelectedValue() == pyPackage) {
                  myVersionComboBox.removeAllItems();
                  for (String release : releases) {
                    myVersionComboBox.addItem(release);
                  }
                }
              }, ModalityState.any());
            }

            @Override
            public void consume(Exception e) {
              LOG.info("Error retrieving releases", e);
            }
          });
        }
        myInstallButton.setEnabled(!myCurrentlyInstalling.contains(packageName));

        myController.fetchPackageDetails(packageName, new CatchingConsumer<String, Exception>() {
          @Override
          public void consume(final @Nls String details) {
            UIUtil.invokeLaterIfNeeded(() -> {
              if (myPackages.getSelectedValue() == pyPackage) {
                myDescriptionTextArea.setText(details);
                myDescriptionTextArea.setCaretPosition(0);
              }/* else {
                 do nothing, because other package gets selected
              }*/
            });
          }

          @Override
          public void consume(Exception exception) {
            UIUtil.invokeLaterIfNeeded(() -> myDescriptionTextArea.setText(IdeBundle.message("no.information.available")));
            LOG.info("Error retrieving package details", exception);
          }
        });
      }
      else {
        myInstallButton.setEnabled(false);
        myDescriptionTextArea.setText("");
      }
    }
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[0];
  }

  private final class MyTableRenderer extends JPanel implements ListCellRenderer<RepoPackage> {
    private final SimpleColoredComponent myNameComponent = new SimpleColoredComponent();
    private final SimpleColoredComponent myRepositoryComponent = new SimpleColoredComponent();

    private MyTableRenderer() {
      super(new BorderLayout());
      add(myNameComponent, BorderLayout.WEST);
      add(myRepositoryComponent, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends RepoPackage> list, RepoPackage repoPackage, int index, boolean isSelected, boolean cellHasFocus) {
      myNameComponent.clear();
      myRepositoryComponent.clear();

      String packageName = repoPackage.getName();
      SimpleTextAttributes blueText = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.BLUE);
      Color defaultForeground = isSelected ? list.getSelectionForeground() : list.getForeground();
      SimpleTextAttributes defaultText = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, defaultForeground);
      myNameComponent.append(packageName, myInstalledPackages.contains(packageName) ? blueText : defaultText, true);
      if (myCurrentlyInstalling.contains(packageName)) {
        myNameComponent.append(LangBundle.message("package.component.installing.suffix"), blueText, false);
      }
      String repoUrl = repoPackage.getRepoUrl();
      if (StringUtil.isNotEmpty(repoUrl)) {
        myRepositoryComponent.append(repoUrl, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES, false);
      }

      if (isSelected) {
        setBackground(list.getSelectionBackground());
      }
      else {
        setBackground(index % 2 == 1 ? UIUtil.getListBackground() : UIUtil.getDecoratedRowColor());
      }

      return this;
    }
  }
}
