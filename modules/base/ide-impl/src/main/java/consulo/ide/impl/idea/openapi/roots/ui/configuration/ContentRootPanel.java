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

import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.ui.HoverHyperlinkLabel;
import consulo.ide.impl.idea.ui.roots.FilePathClipper;
import consulo.ide.impl.idea.ui.roots.ResizingWrapper;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 * Date: Jan 19, 2004
 */
public abstract class ContentRootPanel extends JPanel {
    protected final ActionCallback myCallback;
    private JComponent myHeader;
    private JComponent myBottom;

    public interface ActionCallback {
        void deleteContentEntry();

        void deleteContentFolder(ContentEntry contentEntry, ContentFolder contentFolder);

        void showChangeOptionsDialog(ContentEntry contentEntry, ContentFolder contentFolder);

        void navigateFolder(ContentEntry contentEntry, ContentFolder contentFolder);
    }

    public ContentRootPanel(ActionCallback callback) {
        super(new GridBagLayout());
        myCallback = callback;
    }

    @Nonnull
    protected abstract ContentEntry getContentEntry();

    public void initUI() {
        myHeader = createHeader();
        this.add(myHeader,
            new GridBagConstraints(0,
                GridBagConstraints.RELATIVE,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 8, 0),
                0,
                0));

        addFolderGroupComponents();

        myBottom = new JPanel(new BorderLayout());
        myBottom.add(Box.createVerticalStrut(3), BorderLayout.NORTH);
        this.add(myBottom,
            new GridBagConstraints(0,
                GridBagConstraints.RELATIVE,
                1,
                1,
                1.0,
                1.0,
                GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(),
                0,
                0));

