package com.yl.clothing.entity;

import java.time.LocalDateTime;
import java.time.LocalDate;

/** 数据库持久化实体。 */
public class Customer {
    private Long id;
    private Long simulationRunId;
    private String phone;
    private String nickname;
    private String gender;
    private LocalDate birthday;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSimulationRunId() { return simulationRunId; }

    public void setSimulationRunId(Long simulationRunId) { this.simulationRunId = simulationRunId; }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
