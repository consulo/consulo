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
package consulo.web.internal.servlet;

import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.ServletContext;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public final class BuiltInWebServerSessionTokens {
    private static final String CONTEXT_ATTRIBUTE = BuiltInWebServerSessionTokens.class.getName();
    private static final String SESSION_ATTRIBUTE = BuiltInWebServerSessionTokens.class.getName() + ".token";

    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    public static BuiltInWebServerSessionTokens get(ServletContext context) {
        if (context.getAttribute(CONTEXT_ATTRIBUTE) instanceof BuiltInWebServerSessionTokens existing) {
            return existing;
        }

        synchronized (BuiltInWebServerSessionTokens.class) {
            if (context.getAttribute(CONTEXT_ATTRIBUTE) instanceof BuiltInWebServerSessionTokens existing) {
                return existing;
            }

            BuiltInWebServerSessionTokens tokens = new BuiltInWebServerSessionTokens();
            context.setAttribute(CONTEXT_ATTRIBUTE, tokens);
            return tokens;
        }
    }

    private final Set<String> myTokens = ConcurrentHashMap.newKeySet();

    private BuiltInWebServerSessionTokens() {
    }

    public String acquire(VaadinSession session) {
        if (session.getAttribute(SESSION_ATTRIBUTE) instanceof String existing) {
            return existing;
        }

        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        myTokens.add(token);
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }

    void revoke(VaadinSession session) {
        if (session.getAttribute(SESSION_ATTRIBUTE) instanceof String token) {
            myTokens.remove(token);
            session.setAttribute(SESSION_ATTRIBUTE, null);
        }
    }

    boolean isValid(String token) {
        return myTokens.contains(token);
    }
}
