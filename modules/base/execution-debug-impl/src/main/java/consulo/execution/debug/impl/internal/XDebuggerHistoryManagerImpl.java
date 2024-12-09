/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.annotation.component.ServiceImpl;
import consulo.execution.debug.XDebuggerHistoryManager;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.internal.breakpoint.XExpressionImpl;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class XDebuggerHistoryManagerImpl implements XDebuggerHistoryManager {
    public static final int MAX_RECENT_EXPRESSIONS = 10;
    private final Map<String, LinkedList<XExpression>> myRecentExpressions = new HashMap<>();

    public static XDebuggerHistoryManagerImpl getInstance(@Nonnull Project project) {
        return project.getInstance(XDebuggerHistoryManagerImpl.class);
    }

    @Override
    public boolean addRecentExpression(@Nonnull String id, @Nullable XExpression expression) {
        if (expression == null || StringUtil.isEmptyOrSpaces(expression.getExpression())) {
            return false;
        }

        LinkedList<XExpression> list = myRecentExpressions.get(id);
        if (list == null) {
            list = new LinkedList<>();
            myRecentExpressions.put(id, list);
        }
        if (list.size() == MAX_RECENT_EXPRESSIONS) {
            list.removeLast();
        }

        XExpression trimmedExpression = new XExpressionImpl(expression.getExpression().trim(), expression.getLanguage(), expression.getCustomInfo());
        list.remove(trimmedExpression);
        list.addFirst(trimmedExpression);
        return true;
    }

    @Nonnull
    @Override
    public List<XExpression> getRecentExpressions(@Nonnull String id) {
        LinkedList<XExpression> list = myRecentExpressions.get(id);
        return list != null ? list : Collections.<XExpression>emptyList();
    }
}
