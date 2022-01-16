package com.intellij.codeInsight.generation;

import com.intellij.openapi.actionSystem.DataContext;
import consulo.component.extension.ExtensionPointName;

/**
 * @author Dmitry Avdeev
 */
public interface PatternProvider {

  ExtensionPointName<PatternProvider> EXTENSION_POINT_NAME = ExtensionPointName.create("consulo.base.patternProvider");

  PatternDescriptor[] getDescriptors();

  boolean isAvailable(DataContext context);
}
