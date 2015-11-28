/*
 * Copyright 2013 must-be.org
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
package com.intellij.execution.testframework;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IconDescriptor;
import com.intellij.ide.IconDescriptorUpdater;
import com.intellij.psi.PsiElement;
import com.intellij.testIntegration.TestFramework;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.RequiredReadAction;

/**
 * @author VISTALL
 * @since 1:22/19.07.13
 */
public class TestIconDescriptorUpdater implements IconDescriptorUpdater {
  @RequiredReadAction
  @Override
  public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    for (TestFramework framework : TestFramework.EXTENSION_NAME.getExtensions()) {
      if (framework.isIgnoredMethod(element)) {
        iconDescriptor.setMainIcon(AllIcons.RunConfigurations.IgnoredTest);
      }

      if (framework.isTestMethod(element) || framework.isTestClass(element)) {
        iconDescriptor.addLayerIcon(AllIcons.RunConfigurations.TestMark);
      }
    }
  }
}
