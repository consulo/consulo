/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.internal;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class DumpExtensionsAction extends DumbAwareAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    List<AreaInstance> areas = new ArrayList<>();
    areas.add(Application.get());
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project != null) {
      areas.add(project);
      final Module[] modules = ModuleManager.getInstance(project).getModules();
      if (modules.length > 0) {
        areas.add(modules[0]);
      }
    }
    System.out.print(areas.size() + " extension areas: ");
    for (AreaInstance area : areas) {
      System.out.print(area + " ");
    }
    System.out.println("\n");

    List<ExtensionPoint> points = new ArrayList<>();
    for (AreaInstance area : areas) {
      points.addAll(Arrays.asList(area.getExtensionsArea().getExtensionPoints()));
    }
    System.out.println(points.size() + " extension points: ");
    for (ExtensionPoint point : points) {
      System.out.println(" " + point.getName());
    }

    List<Object> extensions = new ArrayList<>();
    for (ExtensionPoint point : points) {
      extensions.addAll(Arrays.asList(point.getExtensions()));
    }
    System.out.println("\n" + extensions.size() + " extensions:");
    for (Object extension : extensions) {
      if (extension instanceof Configurable) {
        System.out.println("!!!! Configurable extension found. Kill it !!!");
      }
      System.out.println(extension);
    }
  }
}
