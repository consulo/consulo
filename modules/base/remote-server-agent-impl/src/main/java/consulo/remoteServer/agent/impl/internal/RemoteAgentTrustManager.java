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

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Trusts exactly the agent certificate with the given SHA-256 fingerprint. The host name is not checked: the pin
 * identifies the agent, also when it is reached by an address its certificate does not name.
 */
class RemoteAgentTrustManager extends X509ExtendedTrustManager {
    private final String myFingerprint;

    RemoteAgentTrustManager(String fingerprint) {
        myFingerprint = normalize(fingerprint);
    }

    static String normalize(String fingerprint) {
        return fingerprint.replace(":", "").replace(" ", "").trim().toUpperCase(Locale.ROOT);
    }

    private void check(X509Certificate[] chain) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("The remote agent sent no certificate");
        }
        String actual;
        try {
            actual = HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded()));
        }
        catch (NoSuchAlgorithmException e) {
            throw new CertificateException(e);
        }
        if (!actual.equals(myFingerprint)) {
            throw new CertificateException("The remote agent certificate SHA-256 " + actual + " does not match the configured fingerprint");
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        check(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        check(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        check(chain);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new CertificateException("Client certificates are not accepted");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        throw new CertificateException("Client certificates are not accepted");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        throw new CertificateException("Client certificates are not accepted");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
