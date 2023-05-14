// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.uiOld.components.fields;

import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.desktop.awt.ui.plaf.extend.textBox.SupportTextBoxWithExpandActionExtender;
import consulo.desktop.awt.uiOld.Expandable;
import consulo.process.cmd.ParametersListUtil;

import jakarta.annotation.Nonnull;
import javax.swing.text.JTextComponent;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * @author Sergey Malenkov
 */
public class ExpandableTextField extends ExtendableTextField implements Expandable {
  private final ExpandableSupport support;

  /**
   * Creates an expandable text field with the default line parser/joiner,
   * that uses a whitespaces to split a string to several lines.
   */
  public ExpandableTextField() {
    this(ParametersListUtil.DEFAULT_LINE_PARSER, ParametersListUtil.DEFAULT_LINE_JOINER);
  }

  /**
   * Creates an expandable text field with the specified line parser/joiner.
   *
   * @see ParametersListUtil
   */
  public ExpandableTextField(@Nonnull Function<? super String, ? extends List<String>> parser, @Nonnull Function<? super List<String>, String> joiner) {
    Function<? super String, String> onShow = text -> StringUtil.join(parser.apply(text), "\n");
    Function<? super String, String> onHide = text -> joiner.apply(asList(StringUtil.splitByLines(text)));
    support = new IntelliJExpandableSupport<JTextComponent>(this, onShow, onHide);

    putClientProperty("monospaced", true);
    setExtensions(createExtensions());
  }

  public ExpandableTextField(@Nonnull Function<? super String, ? extends List<String>> parser, @Nonnull Function<? super List<String>, String> joiner, SupportTextBoxWithExpandActionExtender lookAndFeel) {
    Function<? super String, String> onShow = text -> StringUtil.join(parser.apply(text), "\n");
    Function<? super String, String> onHide = text -> joiner.apply(asList(StringUtil.splitByLines(text)));

    support = lookAndFeel.createExpandableSupport(this, onShow, onHide);

    putClientProperty("monospaced", true);
    setExtensions(createExtensions());
  }

  @Nonnull
  protected List<ExtendableTextComponent.Extension> createExtensions() {
    return singletonList(support.createExpandExtension());
  }

  public String getTitle() {
    return support.getTitle();
  }

  public void setTitle(String title) {
    support.setTitle(title);
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (!enabled) support.collapse();
    super.setEnabled(enabled);
  }

  @Override
  public void collapse() {
    support.collapse();
  }

  @Override
  public boolean isExpanded() {
    return support.isExpanded();
  }

  @Override
  public void expand() {
    support.expand();
  }
}
