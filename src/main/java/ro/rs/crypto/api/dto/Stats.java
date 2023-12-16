package ro.rs.crypto.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.rs.crypto.model.CryptoPrice;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public final class Stats {
    private CryptoPrice oldest;
    private CryptoPrice newest;
    private CryptoPrice min;
    private CryptoPrice max;
}
