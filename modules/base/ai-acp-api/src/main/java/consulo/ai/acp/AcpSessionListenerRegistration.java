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
package consulo.ai.acp;

import consulo.ai.AIChatSessionListener;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A listener of a session of an account - attached to every agent process the account runs, so it outlives a restart
 * of the agent.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AcpSessionListenerRegistration {
    private final String mySessionId;
    private final AIChatSessionListener myListener;
    private final Map<AcpAgent, Runnable> myAttached = new IdentityHashMap<>();
    private boolean myRemoved;

    AcpSessionListenerRegistration(String sessionId, AIChatSessionListener listener) {
        mySessionId = sessionId;
        myListener = listener;
    }

    synchronized void attach(AcpAgent agent) {
        if (myRemoved || myAttached.containsKey(agent)) {
            return;
        }
        myAttached.put(agent, agent.addSessionListener(mySessionId, myListener));
    }

    void remove() {
        List<Runnable> actions;
        synchronized (this) {
            myRemoved = true;
            actions = new ArrayList<>(myAttached.values());
            myAttached.clear();
        }
        actions.forEach(Runnable::run);
    }
}
