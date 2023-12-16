package ro.rs.crypto.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.rs.crypto.api.dto.NormalizedValue;
import ro.rs.crypto.api.dto.Stats;
import ro.rs.crypto.model.CryptoPrice;
import ro.rs.crypto.service.CryptoPriceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class CryptoPriceControllerTest {
    @MockBean
    private CryptoPriceService cryptoPriceService;

    @Autowired
    private MockMvc mockMvc;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void before() {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        SimpleModule simpleModule = new SimpleModule();
        mapper.registerModule(simpleModule);
    }

    @Test
    public void testGetNormalized() throws Exception {
        NormalizedValue normalizedValue = new NormalizedValue("BTC", new BigDecimal("100.01"));
        List<NormalizedValue> normalizedValueList = List.of(normalizedValue);
        Mockito.when(cryptoPriceService.cryptoPricesByNormalizedRange()).thenReturn(normalizedValueList);
        MvcResult getResponse = mockMvc.perform(get("/api/v1/cryptos/normalizedlist"))
                .andExpect(status().isOk())
                .andReturn();
        NormalizedValue[] result = mapper.readValue(getResponse.getResponse().getContentAsString(), NormalizedValue[].class);
        assertThat(result).containsExactly(normalizedValue);
    }

    @Test
    void givenSymbolNotSupported_whenGetStats_ThenReturnBadRequest() throws Exception {
        Mockito.when(cryptoPriceService.isCryptoSupported("BTC")).thenReturn(false);
        mockMvc.perform(get("/api/v1/cryptos/BTC/stats"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetStats() throws Exception {
        CryptoPrice cp1 = CryptoPrice.fromCSVLine("1641009600000,BTC,46813.21");
        CryptoPrice cp2 = CryptoPrice.fromCSVLine("1641020400000,BTC,46979.61");
        CryptoPrice cp3 = CryptoPrice.fromCSVLine("1643626800000,BTC,37300.31");
        CryptoPrice cp4 = CryptoPrice.fromCSVLine("1643659200000,BTC,38415.79");

        Mockito.when(cryptoPriceService.isCryptoSupported("BTC")).thenReturn(true);
        Mockito.when(cryptoPriceService.calculatedMin("BTC")).thenReturn(Optional.of(cp1));
        Mockito.when(cryptoPriceService.calculatedMax("BTC")).thenReturn(Optional.of(cp2));
        Mockito.when(cryptoPriceService.calculateOldest("BTC")).thenReturn(Optional.of(cp3));
        Mockito.when(cryptoPriceService.calculatedNewest("BTC")).thenReturn(Optional.of(cp4));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/cryptos/BTC/stats"))
                .andExpect(status().isOk())
                .andReturn();
        Stats stats = mapper.readValue(mvcResult.getResponse().getContentAsString(), Stats.class);
        assertThat(stats.getMin()).isEqualTo(cp1);
        assertThat(stats.getMax()).isEqualTo(cp2);
        assertThat(stats.getOldest()).isEqualTo(cp3);
        assertThat(stats.getNewest()).isEqualTo(cp4);
    }


    @Test
    void testGetHighestNormalizedRange() throws Exception {
        LocalDate parsedDay = LocalDate.parse("20220101", DateTimeFormatter.BASIC_ISO_DATE);
        Mockito.when(cryptoPriceService.highestNormalizedPerDay(parsedDay)).thenReturn("BTC");

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/cryptos/normalizedhighest")
                        .param("day", "20220101"))
                .andExpect(status().isOk())
                .andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isEqualTo("BTC");
    }

    @Test
    void givenWrongDay_whenGetHighestNormalizedRange_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/normalizedhighest")
                        .param("day", "2020101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenDayInFuture_whenGetHighestNormalizedRange_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/normalizedhighest")
                        .param("day", "20300101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenUnsupportedSymbol_whenGetHighestNormalizedRange_thenReturnNotFound() throws Exception {
        LocalDate parsedDay = LocalDate.parse("20220101", DateTimeFormatter.BASIC_ISO_DATE);
        Mockito.when(cryptoPriceService.highestNormalizedPerDay(parsedDay)).thenReturn("");

        mockMvc.perform(get("/api/v1/cryptos/normalizedhighest")
                        .param("day", "20220101"))
                .andExpect(status().isNotFound());
    }
}
