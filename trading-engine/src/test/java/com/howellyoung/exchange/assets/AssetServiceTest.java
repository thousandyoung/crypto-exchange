package com.howellyoung.exchange.assets;

import com.howellyoung.exchange.enums.AssetEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;


import com.howellyoung.exchange.assets.*;
public class AssetServiceTest {

    static final Long DEBT_ID = 1L;
    static final Long USER_A_ID = 2000L;
    static final Long USER_B_ID = 3000L;
    static final Long USER_C_ID = 4000L;

    AssetService service;

    @BeforeEach
    public void setUp() {
        service = new AssetService();
        init();
    }

    @AfterEach
    public void verify() {
        BigDecimal totalUSD = BigDecimal.ZERO;
        BigDecimal totalBTC = BigDecimal.ZERO;
        for (Long userId : service.getAllUserAssetsMap().keySet()) {
            var assetUSD = service.getAssetForUser(userId, AssetEnum.USD);
            if (assetUSD != null) {
                totalUSD = totalUSD.add(assetUSD.available).add(assetUSD.frozen);
            }
            var assetBTC = service.getAssetForUser(userId, AssetEnum.BTC);
            if (assetBTC != null) {
                totalBTC = totalBTC.add(assetBTC.available).add(assetBTC.frozen);
            }
        }
        assertBDEquals(0, totalUSD);
        assertBDEquals(0, totalBTC);
    }

    @Test
    void tryFreeze() {
        // A USD = 12300
        // freeze 12000 ok:
        service.tryFreeze(USER_A_ID, AssetEnum.USD, new BigDecimal("12000"));
        assertBDEquals(300, service.getAssetForUser(USER_A_ID, AssetEnum.USD).available);
        assertBDEquals(12000, service.getAssetForUser(USER_A_ID, AssetEnum.USD).frozen);

        // freeze 301 failed:
        assertFalse(service.tryFreeze(USER_A_ID, AssetEnum.USD, new BigDecimal("301")));

        assertBDEquals(300, service.getAssetForUser(USER_A_ID, AssetEnum.USD).available);
        assertBDEquals(12000, service.getAssetForUser(USER_A_ID, AssetEnum.USD).frozen);
    }

    @Test
    void unfreeze() {
        // A USD = 12300
        // freeze 12000 ok:
        service.tryFreeze(USER_A_ID, AssetEnum.USD, new BigDecimal("12000"));
        assertBDEquals(300, service.getAssetForUser(USER_A_ID, AssetEnum.USD).available);
        assertBDEquals(12000, service.getAssetForUser(USER_A_ID, AssetEnum.USD).frozen);

        // unfreeze 9000 ok:
        service.unfreeze(USER_A_ID, AssetEnum.USD, new BigDecimal("9000"));
        assertBDEquals(9300, service.getAssetForUser(USER_A_ID, AssetEnum.USD).available);
        assertBDEquals(3000, service.getAssetForUser(USER_A_ID, AssetEnum.USD).frozen);

        // unfreeze 3001 failed:
        assertThrows(RuntimeException.class, () -> {
            service.unfreeze(USER_A_ID, AssetEnum.USD, new BigDecimal("3001"));
        });
    }

    @Test
    void transferFrozenToAvailable() {

        // A USD -> A frozen:
        service.tryFreeze(USER_A_ID, AssetEnum.USD, new BigDecimal("9000"));
        assertBDEquals(3300, service.getAssetForUser(USER_A_ID, AssetEnum.USD).available);
        assertBDEquals(9000, service.getAssetForUser(USER_A_ID, AssetEnum.USD).frozen);

        // A frozen -> C available:
        service.transferFrozenToAvailable(USER_A_ID, USER_C_ID, AssetEnum.USD, new BigDecimal("8000"), true);
        assertBDEquals(1000, service.getAssetForUser(USER_A_ID, AssetEnum.USD).frozen);
        assertBDEquals(8000, service.getAssetForUser(USER_C_ID, AssetEnum.USD).available);

        // A frozen -> B available failed:
        assertThrows(RuntimeException.class, () -> {
            service.transferFrozenToAvailable(USER_A_ID, USER_B_ID, AssetEnum.USD, new BigDecimal("1001"), true);
        });
    }



    /**
     * A: USD=12300, BTC=12
     *
     * B: USD=45600
     *
     * C: BTC=34
     */
    void init() {
        service.transferFrozenToAvailable(DEBT_ID, USER_A_ID, AssetEnum.USD, BigDecimal.valueOf(12300),
                false);
        service.transferFrozenToAvailable(DEBT_ID, USER_A_ID, AssetEnum.BTC, BigDecimal.valueOf(12),
                false);
        service.transferFrozenToAvailable(DEBT_ID, USER_B_ID,  AssetEnum.USD, BigDecimal.valueOf(45600),
                false);

        service.transferFrozenToAvailable(DEBT_ID, USER_C_ID, AssetEnum.BTC, BigDecimal.valueOf(34),
                false);

        assertBDEquals(-57900, service.getAssetForUser(DEBT_ID, AssetEnum.USD).frozen);
        assertBDEquals(-46, service.getAssetForUser(DEBT_ID, AssetEnum.BTC).frozen);
    }



    void assertBDEquals(long value, BigDecimal bd) {
        String valueStr = String.valueOf(value);
        assertTrue(new BigDecimal(valueStr).compareTo(bd) == 0,
                String.format("Expected %s but actual %s.", value, bd.toPlainString()));
    }


}
