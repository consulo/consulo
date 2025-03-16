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
package consulo.desktop.awt.tipOfDay;

import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.ide.impl.idea.ide.util.TipUIUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.tipOfDay.TipOfDayContributor;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TipPanel extends JPanel implements DialogWrapper.DoNotAskOption {
    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 200;

    private final JEditorPane myBrowser;
    private final JLabel myPoweredByLabel;

    private final List<Pair<LocalizeValue, PluginDescriptor>> myTips = new ArrayList<>();

    public TipPanel() {
        setLayout(new BorderLayout());
        myBrowser = TipUIUtil.createTipBrowser();
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myBrowser, true);
        add(scrollPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());

        myPoweredByLabel = new JBLabel();
        myPoweredByLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        myPoweredByLabel.setForeground(SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES.getFgColor());

        southPanel.add(myPoweredByLabel, BorderLayout.EAST);
        add(southPanel, BorderLayout.SOUTH);

        ExtensionPoint<TipOfDayContributor> contributors = Application.get().getExtensionPoint(TipOfDayContributor.class);

        contributors.forEachExtensionSafe(tipOfDayContributor -> {
            PluginDescriptor plugin = PluginManager.getPlugin(tipOfDayContributor.getClass());
            tipOfDayContributor.contribute(it -> myTips.add(Pair.create(it, plugin)));
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new JBDimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public void prevTip() {
        if (myTips.size() == 0) {
            myBrowser.setText(IdeLocalize.errorTipsNotFound(Application.get().getName().toString()).get());
            return;
        }
        final GeneralSettings settings = GeneralSettings.getInstance();
        int lastTip = settings.getLastTip();

        final Pair<LocalizeValue, PluginDescriptor> tip;
        lastTip--;
        if (lastTip <= 0) {
            tip = myTips.get(myTips.size() - 1);
            lastTip = myTips.size();
        }
        else {
            tip = myTips.get(lastTip - 1);
        }

        setTip(tip, lastTip, myBrowser, settings);
    }

    private void setTip(Pair<LocalizeValue, PluginDescriptor> tip, int lastTip, JEditorPane browser, GeneralSettings settings) {
        TipUIUtil.openTipInBrowserByLocalize(tip, browser);
        myPoweredByLabel.setText(TipUIUtil.getPoweredByTextByLocalize(tip));
        settings.setLastTip(lastTip);
    }

    public void nextTip() {
        if (myTips.size() == 0) {
            myBrowser.setText(IdeLocalize.errorTipsNotFound(Application.get().getName()).get());
            return;
        }
        GeneralSettings settings = GeneralSettings.getInstance();
        int lastTip = settings.getLastTip();
        Pair<LocalizeValue, PluginDescriptor> tip;
        lastTip++;
        if (lastTip - 1 >= myTips.size()) {
            tip = myTips.get(0);
            lastTip = 1;
        }
        else {
            tip = myTips.get(lastTip - 1);
        }

        setTip(tip, lastTip, myBrowser, settings);
    }

    @Override
    public boolean isToBeShown() {
        return !GeneralSettings.getInstance().isShowTipsOnStartup();
    }

    @Override
    public void setToBeShown(boolean toBeShown, int exitCode) {
        GeneralSettings.getInstance().setShowTipsOnStartup(!toBeShown);
    }

    @Override
    public boolean canBeHidden() {
        return true;
    }

    @Override
    public boolean shouldSaveOptionsOnCancel() {
        return true;
    }

    @Nonnull
    @Override
    public LocalizeValue getDoNotShowMessage() {
        return IdeLocalize.checkboxShowTipsOnStartup();
    }
}