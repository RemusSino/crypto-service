package ro.rs.crypto.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ro.rs.crypto.api.dto.NormalizedValue;
import ro.rs.crypto.model.CryptoPrice;
import ro.rs.crypto.store.CryptoPriceRepository;
import ro.rs.crypto.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Service class for CryptoService operations.
 * For computation methods, the operations are done only first time and then saved(cached) for latter calls.
 */
@Component
@Slf4j
public class CryptoPriceService {
    private final Pattern priceFileNamePattern = Pattern.compile("^.+_values\\.csv$");
    private final CryptoPriceRepository cryptoPriceRepository;
    private final Map<String, CryptoPrice> oldestCryptoPriceBySymbol,
            newestCryptoPriceBySymbol,
            minCryptoPriceBySymbol,
            maxCryptoPriceBySymbol;
    private List<NormalizedValue> cryptoPricesByNormalizedRange;
    private final Set<String> cryptoSymbols;

    public CryptoPriceService(final CryptoPriceRepository cryptoPriceRepository) {
        this.cryptoPriceRepository = cryptoPriceRepository;
        this.oldestCryptoPriceBySymbol = new HashMap<>();
        this.newestCryptoPriceBySymbol = new HashMap<>();
        this.minCryptoPriceBySymbol = new HashMap<>();
        this.maxCryptoPriceBySymbol = new HashMap<>();
        this.cryptoSymbols = cryptoPriceRepository.findSupportedSymbols();
    }

    /**
     * Read all the csv files containing crypto values, from the given prices directory
     *
     * @param pricesDir - directory of the csv files with the prices values
     */
    public void readAndStoreAllCryptoPrices(Path pricesDir) {
        if (!pricesDir.toFile().exists()) {
            log.error("Path {} doesn't exist", pricesDir);
            return;
        }

        if (!pricesDir.toFile().isDirectory()) {
            log.error("Path {} is not a directory", pricesDir);
            return;
        }

        File[] priceFiles = pricesDir.toFile().listFiles();
        if (priceFiles == null) {
            log.error("No price file found found");
            return;
        }

        for (File priceFile : priceFiles) {
            try {
                List<CryptoPrice> cryptoPrices = this.readCryptoFromCsv(priceFile);
                if (!isEmpty(cryptoPrices)) {
                    cryptoPriceRepository.saveAll(cryptoPrices);
                    cryptoSymbols.add(cryptoPrices.get(0).getCryptoSymbol());
                }
            } catch (IOException e) {
                log.error("Error when reading file {} ", priceFile.getName(), e);
            }
        }
    }

