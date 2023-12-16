package ro.rs.crypto.service;

import org.apache.commons.lang3.stream.Streams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.rs.crypto.api.dto.NormalizedValue;
import ro.rs.crypto.model.CryptoPrice;
import ro.rs.crypto.store.CryptoPriceRepository;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CryptoPriceServiceTest {
    @InjectMocks
    private CryptoPriceService cryptoPriceService;
    @Mock
    private CryptoPriceRepository cryptoPriceRepository;

    @Test
    void readAllCryptoPricesTest() {
        Mockito.when(cryptoPriceRepository.findByCryptoSymbol("BTC")).thenReturn(createBTCPrices());
        List<CryptoPrice> btcPrices = cryptoPriceRepository.findByCryptoSymbol("BTC");
        assertThat(btcPrices).hasSize(4);
    }

    @Test
    void whenPathIsWrong_readAllCryptoPricesTest() {
        Path pricesPath = Paths.get("whatever");
        cryptoPriceService.readAndStoreAllCryptoPrices(pricesPath);
        Mockito.verify(cryptoPriceRepository, Mockito.times(1)).findSupportedSymbols();
        Mockito.verifyNoMoreInteractions(cryptoPriceRepository);
    }

    @Test
    void whenPathIsNotADir_readAllCryptoPricesErrorTest() throws URISyntaxException {
        Path pricesPath = Paths.get(this.getClass().getClassLoader().getResource("application.yml").toURI());
        cryptoPriceService.readAndStoreAllCryptoPrices(pricesPath);
        Mockito.verify(cryptoPriceRepository, Mockito.times(1)).findSupportedSymbols();
        Mockito.verifyNoMoreInteractions(cryptoPriceRepository);
    }

    @Test
    void whenPathIsEmptyDir_readAllCryptoPricesErrorTest() throws URISyntaxException {
        Path pricesPath = Paths.get(this.getClass().getClassLoader().getResource("emptyDir").toURI());
        cryptoPriceService.readAndStoreAllCryptoPrices(pricesPath);
        Mockito.verify(cryptoPriceRepository, Mockito.times(1)).findSupportedSymbols();
        Mockito.verifyNoMoreInteractions(cryptoPriceRepository);
    }

    @Test
    void calculateOldestTest() {
        Mockito.when(cryptoPriceRepository.findByCryptoSymbol("BTC")).thenReturn(createBTCPrices());
        Optional<CryptoPrice> oldest = cryptoPriceService.calculateOldest("BTC");
        assertThat(oldest).isNotEmpty();
        assertThat(oldest.get().getUsdPrice()).isEqualTo(new BigDecimal("46813.21"));
    }

    @Test
    void calculateNewestTest() {
        Mockito.when(cryptoPriceRepository.findByCryptoSymbol("BTC")).thenReturn(createBTCPrices());
        Optional<CryptoPrice> newest = cryptoPriceService.calculatedNewest("BTC");
        assertThat(newest).isNotEmpty();
        assertThat(newest.get().getUsdPrice()).isEqualTo(new BigDecimal("38415.79"));
    }

    @Test
    void calculateMinTest() {
        Mockito.when(cryptoPriceRepository.findByCryptoSymbol("BTC")).thenReturn(createBTCPrices());
        Optional<CryptoPrice> oldest = cryptoPriceService.calculatedMin("BTC");
        assertThat(oldest).isNotEmpty();
        assertThat(oldest.get().getUsdPrice()).isEqualTo(new BigDecimal("37300.31"));
    }

    @Test
    void calculateMaxTest() {
        Mockito.when(cryptoPriceRepository.findByCryptoSymbol("BTC")).thenReturn(createBTCPrices());
        Optional<CryptoPrice> newest = cryptoPriceService.calculatedMax("BTC");
        assertThat(newest).isNotEmpty();
        assertThat(newest.get().getUsdPrice()).isEqualTo(new BigDecimal("46979.61"));
    }

    @Test
    void cryptoPricesByNormalizedValueTest() throws URISyntaxException {
        Mockito.when(cryptoPriceRepository.findByCryptoSymbol("BTC")).thenReturn(createBTCPrices());
        Mockito.when(cryptoPriceRepository.findByCryptoSymbol("ETH")).thenReturn(createETHPrices());

        Path pricesPath = Paths.get(this.getClass().getClassLoader().getResource("prices").toURI());
        cryptoPriceService.readAndStoreAllCryptoPrices(pricesPath);
        List<NormalizedValue> prices = cryptoPriceService.cryptoPricesByNormalizedRange();
        assertThat(prices).hasSize(2);
        assertThat(prices.get(0).getValue()).isGreaterThan(prices.get(1).getValue());
    }

    @Test
    void highestNormalizedPerDayTest() {
        List<CryptoPrice> pricesForDay = createPricesForDay();
        LocalDate date = LocalDate.parse("20220101", DateTimeFormatter.BASIC_ISO_DATE);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusHours(23).plusMinutes(59).plusSeconds(59);
        Mockito.when(cryptoPriceRepository.findByDate(start, end)).thenReturn(pricesForDay);

        String s = cryptoPriceService.highestNormalizedPerDay(date);
        assertThat(s).isEqualTo("ETH");
//test no values for day
        s = cryptoPriceService.highestNormalizedPerDay(LocalDate.parse("20230101", DateTimeFormatter.BASIC_ISO_DATE));
        assertThat(s).isEmpty();
    }

    @Test
    void testIsSymbolSupported() throws URISyntaxException {
        Path pricesPath = Paths.get(this.getClass().getClassLoader().getResource("prices").toURI());
        cryptoPriceService.readAndStoreAllCryptoPrices(pricesPath);
        assertThat(cryptoPriceService.isCryptoSupported("BTC")).isTrue();
        assertThat(cryptoPriceService.isCryptoSupported("SMT")).isFalse();
    }

    public static List<CryptoPrice> createBTCPrices() {
        return Streams.of("1641009600000,BTC,46813.21",
                        "1641020400000,BTC,46979.61",
                        "1643626800000,BTC,37300.31",
                        "1643659200000,BTC,38415.79")
                .map(CryptoPrice::fromCSVLine)
                .toList();
    }

    public static List<CryptoPrice> createETHPrices() {
        return Streams.of("1641024000000,ETH,3715.32",
                        "1641031200000,ETH,3718.67",
                        "1643634000000,ETH,2540.2",
                        "1643659200000,ETH,2672.5")
                .map(CryptoPrice::fromCSVLine)
                .toList();
    }

    public List<CryptoPrice> createPricesForDay() {
        return Streams.of("1641009600000,BTC,46813.21",
                        "1641020400000,BTC,46979.61",
                        "1641024000000,ETH,3715.32",
                        "1641031200000,ETH,3718.67")
                .map(CryptoPrice::fromCSVLine)
                .toList();
    }

}
