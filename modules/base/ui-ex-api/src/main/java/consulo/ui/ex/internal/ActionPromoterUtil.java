// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.internal;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import consulo.ui.ex.action.ActionPromoter;
import consulo.ui.ex.action.AnAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ActionPromoterUtil {
    private static final Logger LOG = Logger.getInstance(ActionPromoterUtil.class);

    private ActionPromoterUtil() {
    }

    /**
     * The actions of one shortcut in the order the promoters give them: the extensions and the actions that are
     * promoters themselves move actions first or drop them.
     */
    public static List<AnAction> rearrangeByPromoters(List<AnAction> actions, DataContext dataContext) {
        if (actions.isEmpty()) {
            return actions;
        }
        try {
            List<ActionPromoter> promoters = new ArrayList<>();
            Application.get().getExtensionPoint(ActionPromoter.class).forEach(promoters::add);
            for (AnAction action : actions) {
                if (action instanceof ActionPromoter promoter) {
                    promoters.add(promoter);
                }
            }
            return rearrangeByPromotersImpl(actions, dataContext, promoters);
        }
        catch (Throwable e) {
            LOG.error(e);
            return actions;
        }
    }

    public static List<AnAction> rearrangeByPromotersImpl(List<AnAction> actions, DataContext dataContext, List<ActionPromoter> promoters) {
        if (promoters.isEmpty()) {
            return actions;
        }
        List<AnAction> result = new ArrayList<>(actions);
        List<AnAction> copy = new ArrayList<>(actions);
        boolean updateCopy = false;
        for (ActionPromoter promoter : promoters) {
            if (updateCopy) {
                copy.clear();
                copy.addAll(result);
                updateCopy = false;
            }
            List<AnAction> promoted = promoter.promote(Collections.unmodifiableList(copy), dataContext);
            if (promoted != null && !promoted.isEmpty()) {
                result.removeAll(promoted);
                result.addAll(0, promoted);
                updateCopy = true;
            }
            List<AnAction> suppressed = promoter.suppress(Collections.unmodifiableList(copy), dataContext);
            if (suppressed != null && !suppressed.isEmpty()) {
                result.removeAll(suppressed);
                updateCopy = true;
            }
        }
        return result;
    }
}
