package ro.rs.crypto.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class PairTest {
    @Test
    void testPair() {
        Pair<String, Integer> pair = new Pair<>("test", 1);
        Assertions.assertThat(pair.getFirst()).isEqualTo("test");
        Assertions.assertThat(pair.getSecond()).isEqualTo(1);
    }
}
