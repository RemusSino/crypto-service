package ro.rs.crypto.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.rs.crypto.api.dto.NormalizedValue;
import ro.rs.crypto.api.dto.Stats;
import ro.rs.crypto.model.CryptoPrice;
import ro.rs.crypto.service.CryptoPriceService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
public class CryptoPriceController {
    private final CryptoPriceService cryptoPriceService;

    public CryptoPriceController(final CryptoPriceService cryptoPriceService) {
        this.cryptoPriceService = cryptoPriceService;
    }

    @Operation(summary = "Retrieves a descending sorted list of all the stored crypto symbols, comparing by the normalized range (i.e. (max-min)/min)).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NormalizedValue.class))})})
    @RateLimiter(name = "cryptoRateLimiter")
    @RequestMapping(method = RequestMethod.GET, value = "/api/v1/cryptos/normalizedlist", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NormalizedValue>> getSortedNormalizedValues() {
        return ResponseEntity.ok().body(cryptoPriceService.cryptoPricesByNormalizedRange());
    }

    @Operation(summary = "Return the oldest/newest/min/max values for a requested crypto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {@Content(mediaType = APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Stats.class))}),
            @ApiResponse(responseCode = "404", description = "Bad request in case of unsupported symbol")})
    @RateLimiter(name = "cryptoRateLimiter")
    @RequestMapping(method = RequestMethod.GET, value = "/api/v1/cryptos/{symbol}/stats", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Stats> getCryptoStatsBySymbol(@PathVariable("symbol") final String symbol) {
        if (!cryptoPriceService.isCryptoSupported(symbol)) {
            return ResponseEntity.badRequest().build();
        }

        Optional<CryptoPrice> min = cryptoPriceService.calculatedMin(symbol);
        Optional<CryptoPrice> max = cryptoPriceService.calculatedMax(symbol);
        Optional<CryptoPrice> oldest = cryptoPriceService.calculateOldest(symbol);
        Optional<CryptoPrice> newest = cryptoPriceService.calculatedNewest(symbol);

        return ResponseEntity.ok().body(new Stats(oldest.orElse(null), newest.orElse(null), min.orElse(null), max.orElse(null)));
    }

    @Operation(summary = "Return the crypto symbol with the highest normalized value for a given day")
    @Parameters(value = {@Parameter(name = "day", description = "day in Basic ISO date YYYYMMDD format")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The symbol with the highest normalized range, i.e. BTC",
                    content = {@Content(mediaType = "application/text",
                            schema = @Schema(implementation = String.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request in case of wrong format of the day parameter or the given day is in the future"),
            @ApiResponse(responseCode = "404", description = "Not found in case of there is no crypto value for that day")
    })
    @RateLimiter(name = "cryptoRateLimiter")
    @RequestMapping(method = RequestMethod.GET, value = "/api/v1/cryptos/normalizedhighest")
    public ResponseEntity<String> getHighestNormalizedRange(@RequestParam(required = true) String day) {
        try {
            LocalDate parsedDay = LocalDate.parse(day, DateTimeFormatter.BASIC_ISO_DATE);
            if (parsedDay.isAfter(LocalDate.now())) {
                log.error("The given day is in the future");
                return ResponseEntity.badRequest().build();
            }

            String symbol = cryptoPriceService.highestNormalizedPerDay(parsedDay);
            if (symbol.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(symbol);
        } catch (DateTimeParseException e) {
            log.error("{} is not in YYYYMMDD format", day, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
