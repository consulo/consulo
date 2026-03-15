// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.application.util.query;

import consulo.application.util.function.Processor;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncFuture;
import consulo.util.concurrent.AsyncUtil;


import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * @author max
 */
public class CollectionQuery<T> implements Query<T> {
    private final Collection<T> myCollection;

    public CollectionQuery(Collection<T> collection) {
        myCollection = collection;
    }

    @Override
    
    public Collection<T> findAll() {
        return myCollection;
    }

    @Override
    public T findFirst() {
        Iterator<T> i = iterator();
        return i.hasNext() ? i.next() : null;
    }

    @Override
    public boolean forEach(Predicate<? super T> consumer) {
        return ContainerUtil.process(myCollection, consumer);
    }

    
    @Override
    public AsyncFuture<Boolean> forEachAsync(Predicate<? super T> consumer) {
        return AsyncUtil.wrapBoolean(forEach(consumer));
    }

    
    @Override
    public T[] toArray(T[] a) {
        return findAll().toArray(a);
    }

    
    @Override
    public Iterator<T> iterator() {
        return myCollection.iterator();
    }
}
