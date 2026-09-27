package com.yl.clothing.vo;

public record DataSimulatorStatusVO(
        boolean enabled,
        boolean retainData,
        String state,
        long generatedCount,
        long updatedCount,
        long tickCount,
        int intervalMs,
        String lastTickAt,
        String blockedReason,
        String error,
        long generatedUserCount,
        int userIntervalMs) {
}
