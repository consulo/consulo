/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.psi.statistics.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.SettingsSavingComponent;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.util.ScrambledInputStream;
import consulo.ide.impl.idea.util.ScrambledOutputStream;
import consulo.ide.localize.IdeLocalize;
import consulo.language.statistician.StatisticsInfo;
import consulo.language.statistician.StatisticsManager;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ref.SoftReference;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.util.*;

@Singleton
@ServiceImpl
public class StatisticsManagerImpl extends StatisticsManager implements SettingsSavingComponent {
    private static final int UNIT_COUNT = 997;
    private static final Object LOCK = new Object();

    private static final String STORE_PATH = ContainerPathManager.get().getSystemPath() + File.separator + "stat";

    private final List<SoftReference<StatisticsUnit>> myUnits = new ArrayList<>(Collections.nCopies(UNIT_COUNT, null));
    private final Set<StatisticsUnit> myModifiedUnits = new HashSet<>();
    private boolean myTestingStatistics;

    @Override
    public int getUseCount(@Nonnull StatisticsInfo info) {
        if (info == StatisticsInfo.EMPTY) {
            return 0;
        }

        int useCount = 0;

        for (StatisticsInfo conjunct : info.getConjuncts()) {
            useCount = Math.max(doGetUseCount(conjunct), useCount);
        }

        return useCount;
    }

    private int doGetUseCount(StatisticsInfo info) {
        String key1 = info.getContext();
        int unitNumber = getUnitNumber(key1);
        synchronized (LOCK) {
            StatisticsUnit unit = getUnit(unitNumber);
            return unit.getData(key1, info.getValue());
        }
    }

    @Override
    public int getLastUseRecency(@Nonnull StatisticsInfo info) {
        if (info == StatisticsInfo.EMPTY) {
            return 0;
        }

        int recency = Integer.MAX_VALUE;
        for (StatisticsInfo conjunct : info.getConjuncts()) {
            recency = Math.min(doGetRecency(conjunct), recency);
        }
        return recency;
    }

    private int doGetRecency(StatisticsInfo info) {
        String key1 = info.getContext();
        int unitNumber = getUnitNumber(key1);
        synchronized (LOCK) {
            StatisticsUnit unit = getUnit(unitNumber);
            return unit.getRecency(key1, info.getValue());
        }
    }

    @Override
    @RequiredUIAccess
    public void incUseCount(@Nonnull StatisticsInfo info) {
        if (info == StatisticsInfo.EMPTY) {
            return;
        }
        if (Application.get().isUnitTestMode() && !myTestingStatistics) {
            return;
        }

        UIAccess.assertIsUIThread();

        for (StatisticsInfo conjunct : info.getConjuncts()) {
            doIncUseCount(conjunct);
        }
    }

    private void doIncUseCount(StatisticsInfo info) {
        String key1 = info.getContext();
        int unitNumber = getUnitNumber(key1);
        synchronized (LOCK) {
            StatisticsUnit unit = getUnit(unitNumber);
            unit.incData(key1, info.getValue());
            myModifiedUnits.add(unit);
        }
    }

    @Override
    public StatisticsInfo[] getAllValues(@Nonnull String context) {
        String[] strings;
        synchronized (LOCK) {
            strings = getUnit(getUnitNumber(context)).getKeys2(context);
        }
        return ContainerUtil.map2Array(strings, StatisticsInfo.class, s -> new StatisticsInfo(context, s));
    }

    @Override
    @RequiredUIAccess
    public void save() {
        synchronized (LOCK) {
            if (!Application.get().isUnitTestMode()) {
                UIAccess.assertIsUIThread();
                for (StatisticsUnit unit : myModifiedUnits) {
                    saveUnit(unit.getNumber());
                }
            }
            myModifiedUnits.clear();
        }
    }

    private StatisticsUnit getUnit(int unitNumber) {
        StatisticsUnit unit = SoftReference.dereference(myUnits.get(unitNumber));
        if (unit != null) {
            return unit;
        }
        unit = loadUnit(unitNumber);
        if (unit == null) {
            unit = new StatisticsUnit(unitNumber);
        }
        myUnits.set(unitNumber, new SoftReference<>(unit));
        return unit;
    }

    private static StatisticsUnit loadUnit(int unitNumber) {
        StatisticsUnit unit = new StatisticsUnit(unitNumber);
        if (!Application.get().isUnitTestMode()) {
            String path = getPathToUnit(unitNumber);
            try (InputStream in = new ScrambledInputStream(new BufferedInputStream(new FileInputStream(path)))) {
                unit.read(in);
            }
            catch (IOException | WrongFormatException ignored) {
            }
        }
        return unit;
    }

    @RequiredUIAccess
    private void saveUnit(int unitNumber) {
        if (!createStoreFolder()) {
            return;
        }
        StatisticsUnit unit = getUnit(unitNumber);
        String path = getPathToUnit(unitNumber);
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(path));
            out = new ScrambledOutputStream(out);
            try {
                unit.write(out);
            }
            finally {
                out.close();
            }
        }
        catch (IOException e) {
            Messages.showMessageDialog(
                IdeLocalize.errorSavingStatistics(e.getLocalizedMessage()).get(),
                CommonLocalize.titleError().get(),
                UIUtil.getErrorIcon()
            );
        }
    }

    private static int getUnitNumber(String key1) {
        return Math.abs(key1.hashCode() % UNIT_COUNT);
    }

    @RequiredUIAccess
    private static boolean createStoreFolder() {
        File homeFile = new File(STORE_PATH);
        if (!homeFile.exists() && !homeFile.mkdirs()) {
            Messages.showMessageDialog(
                IdeLocalize.errorSavingStatisticFailedToCreateFolder(STORE_PATH).get(),
                CommonLocalize.titleError().get(),
                UIUtil.getErrorIcon()
            );
            return false;
        }
        return true;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    private static String getPathToUnit(int unitNumber) {
        return STORE_PATH + File.separator + "unit." + unitNumber;
    }

    @TestOnly
    public void enableStatistics(@Nonnull Disposable parentDisposable) {
        myTestingStatistics = true;
        Disposer.register(
            parentDisposable,
            () -> {
                synchronized (LOCK) {
                    Collections.fill(myUnits, null);
                }
                myTestingStatistics = false;
            }
        );
    }
}