    /**
     * Reads one csv file containing crypto values
     *
     * @param priceFile
     * @return returns the list of Crypto values
     * @throws IOException              if the priceFile does not exist of is empty
     * @throws IllegalArgumentException if the file name doesn't respect the naming convention SYMBOL_values.csv (i.e. BTC_values.csv)
     */
    public List<CryptoPrice> readCryptoFromCsv(File priceFile) throws IOException {
        if (!priceFile.exists() || !priceFile.canRead() || priceFile.length() == 0) {
            throw new IOException("File " + priceFile.getName() + " does not exist or is empty");
        }
        Matcher matcher = priceFileNamePattern.matcher(priceFile.getName());
        if (!matcher.find()) {
            throw new IllegalArgumentException("File name doesn't match pattern SYMBOL_values.csv");
        }

        List<CryptoPrice> cryptoPrices = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(priceFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("timestamp")) {
                    continue;
                }
                CryptoPrice cryptoPrice = CryptoPrice.fromCSVLine(line);
                cryptoPrices.add(cryptoPrice);
            }
        }

        return cryptoPrices;
    }

    /**
     * Return the oldest value for a given symbol.
     *
     * @param symbol
     * @return if the symbol is found in the store, calculates and returns the oldest.
     * *               The method only calculates the oldest once and saves it; in the subsequent calls, it just retrieves it.
     */
    public Optional<CryptoPrice> calculateOldest(String symbol) {
        List<CryptoPrice> cryptoPrices = cryptoPriceRepository.findByCryptoSymbol(symbol);
        if (isEmpty(cryptoPrices)) {
            return Optional.empty();
        } else {
            return Optional.of(oldestCryptoPriceBySymbol.computeIfAbsent(symbol,
                    s -> cryptoPrices.stream().min(Comparator.comparing(CryptoPrice::getPriceTimestamp)).get()));
        }
    }

    /**
     * Return the newest value for a given symbol.
     *
     * @param symbol
     * @return if the symbol is found in the store, calculates and returns the newest.
     * *               The method only calculates once and saves it; in the subsequent calls, it just retrieves it.
     */
    public Optional<CryptoPrice> calculatedNewest(String symbol) {
        List<CryptoPrice> cryptoPrices = cryptoPriceRepository.findByCryptoSymbol(symbol);
        if (isEmpty(cryptoPrices)) {
            return Optional.empty();
        } else {
            return Optional.of(newestCryptoPriceBySymbol.computeIfAbsent(symbol,
                    s -> cryptoPrices.stream().max(Comparator.comparing(CryptoPrice::getPriceTimestamp)).get()));
        }
    }

    /**
     * Return the max value for a given crypto symbol.
     *
     * @param symbol
     * @return if the symbol is found in the store, calculates and returns the oldest.
     * *               The method only calculates once and saves it; in the subsequent calls, it just retrieves it.
     */
    public Optional<CryptoPrice> calculatedMax(String symbol) {
        List<CryptoPrice> cryptoPrices = cryptoPriceRepository.findByCryptoSymbol(symbol);
        if (isEmpty(cryptoPrices)) {
            return Optional.empty();
        } else {
            return Optional.of(maxCryptoPriceBySymbol.computeIfAbsent(symbol,
                    s -> cryptoPrices.stream().max(Comparator.comparing(CryptoPrice::getUsdPrice)).get()));
        }
    }

    /**
     * Return the min value for a given crypto symbol.
     *
     * @param symbol
     * @return if the symbol is found in the store, calculates and returns the oldest.
     * *               The method only calculates once and saves it; in the subsequent calls, it just retrieves it.
     */
    public Optional<CryptoPrice> calculatedMin(String symbol) {
        List<CryptoPrice> cryptoPrices = cryptoPriceRepository.findByCryptoSymbol(symbol);
        if (isEmpty(cryptoPrices)) {
            return Optional.empty();
        } else {
            return Optional.of(minCryptoPriceBySymbol.computeIfAbsent(symbol,
                    s -> cryptoPrices.stream().min(Comparator.comparing(CryptoPrice::getUsdPrice)).get()));
        }
    }

    /**
     * Retrieves a descending sorted list of all the stored crypto symbols, comparing by the normalized range (i.e. (max-min)/min)).
     *
     * @return
     */
    public List<NormalizedValue> cryptoPricesByNormalizedRange() {
        if (cryptoPricesByNormalizedRange != null) {
            return cryptoPricesByNormalizedRange;
        }

        cryptoPricesByNormalizedRange = cryptoSymbols
                .stream()
                .map(symbol -> new NormalizedValue(symbol, computeNormalizedRange(symbol)))
                .sorted(Comparator.comparing(NormalizedValue::getValue))
                .collect(Collectors.toList());
        Collections.reverse(cryptoPricesByNormalizedRange);
        return cryptoPricesByNormalizedRange;
    }

    /**
     * Computes the normalized range of a given symbol (i.e. (max-min)/min)). If the symbol doesn't exist, it returns -1.
     *
     * @param symbol
     * @return
     */
    private BigDecimal computeNormalizedRange(String symbol) {
        Optional<CryptoPrice> maxCryptoPrice = calculatedMax(symbol);
        Optional<CryptoPrice> minCryptoPrice = calculatedMin(symbol);

        if (maxCryptoPrice.isEmpty() || minCryptoPrice.isEmpty()) {
            return BigDecimal.valueOf(-1);
        } else {
            return maxCryptoPrice.get().getUsdPrice().subtract(minCryptoPrice.get().getUsdPrice()).divide(minCryptoPrice.get().getUsdPrice(), 10, RoundingMode.UP);
        }
    }

    private BigDecimal computeNormalizedRange(String symbol, List<CryptoPrice> cryptoPrices) {
        Optional<CryptoPrice> maxCryptoPrice = calculatedMax(symbol);
        Optional<CryptoPrice> minCryptoPrice = calculatedMin(symbol);

        if (maxCryptoPrice.isEmpty() || minCryptoPrice.isEmpty()) {
            return BigDecimal.valueOf(-1);
        } else {
            BigDecimal normalizedRange = maxCryptoPrice.get().getUsdPrice().subtract(minCryptoPrice.get().getUsdPrice()).divide(minCryptoPrice.get().getUsdPrice(), 10, RoundingMode.UP);
            return normalizedRange;
        }
    }

    public boolean isCryptoSupported(String symbol) {
        return cryptoSymbols.contains(symbol);
    }

    public String highestNormalizedPerDay(LocalDate parsedDay) {
        LocalDateTime start = parsedDay.atStartOfDay();
        LocalDateTime end = start.plusHours(23).plusMinutes(59).plusSeconds(59);
        Map<String, List<CryptoPrice>> bySymbol = cryptoPriceRepository.findByDate(start, end).stream()
                .collect(Collectors.groupingBy(CryptoPrice::getCryptoSymbol, Collectors.toList()));
        List<Pair<String, BigDecimal>> list = bySymbol.entrySet().stream()
                .map(e -> new Pair<>(e.getKey(), computeNormalizedRange(e.getKey(), e.getValue())))
                .sorted(Comparator.comparing(Pair::getSecond))
                .toList();
        if (list.isEmpty()) {
            return "";
        }
        return list.get(list.size() - 1).getFirst();
    }
}
