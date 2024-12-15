/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.plugins.whatsNew;

import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.application.util.Html;
import consulo.application.util.HtmlBuilder;
import consulo.application.util.JBDateFormat;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.fileEditor.FileEditor;
import consulo.ide.impl.externalService.impl.repository.history.PluginHistoryEntry;
import consulo.ide.impl.externalService.impl.repository.history.PluginHistoryManager;
import consulo.ide.impl.externalService.impl.repository.history.PluginHistoryRequest;
import consulo.ide.impl.externalService.impl.repository.history.PluginHistoryResponse;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.ui.components.JBLoadingPanel;
import consulo.ide.impl.plugins.PluginIconHolder;
import consulo.ide.impl.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.ide.impl.updateSettings.impl.UpdateHistory;
import consulo.project.Project;
import consulo.ui.ex.awt.BrowserHyperlinkListener;
import consulo.ui.ex.awt.JBHtmlEditorKit;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.style.StandardColors;
import consulo.ui.util.ColorValueUtil;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import kava.beans.PropertyChangeListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 2021-11-15
 */
public class WhatsNewVirtualFileEditor extends UserDataHolderBase implements FileEditor {
    private final UpdateHistory myUpdateHistory;
    private final VirtualFile myFile;
    private JEditorPane myEditorPanel;

    private JBLoadingPanel myLoadingPanel;

    private Future<?> myLoadingFuture = CompletableFuture.completedFuture(null);

    public WhatsNewVirtualFileEditor(Project project, UpdateHistory updateHistory, VirtualFile file) {
        myUpdateHistory = updateHistory;
        myFile = file;
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        myLoadingPanel = new JBLoadingPanel(new BorderLayout(), this);

        myEditorPanel = new JEditorPane("text/html", "");
        myEditorPanel.setFont(EditorUtil.getEditorFont());
        JBHtmlEditorKit kit = JBHtmlEditorKit.create();
        kit.setImageResolver(src -> {
            PluginId pluginId = PluginId.getId(src);
            if (PlatformOrPluginUpdateChecker.isPlatform(pluginId)) {
                return PluginIconHolder.decorateIcon(Application.get().getBigIcon());
            }

            PluginDescriptor plugin = PluginManager.findPlugin(pluginId);
            if (plugin != null) {
                return PluginIconHolder.get(plugin);
            }

            return null;
        });
        myEditorPanel.setEditorKit(kit);
        myEditorPanel.setEditable(false);
        myEditorPanel.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);

        myLoadingPanel.add(ScrollPaneFactory.createScrollPane(myEditorPanel, true), BorderLayout.CENTER);

        UiNotifyConnector.doWhenFirstShown(myLoadingPanel, () -> {
            myLoadingPanel.setLoadingText("Fetching change list...");
            myLoadingPanel.startLoading();
            fetchData();
        });

