package com.yl.clothing.entity;

import java.time.LocalDateTime;

/** Persistent ownership and retention policy for one simulator run. */
public class SimulationRun {
    private Long id;
    private String mode;
    private boolean retainData;
    private LocalDateTime startedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isRetainData() { return retainData; }
    public void setRetainData(boolean retainData) { this.retainData = retainData; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
}
