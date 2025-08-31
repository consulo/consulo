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
package consulo.ide.impl.idea.profile.codeInspection.ui;

import consulo.application.dumb.DumbAware;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.ide.impl.idea.codeInspection.ex.Descriptor;
import consulo.ide.impl.idea.packageDependencies.DefaultScopesProvider;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.scope.NonProjectFilesScope;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.action.ComboBoxAction;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
public abstract class ScopesChooser extends ComboBoxAction implements DumbAware {
    public static final String TITLE = "Select a scope to change its settings";

    private final List<Descriptor> myDefaultDescriptors;
    private final InspectionProfileImpl myInspectionProfile;
    private final Project myProject;
    private final Set<String> myExcludedScopeNames;

    public ScopesChooser(
        List<Descriptor> defaultDescriptors,
        InspectionProfileImpl inspectionProfile,
        Project project,
        String[] excludedScopeNames
    ) {
        myDefaultDescriptors = defaultDescriptors;
        myInspectionProfile = inspectionProfile;
        myProject = project;
        myExcludedScopeNames = excludedScopeNames == null ? Collections.<String>emptySet() : ContainerUtil.newHashSet(excludedScopeNames);
        setPopupTitle(TITLE);
        getTemplatePresentation().setText("In All Scopes");
    }

    @Nonnull
    @Override
    public DefaultActionGroup createPopupActionGroup(JComponent component) {
        DefaultActionGroup group = new DefaultActionGroup();

        List<NamedScope> predefinedScopes = new ArrayList<>();
        List<NamedScope> customScopes = new ArrayList<>();
        for (NamedScopesHolder holder : NamedScopesHolder.getAllNamedScopeHolders(myProject)) {
            Collections.addAll(customScopes, holder.getEditableScopes());
            predefinedScopes.addAll(holder.getPredefinedScopes());
        }
        predefinedScopes.remove(DefaultScopesProvider.getAllScope());
        for (NamedScope predefinedScope : predefinedScopes) {
            if (predefinedScope instanceof NonProjectFilesScope) {
                predefinedScopes.remove(predefinedScope);
                break;
            }
        }

        fillActionGroup(group, predefinedScopes, myDefaultDescriptors, myInspectionProfile, myExcludedScopeNames);
        group.addSeparator();
        fillActionGroup(group, customScopes, myDefaultDescriptors, myInspectionProfile, myExcludedScopeNames);

        group.addSeparator();
        group.add(new DumbAwareAction("Edit Scopes Order...") {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                ScopesOrderDialog dlg = new ScopesOrderDialog(myInspectionProfile, myProject);
                if (dlg.showAndGet()) {
                    onScopesOrderChanged();
                }
            }
        });

        return group;
    }

    protected abstract void onScopesOrderChanged();

    protected abstract void onScopeAdded();

    private void fillActionGroup(
        DefaultActionGroup group,
        List<NamedScope> scopes,
        final List<Descriptor> defaultDescriptors,
        final InspectionProfileImpl inspectionProfile,
        Set<String> excludedScopeNames
    ) {
        for (final NamedScope scope : scopes) {
            final String scopeName = scope.getName();
            if (excludedScopeNames.contains(scopeName)) {
                continue;
            }
            group.add(new DumbAwareAction(scopeName) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    for (Descriptor defaultDescriptor : defaultDescriptors) {
                        inspectionProfile.addScope(
                            defaultDescriptor.getToolWrapper().createCopy(),
                            scope,
                            defaultDescriptor.getLevel(),
                            true,
                            e.getData(Project.KEY)
                        );
                    }
                    onScopeAdded();
                }
            });
        }
    }
}