        return myLoadingPanel;
    }

    private void fetchData() {
        myLoadingFuture = AppExecutorUtil.getAppExecutorService().submit(() -> {
            List<PluginDescriptor> plugins = PluginManager.getPlugins();

            MultiMap<PluginId, PluginHistoryEntry> entries = MultiMap.createLinked();
            PluginId platformPluginId = PlatformOrPluginUpdateChecker.getPlatformPluginId();
            String platformBuild = ApplicationInfo.getInstance().getBuild().asString();

            List<PluginHistoryRequest.PluginInfo> infos = new ArrayList<>(plugins.size() + 1);

            addPlugin(infos, platformPluginId, platformBuild);
            for (PluginDescriptor plugin : plugins) {
                if (myLoadingFuture.isCancelled()) {
                    return;
                }

                if (PluginIds.isPlatformPlugin(plugin.getPluginId())) {
                    continue;
                }

                String version = plugin.getVersion();
                if (version == null || "SNAPSHOT".equals(version)) {
                    continue;
                }

                addPlugin(infos, plugin.getPluginId(), version);
            }


            PluginHistoryResponse response = PluginHistoryManager.fetchBatchHistory(new PluginHistoryRequest(infos));

            for (PluginHistoryResponse.PluginHistory entry : response.entries) {
                if (myLoadingFuture.isCancelled()) {
                    return;
                }

                entries.putValue(PluginId.getId(entry.id), entry);
            }

            SwingUtilities.invokeLater(() -> {
                setHtmlFromEntries(entries);

                myLoadingPanel.invalidate();

                myLoadingPanel.stopLoading();
            });
        });
    }

    private void addPlugin(List<PluginHistoryRequest.PluginInfo> result, PluginId pluginId, String version) {
        String oldVersion = myUpdateHistory.getHistoryVersion(pluginId, version);

        if (Objects.equals(oldVersion, version)) {
            result.add(new PluginHistoryRequest.PluginInfo(pluginId.getIdString(), version));
        }
        else {
            result.add(new PluginHistoryRequest.PluginInfo(pluginId.getIdString(), oldVersion, version, true));
        }
    }

    private void setHtmlFromEntries(MultiMap<PluginId, PluginHistoryEntry> map) {
        HtmlBuilder html = new HtmlBuilder();

        Html.Element body = Html.body();
        body = body.style("padding: 0px 15px");
        body = body.child(Html.tag("h1").addText(myFile.getName()));

        if (map != null && !map.isEmpty()) {
            for (Map.Entry<PluginId, Collection<PluginHistoryEntry>> entry : map.entrySet()) {
                PluginId key = entry.getKey();

                Set<PluginHistoryEntry> entries = new TreeSet<>((o1, o2) -> Long.compareUnsigned(o1.commitTimestamp, o2.commitTimestamp));
                entries.addAll(entry.getValue());

                String pluginName;
                String pluginVersion;
                if (PlatformOrPluginUpdateChecker.isPlatform(key)) {
                    pluginName = "Platform";
                    pluginVersion = ApplicationInfo.getInstance().getBuild().asString();
                }
                else {
                    PluginDescriptor plugin = PluginManager.findPlugin(key);
                    assert plugin != null;
                    pluginName = plugin.getName();
                    pluginVersion = plugin.getVersion();
                }

                Html.Element imgTd = Html.tag("td");

                Html.Element pluginImg = Html.tag("img")
                    .attr("src", key.getIdString())
                    .attr("width", PluginIconHolder.ICON_SIZE)
                    .attr("height", PluginIconHolder.ICON_SIZE);
                imgTd = imgTd.child(pluginImg);

                Html.Element nameTd = Html.tag("td").style("padding-left: 10px");

                Font font = UIUtil.getLabelFont(UIUtil.FontSize.BIGGER);

                nameTd = nameTd.child(Html.span("font-weight: bold; font-size: " + font.getSize()).addText(pluginName));

                StringBuilder versionHistorySpan = new StringBuilder();
                String historyVersion = myUpdateHistory.getHistoryVersion(key, pluginVersion);
                if (!historyVersion.equals(pluginVersion)) {
                    versionHistorySpan.append("#");
                    versionHistorySpan.append(historyVersion);
                    versionHistorySpan.append(" ");
                    versionHistorySpan.append(UIUtil.rightArrow());
                    versionHistorySpan.append(" ");
                }

                versionHistorySpan.append("#");
                versionHistorySpan.append(pluginVersion);

                nameTd = nameTd.child(Html.br()).child(Html.tag("code").addText(versionHistorySpan.toString()));

                Html.Element tr = Html.tag("tr");
                tr = tr.children(imgTd, nameTd);

                body = body.child(Html.tag("table").child(tr));

                Html.Element ul = Html.ul();

                for (PluginHistoryEntry pluginHistoryEntry : entries) {
                    List<Html.Chunk> children = new ArrayList<>();

                    children.add(Html.tag("code").addText("#").addText(pluginHistoryEntry.pluginVersion));
                    children.add(Html.nbsp());

                    if (pluginHistoryEntry.commitTimestamp != 0) {
                        String date = JBDateFormat.getFormatter().formatPrettyDateTime(pluginHistoryEntry.commitTimestamp);

                        children.add(Html.tag("code").addText("[" + date + "]"));
                        children.add(Html.nbsp());
                    }

                    children.add(WhatsNewCommitParser.parse(pluginHistoryEntry.commitMessage));

                    if (!StringUtil.isEmptyOrSpaces(pluginHistoryEntry.commitHash)) {
                        children.add(Html.nbsp());
                        String commitShort = StringUtil.first(pluginHistoryEntry.commitHash, 7, false);
                        Html.Element commitSpan = Html.span();
                        commitSpan = commitSpan.addText("(commit: ");
                        String commitUrl = buildCommitUrl(pluginHistoryEntry.repoUrl, pluginHistoryEntry.commitHash);
                        if (commitUrl != null) {
                            commitSpan = commitSpan.child(Html.tag("a").attr("href", commitUrl).addText(commitShort));
                        }
                        else {
                            commitSpan = commitSpan.addText(commitShort);
                        }
                        commitSpan = commitSpan.addText(")");

                        children.add(commitSpan);
                    }
                    ul = ul.child(Html.li().children(children));
                }

                body = body.child(ul);

                body = body.child(Html.hr()
                    .attr("size", 1)
                    .attr("noshade", "")
                    .attr("color", "#" + ColorValueUtil.toHex(StandardColors.LIGHT_GRAY)));

                body = body.child(Html.br());
            }
        }
        else {
            body = body.child(Html.span().addText("No changes"));
        }

        html.append(body);

        myEditorPanel.setText(html.wrapWith("html").toString());
        myEditorPanel.setCaretPosition(0);
    }

    private static String buildCommitUrl(String url, String commitHash) {
        if (StringUtil.isEmptyOrSpaces(url) || StringUtil.isEmptyOrSpaces(commitHash)) {
            return null;
        }

        if (url.startsWith("https://github.com")) {
            StringBuilder builder = new StringBuilder();
            builder.append(url);
            if (!url.endsWith("/")) {
                builder.append("/");
            }
            builder.append("commit/");
            builder.append(commitHash);
            return builder.toString();
        }

        return null;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myEditorPanel;
    }

    @Nonnull
    @Override
    public String getName() {
        return myFile.getName();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void selectNotify() {

    }

    @Override
    public void deselectNotify() {

    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {

    }

    @Override
    public void dispose() {
        myLoadingFuture.cancel(false);
    }
}
