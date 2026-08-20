package com.wallet.shared.util;

import com.fasterxml.uuid.Generators;

import java.util.UUID;

/**
 * UUID v7 generator: time-ordered (k-sortable), 128-bit, collision-resistant.
 * Why v7: monotonic IDs make excellent Postgres primary keys (no hot-spot inserts)
 * and let consumers of Kafka events sort by eventId without reading a timestamp.
 *
 * Uses com.fasterxml.uuid.Generators.timeBasedEpochGenerator().
 * (Java 21 itself does not yet ship a stable v7 generator.)
 */
public final class IdGenerator {

    private static final com.fasterxml.uuid.impl.TimeBasedEpochGenerator V7 =
        Generators.timeBasedEpochGenerator();

    private IdGenerator() { }

    public static String newId() {
        UUID uuid = V7.generate();
        return uuid.toString();
    }
}
