/*
 * Copyright 2013-2014 must-be.org
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
package consulo.ide.impl;

import com.intellij.icons.AllIcons;
import consulo.ide.newProject.NewModuleBuilder;
import consulo.ide.newProject.NewModuleContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class EmptyNewModuleBuilder implements NewModuleBuilder {
  @Override
  public void setupContext(@NotNull NewModuleContext context) {
    NewModuleContext.Group group = context.createGroup(NewModuleContext.UGROUPED, "Ungrouped");

    group.add("Empty", AllIcons.FileTypes.Any_type, () -> new JPanel(new BorderLayout()));
  }
}
