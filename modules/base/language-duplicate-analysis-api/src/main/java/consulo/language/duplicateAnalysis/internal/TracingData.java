package consulo.language.duplicateAnalysis.internal;

import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.index.io.PersistentHashMap;
import consulo.language.duplicateAnalysis.util.PsiFragment;
import consulo.util.collection.primitive.ints.IntIntMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.util.lang.ShutDownTracker;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TracingData {
  private static final String tracingDataLocation = "E:\\ultimate\\system\\occurrences";
  private final PersistentHashMap<Integer, Integer> keys;
  private final ScheduledThreadPoolExecutor pool = ConcurrencyUtil.newSingleScheduledThreadExecutor("My flushing thread");

  private final AtomicInteger maxHash = new AtomicInteger();
  private final AtomicInteger maxValue = new AtomicInteger();
  private ScheduledFuture<?> flushingFuture;

  TracingData() {
    PersistentHashMap<Integer, Integer> lkeys = null;
    try {
      lkeys = createOrOpenMap();
      flushingFuture = pool.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
          if (keys.isDirty() && !keys.isClosed()) {
            keys.force();
          }
        }
      }, 5, 5, TimeUnit.SECONDS);
      ShutDownTracker.getInstance().registerShutdownTask(() -> flushingFuture.cancel(false));
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
    keys = lkeys;
  }

  private static PersistentHashMap<Integer, Integer> createOrOpenMap() throws IOException {
    return new PersistentHashMap<>(new File(tracingDataLocation), EnumeratorIntegerDescriptor.INSTANCE, EnumeratorIntegerDescriptor.INSTANCE);
  }

  public void record(int hash, int cost, PsiFragment frag) {
    //System.out.println(frag.getElements()[0].getText());

    if (keys != null) {
      try {
        Integer integer = keys.get(hash);
        int value = integer != null ? integer + 1 : 1;
        keys.put(hash, value);
        int currentMaxValue = maxValue.get();
        while (value > currentMaxValue) {
          if (maxValue.compareAndSet(currentMaxValue, value)) {
            maxHash.set(hash);
            System.out.println(maxValue + "," + maxHash + "," + frag.getElements()[0].getText());
            break;
          }
          currentMaxValue = maxValue.get();
        }
      }
      catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  public static void main(String[] args) throws IOException {
    PersistentHashMap<Integer, Integer> lkeys = createOrOpenMap();
    List<Integer> mapping = (List<Integer>)lkeys.getAllKeysWithExistingMapping();
    System.out.println(mapping.size());
    IntIntMap map = IntMaps.newIntIntHashMap(mapping.size());
    for (Integer i : mapping) map.putInt(i, lkeys.get(i));

    Collections.sort(mapping, (o1, o2) -> map.getInt(o2) - map.getInt(o1));

    for (int i = 0; i < 500; ++i) {
      //System.out.println(mapping.get(i) + ",");
      System.out.println(mapping.get(i) + ":" + map.getInt(mapping.get(i)));
    }
    lkeys.close();
  }
}
