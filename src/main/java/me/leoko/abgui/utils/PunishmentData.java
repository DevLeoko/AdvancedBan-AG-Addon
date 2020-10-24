package me.leoko.abgui.utils;

import me.leoko.advancedban.utils.PunishmentType;

public class PunishmentData {
    public static final String DEFAULT_DURATION = "10m";

    private final PunishmentType basicType;
    private boolean silent = false;
    private boolean permanent = true;
    private String duration = DEFAULT_DURATION;
    private String reason;

    public PunishmentData(PunishmentType basicType) {
        this.basicType = basicType;
    }

    public PunishmentType getBasicType() {
        return basicType;
    }

    public PunishmentType getType() {
        if(permanent)
            return basicType;

        if(basicType == PunishmentType.BAN)
            return PunishmentType.TEMP_BAN;
        else if(basicType == PunishmentType.WARNING)
            return PunishmentType.TEMP_WARNING;
        else if(basicType == PunishmentType.MUTE)
            return PunishmentType.TEMP_MUTE;
        return basicType;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
