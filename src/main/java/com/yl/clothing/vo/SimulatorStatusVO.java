package com.yl.clothing.vo;



public record SimulatorStatusVO(
        boolean enabled,
        boolean retainData,
        String state,
        long generatedCount,
        long updatedCount,
        long tickCount,
        int intervalMs,
        String lastTickAt,
        String blockedReason,
        String error) {
}
