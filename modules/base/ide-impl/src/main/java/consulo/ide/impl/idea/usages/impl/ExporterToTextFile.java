/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.ide.impl.idea.usages.impl;

import consulo.usage.UsageViewBundle;
import consulo.usage.TextChunk;
import consulo.usage.UsageGroup;
import consulo.usage.UsageViewSettings;
import consulo.util.lang.SystemProperties;
import jakarta.annotation.Nonnull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

/**
 * @author max
 */
public class ExporterToTextFile implements consulo.ui.ex.action.ExporterToTextFile {
  private final UsageViewImpl myUsageView;
  @Nonnull
  private final UsageViewSettings myUsageViewSettings;

  public ExporterToTextFile(@Nonnull UsageViewImpl usageView, @Nonnull UsageViewSettings usageViewSettings) {
    myUsageView = usageView;
    myUsageViewSettings = usageViewSettings;
  }

  @Nonnull
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

  private void appendNodeText(StringBuilder buf, DefaultMutableTreeNode node, String lineSeparator) {
    if (node instanceof Node && ((Node)node).isExcluded()) {
      buf.append("(").append(UsageViewBundle.message("usage.excluded")).append(") ");
    }

    if (node instanceof UsageNode) {
      appendUsageNodeText(buf, (UsageNode)node);
    }
    else if (node instanceof GroupNode) {
      UsageGroup group = ((GroupNode)node).getGroup();
      buf.append(group != null ? group.getText(myUsageView) : UsageViewBundle.message("usages.title"));
      buf.append(" ");
      int count = ((GroupNode)node).getRecursiveUsageCount();
      buf.append(" (").append(UsageViewBundle.message("usages.n", count)).append(")");
    }
    else if (node instanceof UsageTargetNode) {
      buf.append(((UsageTargetNode)node).getTarget().getPresentation().getPresentableText());
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

  @Nonnull
  @Override
  public String getDefaultFilePath() {
    return myUsageViewSettings.getExportFileName();
  }

  @Override
  public void exportedTo(@Nonnull String filePath) {
    myUsageViewSettings.setExportFileName(filePath);
  }

  @Override
  public boolean canExport() {
    return !myUsageView.isSearchInProgress() && myUsageView.areTargetsValid();
  }
}
