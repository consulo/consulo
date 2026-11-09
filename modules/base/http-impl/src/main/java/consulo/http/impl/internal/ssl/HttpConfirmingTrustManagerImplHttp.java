package consulo.http.impl.internal.ssl;

import consulo.application.Application;
import consulo.http.ssl.HttpClientOnlyTrustManager;
import consulo.http.ssl.HttpConfirmingTrustManagerHttp;
import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The central piece of our SSL support - special kind of trust manager, that asks user to confirm
 * untrusted certificate, e.g. if it wasn't found in system-wide storage.
 *
 * @author Mikhail Golubev
 */
public class HttpConfirmingTrustManagerImplHttp extends X509ExtendedTrustManager implements HttpConfirmingTrustManagerHttp {
    private static final Logger LOG = Logger.getInstance(HttpConfirmingTrustManagerImplHttp.class);
    private static final X509Certificate[] NO_CERTIFICATES = new X509Certificate[0];
    private static final X509TrustManager MISSING_TRUST_MANAGER = new HttpClientOnlyTrustManager() {
        @Override
        public void checkServerTrusted(X509Certificate[] certificates, String s) throws CertificateException {
            LOG.debug("Trust manager is missing. Retreating.");
            throw new CertificateException("Missing trust manager");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return NO_CERTIFICATES;
        }
    };

    public static HttpConfirmingTrustManagerImplHttp createForStorage(String path, String password) {
        return new HttpConfirmingTrustManagerImplHttp(getSystemDefault(), new MutableTrustManagerHttp(path, password));
    }

