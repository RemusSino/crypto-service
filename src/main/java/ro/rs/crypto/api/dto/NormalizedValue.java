package ro.rs.crypto.api.dto;

import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@Setter
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public final class NormalizedValue {
    private String symbol;
    private BigDecimal value;
}
