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
package consulo.externalService.impl.internal.statistic;

import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.util.DateFormatUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.externalService.internal.ExternalServiceHelper;
import consulo.externalService.statistic.*;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.TableViewSpeedSearch;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

public class ShowFeatureUsageStatisticsDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(ShowFeatureUsageStatisticsDialog.class);
    private static final Comparator<FeatureDescriptor>
        DISPLAY_NAME_COMPARATOR = (fd1, fd2) -> fd1.getDisplayName().compareTo(fd2.getDisplayName()),
        GROUP_NAME_COMPARATOR = (fd1, fd2) -> getGroupName(fd1).compareTo(getGroupName(fd2)),
        USAGE_COUNT_COMPARATOR = (fd1, fd2) -> fd1.getUsageCount() - fd2.getUsageCount(),
        LAST_USED_COMPARATOR = (fd1, fd2) -> new Date(fd2.getLastTimeUsed()).compareTo(new Date(fd1.getLastTimeUsed()));

    private static final ColumnInfo<FeatureDescriptor, String> DISPLAY_NAME = new ColumnInfo<>(FeatureStatisticsBundle.message("feature.statistics.column.feature")) {
        @Override
        public String valueOf(FeatureDescriptor featureDescriptor) {
            return featureDescriptor.getDisplayName();
        }

        @Override
        public Comparator<FeatureDescriptor> getComparator() {
            return DISPLAY_NAME_COMPARATOR;
        }
    };
    private static final ColumnInfo<FeatureDescriptor, String> GROUP_NAME = new ColumnInfo<>(FeatureStatisticsBundle.message("feature.statistics.column.group")) {
        @Override
        public String valueOf(FeatureDescriptor featureDescriptor) {
            return getGroupName(featureDescriptor);
        }

        @Override
        public Comparator<FeatureDescriptor> getComparator() {
            return GROUP_NAME_COMPARATOR;
        }
    };
    private static final ColumnInfo<FeatureDescriptor, String> USED_TOTAL = new ColumnInfo<>(FeatureStatisticsBundle.message("feature.statistics.column.usage.count")) {
        @Override
        public String valueOf(FeatureDescriptor featureDescriptor) {
            int count = featureDescriptor.getUsageCount();
            return FeatureStatisticsBundle.message("feature.statistics.usage.count", count);
        }

        @Override
        public Comparator<FeatureDescriptor> getComparator() {
            return USAGE_COUNT_COMPARATOR;
        }
    };
    private static final ColumnInfo<FeatureDescriptor, String> LAST_USED = new ColumnInfo<>(FeatureStatisticsBundle.message("feature.statistics.column.last.used")) {
        @Override
        public String valueOf(FeatureDescriptor featureDescriptor) {
            long tm = featureDescriptor.getLastTimeUsed();
            if (tm <= 0) {
                return FeatureStatisticsBundle.message("feature.statistics.not.applicable");
            }
            return DateFormatUtil.formatBetweenDates(tm, System.currentTimeMillis());
        }

        @Override
        public Comparator<FeatureDescriptor> getComparator() {
            return LAST_USED_COMPARATOR;
        }
    };

    private static final ColumnInfo[] COLUMNS = new ColumnInfo[]{DISPLAY_NAME, GROUP_NAME, USED_TOTAL, LAST_USED};

    public ShowFeatureUsageStatisticsDialog(Project project) {
        super(project, true);
        setTitle(FeatureStatisticsBundle.message("feature.statistics.dialog.title"));
        setCancelButtonText(CommonLocalize.buttonClose().get());
        setModal(false);
        init();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#consulo.externalService.impl.internal.statistic.ShowFeatureUsageStatisticsDialog";
    }

    @Override
    @Nonnull
    protected Action[] createActions() {
        return new Action[]{getCancelAction(), getHelpAction()};
    }

    @Override
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp("editing.productivityGuide");
    }

    @Override
    protected JComponent createCenterPanel() {
        Splitter splitter = new Splitter(true);
        splitter.setShowDividerControls(true);

        ProductivityFeaturesRegistry registry = ProductivityFeaturesRegistry.getInstance();
        ArrayList<FeatureDescriptor> features = new ArrayList<>();
        for (String id : registry.getFeatureIds()) {
            features.add(registry.getFeatureDescriptor(id));
        }
        final TableView<FeatureDescriptor> table = new TableView<>(new ListTableModel<>(COLUMNS, features, 0));

        TableViewSpeedSearch.register(table, FeatureDescriptor::getDisplayName);

        JPanel controlsPanel = new JPanel(new VerticalFlowLayout());

        Application app = Application.get();
        long uptime = System.currentTimeMillis() - app.getStartTime();
        long idleTime = app.getIdleTime();

        final String uptimeS = FeatureStatisticsBundle.message("feature.statistics.application.uptime", Application.get().getName(), DateFormatUtil.formatDuration(uptime));

        final String idleTimeS = FeatureStatisticsBundle.message("feature.statistics.application.idle.time", DateFormatUtil.formatDuration(idleTime));

        String labelText = uptimeS + ", " + idleTimeS;
        CompletionStatistics stats = ((FeatureUsageTrackerImpl) FeatureUsageTracker.getInstance()).getCompletionStatistics();
        if (stats.dayCount > 0 && stats.sparedCharacters > 0) {
            String total = formatCharacterCount(stats.sparedCharacters, true);
            String perDay = formatCharacterCount(stats.sparedCharacters / stats.dayCount, false);
            labelText += "<br>Code completion has saved you from typing at least " + total +
                " since " + DateFormatUtil.formatDate(stats.startDate) +
                " (~" + perDay + " per working day)";
        }

        CumulativeStatistics fstats = ((FeatureUsageTrackerImpl) FeatureUsageTracker.getInstance()).getFixesStats();
        if (fstats.dayCount > 0 && fstats.invocations > 0) {
            labelText += "<br>Quick fixes have saved you from " + fstats.invocations +
                " possible bugs since " + DateFormatUtil.formatDate(fstats.startDate) +
                " (~" + fstats.invocations / fstats.dayCount + " per working day)";
        }

        controlsPanel.add(new JLabel("<html><body>" + labelText + "</body></html>"), BorderLayout.NORTH);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controlsPanel, BorderLayout.NORTH);
        topPanel.add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER);

        splitter.setFirstComponent(topPanel);

        final JEditorPane browser = new JEditorPane(UIUtil.HTML_MIME, "");
        browser.setEditable(false);
        splitter.setSecondComponent(ScrollPaneFactory.createScrollPane(browser));

        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            Collection selection = table.getSelection();
            try {
                if (selection.isEmpty()) {
                    browser.read(new StringReader(""), null);
                }
                else {
                    FeatureDescriptor feature = (FeatureDescriptor) selection.iterator().next();
                    String tipFileName = feature.getTipFileName();
                    PluginDescriptor pluginDescriptor;
                    Class<? extends ProductivityFeaturesProvider> provider = feature.getProvider();
                    if (provider != null) {
                        pluginDescriptor = PluginManager.getPlugin(provider);
                    }
                    else {
                        pluginDescriptor = PluginManager.getPlugin(Application.class);
                    }

                    ExternalServiceHelper helper = Application.get().getInstance(ExternalServiceHelper.class);
                    helper.openTipInBrowser(Pair.create("/tips/" + tipFileName, pluginDescriptor), browser);
                }
            }
            catch (IOException ex) {
                LOG.info(ex);
            }
        });

        return splitter;
    }

    private static String formatCharacterCount(int count, boolean full) {
        DecimalFormat oneDigit = new DecimalFormat("0.0");
        String result = count > 1024 * 1024 ? oneDigit.format((double) count / 1024 / 1024) + "M" : count > 1024 ? oneDigit.format((double) count / 1024) + "K" : String.valueOf(count);
        if (full) {
            return result + " characters";
        }
        return result;
    }

    private static String getGroupName(FeatureDescriptor featureDescriptor) {
        final ProductivityFeaturesRegistry registry = ProductivityFeaturesRegistry.getInstance();
        final GroupDescriptor groupDescriptor = registry.getGroupDescriptor(featureDescriptor.getGroupId());
        return groupDescriptor != null ? groupDescriptor.getDisplayName() : "";
    }
}