    private static X509TrustManager getSystemDefault() {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            // hacky way to get default trust store
            factory.init((KeyStore)null);
            // assume that only X509 TrustManagers exist
            X509TrustManager systemManager = findX509TrustManager(factory.getTrustManagers());
            if (systemManager != null && systemManager.getAcceptedIssuers().length != 0) {
                return systemManager;
            }
        }
        catch (Exception e) {
            LOG.error("Cannot get system trust store", e);
        }
        return MISSING_TRUST_MANAGER;
    }

    private final X509TrustManager mySystemManager;
    private final MutableTrustManagerHttp myCustomManager;

    private HttpConfirmingTrustManagerImplHttp(X509TrustManager system, MutableTrustManagerHttp custom) {
        mySystemManager = system;
        myCustomManager = custom;
    }

    private static X509TrustManager findX509TrustManager(TrustManager[] managers) {
        for (TrustManager manager : managers) {
            if (manager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        return null;
    }

    private static class CertificateExtendedCheckException extends CertificateException {
        private CertificateExtendedCheckException(CertificateException e) {
            super(e.getMessage(), e);
        }
    }

    private static void checkServerTrusted(
        X509TrustManager trustManager,
        X509Certificate[] chain,
        String authType,
        @Nullable Socket socket,
        @Nullable SSLEngine engine
    ) throws CertificateException {
        if (!(trustManager instanceof X509ExtendedTrustManager extTrustManager) || (socket == null && engine == null)) {
            trustManager.checkServerTrusted(chain, authType);
        }
        else {
            try {
                if (engine != null) {
                    extTrustManager.checkServerTrusted(chain, authType, engine);
                }
                else {
                    extTrustManager.checkServerTrusted(chain, authType, socket);
                }
            }
            catch (CertificateException e) {
                // X509ExtendedTrustManager validates host name and certificate chain.
                // In order to check the exception cause we run the minimal certificate
                // chain check, if no exception is thrown then we know the cause
                // is the extended checks and not the absence or failure of certification.
                trustManager.checkServerTrusted(chain, authType);
                throw new CertificateExtendedCheckException(e);
            }
        }
    }

    private static @Nullable String getPeerHost(Socket socket) {
        if (socket instanceof SSLSocket sslSocket && sslSocket.getHandshakeSession() != null) {
            String peerHost = sslSocket.getHandshakeSession().getPeerHost();
            if (peerHost != null) {
                return peerHost;
            }
        }
        return socket.getRemoteSocketAddress() instanceof InetSocketAddress address ? address.getHostString() : null;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        throw new UnsupportedOperationException("Should not be called by client");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        throw new UnsupportedOperationException("Should not be called by client");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String s) throws CertificateException {
        checkServerTrusted(certificates, s, null, true, true, null, null);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        checkServerTrusted(chain, authType, getPeerHost(socket), true, true, socket, null);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        checkServerTrusted(chain, authType, engine.getPeerHost(), true, true, null, engine);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String s, boolean addToKeyStore, boolean askUser)
        throws CertificateException {
        checkServerTrusted(certificates, s, null, addToKeyStore, askUser, null, null);
    }

    private void checkServerTrusted(
        X509Certificate[] certificates,
        String s,
        @Nullable String remoteHost,
        boolean addToKeyStore,
        boolean askUser,
        @Nullable Socket socket,
        @Nullable SSLEngine engine
    ) throws CertificateException {
        try {
            checkServerTrusted(mySystemManager, certificates, s, socket, engine);
        }
        catch (CertificateExtendedCheckException e) {
            throw e;
        }
        catch (CertificateException e) {
            // check-then-act sequence
            synchronized (myCustomManager) {
                try {
                    myCustomManager.checkServerTrusted(certificates, s, socket, engine);
                }
                catch (CertificateExtendedCheckException e2) {
                    throw e2;
                }
                catch (CertificateException e2) {
                    if (myCustomManager.isBroken() || !confirmAndUpdate(certificates, remoteHost, addToKeyStore, askUser)) {
                        throw e;
                    }
                }
            }
        }
    }

    private boolean confirmAndUpdate(X509Certificate[] chain, @Nullable String remoteHost, boolean addToKeyStore, boolean askUser) {
        X509Certificate endPoint = chain[0];
        // IDEA-123467 and IDEA-123335 workaround
        String threadClassName = StringUtil.notNullize(Thread.currentThread().getClass().getCanonicalName());
        if (threadClassName.equals("sun.awt.image.ImageFetcher")) {
            LOG.debug("Image Fetcher thread is detected. Certificate check will be skipped.");
            return true;
        }
        HttpCertificateManagerImpl manager = HttpCertificateManagerImpl.getInstance();
        HttpCertificateManagerState config = manager.getState();
        Application app = Application.get();
        if (app.isUnitTestMode() || app.isHeadlessEnvironment() || config.ACCEPT_AUTOMATICALLY) {
            LOG.debug("Certificate will be accepted automatically");
            if (addToKeyStore) {
                myCustomManager.addCertificate(endPoint);
            }
            return true;
        }
        boolean accepted = askUser && HttpCertificateManagerImpl.showAcceptDialog(
            () -> {
                // TODO may be another kind of warning, if default trust store is missing
                return CertificateWarningDialog.createUntrustedCertificateWarning(endPoint);
            },
            manager.findUiOwner(remoteHost)
        );
        if (accepted) {
            LOG.info("Certificate was accepted by user");
            if (addToKeyStore) {
                myCustomManager.addCertificate(endPoint);
            }
        }
        return accepted;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return ArrayUtil.mergeArrays(mySystemManager.getAcceptedIssuers(), myCustomManager.getAcceptedIssuers());
    }

    public X509TrustManager getSystemManager() {
        return mySystemManager;
    }

    public MutableTrustManagerHttp getCustomManager() {
        return myCustomManager;
    }

    /**
     * Trust manager that supports modifications of underlying physical key store.
     * It can also notify clients about such modifications, see {@link #addListener(CertificateListener)}.
     *
     * @see CertificateListener
     */
    public static class MutableTrustManagerHttp extends X509ExtendedTrustManager implements HttpClientOnlyTrustManager {
        private final String myPath;
        private final String myPassword;
        private final TrustManagerFactory myFactory;
        private final KeyStore myKeyStore;
        private final ReadWriteLock myLock = new ReentrantReadWriteLock();
        private final Lock myReadLock = myLock.readLock();
        private final Lock myWriteLock = myLock.writeLock();
        // reloaded after each modification
        private X509TrustManager myTrustManager;

        private final EventDispatcher<CertificateListener> myDispatcher = EventDispatcher.create(CertificateListener.class);

        private MutableTrustManagerHttp(String path, String password) {
            myPath = path;
            myPassword = password;
            // initialization step
            myWriteLock.lock();
            try {
                myFactory = createFactory();
                myKeyStore = createKeyStore(path, password);
                myTrustManager = initFactoryAndGetManager();
            }
            finally {
                myWriteLock.unlock();
            }
        }

        private static TrustManagerFactory createFactory() {
            try {
                return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            }
            catch (NoSuchAlgorithmException e) {
                return null;
            }
        }

        private static KeyStore createKeyStore(String path, String password) {
            KeyStore keyStore;
            try {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                File cacertsFile = new File(path);
                if (cacertsFile.exists()) {
                    FileInputStream stream = null;
                    try {
                        stream = new FileInputStream(path);
                        keyStore.load(stream, password.toCharArray());
                    }
                    finally {
                        StreamUtil.closeStream(stream);
                    }
                }
                else {
                    if (!FileUtil.createParentDirs(cacertsFile)) {
                        LOG.error("Cannot create directories: " + cacertsFile.getParent());
                        return null;
                    }
                    keyStore.load(null, password.toCharArray());
                }
            }
            catch (Exception e) {
                LOG.error(e);
                return null;
            }
            return keyStore;
        }

        /**
         * Add certificate to underlying trust store.
         *
         * @param certificate server's certificate
         * @return whether the operation was successful
         */
        public boolean addCertificate(X509Certificate certificate) {
            myWriteLock.lock();
            try {
                if (isBroken()) {
                    return false;
                }
                myKeyStore.setCertificateEntry(createAlias(certificate), certificate);
                flushKeyStore();
                // trust manager should be updated each time its key store was modified
                myTrustManager = initFactoryAndGetManager();
                myDispatcher.getMulticaster().certificateAdded(certificate);
                return true;
            }
            catch (Exception e) {
                LOG.error("Can't add certificate", e);
                return false;
            }
            finally {
                myWriteLock.unlock();
            }
        }

        /**
         * Add certificate, loaded from file at {@code path}, to underlying trust store.
         *
         * @param path path to file containing certificate
         * @return whether the operation was successful
         */
        public boolean addCertificate(String path) {
            X509Certificate certificate = CertificateUtil.loadX509Certificate(path);
            return certificate != null && addCertificate(certificate);
        }

        private static String createAlias(X509Certificate certificate) {
            return CertificateUtil.getCommonName(certificate);
        }

        /**
         * Remove certificate from underlying trust store.
         *
         * @param certificate certificate alias
         * @return whether the operation was successful
         */
        public boolean removeCertificate(X509Certificate certificate) {
            return removeCertificate(createAlias(certificate));
        }

        /**
         * Remove certificate, specified by its alias, from underlying trust store.
         *
         * @param alias certificate's alias
         * @return true if removal operation was successful and false otherwise
         */
        public boolean removeCertificate(String alias) {
            myWriteLock.lock();
            try {
                if (isBroken()) {
                    return false;
                }
                // for listeners
                X509Certificate certificate = getCertificate(alias);
                if (certificate == null) {
                    LOG.error("No certificate found for alias: " + alias);
                    return false;
                }
                myKeyStore.deleteEntry(alias);
                flushKeyStore();
                // trust manager should be updated each time its key store was modified
                myTrustManager = initFactoryAndGetManager();
                myDispatcher.getMulticaster().certificateRemoved(certificate);
                return true;
            }
            catch (Exception e) {
                LOG.error("Can't remove certificate for alias: " + alias, e);
                return false;
            }
            finally {
                myWriteLock.unlock();
            }
        }

        /**
         * Get certificate, specified by its alias, from underlying trust store.
         *
         * @param alias certificate's alias
         * @return certificate or null if it's not present
         */
        public @Nullable X509Certificate getCertificate(String alias) {
            myReadLock.lock();
            try {
                return (X509Certificate)myKeyStore.getCertificate(alias);
            }
            catch (KeyStoreException e) {
                return null;
            }
            finally {
                myReadLock.unlock();
            }
        }

        /**
         * Select all available certificates from underlying trust store. Returned list is not supposed to be modified.
         *
         * @return certificates
         */
        public List<X509Certificate> getCertificates() {
            myReadLock.lock();
            try {
                List<X509Certificate> certificates = new ArrayList<>();
                for (String alias : Collections.list(myKeyStore.aliases())) {
                    certificates.add(getCertificate(alias));
                }
                return List.copyOf(certificates);
            }
            catch (Exception e) {
                LOG.error(e);
                return List.of();
            }
            finally {
                myReadLock.unlock();
            }
        }

        /**
         * Check that underlying trust store contains certificate with specified alias.
         *
         * @param alias - certificate's alias to be checked
         * @return - whether certificate is in storage
         */
        public boolean containsCertificate(String alias) {
            myReadLock.lock();
            try {
                return myKeyStore.containsAlias(alias);
            }
            catch (KeyStoreException e) {
                LOG.error(e);
                return false;
            }
            finally {
                myReadLock.unlock();
            }
        }

        boolean removeAllCertificates() {
            for (X509Certificate certificate : getCertificates()) {
                if (!removeCertificate(certificate)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            throw new UnsupportedOperationException("Should not be called by client");
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            throw new UnsupportedOperationException("Should not be called by client");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certificates, String s) throws CertificateException {
            checkServerTrusted(certificates, s, null, null);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certificates, String s, Socket socket) throws CertificateException {
            checkServerTrusted(certificates, s, socket, null);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certificates, String s, SSLEngine engine) throws CertificateException {
            checkServerTrusted(certificates, s, null, engine);
        }

        private void checkServerTrusted(X509Certificate[] certificates, String s, @Nullable Socket socket, @Nullable SSLEngine engine)
            throws CertificateException {
            myReadLock.lock();
            try {
                if (keyStoreIsEmpty() || isBroken()) {
                    throw new CertificateException();
                }
                HttpConfirmingTrustManagerImplHttp.checkServerTrusted(myTrustManager, certificates, s, socket, engine);
            }
            finally {
                myReadLock.unlock();
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            myReadLock.lock();
            try {
                // trust no one if broken
                if (keyStoreIsEmpty() || isBroken()) {
                    return NO_CERTIFICATES;
                }
                return myTrustManager.getAcceptedIssuers();
            }
            finally {
                myReadLock.unlock();
            }
        }

        public void addListener(CertificateListener listener) {
            myDispatcher.addListener(listener);
        }

        public void removeListener(CertificateListener listener) {
            myDispatcher.removeListener(listener);
        }

        // Guarded by caller's lock
        private boolean keyStoreIsEmpty() {
            try {
                return myKeyStore.size() == 0;
            }
            catch (KeyStoreException e) {
                LOG.error(e);
                return true;
            }
        }

        // Guarded by caller's lock
        private X509TrustManager initFactoryAndGetManager() {
            try {
                if (myFactory != null && myKeyStore != null) {
                    myFactory.init(myKeyStore);
                    return findX509TrustManager(myFactory.getTrustManagers());
                }
            }
            catch (KeyStoreException e) {
                LOG.error(e);
            }
            return null;
        }

        // Guarded by caller's lock
        private boolean isBroken() {
            return myKeyStore == null || myFactory == null || myTrustManager == null;
        }

        private void flushKeyStore() throws Exception {
            FileOutputStream stream = new FileOutputStream(myPath);
            try {
                myKeyStore.store(stream, myPassword.toCharArray());
            }
            finally {
                StreamUtil.closeStream(stream);
            }
        }
    }
}
