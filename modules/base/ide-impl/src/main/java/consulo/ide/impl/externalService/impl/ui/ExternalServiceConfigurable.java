/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.externalService.impl.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.ExternalServiceConfigurationListener;
import consulo.ide.impl.idea.internal.statistic.configurable.SendPeriod;
import consulo.ide.impl.idea.internal.statistic.persistence.UsageStatisticsPersistenceComponent;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.ThreeState;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 04/09/2021
 */
@ExtensionImpl
public class ExternalServiceConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable {
  private final Application myApplication;
  private final Provider<ExternalServiceConfiguration> myExternalServiceConfigurationProvider;
  private final Provider<UsageStatisticsPersistenceComponent> myUsageStatisticsPersistenceComponentProvider;

  @Inject
  public ExternalServiceConfigurable(Application application,
                                     Provider<ExternalServiceConfiguration> externalServiceConfigurationProvider,
                                     Provider<UsageStatisticsPersistenceComponent> usageStatisticsPersistenceComponentProvider) {
    myApplication = application;
    myExternalServiceConfigurationProvider = externalServiceConfigurationProvider;
    myUsageStatisticsPersistenceComponentProvider = usageStatisticsPersistenceComponentProvider;
  }

  @Nonnull
  @Override
  public String getId() {
    return "externalServices";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.PLATFORM_AND_PLUGINS_GROUP;
  }

  @Override
  public String getDisplayName() {
    return "External Services";
  }

  @Override
  protected void afterApply() {
    super.afterApply();

    myApplication.getMessageBus().syncPublisher(ExternalServiceConfigurationListener.class).configurationChanged(myExternalServiceConfigurationProvider.get());
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(@Nonnull PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
    ExternalServiceConfiguration extService = myExternalServiceConfigurationProvider.get();
    UsageStatisticsPersistenceComponent statistics = myUsageStatisticsPersistenceComponentProvider.get();

    boolean authorized = extService.isAuthorized();

    VerticalLayout layout = VerticalLayout.create();
    layout.add(DockLayout.create().left(Label.create(LocalizeValue.localizeTODO("Account: "))).right(Label.create(authorized ? LocalizeValue.of(extService.getEmail()) : LocalizeValue.localizeTODO("<none>"))));

    VerticalLayout servicesLayout = VerticalLayout.create();

    for (ExternalService service : ExternalService.values()) {
      DockLayout line = DockLayout.create();
      Label serviceLabel = Label.create(service.getName());
      line.left(serviceLabel);

      List<ThreeState> states = ContainerUtil.newArrayList(service.getAllowedStates());

      if (!authorized) {
        states.remove(ThreeState.YES);
      }

      ComboBox<ThreeState> stateBox = ComboBox.create(states);
      stateBox.setTextRender(threeState -> {
        switch (threeState) {
          case YES:
            return LocalizeValue.localizeTODO("Enabled (Unanonymous)");
          case UNSURE:
            return LocalizeValue.localizeTODO("Enabled (Anonymous)");
          case NO:
            return LocalizeValue.localizeTODO("Disabled");
          default:
            return LocalizeValue.empty();
        }
      });

      if (service == ExternalService.STATISTICS) {
        ComboBox<SendPeriod> periodBox = ComboBox.create(SendPeriod.values());
        Label sendPeriodLabel = Label.create(LocalizeValue.localizeTODO("Send Period:"));

        stateBox.addValueListener(event -> {
          periodBox.setEnabled(event.getValue() != ThreeState.NO);
          sendPeriodLabel.setEnabled(event.getValue() != ThreeState.NO);
        });

        propertyBuilder.add(periodBox, statistics::getPeriod, statistics::setPeriod);

        line.right(HorizontalLayout.create().add(sendPeriodLabel).add(periodBox).add(stateBox));
      }
      else {
        line.right(stateBox);
      }

      stateBox.setValue(service.getDefaultState());

      propertyBuilder.add(stateBox, () -> extService.getState(service), it -> extService.setState(service, it));

      if (states.size() == 1) {
        stateBox.setValue(states.get(0));
        stateBox.setEnabled(false);
        serviceLabel.setEnabled(false);
      }

      servicesLayout.add(line);
    }

    layout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Services"), servicesLayout));

    DockLayout block = DockLayout.create();
    block.top(layout);

    LocalizeValue html =
            LocalizeValue.localizeTODO("Anonymous data, does not contain any personal information, collected for use only by <b>consulo.io</b><br> and will never be transmitted to any third party.");

    block.bottom(LabeledLayout.create(LocalizeValue.localizeTODO("Info"), VerticalLayout.create().add(HtmlLabel.create(html))));

    return block;
  }
}
