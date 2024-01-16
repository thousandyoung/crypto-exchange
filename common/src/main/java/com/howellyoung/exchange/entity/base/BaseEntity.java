package com.howellyoung.exchange.entity.base;

public interface BaseEntity {
    /**
     * Default big decimal storage type: DECIMAL(PRECISION, SCALE)
     *
     * Range = +/-999999999999999999.999999999999999999
     */
    int DECIMAL_PRECISION = 36;

    /**
     * Default big decimal storage scale. Minimum is 0.000000000000000001.
     */
    int DECIMAL_SCALE = 18;

    int ENUM_LENGTH = 32;
    int CHAR_LENGTH_50 = 50;
    int CHAR_LENGTH_100 = 100;
    int CHAR_LENGTH_200 = 200;
    int CHAR_LENGTH_1000 = 1000;
    int CHAR_LENGTH_10000 = 10000;

}
