package me.leoko.abgui.utils;

import me.leoko.advancedban.utils.Punishment;

public class PunishmentStatus {
    private Punishment punishment;
    private boolean active;

    public PunishmentStatus(Punishment punishment, boolean active) {
        this.punishment = punishment;
        this.active = active;
    }

    public Punishment getPunishment() {
        return punishment;
    }

    public boolean isActive() {
        return active;
    }
}
