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
package consulo.externalService.impl.internal.whatsNew;

import consulo.application.Application;
import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.application.util.JBDateFormat;
import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.PluginIconHolder;
import consulo.externalService.impl.internal.pluginHistory.UpdateHistory;
import consulo.externalService.impl.internal.repository.api.pluginHistory.PluginHistoryEntry;
import consulo.externalService.impl.internal.repository.api.pluginHistory.PluginHistoryManager;
import consulo.externalService.impl.internal.repository.api.pluginHistory.PluginHistoryRequest;
import consulo.externalService.impl.internal.repository.api.pluginHistory.PluginHistoryResponse;
import consulo.externalService.impl.internal.update.PlatformOrPluginUpdateChecker;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.style.StandardColors;
import consulo.ui.util.ColorValueUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 2021-11-15
 */
public class WhatsNewVirtualFileEditor extends ConfigurationFileEditor {
    private static final Logger LOG = Logger.getInstance(WhatsNewVirtualFileEditor.class);

    private final UpdateHistory myUpdateHistory;

    private HtmlView myHtmlView;
    private Component myComponent;

    private Future<?> myLoadingFuture = CompletableFuture.completedFuture(null);

    public WhatsNewVirtualFileEditor(Project project, UpdateHistory updateHistory, VirtualFile file) {
        super(project, file);
        myUpdateHistory = updateHistory;
    }

    /**
     * Built once and kept. The platform asks an editor for its component again whenever the selection changes,
     * and a fresh one per call would leave the tab holding a layout nobody is filling any more - the view is
     * moved into whichever layout the fetch that finishes last was started for.
     */
    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        if (myComponent != null) {
            return myComponent;
        }

