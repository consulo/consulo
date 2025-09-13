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
package consulo.versionControlSystem.update;

import consulo.configurable.UnnamedConfigurable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.UpdateOrStatusOptionsDialog;
import jakarta.annotation.Nonnull;

import java.util.SequencedMap;

public interface ActionInfo {
    ActionInfo UPDATE = new ActionInfo() {
        @Override
        public boolean showOptions(Project project) {
            return ProjectLevelVcsManager.getInstance(project).getOptions(VcsConfiguration.StandardOption.UPDATE).getValue();
        }

        @Override
        public UpdateEnvironment getEnvironment(AbstractVcs vcs) {
            return vcs.getUpdateEnvironment();
        }

        @Override
        public String getActionName() {
            return VcsLocalize.actionNameUpdate().get();
        }

        @Override
        @RequiredUIAccess
        public UpdateOrStatusOptionsDialog createOptionsDialog(
            Project project,
            SequencedMap<UnnamedConfigurable, AbstractVcs> envToConfMap,
            String scopeName
        ) {
            return new UpdateOrStatusOptionsDialog(project, envToConfMap) {
                @Nonnull
                @Override
                protected LocalizeValue getRealTitle() {
                    return VcsLocalize.actionDisplayNameUpdateScope(scopeName);
                }

                @Override
                protected String getActionNameForDimensions() {
                    return "update";
                }

                @Override
                protected boolean isToBeShown() {
                    return ProjectLevelVcsManager.getInstance(project).getOptions(VcsConfiguration.StandardOption.UPDATE).getValue();
                }

                @Override
                protected void setToBeShown(boolean value, boolean onOk) {
                    if (onOk) {
                        ProjectLevelVcsManager.getInstance(project).getOptions(VcsConfiguration.StandardOption.UPDATE).setValue(value);
                    }
                }
            };
        }

        @Override
        public String getActionName(String scopeName) {
            return VcsLocalize.actionNameUpdateScope(scopeName).get();
        }

        @Override
        public String getGroupName(FileGroup fileGroup) {
            return fileGroup.getUpdateName();
        }

        @Override
        public boolean canGroupByChangelist() {
            return true;
        }

        @Override
        public boolean canChangeFileStatus() {
            return false;
        }
    };

    ActionInfo STATUS = new ActionInfo() {
        @Override
        public boolean showOptions(Project project) {
            return ProjectLevelVcsManager.getInstance(project).getOptions(VcsConfiguration.StandardOption.STATUS).getValue();
        }

        @Override
        public UpdateEnvironment getEnvironment(AbstractVcs vcs) {
            return vcs.getStatusEnvironment();
        }

        @Override
        @RequiredUIAccess
        public UpdateOrStatusOptionsDialog createOptionsDialog(
            Project project,
            SequencedMap<UnnamedConfigurable, AbstractVcs> envToConfMap,
            String scopeName
        ) {
            return new UpdateOrStatusOptionsDialog(project, envToConfMap) {
                @Nonnull
                @Override
                protected LocalizeValue getRealTitle() {
                    return VcsLocalize.actionDisplayNameCheckScopeStatus(scopeName);
                }

                @Override
                protected String getActionNameForDimensions() {
                    return "status";
                }

                @Override
                protected boolean isToBeShown() {
                    return ProjectLevelVcsManager.getInstance(project).getOptions(VcsConfiguration.StandardOption.STATUS).getValue();
                }

                @Override
                protected void setToBeShown(boolean value, boolean onOk) {
                    if (onOk) {
                        ProjectLevelVcsManager.getInstance(project).getOptions(VcsConfiguration.StandardOption.STATUS).setValue(value);
                    }
                }
            };
        }

        @Override
        public String getActionName() {
            return VcsLocalize.actionNameCheckStatus().get();
        }

        @Override
        public String getActionName(String scopeName) {
            return VcsLocalize.actionNameCheckScopeStatus(scopeName).get();
        }

        @Override
        public String getGroupName(FileGroup fileGroup) {
            return fileGroup.getStatusName();
        }

        @Override
        public boolean canGroupByChangelist() {
            return false;
        }

        @Override
        public boolean canChangeFileStatus() {
            return true;
        }
    };

    ActionInfo INTEGRATE = new ActionInfo() {
        @Override
        public boolean showOptions(Project project) {
            return true;
        }

        @Override
        public UpdateEnvironment getEnvironment(AbstractVcs vcs) {
            return vcs.getIntegrateEnvironment();
        }

        @Override
        @RequiredUIAccess
        public UpdateOrStatusOptionsDialog createOptionsDialog(
            Project project,
            SequencedMap<UnnamedConfigurable, AbstractVcs> envToConfMap,
            String scopeName
        ) {
            return new UpdateOrStatusOptionsDialog(project, envToConfMap) {
                @Nonnull
                @Override
                protected LocalizeValue getRealTitle() {
                    return VcsLocalize.actionDisplayNameIntegrateScope(scopeName);
                }

                @Override
                protected String getActionNameForDimensions() {
                    return "integrate";
                }

                @Override
                protected boolean canBeHidden() {
                    return false;
                }

                @Override
                protected boolean isToBeShown() {
                    return true;
                }

                @Override
                protected void setToBeShown(boolean value, boolean onOk) {
                }
            };
        }

        @Override
        public boolean canChangeFileStatus() {
            return true;
        }

        @Override
        public String getActionName(String scopeName) {
            return VcsLocalize.actionNameIntegrateScope(scopeName).get();
        }

        @Override
        public String getActionName() {
            return VcsLocalize.actionNameIntegrate().get();
        }

        @Override
        public String getGroupName(FileGroup fileGroup) {
            return fileGroup.getUpdateName();
        }

        @Override
        public boolean canGroupByChangelist() {
            return false;
        }
    };


    boolean showOptions(Project project);

    UpdateEnvironment getEnvironment(AbstractVcs vcs);

    @RequiredUIAccess
    UpdateOrStatusOptionsDialog createOptionsDialog(
        Project project,
        SequencedMap<UnnamedConfigurable, AbstractVcs> envToConfMap,
        String scopeName
    );

    String getActionName(String scopeName);

    String getActionName();

    String getGroupName(FileGroup fileGroup);

    boolean canGroupByChangelist();

    boolean canChangeFileStatus();
}
