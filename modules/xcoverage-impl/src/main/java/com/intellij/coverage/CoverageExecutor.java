package com.intellij.coverage;

import com.intellij.execution.Executor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.wm.ToolWindowId;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CoverageExecutor extends Executor {

  public static final String EXECUTOR_ID = "Coverage";

  @Override
  @NotNull
  public String getStartActionText() {
    return "Run with Co_verage";
  }

  @NotNull
  @Override
  public String getStartActionText(boolean emptyName) {
    return "Run" + (emptyName ? "" :  " ''{0}''") + " with Co_verage";
  }

  @Override
  public String getToolWindowId() {
    return ToolWindowId.RUN;
  }

  @Override
  public Icon getToolWindowIcon() {
    return AllIcons.General.RunWithCoverage;
  }

  @Override
  @NotNull
  public Icon getIcon() {
    return AllIcons.General.RunWithCoverage;
  }

  @Override
  public Icon getDisabledIcon() {
    return null;
  }

  @Override
  public String getDescription() {
    return "Run selected configuration with coverage enabled";
  }

  @Override
  @NotNull
  public String getActionName() {
    return "Cover";
  }

  @Override
  @NotNull
  public String getId() {
    return EXECUTOR_ID;
  }

  @Override
  public String getContextActionId() {
    return "RunCoverage";
  }

  @Override
  public String getHelpId() {
    return null;//todo
  }
}
