// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.autoimport.update;

import consulo.ui.ex.awt.util.Update;

public abstract class PriorityEatUpdate extends Update {
    protected PriorityEatUpdate(int priority) {
        super(priority, priority);
    }

    @Override
    public boolean canEat(Update update) {
        if (!(update instanceof PriorityEatUpdate)) {
            return false;
        }
        return getPriority() <= update.getPriority();
    }

    public static PriorityEatUpdate create(int priority, Runnable update) {
        return new PriorityEatUpdate(priority) {
            @Override
            public void run() {
                update.run();
            }
        };
    }
}
