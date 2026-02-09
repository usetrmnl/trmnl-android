package ink.trmnl.android.network

/**
 * Interface for sleeping/delaying execution.
 * Allows injection of fake implementations for testing.
 */
interface Sleeper {
    /**
     * Sleeps for the specified duration in milliseconds.
     * @param durationMs Duration to sleep in milliseconds
     * @throws InterruptedException if interrupted while sleeping
     */
    @Throws(InterruptedException::class)
    fun sleep(durationMs: Long)
}

/**
 * Production implementation that uses Thread.sleep().
 */
class ThreadSleeper : Sleeper {
    override fun sleep(durationMs: Long) {
        Thread.sleep(durationMs)
    }
}

/**
 * Fake sleeper for testing that doesn't actually sleep.
 * Records sleep calls for verification.
 */
class FakeSleeper : Sleeper {
    val sleepCalls = mutableListOf<Long>()

    override fun sleep(durationMs: Long) {
        sleepCalls.add(durationMs)
        // Don't actually sleep - tests run instantly
    }

    fun reset() {
        sleepCalls.clear()
    }
}
