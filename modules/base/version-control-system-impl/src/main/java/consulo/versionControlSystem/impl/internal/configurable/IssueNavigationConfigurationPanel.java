/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.application.Application;
import consulo.configurable.Configurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.versionControlSystem.IssueNavigationConfiguration;
import consulo.versionControlSystem.IssueNavigationLink;
import consulo.versionControlSystem.IssueNavigationLinkProvider;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
@SuppressWarnings("ExtensionImplIsNotAnnotated")
public class IssueNavigationConfigurationPanel implements SearchableConfigurable, Configurable.NoScroll, ProjectConfigurable {
    private final Project myProject;

    private @Nullable MutableFlatDataModel<IssueNavigationLink> myModel;
    private @Nullable Table<IssueNavigationLink> myTable;
    private @Nullable Component myComponent;

    public IssueNavigationConfigurationPanel(Project project) {
        myProject = project;
    }

    private class GeneralIssueLinkAction extends DumbAwareAction {
        GeneralIssueLinkAction() {
            super(VcsLocalize.settingsIssueNavigationGeneralPattern(), LocalizeValue.empty());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            IssueLinkConfigurationDialog dialog = new IssueLinkConfigurationDialog(myProject);
            dialog.setTitle(VcsLocalize.issueLinkAddTitle());
            dialog.show();
            if (dialog.isOK()) {
                addLink(dialog.getLink());
            }
        }
    }

    private class ProviderIssueLinkAction extends DumbAwareAction {
        private final IssueNavigationLinkProvider myProvider;

        ProviderIssueLinkAction(IssueNavigationLinkProvider provider) {
            super(provider.getDisplayName(), provider.getDisplayName(), provider.getIcon());
            myProvider = provider;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            // the provider hands the link back off whatever thread it finished on, so the row is added
            // through the project's ui access rather than straight from the callback
            myProvider.ask((JComponent)TargetAWT.to(myComponent)).whenComplete((link, throwable) -> {
                if (link != null) {
                    myProject.getUIAccess().give(() -> addLink(link));
                }
            });
        }
    }

    @RequiredUIAccess
    private void addLink(IssueNavigationLink link) {
        if (myModel != null) {
            myModel.add(link);
        }
    }

    @RequiredUIAccess
    private @Nullable IssueNavigationLink selectedLink() {
        return myTable == null ? null : myTable.getSelectedItem();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable uiDisposable) {
        MutableFlatDataModel<IssueNavigationLink> model = FlatDataModel.of(List.of());
        myModel = model;

        Table<IssueNavigationLink> table = Table.create(model);
        table.addColumn(VcsLocalize.issueLinkIssueColumn(), IssueNavigationLink::getIssueRegexp);
        table.addColumn(VcsLocalize.issueLinkLinkColumn(), IssueNavigationLink::getLinkRegexp);
        myTable = table;

        ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();
        builder.add(new AddActionGroup());
        builder.add(new RemoveAction());
        builder.add(new EditAction());

        ActionToolbar toolbar =
            ActionManager.getInstance().createActionToolbar("IssueNavigationPanel", builder.build(), true);
        toolbar.setTargetUIComponent(table);

        DockLayout tableLayout = DockLayout.create();
        tableLayout.top(toolbar.getUIComponent());
        tableLayout.center(ScrollableLayout.create(table));

        DockLayout root = DockLayout.create();
        root.top(Label.create(VcsLocalize.settingsIssueNavigationDescription(Application.get().getName())));
        root.center(tableLayout);

        myComponent = root;
        return root;
    }

    private class AddActionGroup extends ActionGroup {
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            List<AnAction> list = new ArrayList<>();
            list.add(new GeneralIssueLinkAction());
            list.add(AnSeparator.create());

            myProject.getApplication()
                .getExtensionPoint(IssueNavigationLinkProvider.class)
                .forEachExtensionSafe(provider -> list.add(new ProviderIssueLinkAction(provider)));

            return list.toArray(AnAction.ARRAY_FACTORY);
        }

        @Override
        protected @Nullable Image getTemplateIcon() {
            return PlatformIconGroup.generalAdd();
        }

        @Override
        public boolean isPopup() {
            return true;
        }

        @Override
        public boolean isDumbAware() {
            return true;
        }
    }

    private class RemoveAction extends LegacyDumbAwareAction {
        RemoveAction() {
            super(CommonLocalize.buttonRemove(), LocalizeValue.empty(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            IssueNavigationLink link = selectedLink();
            if (link == null) {
                return;
            }

            Alerts.okCancel()
                .asQuestion()
                .text(VcsLocalize.issueLinkDeletePrompt())
                .title(VcsLocalize.issueLinkDeleteTitle())
                .showAsync(myComponent)
                .whenComplete((confirmed, throwable) -> {
                    if (Boolean.TRUE.equals(confirmed) && myModel != null) {
                        myProject.getUIAccess().give(() -> myModel.remove(link));
                    }
                });
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(selectedLink() != null);
        }
    }

    private class EditAction extends LegacyDumbAwareAction {
        EditAction() {
            super(CommonLocalize.buttonEdit(), LocalizeValue.empty(), PlatformIconGroup.actionsEdit());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            IssueNavigationLink link = selectedLink();
            if (link == null) {
                return;
            }

            IssueLinkConfigurationDialog dialog = new IssueLinkConfigurationDialog(myProject);
            dialog.setTitle(VcsLocalize.issueLinkEditTitle());
            dialog.setLink(link);
            dialog.show();
            if (dialog.isOK()) {
                IssueNavigationLink editedLink = dialog.getLink();
                link.setIssueRegexp(editedLink.getIssueRegexp());
                link.setLinkRegexp(editedLink.getLinkRegexp());

                if (myModel != null) {
                    myModel.update(link);
                }
            }
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(selectedLink() != null);
        }
    }

    private List<IssueNavigationLink> currentLinks() {
        List<IssueNavigationLink> links = new ArrayList<>();
        if (myModel != null) {
            for (IssueNavigationLink link : myModel) {
                links.add(link);
            }
        }
        return links;
    }

    @RequiredUIAccess
    @Override
    public void apply() {
        IssueNavigationConfiguration.getInstance(myProject).setLinks(currentLinks());
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return !currentLinks().equals(IssueNavigationConfiguration.getInstance(myProject).getLinks());
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        if (myModel == null) {
            return;
        }

        // the rows are edited in place, so the page works on copies and the stored links only change on apply
        List<IssueNavigationLink> links = new ArrayList<>();
        for (IssueNavigationLink link : IssueNavigationConfiguration.getInstance(myProject).getLinks()) {
            links.add(new IssueNavigationLink(link.getIssueRegexp(), link.getLinkRegexp()));
        }
        myModel.replaceAll(links);
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Issue Navigation");
    }

    @Override
    public String getId() {
        return "project.propVCSSupport.Issue.Navigation";
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myComponent = null;
        myTable = null;
        myModel = null;
    }
}
