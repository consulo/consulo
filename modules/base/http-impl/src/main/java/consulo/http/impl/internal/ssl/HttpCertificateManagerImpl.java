package consulo.http.impl.internal.ssl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.container.boot.ContainerPathManager;
import consulo.http.HttpCertificateManager;
import consulo.http.internal.HttpRequestOptions;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.util.io.FileUtil;
import consulo.util.io.StreamUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.crypto.BadPaddingException;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * {@code CertificateManager is responsible for negotiation SSL connection with server
 * and deals with untrusted/self-singed/expired and other kinds of digital certificates.
 * <h1>Integration details:</h1>
 * If you're using httpclient-3.1 without custom {@code Protocol} instance for HTTPS you don't have to do anything
 * at all: default {@code HttpClient} will use "Default" {@code SSLContext}, which is set up by this component itself.
 * <p/>
 * However for httpclient-4.x you have several of choices:
 * <pre>
 * <ol>
 *  <li>Client returned by {@code HttpClients.createSystem()} will use "Default" SSL context as it does in httpclient-3.1.</li>
 *  <li>If you want to customize {@code HttpClient} using {@code HttpClients.custom()}, you can use the following methods of the builder
 *  (in the order of increasing complexity/flexibility)
 *    <ol>
 *      <li>{@code useSystemProperties()} methods makes {@code HttpClient} use "Default" SSL context again</li>
 *      <li>{@code setSSLContext()} and pass result of the {@link #getSslContext()}</li>
 *      <li>{@code setSSLSocketFactory()} and specify instance {@code SSLConnectionSocketFactory}
 *          which uses result of {@link #getSslContext()}.</li>
 *      <li>{@code setConnectionManager} and initialize it with {@code Registry} that binds aforementioned
 *          {@code SSLConnectionSocketFactory} to HTTPS protocol</li>
 *      </ol>
 *    </li>
 * </ol>
 * </pre>
 *
 * @author Mikhail Golubev
 */
@Singleton
@State(name = "CertificateManagerImpl", storages = @Storage("other"))
@ServiceImpl
public class HttpCertificateManagerImpl implements HttpCertificateManager, PersistentStateComponent<HttpCertificateManagerState> {
    private static final String DEFAULT_PATH = FileUtil.join(ContainerPathManager.get().getSystemPath(), "tasks", "cacerts");
    private static final String DEFAULT_PASSWORD = "changeit";

    private static final Logger LOG = Logger.getInstance(HttpCertificateManagerImpl.class);

    /**
     * Used to check whether dialog is visible to prevent possible deadlock, e.g. when some external resource is loaded by
     * {@link MediaTracker}.
     */
    static final long DIALOG_VISIBILITY_TIMEOUT = 5000; // ms

    public static HttpCertificateManagerImpl getInstance() {
        return (HttpCertificateManagerImpl)Application.get().getInstance(HttpCertificateManager.class);
    }

    private final String myCacertsPath;
    private final String myPassword;
    private HttpCertificateManagerState myConfig = new HttpCertificateManagerState();

    private final HttpConfirmingTrustManagerImplHttp myTrustManager;

    /**
     * Lazy initialized
     */
    private SSLContext mySslContext;

    private final Map<String, Deque<Component>> myUiOwners = new ConcurrentHashMap<>();

    /**
     * Component initialization constructor
     */
    public HttpCertificateManagerImpl() {
        myCacertsPath = DEFAULT_PATH;
        myPassword = DEFAULT_PASSWORD;
        myTrustManager = HttpConfirmingTrustManagerImplHttp.createForStorage(myCacertsPath, myPassword);
        initComponent();
    }

    public void initComponent() {
        try {
            // Don't do this: protocol created this way will ignore SSL tunnels. See IDEA-115708.
            // Protocol.registerProtocol("https", CertificateManagerImpl.createDefault().createProtocol());
            if (Registry.is("ide.certificate.manager")) {
                SSLContext.setDefault(getSslContext());
                LOG.debug("Default SSL context initialized");
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    /**
     * Creates special kind of {@code SSLContext}, which X509TrustManager first checks certificate presence in
     * in default system-wide trust store (usually located at {@code ${JAVA_HOME}/lib/security/cacerts} or specified by
     * {@code javax.net.ssl.trustStore} property) and when in the one specified by field {@link #myCacertsPath}.
     * If certificate wasn't found in either, manager will ask user, whether it can be
     * accepted (like web-browsers do) and then, if it does, certificate will be added to specified trust store.
     * <p/>
     * If any error occurred during creation its message will be logged and system default SSL context will be returned
     * so clients don't have to deal with awkward JSSE errors.
     * </p>
     * This method may be used for transition to HttpClient 4.x (see {@code HttpClientBuilder#setSslContext(SSLContext)})
     * and {@code org.apache.http.conn.ssl.SSLConnectionSocketFactory()}.
     *
     * @return instance of SSLContext with described behavior or default SSL context in case of error
     */
    @Override
    public synchronized SSLContext getSslContext() {
        if (mySslContext == null) {
            SSLContext context = getSystemSslContext();
            if (Registry.is("ide.certificate.manager")) {
                try {
                    // SSLContext context = SSLContext.getDefault();
                    // NOTE: existence of default trust manager can be checked here as
                    // assert systemManager.getAcceptedIssuers().length != 0
                    context.init(getDefaultKeyManagers(), new TrustManager[]{getTrustManager()}, null);
                }
                catch (KeyManagementException e) {
                    LOG.error(e);
                }
            }
            else {
                // IDEA-124057 Do not touch default context at all if certificate manager was disabled.

                // For some reason passing `null` as first parameter of SSLContext#init is not enough to
                // use -Djavax.net.ssl.keyStore VM parameters, although -Djavax.net.ssl.trustStore is used
                // successfully. See this question on StackOverflow for details
                // http://stackoverflow.com/questions/23205266/java-key-store-is-not-found-when-default-ssl-context-is-redefined
                context = getDefaultSslContext();
            }
            mySslContext = context;
        }
        return mySslContext;
    }

    @Override
    public SSLContext getSystemSslContext() {
        // NOTE: SSLContext.getDefault() should not be called because it automatically creates
        // default context which can't be initialized twice
        try {
            // actually TLSv1 support is mandatory for Java platform
            SSLContext context = SSLContext.getInstance(CertificateUtil.TLS);
            context.init(null, null, null);
            return context;
        }
        catch (NoSuchAlgorithmException e) {
            LOG.error(e);
            throw new AssertionError("Cannot get system SSL context");
        }
        catch (KeyManagementException e) {
            LOG.error(e);
            throw new AssertionError("Cannot initialize system SSL context");
        }
    }

    
    private SSLContext getDefaultSslContext() {
        try {
            return SSLContext.getDefault();
        }
        catch (NoSuchAlgorithmException e) {
            LOG.error("Default SSL context not available. Using system instead.");
            return getSystemSslContext();
        }
    }

    /**
     * Workaround for IDEA-124057. Manually find key store specified via VM options.
     *
     * @return key managers or {@code null} in case of any error
     */
    @Override
    public KeyManager @Nullable [] getDefaultKeyManagers() {
        String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
        if (keyStorePath != null) {
            LOG.info("Loading custom key store specified with VM options: " + keyStorePath);
            try {
                KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore keyStore;
                String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());
                try {
                    keyStore = KeyStore.getInstance(keyStoreType);
                }
                catch (KeyStoreException e) {
                    if (e.getCause() instanceof NoSuchAlgorithmException) {
                        LOG.error("Wrong key store type: " + keyStoreType, e);
                        return null;
                    }
                    throw e;
                }
                String password = System.getProperty("javax.net.ssl.keyStorePassword", "");
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(keyStorePath);
                    keyStore.load(inputStream, password.toCharArray());
                    factory.init(keyStore, password.toCharArray());
                }
                catch (FileNotFoundException e) {
                    LOG.error("Key store file not found: " + keyStorePath);
                    return null;
                }
                catch (Exception e) {
                    if (e.getCause() instanceof BadPaddingException) {
                        LOG.error("Wrong key store password: " + password, e);
                        return null;
                    }
                    throw e;
                }
                finally {
                    StreamUtil.closeStream(inputStream);
                }
                return factory.getKeyManagers();
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
        return null;
    }

    public String getCacertsPath() {
        return myCacertsPath;
    }

    public String getPassword() {
        return myPassword;
    }

    @Override
    public HttpConfirmingTrustManagerImplHttp getTrustManager() {
        return myTrustManager;
    }

    public HttpConfirmingTrustManagerImplHttp.MutableTrustManagerHttp getCustomTrustManager() {
        return myTrustManager.getCustomManager();
    }

    /**
     * Remembers the component a request is sent for, so a certificate question about its host is asked in the ui of
     * that component.
     *
     * @return removes the component again, once the request is done
     */
    public static Runnable registerUiOwner(HttpCertificateManager manager, HttpRequestOptions options) {
        Component owner = options.uiOwner();
        String host = hostOf(options.url());
        if (owner == null || host == null || !(manager instanceof HttpCertificateManagerImpl impl)) {
            return () -> {
            };
        }

        impl.myUiOwners.compute(host, (key, owners) -> {
            Deque<Component> result = owners == null ? new ConcurrentLinkedDeque<>() : owners;
            result.addLast(owner);
            return result;
        });
        return () -> impl.myUiOwners.computeIfPresent(host, (key, owners) -> {
            owners.removeLastOccurrence(owner);
            return owners.isEmpty() ? null : owners;
        });
    }

    public @Nullable Component findUiOwner(@Nullable String host) {
        if (host == null) {
            return null;
        }
        Deque<Component> owners = myUiOwners.get(host.toLowerCase(Locale.ROOT));
        return owners == null ? null : owners.peekLast();
    }

    private static @Nullable String hostOf(String url) {
        try {
            String host = new URI(url).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Shows the dialog and blocks the calling (network) thread until the user answers. Never called on the UI thread -
     * the dialog is asynchronous there and the answer could not be waited for, so the certificate is rejected.
     */
    @SuppressWarnings("deprecation")
    public static boolean showAcceptDialog(Supplier<? extends DialogDescriptor> dialogFactory, @Nullable Component owner) {
        if (UIAccess.isUIThread()) {
            LOG.warn("Certificate confirmation requested on the UI thread. Rejecting certificate.");
            return false;
        }

        UIAccess ownerUIAccess = owner == null ? null : owner.getUIAccess();
        UIAccess uiAccess = ownerUIAccess != null ? ownerUIAccess : Application.get().getLastUIAccess();
        if (!uiAccess.isValid()) {
            LOG.warn("No UI attached. Rejecting certificate.");
            return false;
        }

        CountDownLatch proceeded = new CountDownLatch(1);
        AtomicBoolean accepted = new AtomicBoolean();
        AtomicBoolean shown = new AtomicBoolean();
        uiAccess.give(() -> {
            // skip if certificate was already rejected due to timeout or interrupt
            if (proceeded.getCount() == 0) {
                return;
            }
            try {
                DialogDescriptor descriptor = dialogFactory.get();
                DialogService dialogService = Application.get().getInstance(DialogService.class);
                shown.set(true);
                (ownerUIAccess != null ? dialogService.build(owner, descriptor) : dialogService.build(descriptor))
                    .showAsync()
                    .whenComplete((value, error) -> {
                        accepted.set(error == null && value != null);
                        proceeded.countDown();
                    });
            }
            catch (Exception e) {
                LOG.error(e);
                proceeded.countDown();
            }
        });

        try {
            // IDEA-123467 and IDEA-123335 workaround
            boolean inTime = proceeded.await(DIALOG_VISIBILITY_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!inTime) {
                if (!shown.get()) {
                    LOG.debug(
                        "After " + DIALOG_VISIBILITY_TIMEOUT + " ms dialog was not shown. " +
                            "Rejecting certificate. Current thread: " + Thread.currentThread().getName()
                    );
                    proceeded.countDown();
                    return false;
                }
                else {
                    proceeded.await(); // if dialog is already shown continue waiting
                }
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            proceeded.countDown();
        }
        return accepted.get();
    }

    
    @Override
    public HttpCertificateManagerState getState() {
        return myConfig;
    }

    @Override
    public void loadState(HttpCertificateManagerState state) {
        myConfig = state;
    }
}
