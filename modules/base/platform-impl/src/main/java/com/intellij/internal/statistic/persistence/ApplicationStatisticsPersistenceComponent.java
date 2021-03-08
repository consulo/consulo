/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.internal.statistic.persistence;

import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.components.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@State(name = "StatisticsApplicationUsages", storages = @Storage(value = "statistics.application.usages.xml", roamingType = RoamingType.DISABLED))
@Singleton
public class ApplicationStatisticsPersistenceComponent extends ApplicationStatisticsPersistence implements PersistentStateComponent<Element> {
  public static ApplicationStatisticsPersistenceComponent getInstance() {
    return ServiceManager.getService(ApplicationStatisticsPersistenceComponent.class);
  }

  private static final String TOKENIZER = ",";

  private static final String GROUP_TAG = "group";
  private static final String GROUP_NAME_ATTR = "name";
  private static final String PROJECT_TAG = "project";
  private static final String PROJECT_ID_ATTR = "id";
  private static final String VALUES_ATTR = "values";

  @Override
  public void loadState(final Element element) {
    List<Element> groups = element.getChildren(GROUP_TAG);

    for (Element groupElement : groups) {
      String groupName = groupElement.getAttributeValue(GROUP_NAME_ATTR);

      List<Element> projectsList = groupElement.getChildren(PROJECT_TAG);
      for (Element projectElement : projectsList) {
        String projectId = projectElement.getAttributeValue(PROJECT_ID_ATTR);
        String frameworks = projectElement.getAttributeValue(VALUES_ATTR);
        if (!StringUtil.isEmptyOrSpaces(projectId) && !StringUtil.isEmptyOrSpaces(frameworks)) {
          Set<UsageDescriptor> frameworkDescriptors = new HashSet<UsageDescriptor>();
          for (String key : StringUtil.split(frameworks, TOKENIZER)) {
            final UsageDescriptor descriptor = getUsageDescriptor(key);
            if (descriptor != null) frameworkDescriptors.add(descriptor);
          }
          getApplicationData(groupName).put(projectId, frameworkDescriptors);
        }
      }
    }
  }

  @Override
  public Element getState() {
    Element element = new Element("state");

    for (Map.Entry<String, Map<String, Set<UsageDescriptor>>> appData : getApplicationData().entrySet()) {
      Element groupElement = new Element(GROUP_TAG);
      groupElement.setAttribute(GROUP_NAME_ATTR, appData.getKey());
      boolean isEmptyGroup = true;

      for (Map.Entry<String, Set<UsageDescriptor>> projectData : appData.getValue().entrySet()) {
        Element projectElement = new Element(PROJECT_TAG);
        projectElement.setAttribute(PROJECT_ID_ATTR, projectData.getKey());
        final Set<UsageDescriptor> projectDataValue = projectData.getValue();
        if (!projectDataValue.isEmpty()) {
          projectElement.setAttribute(VALUES_ATTR, joinUsages(projectDataValue));
          groupElement.addContent(projectElement);
          isEmptyGroup = false;
        }
      }

      if (!isEmptyGroup) {
        element.addContent(groupElement);
      }
    }

    return element;
  }

  private static UsageDescriptor getUsageDescriptor(String usage) {
    // for instance, usage can be: "_foo"(equals "_foo=1") or "_foo=2"
    try {
      final int i = usage.indexOf('=');
      if (i > 0 && i < usage.length() - 1) {
        String key = usage.substring(0, i).trim();
        String value = usage.substring(i + 1).trim();
        if (!StringUtil.isEmptyOrSpaces(key) && !StringUtil.isEmptyOrSpaces(value)) {
          try {
            final int count = Integer.parseInt(value);
            if (count > 0) {
              return new UsageDescriptor(key, count);
            }
          }
          catch (NumberFormatException ignored) {
          }
        }
      }
      return new UsageDescriptor(usage, 1);
    }
    catch (AssertionError e) {
      //escape loading of invalid usages
    }
    return null;
  }

  private static String joinUsages(@Nonnull Set<UsageDescriptor> usages) {
    // for instance, usage can be: "_foo"(equals "_foo=1") or "_foo=2"
    return StringUtil.join(usages, new Function<UsageDescriptor, String>() {
      @Override
      public String fun(UsageDescriptor usageDescriptor) {
        final String key = usageDescriptor.getKey();
        final int value = usageDescriptor.getValue();

        return value > 1 ? key + "=" + value : key;
      }
    }, TOKENIZER);
  }
}
