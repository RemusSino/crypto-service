package ro.rs.crypto.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Slf4j
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "crypto_price")
public final class CryptoPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "price_timestamp")
    private LocalDateTime priceTimestamp;
    @Column(name = "crypto_symbol")
    private String cryptoSymbol;
    @Column(name = "usd_price")
    private BigDecimal usdPrice;

    public CryptoPrice(LocalDateTime priceTimestamp, String cryptoSymbol, BigDecimal usdPrice) {
        this.priceTimestamp = priceTimestamp;
        this.cryptoSymbol = cryptoSymbol;
        this.usdPrice = usdPrice;
    }

    public static CryptoPrice fromCSVLine(String line) {
        Objects.requireNonNull(line);

        String[] values = line.split(",");
        if (values.length != 3) {
            throw new IllegalArgumentException(String.format("%s doesn't follow the expected csv line format TIMESTAMP,SYMBOL,PRICE", line));
        }

        LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(values[0])),
                ZoneId.of("UTC"));
        String symbol = values[1];
        BigDecimal price = new BigDecimal(values[2]);

        Objects.requireNonNull(line);
        Objects.requireNonNull(line);
        return new CryptoPrice(timestamp, symbol, price);
    }
}