        myHtmlView = HtmlView.create();
        // an img of the document names a plugin, and only this editor knows that a plugin id stands for the
        // icon of that plugin rather than for something the view could fetch
        myHtmlView.setImageResolver(src -> {
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

        LoadingLayout<DockLayout> loadingLayout = LoadingLayout.create(DockLayout.create(), this);
        loadingLayout.setLoadingText(ExternalServiceLocalize.whatsnewLoadingText());

        myLoadingFuture = loadingLayout.startLoading(this::fetchHistorySafe, (layout, entries) -> {
            // the view goes in whatever the html turns out to be - an editor which drops out of here before it
            // adds anything leaves a tab with nothing in it and no way to tell why
            layout.center(myHtmlView);

            String html;
            try {
                html = buildHtml(entries);
            }
            catch (Exception e) {
                LOG.error("Failed to build the what's new page", e);
                return;
            }

            myHtmlView.render(new HtmlView.RenderData(html));
        });

        myComponent = loadingLayout;
        return loadingLayout;
    }

    /**
     * A repository which is unreachable leaves the page saying there is nothing new rather than leaving the
     * spinner up - the loading layout only ever stops on a value, and a throw out of here would be swallowed by
     * the future nobody waits on and hold the editor empty for good.
     */
    private MultiMap<PluginId, PluginHistoryEntry> fetchHistorySafe() {
        try {
            return fetchHistory();
        }
        catch (Exception e) {
            LOG.warn("Failed to fetch the plugin history of the what's new page", e);
            return MultiMap.createLinked();
        }
    }

    private MultiMap<PluginId, PluginHistoryEntry> fetchHistory() {
        List<PluginDescriptor> plugins = PluginManager.getPlugins();

        MultiMap<PluginId, PluginHistoryEntry> entries = MultiMap.createLinked();
        PluginId platformPluginId = PlatformOrPluginUpdateChecker.getPlatformPluginId();
        String platformBuild = Application.get().getBuildNumber().asString();

        List<PluginHistoryRequest.PluginInfo> infos = new ArrayList<>(plugins.size() + 1);

        addPlugin(infos, platformPluginId, platformBuild);
        for (PluginDescriptor plugin : plugins) {
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
            entries.putValue(PluginId.getId(entry.id), entry);
        }

        return entries;
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

    private String buildHtml(MultiMap<PluginId, PluginHistoryEntry> map) {
        HtmlBuilder html = new HtmlBuilder();

        HtmlChunk.Element body = HtmlChunk.body();
        body = body.style("padding: 0px 15px");
        body = body.child(HtmlChunk.tag("h1").addText(myVirtualFile.getName()));

        if (map != null && !map.isEmpty()) {
            for (Map.Entry<PluginId, Collection<PluginHistoryEntry>> entry : map.entrySet()) {
                PluginId key = entry.getKey();

                Set<PluginHistoryEntry> entries =
                    new TreeSet<>((o1, o2) -> Long.compareUnsigned(o1.commitTimestamp, o2.commitTimestamp));
                entries.addAll(entry.getValue());

                String pluginName;
                String pluginVersion;
                if (PlatformOrPluginUpdateChecker.isPlatform(key)) {
                    pluginName = ExternalServiceLocalize.whatsnewPlatformText().get();
                    pluginVersion = myProject.getApplication().getBuildNumber().asString();
                }
                else {
                    // the repository answers for what was asked, and a plugin can be gone by the time the
                    // answer arrives - it was worth an assert while this ran on a swing editor which swallowed
                    // it, but here the throw would cost the whole page
                    PluginDescriptor plugin = PluginManager.findPlugin(key);
                    if (plugin == null) {
                        continue;
                    }
                    pluginName = plugin.getName();
                    pluginVersion = plugin.getVersion();
                }

                HtmlChunk.Element imgTd = HtmlChunk.tag("td");

                HtmlChunk.Element pluginImg = HtmlChunk.tag("img")
                    .attr("src", HtmlView.IMAGE_SRC_PREFIX + key.getIdString())
                    .attr("width", PluginIconHolder.ICON_SIZE)
                    .attr("height", PluginIconHolder.ICON_SIZE);
                imgTd = imgTd.child(pluginImg);

                HtmlChunk.Element nameTd = HtmlChunk.tag("td").style("padding-left: 10px");

                // relative rather than a size in points off the label font - the document is laid out by
                // whichever renderer the frontend brings, and only it knows what it is scaling against
                nameTd = nameTd.child(HtmlChunk.span("font-weight: bold; font-size: 120%").addText(pluginName));

                StringBuilder versionHistorySpan = new StringBuilder();
                String historyVersion = myUpdateHistory.getHistoryVersion(key, StringUtil.notNullize(pluginVersion, "N/A"));
                if (!historyVersion.equals(pluginVersion)) {
                    versionHistorySpan.append("#");
                    versionHistorySpan.append(historyVersion);
                    versionHistorySpan.append(" ");
                    versionHistorySpan.append('\u2192');
                    versionHistorySpan.append(" ");
                }

                versionHistorySpan.append("#");
                versionHistorySpan.append(pluginVersion);

                nameTd = nameTd.child(HtmlChunk.br()).child(HtmlChunk.tag("code").addText(versionHistorySpan.toString()));

                HtmlChunk.Element tr = HtmlChunk.tag("tr");
                tr = tr.children(imgTd, nameTd);

                body = body.child(HtmlChunk.tag("table").child(tr));

                HtmlChunk.Element ul = HtmlChunk.ul();

                for (PluginHistoryEntry pluginHistoryEntry : entries) {
                    List<HtmlChunk> children = new ArrayList<>();

                    children.add(HtmlChunk.tag("code").addText("#").addText(pluginHistoryEntry.pluginVersion));
                    children.add(HtmlChunk.nbsp());

                    if (pluginHistoryEntry.commitTimestamp != 0) {
                        String date = JBDateFormat.getFormatter().formatPrettyDateTime(pluginHistoryEntry.commitTimestamp);

                        children.add(HtmlChunk.tag("code").addText("[" + date + "]"));
                        children.add(HtmlChunk.nbsp());
                    }

                    children.add(WhatsNewCommitParser.parse(pluginHistoryEntry.commitMessage));

                    if (!StringUtil.isEmptyOrSpaces(pluginHistoryEntry.commitHash)) {
                        children.add(HtmlChunk.nbsp());
                        String commitShort = StringUtil.first(pluginHistoryEntry.commitHash, 7, false);
                        HtmlChunk.Element commitSpan = HtmlChunk.span();
                        commitSpan = commitSpan.addText(LocalizeValue.join(
                            LocalizeValue.of('('),
                            ExternalServiceLocalize.whatsnewCommitLabel(),
                            LocalizeValue.space()
                        ));
                        String commitUrl = buildCommitUrl(pluginHistoryEntry.repoUrl, pluginHistoryEntry.commitHash);
                        if (commitUrl != null) {
                            // a commit is read outside of the ide - without this the browser of the web frontend
                            // would follow the link in the tab the ide itself is running in
                            commitSpan = commitSpan.child(HtmlChunk.tag("a")
                                .attr("href", commitUrl)
                                .attr("target", "_blank")
                                .addText(commitShort));
                        }
                        else {
                            commitSpan = commitSpan.addText(commitShort);
                        }
                        commitSpan = commitSpan.addText(")");

                        children.add(commitSpan);
                    }
                    ul = ul.child(HtmlChunk.li().children(children));
                }

                body = body.child(ul);

                body = body.child(
                    HtmlChunk.hr()
                        .attr("size", 1)
                        .attr("noshade", "")
                        .attr("color", "#" + ColorValueUtil.toHex(StandardColors.LIGHT_GRAY))
                );

                body = body.child(HtmlChunk.br());
            }
        }
        else {
            body = body.child(HtmlChunk.span().addText(ExternalServiceLocalize.whatsnewNoChangesText()));
        }

        html.append(body);

        // a head of its own, empty - the view puts the colours of the theme and the stylesheets it was handed
        // in there, and a document without one gets none of that
        return html.wrapWith("html").toString().replace("<html>", "<html><head></head>");
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

    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        return myHtmlView;
    }

    @Override
    public void dispose() {
        myLoadingFuture.cancel(false);
    }
}
