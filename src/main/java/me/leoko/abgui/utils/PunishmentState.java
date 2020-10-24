package me.leoko.abgui.utils;

import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.PunishmentType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PunishmentState {

    private String targetName;
    private String targetUUID;

    private Punishment mute;
    private Punishment ban;

    private List<Punishment> warns = new ArrayList<>();
    private List<Punishment> notes = new ArrayList<>();

    private List<Punishment> history;

    public PunishmentState(String targetName, String targetUUID) {
        this.targetName = targetName;
        this.targetUUID = targetUUID;

        refresh();
    }

    public void refresh(){
        history = PunishmentManager.get().getPunishments(targetUUID, null, false);
        history.sort(Comparator.comparingLong(Punishment::getStart).reversed());

        ban = null;
        mute = null;
        warns.clear();
        notes.clear();
        for (Punishment punishment : PunishmentManager.get().getPunishments(targetUUID, null, true)) {
            if(punishment.getType().getBasic() == PunishmentType.BAN)
                ban = punishment;
            else if(punishment.getType().getBasic() == PunishmentType.MUTE)
                mute = punishment;
            else if(punishment.getType().getBasic() == PunishmentType.WARNING)
                warns.add(punishment);
            else if(punishment.getType().getBasic() == PunishmentType.NOTE)
                notes.add(punishment);
        }
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetUUID() {
        return targetUUID;
    }

    public Punishment getMute() {
        return mute;
    }

    public Punishment getBan() {
        return ban;
    }

    public List<Punishment> getWarns() {
        return warns;
    }

    public List<Punishment> getNotes() {
        return notes;
    }

    public List<Punishment> getHistory() {
        return history;
    }

    public List<PunishmentStatus> getHistoryInfo() {
        return history.stream().map(punishment -> {
            boolean active = false;
            if(punishment.getType().getBasic() == PunishmentType.BAN)
                active = getBan() != null && getBan().getId() == punishment.getId();
            else if(punishment.getType().getBasic() == PunishmentType.MUTE)
                active = getMute() != null && getMute().getId() == punishment.getId();
            else if(punishment.getType().getBasic() == PunishmentType.WARNING)
                active = getWarns().stream().anyMatch(pt -> pt.getId() == punishment.getId());
            else if(punishment.getType().getBasic() == PunishmentType.NOTE)
                active = getNotes().stream().anyMatch(pt -> pt.getId() == punishment.getId());
            return new PunishmentStatus(punishment, active);
        }).collect(Collectors.toList());
    }
}
