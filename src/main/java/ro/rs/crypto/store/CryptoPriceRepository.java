package ro.rs.crypto.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ro.rs.crypto.model.CryptoPrice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface CryptoPriceRepository extends JpaRepository<CryptoPrice, Long> {
    List<CryptoPrice> findByCryptoSymbol(String cryptoSymbol);

    @Query("select c from CryptoPrice c where c.priceTimestamp >= :start and c.priceTimestamp <= :end")
    List<CryptoPrice> findByDate(LocalDateTime start, LocalDateTime end);

    @Query("select distinct(c.cryptoSymbol) from CryptoPrice c")
    Set<String> findSupportedSymbols();
}
