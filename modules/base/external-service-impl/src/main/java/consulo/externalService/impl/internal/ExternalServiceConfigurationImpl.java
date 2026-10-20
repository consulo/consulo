/*
 * Copyright 2013-2017 consulo.io
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
package consulo.externalService.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.ExternalServiceConfigurationListener;
import consulo.externalService.impl.internal.auth.HubAccounts;
import consulo.logging.Logger;
import consulo.util.lang.ThreeState;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;



/**
 * @author VISTALL
 * @since 2017-03-06
 */
@Singleton
@State(name = "ExternalServiceConfiguration", storages = @Storage(value = "externalService.xml", roamingType = RoamingType.DISABLED))
@ServiceImpl
public class ExternalServiceConfigurationImpl
    implements PersistentStateComponent<ExternalServiceConfigurationState>, ExternalServiceConfiguration {
    private static final Logger LOG = Logger.getInstance(ExternalServiceConfigurationImpl.class);

    private final ExternalServiceConfigurationState myState = new ExternalServiceConfigurationState();

    private final Application myApplication;

    @Inject
    public ExternalServiceConfigurationImpl(Application application) {
        myApplication = application;
    }

    @Override
    public ExternalServiceConfigurationState getState() {
        return myState;
    }

    @Override
    public void loadState(ExternalServiceConfigurationState state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    @Override
    public void afterLoad(boolean first) {
        migrateLegacyLogin();

        myApplication.getMessageBus().syncPublisher(ExternalServiceConfigurationListener.class).configurationChanged(this);
    }

    private void migrateLegacyLogin() {
        String email = myState.email;
        String token = myState.oauthKey;
        if (email == null || token == null) {
            return;
        }
        myState.email = null;
        myState.oauthKey = null;
        myApplication.getInstance(HubAccounts.class).importLegacy(email, token).whenComplete((account, error) -> {
            if (error != null) {
                LOG.warn("Failed to move the Consulo Hub login of " + email + " to accounts", error);
            }
        });
    }

    @Override
    public ThreeState getState(ExternalService externalService) {
        ThreeState state = myState.states.getOrDefault(externalService, externalService.getDefaultState());
        if (state == ThreeState.YES && !myApplication.getInstance(HubAccounts.class).isAuthorized()) {
            return ThreeState.NO;
        }
        return state;
    }

    @Override
    public void setState(ExternalService externalService, ThreeState state) {
        if (externalService.getDefaultState() == state) {
            myState.states.remove(externalService);
        }
        else {
            myState.states.put(externalService, state);
        }
    }
}
