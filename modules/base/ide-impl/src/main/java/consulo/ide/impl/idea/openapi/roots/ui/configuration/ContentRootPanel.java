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
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author Eugene Zhuravlev
 * @since 2004-01-19
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
    }

    protected void addBottomComponents() {
    }

    @RequiredUIAccess
    private JComponent createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(4));

        JLabel headerLabel = new JLabel(toDisplayPath(getContentEntry().getUrl()));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
        headerLabel.setOpaque(false);
        if (getContentEntry().getFile() == null) {
            headerLabel.setForeground(JBColor.RED);
        }

        Button button = Button.create(LocalizeValue.of());
        button.setToolTipText(ProjectLocalize.modulePathsRemoveContentTooltip());
        button.addStyle(ButtonStyle.INPLACE);
        button.addClickListener(event -> myCallback.deleteContentEntry());
        button.setIcon(PlatformIconGroup.actionsClose());

        panel.add(headerLabel, BorderLayout.CENTER);
        panel.add(TargetAWT.to(button), BorderLayout.EAST);

        FilePathClipper.install(headerLabel, panel);
        return panel;
    }

    protected static String toDisplayPath(String url) {
        return VirtualFileManager.extractPath(url).replace('/', File.separatorChar);
    }

    public void setSelected(boolean selected) {
    }
}
