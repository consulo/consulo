/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.ide.actions;

import com.intellij.CommonBundle;
import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.TextTransferable;
import com.intellij.util.ui.UIUtil;
import consulo.application.ui.WholeWestDialogWrapper;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2019-08-11
 */
public class AboutNewDialog extends WholeWestDialogWrapper {
  private static class AboutBuilder {
    private StringBuilder myBuilder = new StringBuilder();

    public void group(String name) {
      myBuilder.append(" ").append(name).append(":\n");
    }

    public void item(String name, String value) {
      myBuilder.append("  ").append(name).append(" = ").append(value).append("\n");
    }

    public String toString() {
      return myBuilder.toString();
    }
  }

  public AboutNewDialog() {
    super(false);
    setTitle("About");
    init();
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    return getClass().getSimpleName() + ".dimensionKey";
  }

  @Override
  public Dimension getDefaultSize() {
    return new Dimension(500, 600);
  }

  @Override
  public float getSplitterDefaultValue() {
    return 0.8f;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Couple<JComponent> createSplitterComponents(JPanel rootPanel) {
    JTextArea area = new JTextArea();
    area.setEditable(false);
    area.setText(buildAboutInfo());

    setOKButtonText(CommonBundle.getCloseButtonText());

    JButton okButton = createJButtonForAction(getOKAction());

    JPanel eastPanel = new JPanel(new VerticalFlowLayout());
    eastPanel.add(okButton);

    JButton copyToClipboard = new JButton("Copy to clipboard");
    copyToClipboard.addActionListener(e -> {
      CopyPasteManager.getInstance().setContents(new TextTransferable(area.getText(), area.getText()));

      copyToClipboard.setEnabled(false);

      JobScheduler.getScheduler().schedule(() -> UIUtil.invokeLaterIfNeeded(() -> copyToClipboard.setEnabled(true)), 2, TimeUnit.SECONDS);
    });

    eastPanel.add(copyToClipboard);

    return Couple.of(ScrollPaneFactory.createScrollPane(area, true), eastPanel);
  }

  @Nonnull
  private String buildAboutInfo() {

    ApplicationInfo info = ApplicationInfo.getInstance();

    AboutBuilder builder = new AboutBuilder();
    builder.group(Application.get().getName().get());
    builder.item("version", info.getFullVersion());
    builder.item("build number", String.valueOf(info.getBuild()));
    builder.item("build date", DateFormatUtil.formatAboutDialogDate(info.getBuildDate().getTime()));

    builder.group("Plugins");

    Map<String, String> plugins = new TreeMap<>();
    for (PluginDescriptor plugin : PluginManager.getPlugins()) {
      plugins.put(plugin.getPluginId().toString(), StringUtil.notNullize(plugin.getVersion(), info.getBuild().toString()));
    }

    for (Map.Entry<String, String> entry : plugins.entrySet()) {
      builder.item(entry.getKey(), entry.getValue());
    }

    Platform platform = Platform.current();

    builder.group("JVM");
    builder.item("vendor", platform.jvm().vendor());
    builder.item("version", platform.jvm().version());
    builder.item("runtimeVersion", platform.jvm().runtimeVersion());
    builder.item("locale", Locale.getDefault().toString());
    builder.group("JVM Env");
    for (Map.Entry<String, String> entry : new TreeMap<>(platform.jvm().getRuntimeProperties()).entrySet()) {
      builder.item(entry.getKey(), StringUtil.escapeCharCharacters(entry.getValue()));
    }

    builder.group("OS");
    builder.item("name", platform.os().name());
    builder.item("version", platform.os().version());
    builder.item("arch", platform.os().arch());

    builder.group("Env");
    for (Map.Entry<String, String> entry : new TreeMap<>(platform.os().environmentVariables()).entrySet()) {
      builder.item(entry.getKey(), StringUtil.escapeCharCharacters(entry.getValue()));
    }

    return StringUtil.trimTrailing(builder.toString());
  }

  @Nullable
  @Override
  protected JComponent createSouthPanel() {
    return null;
  }
}
