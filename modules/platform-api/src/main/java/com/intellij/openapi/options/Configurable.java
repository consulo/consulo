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
package com.intellij.openapi.options;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.ArrayFactory;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Named component which provides a configuration user interface.
 * <p>
 * <p><p>
 * Use {@code com.intellij.projectConfigurable} and {@code com.intellij.applicationConfigurable} extensions to provide items for
 * "Project Settings" and "IDE Settings" groups correspondingly in the "Settings" dialog. There are two ways to declare such extension:
 * <ul>
 * <li> an extension element with 'instance' attribute
 * <br>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;projectConfigurable instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;<br>
 * where 'class-name' implements {@link Configurable} means that a new instance of the specified class will be created each time when
 * the dialog is opened.
 * <p>
 * <li> an extension with 'provider' attribute<br>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;projectConfigurable provider="class-name"/&gt;<br>
 * &lt;/extensions&gt;<br>
 * where 'class-name' implements {@link ConfigurableProvider} means that method {@link ConfigurableProvider#createConfigurable()}
 * will be used to create instance each time when the dialog is opened.
 * </ul>
 *
 * @see SearchableConfigurable
 * @see consulo.options.SimpleConfigurable
 */
public interface Configurable extends UnnamedConfigurable {
  public static final Configurable[] EMPTY_ARRAY = new Configurable[0];

  public static ArrayFactory<Configurable> ARRAY_FACTORY = new ArrayFactory<Configurable>() {
    @NotNull
    @Override
    public Configurable[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new Configurable[count];
    }
  };

  ExtensionPointName<ConfigurableEP<Configurable>> APPLICATION_CONFIGURABLE = ExtensionPointName.create("com.intellij.applicationConfigurable");

  ExtensionPointName<ConfigurableEP<Configurable>> PROJECT_CONFIGURABLE = ExtensionPointName.create("com.intellij.projectConfigurable");

  /**
   * Returns the user-visible name of the settings component.
   *
   * @return the visible name of the component.
   */
  @Nls
  default String getDisplayName() {
    return null;
  }

  /**
   * Returns the topic in the help file which is shown when help for the configurable
   * is requested.
   *
   * @return the help topic, or null if no help is available.
   */
  @Nullable
  @NonNls
  default String getHelpTopic() {
    return null;
  }

  /**
   * @deprecated this marker interface was used to hide a Configurable declared as applicationConfigurable or projectConfigurable extension
   * from the Settings dialog. However it makes no sense to register a Configurable as extension if you don't want to see it in the Settings
   * dialog
   */
  interface Assistant extends Configurable {
  }

  interface Composite {
    @NotNull
    Configurable[] getConfigurables();
  }

  /**
   * Forbids wrapping the content of the configurable in a scroll pane. Required when
   * the configurable contains its own scrollable components.
   */
  interface NoScroll {
  }

  /**
   * This marker interface notifies the Settings dialog to not add an empty border to the Swing form.
   * Required when the Swing form is a tabbed pane.
   */
  interface NoMargin {
  }

  interface HoldPreferredFocusedComponent {
    @Nullable
    @RequiredUIAccess
    default JComponent getPreferredFocusedComponent() {
      return null;
    }

    @RequiredUIAccess
    @Nullable
    default Component getPreferredFocusedUIComponent() {
      return null;
    }
  }
}
