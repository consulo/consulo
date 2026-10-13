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
package consulo.web.internal.credential;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.web.ui.impl.internal.WebApplicationImpl;
import consulo.web.ui.impl.internal.WebUIAccessImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Encrypts and decrypts with a key which never leaves the browser: an AES-GCM key created by Web Crypto as
 * non-extractable and kept in the IndexedDB of the page origin. Any attached page of that browser can do the work,
 * and when none is attached the request waits for the next one.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class WebBrowserCrypto {
    private static final Logger LOG = Logger.getInstance(WebBrowserCrypto.class);

    private static final long BROWSER_TIMEOUT_SECONDS = 30;

    private static final String SCRIPT = """
        const api = window.__consuloCredentials || (window.__consuloCredentials = (() => {
          const DB = 'consulo-credentials', STORE = 'keys', ID = 'main';
          const request = r => new Promise((resolve, reject) => {
            r.onsuccess = () => resolve(r.result);
            r.onerror = () => reject(r.error);
          });
          const openDb = () => {
            const r = indexedDB.open(DB, 1);
            r.onupgradeneeded = () => r.result.createObjectStore(STORE);
            return request(r);
          };
          const loadKey = async () => {
            if (!window.isSecureContext || !crypto.subtle) {
              throw new Error('Web Crypto needs a secure page (https or localhost)');
            }
            const db = await openDb();
            try {
              const existing = await request(db.transaction(STORE).objectStore(STORE).get(ID));
              if (existing) {
                return existing;
              }
              const created = await crypto.subtle.generateKey({name: 'AES-GCM', length: 256}, false, ['encrypt', 'decrypt']);
              try {
                await request(db.transaction(STORE, 'readwrite').objectStore(STORE).add(created, ID));
                return created;
              }
              catch (e) {
                return await request(db.transaction(STORE).objectStore(STORE).get(ID));
              }
            }
            finally {
              db.close();
            }
          };
          let keyPromise = null;
          const key = () => keyPromise || (keyPromise = loadKey().catch(e => {
            keyPromise = null;
            throw e;
          }));
          const toBase64 = bytes => {
            let text = '';
            bytes.forEach(b => text += String.fromCharCode(b));
            return btoa(text);
          };
          const fromBase64 = text => Uint8Array.from(atob(text), c => c.charCodeAt(0));
          return {
            encrypt: async text => {
              const iv = crypto.getRandomValues(new Uint8Array(12));
              const data = new Uint8Array(await crypto.subtle.encrypt({name: 'AES-GCM', iv}, await key(), new TextEncoder().encode(text)));
              const out = new Uint8Array(iv.length + data.length);
              out.set(iv);
              out.set(data, iv.length);
              return toBase64(out);
            },
            decrypt: async text => {
              const bytes = fromBase64(text);
              const plain = await crypto.subtle.decrypt({name: 'AES-GCM', iv: bytes.subarray(0, 12)}, await key(), bytes.subarray(12));
              return new TextDecoder().decode(plain);
            }
          };
        })());
        return api[$0]($1);
        """;

    private record Request(String function, String argument, CompletableFuture<String> result) {
    }

    private final Application myApplication;
    private final Deque<Request> myPending = new ArrayDeque<>();

    @Inject
    public WebBrowserCrypto(Application application) {
        myApplication = application;
        if (application instanceof WebApplicationImpl webApplication) {
            webApplication.addUIAccessListener(uiAccess -> drainPending());
        }
    }

    public CompletableFuture<String> encrypt(String text) {
        return call("encrypt", text);
    }

    public CompletableFuture<String> decrypt(String text) {
        return call("decrypt", text);
    }

    private CompletableFuture<String> call(String function, String argument) {
        Request request = new Request(function, argument, new CompletableFuture<>());
        dispatch(request);
        return request.result();
    }

    private void dispatch(Request request) {
        while (true) {
            UI ui = findUI();
            if (ui == null) {
                synchronized (myPending) {
                    myPending.add(request);
                }
                if (findUI() != null) {
                    drainPending();
                }
                return;
            }

            try {
                ui.access(() -> ui.getPage().executeJs(SCRIPT, request.function(), request.argument()).then(
                    String.class,
                    request.result()::complete,
                    error -> request.result().completeExceptionally(new IOException(error))
                ));
                request.result().orTimeout(BROWSER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return;
            }
            catch (UIDetachedException e) {
                LOG.warn("Browser page went away before the credential request reached it, trying another one");
            }
        }
    }

    private void drainPending() {
        while (true) {
            Request request;
            synchronized (myPending) {
                request = myPending.poll();
            }
            if (request == null) {
                return;
            }
            if (findUI() == null) {
                synchronized (myPending) {
                    myPending.addFirst(request);
                }
                return;
            }
            dispatch(request);
        }
    }

    private @Nullable UI findUI() {
        if (!(myApplication instanceof WebApplicationImpl webApplication)) {
            return null;
        }
        for (UIAccess uiAccess : webApplication.getUIAccesses().reversed()) {
            if (uiAccess instanceof WebUIAccessImpl webUIAccess && webUIAccess.isValid()) {
                return webUIAccess.getUI();
            }
        }
        return null;
    }
}
