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
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.ide.impl.idea.analysis.PerformAnalysisInBackgroundOption;
import consulo.ide.impl.idea.codeInspection.InspectionApplication;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.InspectionManagerImpl;
import consulo.ide.impl.idea.codeInspection.offline.OfflineProblemDescriptor;
import consulo.ide.impl.idea.codeInspection.offlineViewer.OfflineInspectionRVContentProvider;
import consulo.ide.impl.idea.codeInspection.offlineViewer.OfflineViewParseUtil;
import consulo.ide.impl.idea.codeInspection.ui.InspectionResultsView;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.language.editor.impl.inspection.reference.RefManagerImpl;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionToolRegistrar;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author anna
 * @since 2007-01-09
 */
@ActionImpl(id = "ViewOfflineInspection")
public class ViewOfflineResultsAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(ViewOfflineResultsAction.class);
    private static final String XML_EXTENSION = "xml";

    @Nonnull
    private final Application myApplication;

    @Inject
    public ViewOfflineResultsAction(@Nonnull Application application) {
        super(ActionLocalize.actionViewofflineinspectionText(), ActionLocalize.actionViewofflineinspectionDescription());
        myApplication = application;
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(event.hasData(Project.KEY));
        presentation.setVisible(ActionPlaces.MAIN_MENU.equals(event.getPlace()));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent event) {
        Project project = event.getRequiredData(Project.KEY);

        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
            @Override
            public Image getIcon(VirtualFile file) {
                if (file.isDirectory() && file.findChild(InspectionApplication.DESCRIPTIONS + ".xml") != null) {
                    return PlatformIconGroup.nodesInspectionresults();
                }
                return super.getIcon(file);
            }
        };
        descriptor.setTitle("Select Path");
        descriptor.setDescription("Select directory which contains exported inspections results");
        VirtualFile virtualFile = IdeaFileChooser.chooseFile(descriptor, project, null);
        if (virtualFile == null || !virtualFile.isDirectory()) {
            return;
        }

        Map<String, Map<String, Set<OfflineProblemDescriptor>>> resMap = new HashMap<>();
        String[] profileName = new String[1];
        Runnable process = () -> {
            VirtualFile[] files = virtualFile.getChildren();
            try {
                for (final VirtualFile inspectionFile : files) {
                    if (inspectionFile.isDirectory()) {
                        continue;
                    }
                    String shortName = inspectionFile.getNameWithoutExtension();
                    String extension = inspectionFile.getExtension();
                    if (shortName.equals(InspectionApplication.DESCRIPTIONS)) {
                        profileName[0] = myApplication.runReadAction(
                            (Supplier<String>) () -> OfflineViewParseUtil.parseProfileName(inspectionFile)
                        );
                    }
                    else if (XML_EXTENSION.equals(extension)) {
                        resMap.put(shortName, myApplication.runReadAction(new Supplier<>() {
                            @Override
                            public Map<String, Set<OfflineProblemDescriptor>> get() {
                                return OfflineViewParseUtil.parse(inspectionFile);
                            }
                        }));
                    }
                }
            }
            catch (Exception e) {  //all parse exceptions
                SwingUtilities.invokeLater(() -> Messages.showInfoMessage(
                    e.getMessage(),
                    InspectionLocalize.offlineViewParseExceptionTitle().get()
                ));
                throw new ProcessCanceledException(); //cancel process
            }
        };
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(
            project,
            InspectionLocalize.parsingInspectionsDumpProgressTitle().get(),
            process,
            () -> SwingUtilities.invokeLater(() -> {
                String name = profileName[0];
                showOfflineView(
                    project,
                    name,
                    resMap,
                    InspectionLocalize.offlineViewTitle() +
                        " (" + (name != null ? name : InspectionLocalize.offlineViewEditorSettingsTitle().get()) + ")"
                );
            }),
            null,
            new PerformAnalysisInBackgroundOption(project)
        );
    }

    @RequiredUIAccess
    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public InspectionResultsView showOfflineView(
        @Nonnull Project project,
        @Nullable final String profileName,
        @Nonnull final Map<String, Map<String, Set<OfflineProblemDescriptor>>> resMap,
        @Nonnull String title
    ) {
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
        InspectionProfile inspectionProfile;
        if (profile != null) {
            inspectionProfile = (InspectionProfile) profile;
        }
        else {
            inspectionProfile = new InspectionProfileImpl(
                profileName != null ? profileName : "Server Side",
                InspectionToolRegistrar.fromApplication(myApplication),
                InspectionProfileManager.getInstance()
            ) {
                @Override
                public boolean isToolEnabled(HighlightDisplayKey key, PsiElement element) {
                    return resMap.containsKey(key.toString());
                }

                @Override
                public HighlightDisplayLevel getErrorLevel(@Nonnull HighlightDisplayKey key, PsiElement element) {
                    return ((InspectionProfile) InspectionProfileManager.getInstance().getRootProfile()).getErrorLevel(key, element);
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
    @RequiredUIAccess
    public static InspectionResultsView showOfflineView(
        @Nonnull Project project,
        @Nonnull Map<String, Map<String, Set<OfflineProblemDescriptor>>> resMap,
        @Nonnull InspectionProfile inspectionProfile,
        @Nonnull String title
    ) {
        AnalysisScope scope = new AnalysisScope(project);
        InspectionManagerImpl managerEx = (InspectionManagerImpl) InspectionManager.getInstance(project);
        GlobalInspectionContextImpl context = managerEx.createNewGlobalContext(false);
        context.setExternalProfile(inspectionProfile);
        context.setCurrentScope(scope);
        context.initializeTools(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        InspectionResultsView view =
            new InspectionResultsView(project, inspectionProfile, scope, context, new OfflineInspectionRVContentProvider(resMap, project));
        ((RefManagerImpl) context.getRefManager()).inspectionReadActionStarted();
        view.update();
        TreeUtil.selectFirstNode(view.getTree());
        context.addView(view, title);
        return view;
    }
}
