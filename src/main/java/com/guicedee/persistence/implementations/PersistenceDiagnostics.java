package com.guicedee.persistence.implementations;

import com.guicedee.client.Environment;
import lombok.extern.log4j.Log4j2;
import java.util.function.Supplier;

/** Optional inclusive bootstrap spans, emitted only for an explicitly instrumented launch. */
@Log4j2
public final class PersistenceDiagnostics {
    private static final boolean ENABLED = !Environment.getSystemPropertyOrEnvironment("GUICEDEE_DIAGNOSTICS_DIR", "").isBlank();
    private PersistenceDiagnostics() {}

    public static <T> T measure(String phase, String unit, Supplier<T> work) {
        if (!ENABLED) return work.get();
        long start = System.nanoTime();
        boolean success = false;
        try {
            T result = work.get();
            success = true;
            return result;
        } finally {
            log.info("[PERSISTENCE-PHASE] phase={} unit={} durationNs={} success={}",
                    phase, unit, System.nanoTime() - start, success);
        }
    }
}
