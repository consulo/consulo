/*
 * Copyright 2013-2026 consulo.io
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
package consulo.remoteServer.agent.impl.internal;

import consulo.remoteServer.configuration.ServerConfigurationBase;
import consulo.util.xml.serializer.annotation.Attribute;

import java.util.UUID;

/**
 * Configuration for a remote agent server connection.
 * Persisted in remote-servers.xml.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemoteAgentServerConfiguration extends ServerConfigurationBase<RemoteAgentServerConfiguration> {
    private String myHost = "localhost";
    private int myPort = 57638;
    private boolean myUseTls;
    private String myCertificateFingerprint = "";

    @Attribute("credentialId")
    private String myCredentialId = "";

    @Attribute("host")
    public String getHost() {
        return myHost;
    }

    public void setHost(String host) {
        myHost = host;
    }

    @Attribute("port")
    public int getPort() {
        return myPort;
    }

    public void setPort(int port) {
        myPort = port;
    }

    @Attribute("tls")
    public boolean isUseTls() {
        return myUseTls;
    }

    public void setUseTls(boolean useTls) {
        myUseTls = useTls;
    }

    @Attribute("certificateFingerprint")
    public String getCertificateFingerprint() {
        return myCertificateFingerprint;
    }

    public void setCertificateFingerprint(String certificateFingerprint) {
        myCertificateFingerprint = certificateFingerprint.trim();
    }

    public String getCredentialId() {
        if (myCredentialId.isEmpty()) {
            myCredentialId = UUID.randomUUID().toString();
        }
        return myCredentialId;
    }
}
