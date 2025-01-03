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
package consulo.execution.debug.impl.internal;

import consulo.component.persist.PersistentStateComponent;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.breakpoint.XExpressionState;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author egor
 */
public class XDebuggerWatchesManager implements PersistentStateComponent<XDebuggerWatchesManager.WatchesManagerState> {
    private final Map<String, XExpression[]> watches = new ConcurrentHashMap<>();

    @Nonnull
    public XExpression[] getWatches(String confName) {
        XExpression[] expressions = watches.get(confName);
        if (expressions == null) {
            return new XExpression[0];
        }
        return expressions;
    }

    public void setWatches(String configurationName, XExpression[] expressions) {
        if (expressions != null && expressions.length > 0) {
            watches.put(configurationName, expressions);
        }
        else {
            watches.remove(configurationName);
        }
    }

    @Override
    public WatchesManagerState getState() {
        WatchesManagerState state = new WatchesManagerState();
        for (Map.Entry<String, XExpression[]> entry : watches.entrySet()) {
            state.expressions.add(new ConfigurationState(entry.getKey(), entry.getValue()));
        }
        return state;
    }

    @Override
    public void loadState(WatchesManagerState state) {
        watches.clear();
        if (state != null) {
            for (ConfigurationState expressionState : state.expressions) {
                WatchState[] states = expressionState.myExpressionStates;
                XExpression[] expressions = new XExpression[states.length];
                for (int i = 0; i < states.length; i++) {
                    expressions[i] = states[i].toXExpression();
                }
                watches.put(expressionState.myName, expressions);
            }
        }
    }

    @Tag("watches-manager")
    public static class WatchesManagerState {
        @Property(surroundWithTag = false)
        @AbstractCollection(surroundWithTag = false)
        public List<ConfigurationState> expressions = new ArrayList<ConfigurationState>();
    }

    @Tag("configuration")
    public static class ConfigurationState {
        @Attribute("name")
        public String myName;

        @Property(surroundWithTag = false)
        @AbstractCollection(surroundWithTag = false)
        public WatchState[] myExpressionStates;

        public ConfigurationState() {
        }

        public ConfigurationState(String name, XExpression[] expressions) {
            this.myName = name;
            myExpressionStates = new WatchState[expressions.length];
            for (int i = 0; i < expressions.length; i++) {
                myExpressionStates[i] = new WatchState(expressions[i]);
            }
        }
    }

    @Tag("watch")
    public static class WatchState extends XExpressionState {
        public WatchState() {
        }

        public WatchState(XExpression expression) {
            super(expression);
        }
    }
}
