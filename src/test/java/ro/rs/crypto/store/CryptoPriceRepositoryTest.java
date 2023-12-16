package ro.rs.crypto.store;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ro.rs.crypto.model.CryptoPrice;
import  org.apache.commons.lang3.stream.Streams;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CryptoPriceRepositoryTest {
    @Autowired
    private CryptoPriceRepository cryptoPriceRepository;

    @Test
    void testFindBySymbol() {
        cryptoPriceRepository.saveAll(createBTCPrices());
        List<CryptoPrice> cryptoPrices = cryptoPriceRepository.findByCryptoSymbol("BTC");
        
        assertThat(cryptoPrices).hasSize(4);
    }

    @Test
    void testFindByDate() {
        cryptoPriceRepository.saveAll(createBTCPrices());
        LocalDate parse = LocalDate.parse("20220101", DateTimeFormatter.BASIC_ISO_DATE);
        LocalDateTime start = parse.atStartOfDay();
        LocalDateTime end = start.plusHours(23).plusMinutes(59).plusSeconds(59);
        List<CryptoPrice> cryptoPrices = cryptoPriceRepository.findByDate(start, end);

        assertThat(cryptoPrices).hasSize(2);
    }

    public static List<CryptoPrice> createBTCPrices() {
        return Streams.of("1641009600000,BTC,46813.21",
                        "1641020400000,BTC,46979.61",
                        "1643626800000,BTC,37300.31",
                        "1643659200000,BTC,38415.79")
                .map(CryptoPrice::fromCSVLine)
                .toList();
    }
}
