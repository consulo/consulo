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

/*
 * User: anna
 * Date: 09-Jan-2007
 */
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.scope.AnalysisScope;
import consulo.ide.impl.idea.analysis.PerformAnalysisInBackgroundOption;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.ide.impl.idea.codeInspection.InspectionApplication;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.InspectionManagerEx;
import consulo.ide.impl.idea.codeInspection.ex.InspectionProfileImpl;
import consulo.language.editor.inspection.scheme.Tools;
import consulo.ide.impl.idea.codeInspection.offline.OfflineProblemDescriptor;
import consulo.ide.impl.idea.codeInspection.offlineViewer.OfflineInspectionRVContentProvider;
import consulo.ide.impl.idea.codeInspection.offlineViewer.OfflineViewParseUtil;
import consulo.ide.impl.idea.codeInspection.reference.RefManagerImpl;
import consulo.ide.impl.idea.codeInspection.ui.InspectionResultsView;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.fileChooser.IdeaFileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.application.util.function.Computable;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ViewOfflineResultsAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(ViewOfflineResultsAction.class);
  @NonNls private static final String XML_EXTENSION = "xml";

  @Override
  public void update(AnActionEvent event) {
    final Presentation presentation = event.getPresentation();
    final Project project = event.getData(CommonDataKeys.PROJECT);
    presentation.setEnabled(project != null);
    presentation.setVisible(ActionPlaces.MAIN_MENU.equals(event.getPlace()));
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getData(CommonDataKeys.PROJECT);

    LOG.assertTrue(project != null);

    final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false){
      @Override
      public Image getIcon(VirtualFile file) {
        if (file.isDirectory()) {
          if (file.findChild(InspectionApplication.DESCRIPTIONS + ".xml") != null) {
            return AllIcons.Nodes.InspectionResults;
          }
        }
        return super.getIcon(file);
      }
    };
    descriptor.setTitle("Select Path");
    descriptor.setDescription("Select directory which contains exported inspections results");
    final VirtualFile virtualFile = IdeaFileChooser.chooseFile(descriptor, project, null);
    if (virtualFile == null || !virtualFile.isDirectory()) return;

    final Map<String, Map<String, Set<OfflineProblemDescriptor>>> resMap =
            new HashMap<String, Map<String, Set<OfflineProblemDescriptor>>>();
    final String [] profileName = new String[1];
    final Runnable process = new Runnable() {
      @Override
      public void run() {
        final VirtualFile[] files = virtualFile.getChildren();
        try {
          for (final VirtualFile inspectionFile : files) {
            if (inspectionFile.isDirectory()) continue;
            final String shortName = inspectionFile.getNameWithoutExtension();
            final String extension = inspectionFile.getExtension();
            if (shortName.equals(InspectionApplication.DESCRIPTIONS)) {
              profileName[0] = ApplicationManager.getApplication().runReadAction(
                      new Computable<String>() {
                        @Override
                        @Nullable
                        public String compute() {
                          return OfflineViewParseUtil.parseProfileName(inspectionFile);
                        }
                      }
              );
            }
            else if (XML_EXTENSION.equals(extension)) {
              resMap.put(shortName, ApplicationManager.getApplication().runReadAction(
                      new Computable<Map<String, Set<OfflineProblemDescriptor>>>() {
                        @Override
                        public Map<String, Set<OfflineProblemDescriptor>> compute() {
                          return OfflineViewParseUtil.parse(inspectionFile);
                        }
                      }
              ));
            }
          }
        }
        catch (final Exception e) {  //all parse exceptions
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              Messages.showInfoMessage(e.getMessage(), InspectionsBundle.message("offline.view.parse.exception.title"));
            }
          });
          throw new ProcessCanceledException(); //cancel process
        }
      }
    };
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(project, InspectionsBundle.message("parsing.inspections.dump.progress.title"), process, new Runnable() {
      @Override
      public void run() {
        SwingUtilities.invokeLater(new Runnable(){
          @Override
          public void run() {
            final String name = profileName[0];
            showOfflineView(project, name, resMap,
                            InspectionsBundle.message("offline.view.title") +
                            " (" + (name != null ? name : InspectionsBundle.message("offline.view.editor.settings.title")) +")");
          }
        });
      }
    }, null, new PerformAnalysisInBackgroundOption(project));
  }

  @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"}) //used in TeamCity
  public static InspectionResultsView showOfflineView(@Nonnull Project project,
                                                      @Nullable
                                                      final String profileName,
                                                      @Nonnull final Map<String, Map<String, Set<OfflineProblemDescriptor>>> resMap,
                                                      @Nonnull String title) {
    Profile profile;
    if (profileName != null) {
      profile = InspectionProjectProfileManager.getInstance(project).getProfile(profileName, false);
      if (profile == null) {
        profile = InspectionProfileManager.getInstance().getProfile(profileName, false);
      }
    }
    else {
      profile = null;
    }
    final InspectionProfile inspectionProfile;
    if (profile != null) {
      inspectionProfile = (InspectionProfile)profile;
    }
    else {
      inspectionProfile = new InspectionProfileImpl(profileName != null ? profileName : "Server Side") {
        @Override
        public boolean isToolEnabled(final HighlightDisplayKey key, PsiElement element) {
          return resMap.containsKey(key.toString());
        }

        @Override
        public HighlightDisplayLevel getErrorLevel(@Nonnull final HighlightDisplayKey key, PsiElement element) {
          return ((InspectionProfile)InspectionProfileManager.getInstance().getRootProfile()).getErrorLevel(key, element);
        }

        @Override
        public boolean isEditable() {
          return false;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
          return getName();
        }
      };
    }
    return showOfflineView(project, resMap, inspectionProfile, title);
  }

  @Nonnull
  public static InspectionResultsView showOfflineView(@Nonnull Project project,
                                                      @Nonnull Map<String, Map<String, Set<OfflineProblemDescriptor>>> resMap,
                                                      @Nonnull InspectionProfile inspectionProfile,
                                                      @Nonnull String title) {
    final AnalysisScope scope = new AnalysisScope(project);
    final InspectionManagerEx managerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
    final GlobalInspectionContextImpl context = managerEx.createNewGlobalContext(false);
    context.setExternalProfile(inspectionProfile);
    context.setCurrentScope(scope);
    context.initializeTools(new ArrayList<Tools>(), new ArrayList<Tools>(), new ArrayList<Tools>());
    final InspectionResultsView view = new InspectionResultsView(project, inspectionProfile, scope, context,
                                                                 new OfflineInspectionRVContentProvider(resMap, project));
    ((RefManagerImpl)context.getRefManager()).inspectionReadActionStarted();
    view.update();
    TreeUtil.selectFirstNode(view.getTree());
    context.addView(view, title);
    return view;
  }
}
