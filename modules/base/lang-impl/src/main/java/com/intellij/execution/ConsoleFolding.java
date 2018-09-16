package com.intellij.execution;

import com.intellij.openapi.extensions.ExtensionPointName;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author peter
 */
public abstract class ConsoleFolding {
  public static final ExtensionPointName<ConsoleFolding> EP_NAME = ExtensionPointName.create("com.intellij.console.folding");

  public abstract boolean shouldFoldLine(String line);

  @Nullable
  public abstract String getPlaceholderText(List<String> lines);
}
