package ro.rs.crypto.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Utility value class to hold a pair of values
 * @param <F> - Type of fist value
 * @param <S> - Type of second value
 */
@AllArgsConstructor
@Getter
public final class Pair<F, S> {
    private final F first;
    private final S second;
}

