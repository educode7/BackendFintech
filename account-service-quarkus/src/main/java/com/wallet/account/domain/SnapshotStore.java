package com.wallet.account.domain;

import java.util.Optional;

/**
 * Port: Snapshot store for Account aggregate.
 * <p>
 * Snapshots avoid replaying the full event stream.
 * After loading a snapshot, only events after the snapshot version need replay.
 */
public interface SnapshotStore {

    /**
     * Load the latest snapshot for an aggregate.
     *
     * @param aggregateId account ID
     * @return the latest snapshot, or empty if none exists
     */
    Optional<AccountSnapshot> findLatest(String aggregateId);

    /**
     * Save a snapshot (upserts by aggregateId — only the latest matters).
     *
     * @param snapshot aggregate state at a given version
     */
    void save(AccountSnapshot snapshot);
}
