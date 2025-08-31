/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.editor.ui.scope;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.scope.AnalysisScope;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author anna
 * @since 2006-02-28
 */
@Singleton
@State(name = "AnalysisUIOptions", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class AnalysisUIOptions implements PersistentStateComponent<AnalysisUIOptions> {
    public static AnalysisUIOptions getInstance(Project project) {
        return project.getInstance(AnalysisUIOptions.class);
    }

    public boolean AUTOSCROLL_TO_SOURCE = false;
    public float SPLITTER_PROPORTION = 0.5f;
    public boolean GROUP_BY_SEVERITY = false;
    public boolean FILTER_RESOLVED_ITEMS = true;
    public boolean ANALYZE_TEST_SOURCES = true;
    public boolean ANALYZE_INJECTED_CODE = true;
    public boolean SHOW_DIFF_WITH_PREVIOUS_RUN = false;
    public int SCOPE_TYPE = AnalysisScope.PROJECT;
    public String CUSTOM_SCOPE_NAME = "";
    private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;
    public boolean SHOW_ONLY_DIFF = false;
    public boolean SHOW_STRUCTURE = false;

    public boolean ANALYSIS_IN_BACKGROUND = true;

    public AnalysisUIOptions() {
        myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
            @Override
            protected boolean isAutoScrollMode() {
                return AUTOSCROLL_TO_SOURCE;
            }

            @Override
            protected void setAutoScrollMode(boolean state) {
                AUTOSCROLL_TO_SOURCE = state;
            }
        };
    }

    public AnalysisUIOptions copy() {
        AnalysisUIOptions result = new AnalysisUIOptions();
        XmlSerializerUtil.copyBean(this, result);
        return result;
    }

    public void save(AnalysisUIOptions options) {
        XmlSerializerUtil.copyBean(options, this);
    }

    public AutoScrollToSourceHandler getAutoScrollToSourceHandler() {
        return myAutoScrollToSourceHandler;
    }

    public AnAction createGroupBySeverityAction(final Runnable updater) {
        return new ToggleAction(
            InspectionLocalize.inspectionActionGroupBySeverity(),
            InspectionLocalize.inspectionActionGroupBySeverityDescription(),
            AllIcons.Nodes.SortBySeverity
        ) {
            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return GROUP_BY_SEVERITY;
            }

            @Override
            public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                GROUP_BY_SEVERITY = state;
                updater.run();
            }
        };
    }

    public AnAction createFilterResolvedItemsAction(final Runnable updater) {
        return new ToggleAction(
            InspectionLocalize.inspectionFilterResolvedActionText(),
            InspectionLocalize.inspectionFilterResolvedActionText(),
            AllIcons.General.Filter
        ) {
            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return FILTER_RESOLVED_ITEMS;
            }

            @Override
            public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                FILTER_RESOLVED_ITEMS = state;
                updater.run();
            }
        };
    }

    public AnAction createShowOutdatedProblemsAction(final Runnable updater) {
        return new ToggleAction(
            InspectionLocalize.inspectionFilterShowDiffActionText(),
            InspectionLocalize.inspectionFilterShowDiffActionText(),
            AllIcons.Actions.Diff
        ) {
            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return SHOW_DIFF_WITH_PREVIOUS_RUN;
            }

            @Override
            public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                SHOW_DIFF_WITH_PREVIOUS_RUN = state;
                if (!SHOW_DIFF_WITH_PREVIOUS_RUN) {
                    SHOW_ONLY_DIFF = false;
                }
                updater.run();
            }
        };
    }

    public AnAction createGroupByDirectoryAction(final Runnable updater) {
        return new ToggleAction(
            InspectionLocalize.inspectionActionGroupByDirectory(),
            InspectionLocalize.inspectionActionGroupByDirectory(),
            AllIcons.Actions.GroupByPackage
        ) {
            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return SHOW_STRUCTURE;
            }

            @Override
            public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                SHOW_STRUCTURE = state;
                updater.run();
            }
        };
    }

    public AnAction createShowDiffOnlyAction(final Runnable updater) {
        return new ToggleAction(
            InspectionLocalize.inspectionFilterShowDiffOnlyActionText(),
            InspectionLocalize.inspectionFilterShowDiffOnlyActionText(),
            PlatformIconGroup.actionsDiff()
        ) {
            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return SHOW_ONLY_DIFF;
            }

            @Override
            public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                SHOW_ONLY_DIFF = state;
                updater.run();
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setEnabled(SHOW_DIFF_WITH_PREVIOUS_RUN);
            }
        };
    }

    @Override
    public AnalysisUIOptions getState() {
        return this;
    }

    @Override
    public void loadState(AnalysisUIOptions state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
