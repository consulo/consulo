/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.http.impl.internal.proxy;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class IdeaWideAuthenticator extends NonStaticAuthenticator {
    private final static Logger LOG = Logger.getInstance(IdeaWideAuthenticator.class);
    private final HttpProxyManagerImpl myHttpConfigurable;

    public IdeaWideAuthenticator(HttpProxyManagerImpl configurable) {
        myHttpConfigurable = configurable;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        String host = CommonProxy.getHostNameReliably(getRequestingHost(), getRequestingSite(), getRequestingURL());
        boolean isProxy = Authenticator.RequestorType.PROXY.equals(getRequestorType());
        String prefix = isProxy ? "Proxy authentication: " : "Server authentication: ";
        Application application = ApplicationManager.getApplication();
        if (isProxy) {
            // according to idea-wide settings
            if (myHttpConfigurable.isHttpProxyEnabled()) {
                LOG.debug("CommonAuthenticator.getPasswordAuthentication will return common defined proxy");
                return myHttpConfigurable.getPromptedAuthentication(host + ":" + getRequestingPort(), getRequestingPrompt());
            }
            else if (myHttpConfigurable.isPacProxyEnabled()) {
                LOG.debug("CommonAuthenticator.getPasswordAuthentication will return autodetected proxy");
                if (myHttpConfigurable.isGenericPasswordCanceled(host, getRequestingPort())) {
                    return null;
                }
                // same but without remembering the results..
                PasswordAuthentication password = myHttpConfigurable.getGenericPassword(host, getRequestingPort());
                if (password != null) {
                    return password;
                }
                // do not try to show any dialogs if application is exiting
                if (application == null || application.isDisposeInProgress() || application.isDisposed()) {
                    return null;
                }

                return myHttpConfigurable.getGenericPromptedAuthentication(prefix, host, getRequestingPrompt(), getRequestingPort(), true);
            }
        }

        // do not try to show any dialogs if application is exiting
        if (application == null || application.isDisposeInProgress() || application.isDisposed()) {
            return null;
        }

        LOG.debug("CommonAuthenticator.getPasswordAuthentication generic authentication will be asked");
        //return myHttpConfigurable.getGenericPromptedAuthentication(prefix, host, getRequestingPrompt(), getRequestingPort(), false);
        return null;
    }
}
