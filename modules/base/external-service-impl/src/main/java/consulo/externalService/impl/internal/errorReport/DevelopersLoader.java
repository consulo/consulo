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
package consulo.externalService.impl.internal.errorReport;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.externalService.ExternalService;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.impl.internal.WebServiceApi;
import consulo.externalService.impl.internal.WebServiceApiSender;
import consulo.externalService.impl.internal.repository.api.UserAccount;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DevelopersLoader {
  @Nonnull
  public static List<Developer> fetchDevelopers(ProgressIndicator indicator) throws IOException {
    ExternalServiceConfiguration externalServiceConfiguration = Application.get().getInstance(ExternalServiceConfiguration.class);

    ThreeState state = externalServiceConfiguration.getState(ExternalService.DEVELOPER_LIST);
    if(state == ThreeState.NO) {
      return List.of(Developer.NULL);
    }

    UserAccount[] userAccounts = WebServiceApiSender.doGet(WebServiceApi.DEVELOPER_API, "list", UserAccount[].class);

    List<Developer> developers = new ArrayList<>();
    developers.add(Developer.NULL);

    for (UserAccount userAccount : userAccounts) {
      developers.add(new Developer(userAccount.id, userAccount.username));
    }
    return developers;
  }
}
