// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.combined;

import consulo.diff.chain.DiffRequestProducer;

public class CombinedBlockProducer {
    private final CombinedBlockId myId;
    private final DiffRequestProducer myProducer;

    public CombinedBlockProducer(CombinedBlockId id, DiffRequestProducer producer) {
        myId = id;
        myProducer = producer;
    }

    public CombinedBlockId getId() {
        return myId;
    }

    public DiffRequestProducer getProducer() {
        return myProducer;
    }
}
