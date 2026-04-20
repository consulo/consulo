/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.ide.impl.idea.usages.impl;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.usage.TextChunk;
import consulo.usage.UsageGroup;
import consulo.usage.UsageViewSettings;
import consulo.usage.localize.UsageLocalize;
import consulo.util.lang.SystemProperties;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

/**
 * @author max
 */
public class ExporterToTextFile implements consulo.ui.ex.action.ExporterToTextFile {
  private final UsageViewImpl myUsageView;
  
  private final UsageViewSettings myUsageViewSettings;

  public ExporterToTextFile(UsageViewImpl usageView, UsageViewSettings usageViewSettings) {
    myUsageView = usageView;
    myUsageViewSettings = usageViewSettings;
  }

  @Override
  public String getReportText() {
    StringBuilder buf = new StringBuilder();
    appendNode(buf, myUsageView.getModelRoot(), SystemProperties.getLineSeparator(), "");
    return buf.toString();
  }

  private void appendNode(StringBuilder buf, DefaultMutableTreeNode node, String lineSeparator, String indent) {
    buf.append(indent);
    String childIndent;
    if (node.getParent() != null) {
      childIndent = indent + "    ";
      appendNodeText(buf, node, lineSeparator);
    }
    else {
      childIndent = indent;
    }

    Enumeration enumeration = node.children();
    while (enumeration.hasMoreElements()) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)enumeration.nextElement();
      appendNode(buf, child, lineSeparator, childIndent);
    }
  }

  @RequiredUIAccess
  private void appendNodeText(StringBuilder buf, DefaultMutableTreeNode node, String lineSeparator) {
    if (node instanceof Node excludedNode && excludedNode.isExcluded()) {
      buf.append("(").append(UsageLocalize.usageExcluded().get()).append(") ");
    }

    if (node instanceof UsageNode usageNode) {
      appendUsageNodeText(buf, usageNode);
    }
    else if (node instanceof GroupNode groupNode) {
      UsageGroup group = groupNode.getGroup();
      buf.append(group != null ? group.getText(myUsageView) : UsageLocalize.usagesTitle().get());
      buf.append(" ");
      int count = groupNode.getRecursiveUsageCount();
      buf.append(" (").append(UsageLocalize.usagesN(count).get()).append(")");
    }
    else if (node instanceof UsageTargetNode usageTargetNode) {
      buf.append(usageTargetNode.getTarget().getPresentation().getPresentableText());
    }
    else {
      buf.append(node.toString());
    }
    buf.append(lineSeparator);
  }

  protected void appendUsageNodeText(StringBuilder buf, UsageNode node) {
    TextChunk[] chunks = node.getUsage().getPresentation().getText();
    int chunkCount = 0;
    for (TextChunk chunk : chunks) {
      if (chunkCount == 1) buf.append(" "); // add space after line number
      buf.append(chunk.getText());
      ++chunkCount;
    }
  }

  @Override
  public String getDefaultFilePath() {
    return myUsageViewSettings.getExportFileName();
  }

  @Override
  public void exportedTo(String filePath) {
    myUsageViewSettings.setExportFileName(filePath);
  }

  @Override
  public boolean canExport() {
    return !myUsageView.isSearchInProgress() && myUsageView.areTargetsValid();
  }
}
