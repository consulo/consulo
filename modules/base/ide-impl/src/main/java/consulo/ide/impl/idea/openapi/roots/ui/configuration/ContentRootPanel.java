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

package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.ide.impl.idea.ui.roots.FilePathClipper;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author Eugene Zhuravlev
 * Date: Jan 19, 2004
 */
public class ContentRootPanel extends JPanel {
    protected final ActionCallback myCallback;
    private final ContentEntry myContentEntry;

    public interface ActionCallback {
        void deleteContentEntry();

        void deleteContentFolder(ContentEntry contentEntry, ContentFolder contentFolder);

        void showChangeOptionsDialog(ContentEntry contentEntry, ContentFolder contentFolder);

        void navigateFolder(ContentEntry contentEntry, ContentFolder contentFolder);
    }

    public ContentRootPanel(ActionCallback callback, ContentEntry contentEntry) {
        super(new GridBagLayout());
        myCallback = callback;
        myContentEntry = contentEntry;
    }

    @Nonnull
    protected ContentEntry getContentEntry() {
        return myContentEntry;
    }

    public void initUI() {
        JComponent header = createHeader();
        this.add(header,
            new GridBagConstraints(0,
                GridBagConstraints.RELATIVE,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                JBUI.insetsBottom(8),
                0,
                0));

        addBottomComponents();

        setSelected(false);
    }

    protected void addBottomComponents() {
    }

    private JComponent createHeader() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(4));

        final JLabel headerLabel = new JLabel(toDisplayPath(getContentEntry().getUrl()));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
        headerLabel.setOpaque(false);
        if (getContentEntry().getFile() == null) {
            headerLabel.setForeground(JBColor.RED);
        }

        DumbAwareAction deleteFolder = new DumbAwareAction(ProjectLocalize.modulePathsRemoveContentTooltip(),
            LocalizeValue.of(),
            PlatformIconGroup.actionsClose()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                myCallback.deleteContentEntry();
            }
        };

        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar("ContentFolderHeader",
            ActionGroup.newImmutableBuilder().add(deleteFolder).build(),
            ActionToolbar.Style.INPLACE);

        toolbar.setTargetComponent(panel);
        JComponent component = toolbar.getComponent();
        component.setOpaque(false);
        component.setBorder(JBUI.Borders.empty());

        panel.add(headerLabel, BorderLayout.CENTER);
        panel.add(component, BorderLayout.EAST);

        FilePathClipper.install(headerLabel, panel);
        return panel;
    }

    protected static String toDisplayPath(final String url) {
        return VirtualFileManager.extractPath(url).replace('/', File.separatorChar);
    }

    public void setSelected(boolean selected) {
    }
}
