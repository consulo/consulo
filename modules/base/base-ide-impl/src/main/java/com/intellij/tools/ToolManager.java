/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.tools;

import consulo.ui.ex.action.ActionManager;
import consulo.application.ApplicationManager;
import com.intellij.openapi.options.SchemeProcessor;
import com.intellij.openapi.options.SchemesManagerFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author traff
 */
@Singleton
public class ToolManager extends BaseToolManager<Tool> {
  public static ToolManager getInstance() {
    return ApplicationManager.getApplication().getComponent(ToolManager.class);
  }

  @Inject
  public ToolManager(ActionManager actionManager, SchemesManagerFactory factory) {
    super(actionManager, factory);
  }

  @Override
  protected String getSchemesPath() {
    return "$ROOT_CONFIG$/tools";
  }

  @Override
  protected SchemeProcessor<ToolsGroup<Tool>, ToolsGroup<Tool>> createProcessor() {
    return new ToolsProcessor<>() {

      @Override
      protected ToolsGroup<Tool> createToolsGroup(String groupName) {
        return new ToolsGroup<>(groupName);
      }

      @Override
      protected Tool createTool() {
        return new Tool();
      }
    };
  }

  @Override
  protected String getActionIdPrefix() {
    return Tool.ACTION_ID_PREFIX;
  }
}
