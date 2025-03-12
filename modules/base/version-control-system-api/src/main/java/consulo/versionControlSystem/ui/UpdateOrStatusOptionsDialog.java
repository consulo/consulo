/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.ui;

import consulo.application.HelpManager;
import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBTabbedPane;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.OptionsDialog;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsBundle;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public abstract class UpdateOrStatusOptionsDialog extends OptionsDialog {
    private final JComponent myMainPanel;
    private final Map<AbstractVcs, UnnamedConfigurable> myEnvToConfMap = new HashMap<>();
    protected final Project myProject;


    @RequiredUIAccess
    public UpdateOrStatusOptionsDialog(Project project, Map<UnnamedConfigurable, AbstractVcs> confs) {
        super(project);
        setTitle(getRealTitle());
        myProject = project;
        if (confs.size() == 1) {
            myMainPanel = new JPanel(new BorderLayout());
            final UnnamedConfigurable configurable = confs.keySet().iterator().next();
            addComponent(confs.get(configurable), configurable, BorderLayout.CENTER);
            myMainPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
        }
        else {
            myMainPanel = new JBTabbedPane();
            final ArrayList<AbstractVcs> vcses = new ArrayList<>(confs.values());
            Collections.sort(vcses, (o1, o2) -> o1.getDisplayName().compareTo(o2.getDisplayName()));
            Map<AbstractVcs, UnnamedConfigurable> vcsToConfigurable = revertMap(confs);
            for (AbstractVcs vcs : vcses) {
                addComponent(vcs, vcsToConfigurable.get(vcs), vcs.getDisplayName());
            }
        }
        init();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "consulo.versionControlSystem.ui.UpdateOrStatusOptionsDialog" + getActionNameForDimensions();
    }

    private static Map<AbstractVcs, UnnamedConfigurable> revertMap(final Map<UnnamedConfigurable, AbstractVcs> confs) {
        final HashMap<AbstractVcs, UnnamedConfigurable> result = new HashMap<>();
        for (UnnamedConfigurable configurable : confs.keySet()) {
            result.put(confs.get(configurable), configurable);
        }
        return result;
    }

    protected abstract String getRealTitle();

    protected abstract String getActionNameForDimensions();

    @RequiredUIAccess
    private void addComponent(AbstractVcs vcs, UnnamedConfigurable configurable, String constraint) {
        myEnvToConfMap.put(vcs, configurable);
        myMainPanel.add(ConfigurableUIMigrationUtil.createComponent(configurable, getDisposable()), constraint);
        configurable.reset();
    }

    @Override
    protected void doOKAction() {
        for (UnnamedConfigurable configurable : myEnvToConfMap.values()) {
            try {
                configurable.apply();
            }
            catch (ConfigurationException e) {
                Messages.showErrorDialog(myProject, VcsBundle.message("messge.text.cannot.save.settings", e.getLocalizedMessage()), getRealTitle());
                return;
            }
        }
        super.doOKAction();
    }

    @Override
    protected boolean shouldSaveOptionsOnCancel() {
        return false;
    }

    @Override
    protected JComponent createCenterPanel() {
        return myMainPanel;
    }

    @Nullable
    @Override
    protected String getHelpId() {
        for (UnnamedConfigurable conf : myEnvToConfMap.values()) {
            String helpTopic = conf.getHelpTopic();
            if (helpTopic != null) {
                return helpTopic;
            }
        }
        return null;
    }

    @RequiredUIAccess
    @Override
    protected void doHelpAction() {
        String helpTopic = null;
        final Collection<UnnamedConfigurable> v = myEnvToConfMap.values();
        final UnnamedConfigurable[] configurables = v.toArray(new UnnamedConfigurable[v.size()]);
        if (myMainPanel instanceof JTabbedPane) {
            final int tabIndex = ((JTabbedPane) myMainPanel).getSelectedIndex();
            if (tabIndex >= 0 && tabIndex < configurables.length) {
                helpTopic = configurables[tabIndex].getHelpTopic();
            }
        }
        else {
            helpTopic = configurables[0].getHelpTopic();
        }
        if (helpTopic != null) {
            HelpManager.getInstance().invokeHelp(helpTopic);
        }
        else {
            super.doHelpAction();
        }
    }
}
