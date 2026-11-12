package com.tombtale.serviceinventory.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SellShareTest {

    private static final int THIRTY_PERCENT = 30;
    private static final int WHOLE = 100;
    private static final long PRICE = 1395L;
    /** 418.5, rounded down. */
    private static final long THIRTY_PERCENT_OF_PRICE = 418L;

    @Test
    void aShareIsRoundedDownToWholeCoins() {
        assertThat(new SellShare(THIRTY_PERCENT).payoutFor(PRICE)).isEqualTo(THIRTY_PERCENT_OF_PRICE);
    }

    @Test
    void noShareAndTheWholePriceAreAllowed() {
        assertThat(new SellShare(0).payoutFor(PRICE)).isZero();
        assertThat(new SellShare(WHOLE).payoutFor(PRICE)).isEqualTo(PRICE);
    }

    /** Above 100%, buying from one dealer and selling to another would make coins. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void aShareOutsideZeroToAHundredIsRefused(int percent) {
        assertThatThrownBy(() -> new SellShare(percent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A sell share is 0 to 100 percent, not " + percent);
    }
}
