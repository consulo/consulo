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
package consulo.ide.plugins.whatsNew;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.text.JBDateFormat;
import com.intellij.util.ui.JBHtmlEditorKit;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.repository.history.PluginHistoryEntry;
import consulo.externalService.impl.repository.history.PluginHistoryManager;
import consulo.ide.plugins.PluginIconHolder;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.ide.updateSettings.impl.UpdateHistory;
import consulo.ui.style.StandardColors;
import consulo.ui.util.ColorValueUtil;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 15/11/2021
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

    UiNotifyConnector.doWhenFirstShown(myLoadingPanel, () -> {
      myLoadingPanel.setLoadingText("Fetching change list...");
      myLoadingPanel.startLoading();
      fetchData();
    });

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

    return myLoadingPanel;
  }

  private void fetchData() {
    myLoadingFuture = AppExecutorUtil.getAppExecutorService().submit(() -> {
      List<PluginDescriptor> plugins = PluginManager.getPlugins();

      MultiMap<PluginId, PluginHistoryEntry> entries = MultiMap.createLinked();
      PluginId platformPluginId = PlatformOrPluginUpdateChecker.getPlatformPluginId();
      String platformBuild = ApplicationInfo.getInstance().getBuild().asString();

      PluginHistoryEntry[] pluginHistoryEntries = PluginHistoryManager.fetchHistory(platformPluginId.getIdString(), myUpdateHistory.getHistoryVersion(platformPluginId, platformBuild), platformBuild);

      for (PluginHistoryEntry pluginHistoryEntry : pluginHistoryEntries) {
        entries.putValue(platformPluginId, pluginHistoryEntry);
      }

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

        pluginHistoryEntries = PluginHistoryManager.fetchHistory(plugin.getPluginId().getIdString(), myUpdateHistory.getHistoryVersion(plugin.getPluginId(), version), version);

        for (PluginHistoryEntry pluginHistoryEntry : pluginHistoryEntries) {
          entries.putValue(plugin.getPluginId(), pluginHistoryEntry);
        }
      }

      SwingUtilities.invokeLater(() -> {
        setHtmlFromEntries(entries);

        myLoadingPanel.add(ScrollPaneFactory.createScrollPane(myEditorPanel, true), BorderLayout.CENTER);
        myLoadingPanel.stopLoading();
      });
    });
  }

  private void setHtmlFromEntries(MultiMap<PluginId, PluginHistoryEntry> map) {
    HtmlBuilder html = new HtmlBuilder();

    HtmlChunk.Element body = HtmlChunk.body();
    body = body.style("padding: 0px 15px");
    body = body.child(HtmlChunk.tag("h1").addText(myFile.getName()));

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

        HtmlChunk.Element imgTd = HtmlChunk.tag("td");

        HtmlChunk.Element pluginImg = HtmlChunk.tag("img").attr("src", key.getIdString()).attr("width", PluginIconHolder.ICON_SIZE).attr("height", PluginIconHolder.ICON_SIZE);
        imgTd = imgTd.child(pluginImg);

        HtmlChunk.Element nameTd = HtmlChunk.tag("td").style("padding-left: 10px");

        Font font = UIUtil.getLabelFont(UIUtil.FontSize.BIGGER);

        nameTd = nameTd.child(HtmlChunk.span("font-weight: bold; font-size: " + font.getSize()).addText(pluginName));

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
            String date = JBDateFormat.getDefaultFormatter().formatPrettyDateTime(pluginHistoryEntry.commitTimestamp);

            children.add(HtmlChunk.tag("code").addText("[" + date + "]"));
            children.add(HtmlChunk.nbsp());
          }

          children.add(WhatsNewCommitParser.parse(pluginHistoryEntry.commitMessage));

          if (!StringUtil.isEmptyOrSpaces(pluginHistoryEntry.commitHash)) {
            children.add(HtmlChunk.nbsp());
            String commitShort = StringUtil.first(pluginHistoryEntry.commitHash, 7, false);
            HtmlChunk.Element commitSpan = HtmlChunk.span();
            commitSpan = commitSpan.addText("(commit: ");
            String commitUrl = buildCommitUrl(pluginHistoryEntry.repoUrl, pluginHistoryEntry.commitHash);
            if (commitUrl != null) {
              commitSpan = commitSpan.child(HtmlChunk.tag("a").attr("href", commitUrl).addText(commitShort));
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

        body = body.child(HtmlChunk.hr().attr("size", 1).attr("noshade", "").attr("color", "#" + ColorValueUtil.toHex(StandardColors.LIGHT_GRAY)));

        body = body.child(HtmlChunk.br());
      }
    }
    else {
      body = body.child(HtmlChunk.span().addText("No changes"));
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
