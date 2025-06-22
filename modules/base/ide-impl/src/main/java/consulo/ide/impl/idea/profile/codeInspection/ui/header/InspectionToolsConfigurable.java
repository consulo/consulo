/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.profile.codeInspection.ui.header;

import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.ide.impl.idea.codeInspection.ex.InspectionManagerImpl;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.ide.impl.idea.profile.codeInspection.ui.ErrorsConfigurable;
import consulo.ide.impl.idea.profile.codeInspection.ui.SingleInspectionProfilePanel;
import consulo.language.Language;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionToolRegistrar;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.SmartHashSet;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Anna.Kozlova
 * @since 2006-07-31
 */
public abstract class InspectionToolsConfigurable implements ErrorsConfigurable, SearchableConfigurable, Configurable.NoScroll {
  private static final Logger LOG = Logger.getInstance(InspectionToolsConfigurable.class);

  public static final String ID = "Errors";

  private static final String HEADER_TITLE = "Profile:";
  protected final InspectionProfileManager myProfileManager;
  protected final InspectionProjectProfileManager myProjectProfileManager;

  private final Map<Profile, SingleInspectionProfilePanel> myPanels = new HashMap<>();
  private final List<Profile> myDeletedProfiles = new ArrayList<>();
  private final Set<Language> myFilterLanguages = new SmartHashSet<>();
  protected ProfilesConfigurableComboBox myProfiles;

  private Alarm mySelectionAlarm;

  private CardLayout myLayout;
  private JLabel myDescriptionLabel;
  private JPanel myPanel;
  private JPanel myWholePanel;

  public InspectionToolsConfigurable(@Nonnull final InspectionProjectProfileManager projectProfileManager,
                                     InspectionProfileManager profileManager) {

    ((InspectionManagerImpl)InspectionManager.getInstance(projectProfileManager.getProject())).buildInspectionSearchIndexIfNecessary();
    myProjectProfileManager = projectProfileManager;
    myProfileManager = profileManager;
  }

  private Project getProject() {
    return myProjectProfileManager.getProject();
  }

  @Nullable
  private InspectionProfileImpl copyToNewProfile(ModifiableModel selectedProfile, @Nonnull Project project) {
    String profileDefaultName = selectedProfile.getName();
    do {
      profileDefaultName += " (copy)";
    }
    while (hasName(profileDefaultName, myPanels.get(selectedProfile).isProfileShared()));

    final ProfileManager profileManager = selectedProfile.getProfileManager();
    InspectionProfileImpl inspectionProfile =
      new InspectionProfileImpl(profileDefaultName, InspectionToolRegistrar.getInstance(), profileManager);

    inspectionProfile.copyFrom(selectedProfile);
    inspectionProfile.setName(profileDefaultName);
    inspectionProfile.initInspectionTools(project);
    inspectionProfile.setModified(true);
    return inspectionProfile;
  }

  private void addProfile(InspectionProfileImpl model) {
    final String modelName = model.getName();
    final SingleInspectionProfilePanel panel = createPanel(model, modelName);
    myPanel.add(getCardName(model), panel);

    myProfiles.getModel().addElement(model);
    putProfile(model, panel);
    myProfiles.selectProfile(model);
  }

