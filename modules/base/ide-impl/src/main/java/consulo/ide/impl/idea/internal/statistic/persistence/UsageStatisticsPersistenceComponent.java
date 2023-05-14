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

package consulo.ide.impl.idea.internal.statistic.persistence;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.externalService.statistic.ConvertUsagesUtil;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.ide.impl.idea.internal.statistic.configurable.SendPeriod;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.ide.ServiceManager;
import consulo.util.lang.ThreeState;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

@State(name = "UsagesStatistic", storages = @Storage(value = "usage.statistics.xml", roamingType = RoamingType.DISABLED))
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class UsageStatisticsPersistenceComponent extends BasicSentUsagesPersistenceComponent implements PersistentStateComponent<Element> {

  @Nonnull
  public static UsageStatisticsPersistenceComponent getInstance() {
    return ServiceManager.getService(UsageStatisticsPersistenceComponent.class);
  }

  private static final String DATA_ATTR = "data";
  private static final String GROUP_TAG = "group";
  private static final String GROUP_ID_ATTR = "id";
  private static final String LAST_TIME_ATTR = "time";
  private static final String PERIOD_ATTR = "period";
  private static final String SECRET_KEY = "key";

  @Nonnull
  private SendPeriod myPeriod = SendPeriod.WEEKLY;

  private String mySecretKey;

  private final ExternalServiceConfiguration myExternalServiceConfiguration;

  @Inject
  public UsageStatisticsPersistenceComponent(ExternalServiceConfiguration externalServiceConfiguration) {
    myExternalServiceConfiguration = externalServiceConfiguration;
  }

  @Override
  public void loadState(final Element element) {
    List<Element> groupsList = element.getChildren(GROUP_TAG);
    for (Element groupElement : groupsList) {
      String groupId = groupElement.getAttributeValue(GROUP_ID_ATTR);

      String valueData = groupElement.getAttributeValue(DATA_ATTR);
      if (!StringUtil.isEmptyOrSpaces(groupId) && !StringUtil.isEmptyOrSpaces(valueData)) {
        try {
          getSentUsages().putAll(ConvertUsagesUtil.convertValueString(groupId, valueData));
        }
        catch (AssertionError e) {
          //don't load incorrect groups
        }
      }
    }

    try {
      setSentTime(Long.parseLong(element.getAttributeValue(LAST_TIME_ATTR)));
    }
    catch (NumberFormatException e) {
      setSentTime(0);
    }

    final String isAllowedValue = element.getAttributeValue("allowed");
    // if disabled by hand - store it, anyway - left for selection of new logic
    if(!StringUtil.isEmptyOrSpaces(isAllowedValue) && !Boolean.parseBoolean(isAllowedValue)) {
      myExternalServiceConfiguration.setState(ExternalService.STATISTICS, ThreeState.NO);
    }

    setPeriod(parsePeriod(element.getAttributeValue(PERIOD_ATTR)));
    mySecretKey = element.getAttributeValue(SECRET_KEY);
  }

  @Override
  public Element getState() {
    Element element = new Element("state");

    for (Map.Entry<String, Set<UsageDescriptor>> entry : new TreeMap<>(getSentUsages()).entrySet()) {
      Element projectElement = new Element(GROUP_TAG);
      projectElement.setAttribute(GROUP_ID_ATTR, entry.getKey());
      projectElement.setAttribute(DATA_ATTR, ConvertUsagesUtil.convertValueMap(entry.getValue()));

      element.addContent(projectElement);
    }

    element.setAttribute(LAST_TIME_ATTR, String.valueOf(getLastTimeSent()));
    element.setAttribute(PERIOD_ATTR, myPeriod.getName());
    element.setAttribute(SECRET_KEY, getSecretKey());

    return element;
  }

  @Nonnull
  public String getSecretKey() {
    if (mySecretKey == null) {
      mySecretKey = UUID.randomUUID().toString();
    }

    return mySecretKey;
  }

  @Nonnull
  public SendPeriod getPeriod() {
    return myPeriod;
  }

  public void setPeriod(@Nonnull SendPeriod period) {
    myPeriod = period;
  }

  @Nonnull
  private static SendPeriod parsePeriod(@Nullable String periodAttrValue) {
    if (SendPeriod.DAILY.getName().equals(periodAttrValue)) return SendPeriod.DAILY;
    if (SendPeriod.MONTHLY.getName().equals(periodAttrValue)) return SendPeriod.MONTHLY;

    return SendPeriod.WEEKLY;
  }

  @Override
  public boolean isAllowed() {
    return myExternalServiceConfiguration.getState(ExternalService.STATISTICS) != ThreeState.NO;
  }

  public boolean isTimeToSend() {
    final long timeDelta = System.currentTimeMillis() - getLastTimeSent();

    return Math.abs(timeDelta) > getPeriod().getMillis();
  }
}
