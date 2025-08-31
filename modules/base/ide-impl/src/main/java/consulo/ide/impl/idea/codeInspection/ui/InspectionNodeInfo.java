// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInspection.ui;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dmitry Batkovich
 */
public class InspectionNodeInfo extends JPanel {
  //private final static Logger LOG = Logger.getInstance(InspectionNodeInfo.class);
  //
  //public InspectionNodeInfo(@NotNull final InspectionTree tree, @NotNull final Project project) {
  //  setLayout(new GridBagLayout());
  //  setBorder(JBUI.Borders.emptyTop(11));
  //  final InspectionToolWrapper toolWrapper = tree.getSelectedToolWrapper(/*false*/);
  //  LOG.assertTrue(toolWrapper != null);
  //  InspectionProfileImpl currentProfile = (InspectionProfileImpl)InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
  //  boolean enabled = currentProfile.getTools(toolWrapper.getShortName(), project).isEnabled();
  //
  //  JPanel titlePanel = new JPanel();
  //  titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS));
  //  JBLabelDecorator label = JBLabelDecorator.createJBLabelDecorator().setBold(true);
  //  label.setText(toolWrapper.getDisplayName() + " inspection");
  //  titlePanel.add(label);
  //  titlePanel.add(Box.createHorizontalStrut(JBUIScale.scale(16)));
  //  if (!enabled) {
  //    JBLabel enabledLabel = new JBLabel();
  //    enabledLabel.setForeground(JBColor.GRAY);
  //    enabledLabel.setText("Disabled");
  //    titlePanel.add(enabledLabel);
  //  }
  //
  //  add(titlePanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new JBInsets(0, 12, 5, 16), 0, 0));
  //
  //  JEditorPane description = new JEditorPane();
  //  description.setContentType(UIUtil.HTML_MIME);
  //  description.setEditable(false);
  //  description.setOpaque(false);
  //  description.setBackground(UIUtil.getLabelBackground());
  //  description.addHyperlinkListener(SingleInspectionProfilePanel.createSettingsHyperlinkListener(project));
  //  String descriptionText = toolWrapper.loadDescription();
  //  if (descriptionText == null) {
  //    InspectionEP extension = toolWrapper.getExtension();
  //    LOG.error(new PluginException("Inspection #" + toolWrapper.getShortName() + " has no description", extension != null ? extension.getPluginDescriptor().getPluginId() : null));
  //  }
  //  final String toolDescription = stripUIRefsFromInspectionDescription(StringUtil.notNullize(descriptionText));
  //  SingleInspectionProfilePanel.readHTML(description, SingleInspectionProfilePanel.toHTML(description, toolDescription == null ? "" : toolDescription, false));
  //  JScrollPane pane = ScrollPaneFactory.createScrollPane(description, true);
  //  int maxWidth = getFontMetrics(UIUtil.getLabelFont()).charWidth('f') * 110 - pane.getMinimumSize().width;
  //  pane.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
  //  pane.setAlignmentX(0);
  //
  //  add(StatelessCardLayout.wrap(pane), new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new JBInsets(0, 10, 0, 0), 0, 0));
  //
  //  JButton enableButton = null;
  //  if (currentProfile.getSingleTool() != null) {
  //    enableButton = new JButton((enabled ? "Disable" : "Enable") + " inspection");
  //    new ClickListener() {
  //      @Override
  //      public boolean onClick(@NotNull MouseEvent event, int clickCount) {
  //        InspectionProfileImpl.setToolEnabled(!enabled, currentProfile, toolWrapper.getShortName(), project);
  //        tree.getContext().getView().profileChanged();
  //        return true;
  //      }
  //    }.installOn(enableButton);
  //  }
  //
  //  JButton runInspectionOnButton = new JButton(InspectionsBundle.message("run.inspection.on.file.intention.text"));
  //  new ClickListener() {
  //    @Override
  //    public boolean onClick(@NotNull MouseEvent event, int clickCount) {
  //      RunInspectionAction.runInspection(project, toolWrapper.getShortName(), null, null, null);
  //      return true;
  //    }
  //  }.installOn(runInspectionOnButton);
  //
  //  JPanel buttons = new JPanel();
  //  buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
  //  if (enableButton != null) {
  //    buttons.add(enableButton);
  //  }
  //  buttons.add(Box.createHorizontalStrut(JBUIScale.scale(3)));
  //  buttons.add(runInspectionOnButton);
  //
  //  add(buttons, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new JBInsets(15, 9, 9, 0), 0, 0));
  //
  //}

  public static String stripUIRefsFromInspectionDescription(@Nonnull String description) {
    int descriptionEnd = description.indexOf("<!-- tooltip end -->");
    if (descriptionEnd < 0) {
      Pattern pattern = Pattern.compile(".*Use.*(the (panel|checkbox|checkboxes|field|button|controls).*below).*", Pattern.DOTALL);
      Matcher matcher = pattern.matcher(description);
      int startFindIdx = 0;
      while (matcher.find(startFindIdx)) {
        int end = matcher.end(1);
        startFindIdx = end;
        description = description.substring(0, matcher.start(1)) + " inspection settings " + description.substring(end);
      }
    }
    else {
      description = description.substring(0, descriptionEnd);
    }
    return description;
  }
}