  private void buildUI() {
    if (myWholePanel != null) {
      return;
    }

    myWholePanel = new JPanel(new BorderLayout());
    myLayout = new CardLayout();

    final JPanel toolbar = new JPanel(new GridBagLayout());
    toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));

    myPanel = new JPanel();

    myWholePanel.add(toolbar, BorderLayout.PAGE_START);
    myWholePanel.add(myPanel, BorderLayout.CENTER);

    myProfiles = new ProfilesConfigurableComboBox(new ColoredListCellRenderer<>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList<? extends InspectionProfile> list,
                                           InspectionProfile value,
                                           int index,
                                           boolean selected,
                                           boolean hasFocus) {
        final SingleInspectionProfilePanel singleInspectionProfilePanel = myPanels.get(value);
        setIcon(PlatformIconGroup.generalGearplain());
        append(singleInspectionProfilePanel.getCurrentProfileName());
      }
    }) {
      @Override
      public void onProfileChosen(InspectionProfileImpl inspectionProfile) {
        myLayout.show(myPanel, getCardName(inspectionProfile));
        myDescriptionLabel.setText(inspectionProfile.getDescription());
      }
    };
    JPanel profilesHolder = new JPanel();
    profilesHolder.setLayout(new CardLayout());


    JComponent manageButton = new ManageButton(new ManageButtonBuilder() {
      @Override
      public boolean isSharedToTeamMembers() {
        SingleInspectionProfilePanel panel = getSelectedPanel();
        return panel != null && panel.isProfileShared();
      }

      @Override
      public void setShareToTeamMembers(boolean shared) {
        final SingleInspectionProfilePanel selectedPanel = getSelectedPanel();
        LOG.assertTrue(selectedPanel != null, "No settings selectedPanel for: " + getSelectedObject());

        final String name = getSelectedPanel().getCurrentProfileName();
        for (SingleInspectionProfilePanel p : myPanels.values()) {
          if (p != selectedPanel && Comparing.equal(p.getCurrentProfileName(), name)) {
            final boolean curShared = p.isProfileShared();
            if (curShared == shared) {
              Messages.showErrorDialog((shared ? "Shared" : "Application level") + " profile with same name exists.",
                                       "Inspections Settings");
              return;
            }
          }
        }

        selectedPanel.setProfileShared(shared);
        myProfiles.repaint();
      }

      @Override
      public void copy() {
        final InspectionProfileImpl newProfile = copyToNewProfile(getSelectedObject(), getProject());
        if (newProfile != null) {
          final InspectionProfileImpl modifiableModel = (InspectionProfileImpl)newProfile.getModifiableModel();
          modifiableModel.setModified(true);
          modifiableModel.setProjectLevel(false);
          addProfile(modifiableModel);
          rename(modifiableModel);
        }
      }

      @Override
      public boolean canRename() {
        final InspectionProfileImpl profile = getSelectedObject();
        return !profile.isProfileLocked();
      }

      @Override
      public void rename() {
        rename(getSelectedObject());
      }

      private void rename(@Nonnull final InspectionProfileImpl inspectionProfile) {
        final String initialName = getSelectedPanel().getCurrentProfileName();
        myProfiles.showEditCard(initialName, new SaveInputComponentValidator() {
          @Override
          public void doSave(@Nonnull String text) {
            if (!text.equals(initialName)) {
              getProfilePanel(inspectionProfile).setCurrentProfileName(text);
            }
          }

          @Override
          public boolean checkValid(@Nonnull String text) {
            final SingleInspectionProfilePanel singleInspectionProfilePanel = myPanels.get(inspectionProfile);
            if (singleInspectionProfilePanel == null) {
              return false;
            }
            final boolean isValid = text.equals(initialName) || !hasName(text, singleInspectionProfilePanel.isProfileShared());
            if (isValid) {
              myDescriptionLabel.setText(getSelectedObject().getDescription());
            }
            else {
              //myAuxiliaryRightPanel.showError("Name is already in use. Please change name to unique.");
            }
            return isValid;
          }

          @Override
          public void cancel() {
            myDescriptionLabel.setText(getSelectedObject().getDescription());
          }
        });
      }

      @Override
      public boolean canDelete() {
        return isDeleteEnabled(myProfiles.getSelectedProfile());
      }

      @Override
      public void delete() {
        final InspectionProfileImpl selectedProfile = myProfiles.getSelectedProfile();
        myProfiles.getModel().removeElement(selectedProfile);
        myDeletedProfiles.add(selectedProfile);
      }

      @Override
      public boolean canEditDescription() {
        return true;
      }

      @Override
      @RequiredUIAccess
      public void editDescription() {
        String description = Messages.showInputDialog(myDescriptionLabel,
          "Enter Description",
          "Description",
          null,
          getSelectedObject().getDescription(),
          null
        );
        if (description == null) {
          myDescriptionLabel.setText(getSelectedObject().getDescription());
          return;
        }

        final InspectionProfileImpl inspectionProfile = getSelectedObject();
        if (!Comparing.strEqual(description, inspectionProfile.getDescription())) {
          inspectionProfile.setDescription(description);
          inspectionProfile.setModified(true);
        }
        myDescriptionLabel.setText(getSelectedObject().getDescription());
      }

      @Override
      public boolean hasDescription() {
        return !StringUtil.isEmpty(getSelectedObject().getDescription());
      }

      @Override
      public void export() {
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setDescription("Choose directory to store profile file");
        IdeaFileChooser.chooseFile(descriptor, getProject(), myWholePanel, null, new Consumer<VirtualFile>() {
          @RequiredUIAccess
          @Override
          public void accept(VirtualFile file) {
            final Element element = new Element("inspections");
            try {
              final SingleInspectionProfilePanel panel = getSelectedPanel();
              LOG.assertTrue(panel != null);
              final InspectionProfileImpl profile = getSelectedObject();
              LOG.assertTrue(true);
              profile.writeExternal(element);
              final String filePath =
                FileUtil.toSystemDependentName(file.getPath()) + File.separator + FileUtil.sanitizeFileName(profile.getName()) + ".xml";
              if (new File(filePath).isFile()) {
                int buttonPressed = Messages.showOkCancelDialog(
                  myWholePanel,
                  "File \'" + filePath + "\' already exist. Do you want to overwrite it?",
                  "Warning",
                  UIUtil.getQuestionIcon()
                );
                if (buttonPressed != Messages.OK) {
                  return;
                }
              }
              JDOMUtil.writeDocument(new Document(element), filePath, SystemProperties.getLineSeparator());
            }
            catch (WriteExternalException | IOException e1) {
              LOG.error(e1);
            }
          }
        });
      }

      @Override
      public void doImport() {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
          @RequiredUIAccess
          @Override
          public boolean isFileSelectable(VirtualFile file) {
            return "xml".equals(file.getExtension());
          }
        };
        descriptor.setDescription("Choose profile file");
        IdeaFileChooser.chooseFile(descriptor, getProject(), myWholePanel, null, file -> {
          if (file == null) return;
          InspectionProfileImpl profile =
            new InspectionProfileImpl("TempProfile", InspectionToolRegistrar.getInstance(), myProfileManager);
          try {
            Element rootElement = JDOMUtil.loadDocument(VfsUtilCore.virtualToIoFile(file)).getRootElement();
            if (Comparing.strEqual(rootElement.getName(), "component")) {//import right from .idea/inspectProfiles/xxx.xml
              rootElement = rootElement.getChildren().get(0);
            }
            final Set<String> levels = new HashSet<>();
            for (Object o : rootElement.getChildren("inspection_tool")) {
              final Element inspectElement = (Element)o;
              levels.add(inspectElement.getAttributeValue("level"));
              for (Object s : inspectElement.getChildren("scope")) {
                levels.add(((Element)s).getAttributeValue("level"));
              }
            }
            for (Iterator<String> iterator = levels.iterator(); iterator.hasNext(); ) {
              String level = iterator.next();
              if (myProfileManager.getOwnSeverityRegistrar().getSeverity(level) != null) {
                iterator.remove();
              }
            }
            if (!levels.isEmpty()) {
              int buttonPressed = Messages.showYesNoDialog(
                myWholePanel,
                "Undefined severities detected: " +
                  StringUtil.join(levels, ", ") + ". Do you want to create them?",
                "Warning",
                UIUtil.getWarningIcon()
              );
              if (buttonPressed == Messages.YES) {
                for (String level : levels) {
                  final TextAttributes textAttributes = CodeInsightColors.WARNINGS_ATTRIBUTES.getDefaultAttributes();
                  HighlightInfoType.HighlightInfoTypeImpl info =
                    new HighlightInfoType.HighlightInfoTypeImpl(new HighlightSeverity(level, 50),
                                                                TextAttributesKey.createTextAttributesKey(level));
                  ((SeverityRegistrarImpl)myProfileManager.getOwnSeverityRegistrar()).registerSeverity(new SeverityRegistrarImpl.SeverityBasedTextAttributes(
                    textAttributes.clone(),
                    info), textAttributes.getErrorStripeColor());
                }
              }
            }
            profile.readExternal(rootElement);
            profile.setProjectLevel(false);
            profile.initInspectionTools(getProject());
            if (getProfilePanel(profile) != null) {
              int buttonPressed = Messages.showOkCancelDialog(
                myWholePanel,
                "Profile with name \'" + profile.getName() + "\' already exists. Do you want to overwrite it?",
                "Warning",
                UIUtil.getInformationIcon()
              );
              if (buttonPressed != Messages.OK) {
                return;
              }
            }
            final ModifiableModel model = profile.getModifiableModel();
            model.setModified(true);
            addProfile((InspectionProfileImpl)model);

            //TODO myDeletedProfiles ? really need this
            myDeletedProfiles.remove(profile);
          }
          catch (InvalidDataException | IOException | JDOMException e1) {
            LOG.error(e1);
          }
        });
      }
    }).build();

    myDescriptionLabel = new JBLabel();
    myDescriptionLabel.setUI(new MultiLineLabelUI());

    toolbar.add(
      new JLabel(HEADER_TITLE),
      new GridBagConstraints(
        0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
        JBUI.emptyInsets(), 0, 0
      )
    );

    toolbar.add(
      myProfiles,
      new GridBagConstraints(
        1, 0, 1, 1, 0, 1.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
        JBUI.insetsLeft(6), 0, 0
      )
    );

    toolbar.add(
      manageButton,
      new GridBagConstraints(
        2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
        JBUI.insetsLeft(10), 0, 0
      )
    );

    toolbar.add(
      myDescriptionLabel,
      new GridBagConstraints(
        3, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        JBUI.insetsLeft(15), 0, 0
      )
    );
  }

  @Override
  public String getDisplayName() {
    return "Inspections";
  }

  @Override
  @Nonnull
  public String getId() {
    return ID;
  }

  @Override
  public Runnable enableSearch(final String option) {
    return () -> {
      SingleInspectionProfilePanel panel = getSelectedPanel();
      if (panel != null) {
        panel.setFilter(option);
      }
    };
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    buildUI();

    myPanel.setLayout(myLayout);
    return myWholePanel;
  }

  protected abstract InspectionProfileImpl getCurrentProfile();

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    buildUI();

    final InspectionProfileImpl selectedProfile = getSelectedObject();
    final InspectionProfileImpl currentProfile = getCurrentProfile();
    if (!Comparing.equal(selectedProfile, currentProfile)) {
      return true;
    }
    for (SingleInspectionProfilePanel panel : myPanels.values()) {
      if (panel.isModified()) return true;
    }
    return getProfiles().size() != myPanels.size() || !myDeletedProfiles.isEmpty();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    buildUI();

    final SingleInspectionProfilePanel selectedPanel = getSelectedPanel();
    for (final Profile inspectionProfile : myPanels.keySet()) {
      if (myDeletedProfiles.remove(inspectionProfile)) {
        deleteProfile(getProfilePanel(inspectionProfile).getSelectedProfile());
      }
      else {
        final SingleInspectionProfilePanel panel = getProfilePanel(inspectionProfile);
        panel.apply();
        if (panel == selectedPanel) {
          applyRootProfile(panel.getCurrentProfileName(), panel.isProfileShared());
        }
      }
    }
    doReset();
  }

  protected abstract void applyRootProfile(final String name, final boolean isShared);

  private SingleInspectionProfilePanel getProfilePanel(Profile inspectionProfile) {
    return myPanels.get(inspectionProfile);
  }

  private void putProfile(Profile profile, SingleInspectionProfilePanel panel) {
    myPanels.put(profile, panel);
  }

  protected void deleteProfile(Profile profile) {
    final String name = profile.getName();
    if (profile.getProfileManager() == myProfileManager) {
      if (myProfileManager.getProfile(name, false) != null) {
        myProfileManager.deleteProfile(name);
      }
      return;
    }
    if (profile.getProfileManager() == myProjectProfileManager) {
      if (myProjectProfileManager.getProfile(name, false) != null) {
        myProjectProfileManager.deleteProfile(name);
      }
    }
  }

  protected boolean acceptTool(InspectionToolWrapper entry) {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    buildUI();

    doReset();
  }

  private void doReset() {
    myDeletedProfiles.clear();
    myPanels.clear();
    final Collection<InspectionProfile> profiles = getProfiles();
    final List<InspectionProfile> modifiableProfiles = new ArrayList<>(profiles.size());
    for (InspectionProfile profile : profiles) {
      final String profileName = profile.getName();
      final ModifiableModel modifiableProfile = profile.getModifiableModel();
      modifiableProfiles.add((InspectionProfile)modifiableProfile);
      final InspectionProfileImpl inspectionProfile = (InspectionProfileImpl)modifiableProfile;
      final SingleInspectionProfilePanel panel = createPanel(inspectionProfile, profileName);
      putProfile(modifiableProfile, panel);
      myPanel.add(getCardName(inspectionProfile), panel);
    }
    myProfiles.reset(modifiableProfiles);
    myDescriptionLabel.setText(getSelectedObject().getDescription());

    final InspectionProfileImpl inspectionProfile = getCurrentProfile();
    myProfiles.selectProfile(inspectionProfile);
    myLayout.show(myPanel, getCardName(inspectionProfile));
    final SingleInspectionProfilePanel panel = getSelectedPanel();
    if (panel != null) {
      panel.setVisible(true);//make sure that UI was initialized
      mySelectionAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
      SwingUtilities.invokeLater(() -> {
        if (mySelectionAlarm != null) {
          mySelectionAlarm.addRequest(panel::updateSelection, 200);
        }
      });
    }
  }

  private static String getCardName(final InspectionProfileImpl inspectionProfile) {
    return (inspectionProfile.isProjectLevel() ? "s" : "a") + inspectionProfile.getName();
  }

  @Nonnull
  private SingleInspectionProfilePanel createPanel(InspectionProfileImpl profile, String profileName) {
    SingleInspectionProfilePanel panel = new SingleInspectionProfilePanel(myProjectProfileManager, profileName, profile) {
      @Override
      protected boolean accept(InspectionToolWrapper entry) {
        return super.accept(entry) && acceptTool(entry);
      }
    };

    panel.getInspectionsFilter().addLanguages(myFilterLanguages);
    return panel;
  }

  private boolean isDeleteEnabled(@Nonnull InspectionProfileImpl inspectionProfile) {
    final ProfileManager profileManager = inspectionProfile.getProfileManager();

    boolean projectProfileFound = false;
    boolean ideProfileFound = false;

    final ComboBoxModel<InspectionProfile> model = myProfiles.getModel();
    for (int i = 0; i < model.getSize(); i++) {
      InspectionProfile profile = model.getElementAt(i);
      if (inspectionProfile == profile) continue;
      final boolean isProjectProfile = profile.getProfileManager() == myProjectProfileManager;
      projectProfileFound |= isProjectProfile;
      ideProfileFound |= !isProjectProfile;

      if (ideProfileFound && projectProfileFound) break;
    }

    return profileManager == myProjectProfileManager ? projectProfileFound : ideProfileFound;
  }

  protected Collection<InspectionProfile> getProfiles() {
    final Collection<InspectionProfile> result = new ArrayList<>();
    result.addAll(new TreeSet<>(myProfileManager.getProfiles()));
    result.addAll(myProjectProfileManager.getProfiles());
    return result;
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    for (SingleInspectionProfilePanel panel : myPanels.values()) {
      panel.disposeUI();
    }

    myWholePanel = null;
    myPanels.clear();
    if (mySelectionAlarm != null) {
      Disposer.dispose(mySelectionAlarm);
      mySelectionAlarm = null;
    }
  }

  @Override
  public void selectProfile(Profile profile) {
    myProfiles.selectProfile(profile);
  }

  @Override
  public void setFilterLanguages(@Nonnull Collection<Language> languages) {
    myFilterLanguages.clear();
    myFilterLanguages.addAll(languages);
  }

  @Override
  public void selectInspectionTool(String selectedToolShortName) {
    final InspectionProfileImpl inspectionProfile = getSelectedObject();
    final SingleInspectionProfilePanel panel = getProfilePanel(inspectionProfile);
    LOG.assertTrue(panel != null, "No settings panel for: " + inspectionProfile + "; " + configuredProfiles());
    panel.selectInspectionTool(selectedToolShortName);
  }

  protected SingleInspectionProfilePanel getSelectedPanel() {
    final InspectionProfileImpl inspectionProfile = getSelectedObject();
    return getProfilePanel(inspectionProfile);
  }

  private String configuredProfiles() {
    return "configured profiles: " + StringUtil.join(myPanels.keySet(), ", ");
  }

  private boolean hasName(final @Nonnull String name, boolean shared) {
    for (SingleInspectionProfilePanel p : myPanels.values()) {
      if (name.equals(p.getCurrentProfileName()) && shared == p.isProfileShared()) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  @Override
  public InspectionProfileImpl getSelectedObject() {
    return myProfiles.getSelectedProfile();
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    final InspectionProfileImpl inspectionProfile = getSelectedObject();
    return getProfilePanel(inspectionProfile).getPreferredFocusedComponent();
  }
}
