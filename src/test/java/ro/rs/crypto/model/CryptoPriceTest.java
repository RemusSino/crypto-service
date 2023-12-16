package ro.rs.crypto.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class CryptoPriceTest {
    @Test
    void testCryptoTest() {
        LocalDateTime now = LocalDateTime.now();
        CryptoPrice cp1 = new CryptoPrice(now, "BTC", new BigDecimal("100.10"));

        Assertions.assertThat(cp1.getPriceTimestamp()).isEqualTo(now);
        Assertions.assertThat(cp1.getCryptoSymbol()).isEqualTo("BTC");
        Assertions.assertThat(cp1.getUsdPrice()).isEqualTo(new BigDecimal("100.10"));
    }

    @Test
    void testCreateFromCsv() {
        long now = System.currentTimeMillis();
        CryptoPrice cp1 = CryptoPrice.fromCSVLine(now + ",BTC,100.10");

        Assertions.assertThat(cp1.getPriceTimestamp()).isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochMilli(now),
                ZoneId.of("UTC")));
        Assertions.assertThat(cp1.getCryptoSymbol()).isEqualTo("BTC");
        Assertions.assertThat(cp1.getUsdPrice()).isEqualTo(new BigDecimal("100.10"));
    }

    @Test
    void testCreateFailsFromBadFormatCsv() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> CryptoPrice.fromCSVLine("BTC,100.10"));
    }
}
