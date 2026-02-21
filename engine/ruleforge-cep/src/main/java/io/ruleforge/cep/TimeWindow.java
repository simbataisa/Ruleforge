package io.ruleforge.cep;

import java.time.Duration;

/**
 * Defines a time window for CEP pattern matching.
 *
 * @param duration   the window duration
 * @param type       the window type (tumbling, sliding, session)
 * @param graceTime  additional time to allow for late-arriving events
 */
public record TimeWindow(
        Duration duration,
        WindowType type,
        Duration graceTime
) {
    public TimeWindow {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Window duration must be positive");
        }
        if (type == null) {
            type = WindowType.TUMBLING;
        }
        if (graceTime == null) {
            graceTime = Duration.ZERO;
        }
    }

    public static TimeWindow tumbling(Duration duration) {
        return new TimeWindow(duration, WindowType.TUMBLING, Duration.ZERO);
    }

    public static TimeWindow sliding(Duration duration) {
        return new TimeWindow(duration, WindowType.SLIDING, Duration.ZERO);
    }

    public static TimeWindow session(Duration duration) {
        return new TimeWindow(duration, WindowType.SESSION, Duration.ZERO);
    }

    public enum WindowType {
        TUMBLING,
        SLIDING,
        SESSION
    }
}
