// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.diff.impl.settings;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.contents.DiffContent;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author oleg
 * Implement this interface to enable custom diff preview in Colors & Fonts Settings page
 */
public abstract class DiffPreviewProvider {
  //public static final ExtensionPointName<DiffPreviewProvider> EP_NAME = ExtensionPointName.create("com.intellij.diffPreviewProvider");

  @Nonnull
  public abstract DiffContent[] createContents();

  @Nonnull
  public static DiffContent[] getContents() {
    // Assuming that standalone IDE should provide one provider
    //final List<DiffPreviewProvider> providers = EP_NAME.getExtensionList();
    //if (providers.size() != 0) {
    //  return providers.get(0).createContents();
    //}
    return createContent(LEFT_TEXT, CENTER_TEXT, RIGHT_TEXT, PlainTextFileType.INSTANCE);
  }

  @Nonnull
  public static DiffContent[] createContent(@Nonnull String left, @Nonnull String center, @Nonnull String right, @Nonnull FileType fileType) {
    return new DiffContent[]{createContent(left, fileType), createContent(center, fileType), createContent(right, fileType)};
  }

  @Nonnull
  private static DiffContent createContent(@Nonnull String text, @Nonnull FileType fileType) {
    return DiffContentFactory.getInstance().create(text, fileType);
  }

  @NonNls
  private static final String LEFT_TEXT = "class MyClass {\n" +
                                          "  int value;\n" +
                                          "\n" +
                                          "  void leftOnly() {}\n" +
                                          "\n" +
                                          "  void foo() {\n" +
                                          "   // Left changes\n" +
                                          "  }\n" +
                                          "\n" +
                                          "  void bar() {\n" +
                                          "\n" +
                                          "  }\n" +
                                          "}\n" +
                                          "\n" +
                                          "\n";
  @NonNls
  private static final String CENTER_TEXT =
          "class MyClass {\n" + "  int value;\n" + "\n" + "  void foo() {\n" + "  }\n" + "\n" + "  void removedFromLeft() {}\n" + "\n" + "  void bar() {\n" + "\n" + "  }\n" + "}\n" + "\n" + "\n";
  @NonNls
  private static final String RIGHT_TEXT = "class MyClass {\n" +
                                           "  long value;\n" +
                                           "\n" +
                                           "  void foo() {\n" +
                                           "   // Right changes\n" +
                                           "  }\n" +
                                           "\n" +
                                           "  void removedFromLeft() {}\n" +
                                           "\n" +
                                           "  void bar() {\n" +
                                           "  }\n" +
                                           "\n" +
                                           "}\n" +
                                           "\n" +
                                           "\n";
}
