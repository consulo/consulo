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
package consulo.ide.impl.idea.profile.codeInspection.ui.filter;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.profile.codeInspection.ui.LevelChooserAction;
import consulo.ide.impl.idea.profile.codeInspection.ui.SingleInspectionProfilePanel;
import consulo.language.Language;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.language.util.LanguageUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Dmitry Batkovich
 */
public class InspectionFilterAction extends DefaultActionGroup implements Toggleable, DumbAware {
    private final SeverityRegistrarImpl mySeverityRegistrar;
    private final InspectionsFilter myInspectionsFilter;

    public InspectionFilterAction(InspectionProfileImpl profile, InspectionsFilter inspectionsFilter, Project project) {
        super("Filter Inspections", true);
        myInspectionsFilter = inspectionsFilter;
        mySeverityRegistrar = (SeverityRegistrarImpl) ((SeverityProvider) profile.getProfileManager()).getOwnSeverityRegistrar();
        getTemplatePresentation().setIcon(PlatformIconGroup.generalFilter());
        tune(profile, project);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().putClientProperty(Toggleable.SELECTED_PROPERTY, !myInspectionsFilter.isEmptyFilter());
    }

    private void tune(InspectionProfileImpl profile, Project project) {
        addAction(new ResetFilterAction());
        addSeparator();

        addAction(new ShowEnabledOrDisabledInspectionsAction(true));
        addAction(new ShowEnabledOrDisabledInspectionsAction(false));
        addSeparator();

        SortedSet<HighlightSeverity> severities = LevelChooserAction.getSeverities(mySeverityRegistrar);
        for (HighlightSeverity severity : severities) {
            add(new ShowWithSpecifiedSeverityInspectionsAction(severity));
        }
        addSeparator();

        Set<Language> languages = new TreeSet<>(LanguageUtil.LANGUAGE_COMPARATOR);
        for (ScopeToolState state : profile.getDefaultStates(project)) {
            ContainerUtil.addIfNotNull(languages, state.getTool().getLanguage());
        }

        // if we don't have inspections for this languages, show it anyway
        languages.addAll(myInspectionsFilter.getSuitableLanguages());

        if (!languages.isEmpty()) {
            for (Language language : languages) {
                add(new LanguageFilterAction(language));
            }
            addSeparator();
        }

        add(new ShowAvailableOnlyOnAnalyzeInspectionsAction());
        add(new ShowOnlyCleanupInspectionsAction());
    }

    private class ResetFilterAction extends DumbAwareAction {
        public ResetFilterAction() {
            super("Reset Filter");
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myInspectionsFilter.reset();
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(!myInspectionsFilter.isEmptyFilter());
        }
    }

    private class ShowOnlyCleanupInspectionsAction extends ToggleAction implements DumbAware {
        public ShowOnlyCleanupInspectionsAction() {
            super("Show Only Cleanup Inspections");
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myInspectionsFilter.isShowOnlyCleanupInspections();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myInspectionsFilter.setShowOnlyCleanupInspections(state);
        }
    }

    private class ShowAvailableOnlyOnAnalyzeInspectionsAction extends ToggleAction implements DumbAware {
        public ShowAvailableOnlyOnAnalyzeInspectionsAction() {
            super(LocalizeValue.localizeTODO("Show Only \"Available only for Analyze | Inspect Code\""));
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myInspectionsFilter.isAvailableOnlyForAnalyze();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myInspectionsFilter.setAvailableOnlyForAnalyze(state);
        }
    }

    private class ShowWithSpecifiedSeverityInspectionsAction extends ToggleAction implements DumbAware {

        private final HighlightSeverity mySeverity;

        private ShowWithSpecifiedSeverityInspectionsAction(HighlightSeverity severity) {
            super(
                SingleInspectionProfilePanel.renderSeverity(severity),
                null,
                HighlightDisplayLevel.find(severity).getIcon()
            );
            mySeverity = severity;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myInspectionsFilter.containsSeverity(mySeverity);
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            if (state) {
                myInspectionsFilter.addSeverity(mySeverity);
            }
            else {
                myInspectionsFilter.removeSeverity(mySeverity);
            }
        }
    }

    private class ShowEnabledOrDisabledInspectionsAction extends ToggleAction implements DumbAware {
        private final Boolean myShowEnabledActions;

        public ShowEnabledOrDisabledInspectionsAction(boolean showEnabledActions) {
            super(LocalizeValue.localizeTODO("Show Only " + (showEnabledActions ? "Enabled" : "Disabled")));
            myShowEnabledActions = showEnabledActions;
        }


        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return Objects.equals(myInspectionsFilter.getSuitableInspectionsStates(), myShowEnabledActions);
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            boolean previousState = isSelected(e);
            myInspectionsFilter.setSuitableInspectionsStates(previousState ? null : myShowEnabledActions);
        }
    }

    private class LanguageFilterAction extends consulo.ui.ex.action.CheckboxAction implements DumbAware {
        private final Language myLanguage;

        public LanguageFilterAction(Language language) {
            super(language.getDisplayName());
            myLanguage = language;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myInspectionsFilter.containsLanguage(myLanguage);
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            if (state) {
                myInspectionsFilter.addLanguage(myLanguage);
            }
            else {
                myInspectionsFilter.removeLanguage(myLanguage);
            }
        }
    }
}