        setSelected(false);
    }

    protected void addFolderGroupComponents() {
        final ContentFolder[] contentFolders = getContentEntry().getFolders(LanguageContentFolderScopes.all());
        MultiMap<ContentFolderTypeProvider, ContentFolder> folderByType = new MultiMap<>();
        for (ContentFolder folder : contentFolders) {
            if (folder.isSynthetic()) {
                continue;
            }
            final VirtualFile folderFile = folder.getFile();
            if (folderFile != null && isExcludedOrUnderExcludedDirectory(folderFile)) {
                continue;
            }
            folderByType.putValue(folder.getType(), folder);
        }

        Insets insets = JBUI.insetsBottom(10);
        GridBagConstraints constraints = new GridBagConstraints(
            0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.NORTH,
            GridBagConstraints.HORIZONTAL,
            insets,
            0,
            0
        );
        for (Map.Entry<ContentFolderTypeProvider, Collection<ContentFolder>> entry : folderByType.entrySet()) {
            Collection<ContentFolder> folders = entry.getValue();
            if (folders.isEmpty()) {
                continue;
            }

            ContentFolderTypeProvider contentFolderTypeProvider = entry.getKey();

            ContentFolder[] foldersArray = folders.toArray(new ContentFolder[folders.size()]);
            final JComponent sourcesComponent = createFolderGroupComponent(
                contentFolderTypeProvider.getName(),
                foldersArray,
                TargetAWT.to(contentFolderTypeProvider.getGroupColor()),
                contentFolderTypeProvider
            );
            add(sourcesComponent, constraints);
        }
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

    protected JComponent createFolderGroupComponent(
        String title,
        ContentFolder[] folders,
        Color foregroundColor,
        @Nonnull ContentFolderTypeProvider editor
    ) {
        JPanel rowsPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 4, true, false));
        rowsPanel.setBorder(JBUI.Borders.emptyLeft(14));
        rowsPanel.setOpaque(false);

        for (ContentFolder folder : folders) {
            JPanel rowPanel = new JPanel(new BorderLayout());
            rowPanel.setOpaque(false);
            
            rowsPanel.add(rowPanel);

            rowPanel.add(createFolderComponent(folder, foregroundColor), BorderLayout.CENTER);

            ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();
            builder.add(createChangeOptionsAction(folder, editor));
            builder.add(createFolderDeleteAction(folder, editor));

            ActionToolbar toolbar = ActionToolbarFactory.getInstance()
                .createActionToolbar("FolderGroupd", builder.build(), ActionToolbar.Style.INPLACE);
            toolbar.setTargetComponent(rowsPanel);

            JComponent component = toolbar.getComponent();
            component.setOpaque(false);
            component.setBorder(JBUI.Borders.empty());

            rowPanel.add(component, BorderLayout.EAST);
        }

        JLabel titleLabel = new JLabel(title);
        Font labelFont = UIUtil.getLabelFont();
        titleLabel.setFont(labelFont.deriveFont(Font.BOLD));
        titleLabel.setOpaque(false);
        registerTextComponent(titleLabel, foregroundColor);

        final JPanel groupPanel = new JPanel(new BorderLayout());
        groupPanel.setBorder(JBUI.Borders.empty(0, 4));
        groupPanel.setOpaque(false);
        groupPanel.add(titleLabel, BorderLayout.NORTH);
        groupPanel.add(rowsPanel, BorderLayout.CENTER);

        return groupPanel;
    }

    private void registerTextComponent(final JComponent component, final Color foreground) {
        component.setForeground(foreground);
    }

    private JComponent createFolderComponent(final ContentFolder folder, Color foreground) {
        final VirtualFile folderFile = folder.getFile();
        final VirtualFile contentEntryFile = getContentEntry().getFile();
        final String properties = "";
        if (folderFile != null && contentEntryFile != null) {
            String path =
                folderFile.equals(contentEntryFile) ? "." : VfsUtilCore.getRelativePath(folderFile, contentEntryFile, File.separatorChar);
            HoverHyperlinkLabel hyperlinkLabel = new HoverHyperlinkLabel(path + properties, foreground);
            hyperlinkLabel.setMinimumSize(new Dimension(0, 0));
            hyperlinkLabel.addHyperlinkListener(e -> myCallback.navigateFolder(getContentEntry(), folder));
            registerTextComponent(hyperlinkLabel, foreground);
            return new UnderlinedPathLabel(hyperlinkLabel);
        }
        else {
            String path = toRelativeDisplayPath(folder.getUrl(), getContentEntry().getUrl());
            final JLabel pathLabel = new JLabel(path + properties);
            pathLabel.setOpaque(false);
            pathLabel.setForeground(JBColor.RED);

            return new UnderlinedPathLabel(pathLabel);
        }
    }

    private DumbAwareAction createChangeOptionsAction(ContentFolder folder, @Nonnull ContentFolderTypeProvider editor) {
        return new DumbAwareAction(ProjectLocalize.modulePathsPropertiesTooltip(), LocalizeValue.of(), PlatformIconGroup.generalInline_edit()) {

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                myCallback.showChangeOptionsDialog(getContentEntry(), folder);
            }
        };
    }

    private DumbAwareAction createFolderDeleteAction(final ContentFolder folder, @Nonnull ContentFolderTypeProvider editor) {
        final LocalizeValue tooltipText;
        if (folder.getFile() != null && getContentEntry().getFile() != null) {
            tooltipText = ProjectLocalize.modulePathsUnmark0Tooltip(editor.getName());
        }
        else {
            tooltipText = ProjectLocalize.modulePathsRemoveTooltip();
        }
        return new DumbAwareAction(tooltipText, LocalizeValue.of(), PlatformIconGroup.actionsClose()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                myCallback.deleteContentFolder(getContentEntry(), folder);
            }
        };
    }

    public boolean isExcludedOrUnderExcludedDirectory(final VirtualFile file) {
        final ContentEntry contentEntry = getContentEntry();
        for (VirtualFile excludedDir : contentEntry.getFolderFiles(LanguageContentFolderScopes.of(ExcludedContentFolderTypeProvider.getInstance()))) {
            if (VfsUtilCore.isAncestor(excludedDir, file, false)) {
                return true;
            }
        }
        return false;
    }

    protected static String toRelativeDisplayPath(String url, String ancestorUrl) {
        if (!StringUtil.endsWithChar(ancestorUrl, '/')) {
            ancestorUrl += "/";
        }
        if (url.startsWith(ancestorUrl)) {
            return url.substring(ancestorUrl.length()).replace('/', File.separatorChar);
        }
        return toDisplayPath(url);
    }

    private static String toDisplayPath(final String url) {
        return VirtualFileManager.extractPath(url).replace('/', File.separatorChar);
    }


    public void setSelected(boolean selected) {
    }

    private static class UnderlinedPathLabel extends ResizingWrapper {
        public UnderlinedPathLabel(JLabel wrappedComponent) {
            super(wrappedComponent);
            FilePathClipper.install(wrappedComponent, this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final int startX = myWrappedComponent.getWidth();
            final int endX = getWidth();
            if (endX > startX) {
                final FontMetrics fontMetrics = myWrappedComponent.getFontMetrics(myWrappedComponent.getFont());
                final int y = fontMetrics.getMaxAscent();
                final Color savedColor = g.getColor();
                g.setColor(JBColor.border());
                UIUtil.drawLine((Graphics2D) g, startX, y, endX, y);
                g.setColor(savedColor);
            }
        }
    }
}
