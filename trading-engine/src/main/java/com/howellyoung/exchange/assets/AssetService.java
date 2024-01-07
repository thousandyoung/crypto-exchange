package com.howellyoung.exchange.assets;

import com.howellyoung.exchange.enums.AssetEnum;
import com.howellyoung.exchange.util.AbstractLogger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
public class AssetService extends AbstractLogger {
    private enum Transfer {
        AVAILABLE_TO_FROZEN,
        FROZEN_TO_AVAILABLE;
    }
    final ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> allUserAssetsMap = new ConcurrentHashMap<>();

    public Map<Long, Map<AssetEnum, Asset>> getAllUserAssetsMap() {
        return Collections.unmodifiableMap(allUserAssetsMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Collections.unmodifiableMap(e.getValue()))));
    }

    public Asset getAssetForUser(Long userId, AssetEnum assetEnum) {
        return allUserAssetsMap.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(assetEnum, k -> new Asset());
    }

    public Map<AssetEnum, Asset> getAssetsForUser(Long userId) {
        Map<AssetEnum, Asset> userAssets = allUserAssetsMap.get(userId);
        if (userAssets == null) {
            return Map.of();
        }
        return userAssets;
    }

    public boolean tryFreeze(Long userId, AssetEnum assetEnum, BigDecimal amount) {
        return tryTransfer(Transfer.AVAILABLE_TO_FROZEN, userId, userId, assetEnum, amount, true);
    }

    public void unfreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        if (!tryTransfer(Transfer.FROZEN_TO_AVAILABLE, userId, userId, assetId, amount, true)) {
            throw new RuntimeException(
                    "Unfreeze failed for user " + userId + ", asset = " + assetId + ", amount = " + amount);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("unfreeze user {}, asset {}, amount {}", userId, assetId, amount);
        }
    }


    /*
    For Test, checkBalance can be false, but for real, it must be true
     */
    public void transferFrozenToAvailable(Long fromUser, Long toUser, AssetEnum assetEnum, BigDecimal amount, boolean checkBalance) {
        if (!tryTransfer(Transfer.FROZEN_TO_AVAILABLE, fromUser, toUser, assetEnum, amount, checkBalance)) {
            handleFailedTransfer(Transfer.FROZEN_TO_AVAILABLE, fromUser, toUser, assetEnum, amount);
        }
        logTransfer(assetEnum, fromUser, toUser, amount);
    }


    private void handleFailedTransfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount) {
        throw new RuntimeException("Transfer failed for " + type + ", from user " + fromUser + " to user " + toUser
                + ", asset = " + assetId + ", amount = " + amount);
    }

    private void logTransfer(AssetEnum assetId, Long fromUser, Long toUser, BigDecimal amount) {
        if (logger.isDebugEnabled()) {
            logger.debug("transfer asset {}, from {} => {}, amount {}", assetId, fromUser, toUser, amount);
        }
    }

    private boolean tryTransfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetEnum, BigDecimal amount,
                               boolean checkBalance) {
        if (amount.signum() == 0) {
            return true;
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Negative amount");
        }
        Asset fromAsset = getAssetForUser(fromUser, assetEnum);

        Asset toAsset = getAssetForUser(toUser, assetEnum);

        return switch (type) {
            case AVAILABLE_TO_FROZEN -> {
                // 需要检查余额且余额不足:
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.frozen = toAsset.frozen.add(amount);
                yield true;
            }
            case FROZEN_TO_AVAILABLE -> {
                // 需要检查余额且余额不足:
                if (checkBalance && fromAsset.frozen.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.frozen = fromAsset.frozen.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            default -> {
                throw new IllegalArgumentException("invalid type: " + type);
            }
        };
    }




}
