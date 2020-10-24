package me.leoko.abgui.textprocessors;

import me.leoko.abgui.utils.PunishmentState;
import me.leoko.advancedgui.utils.Interaction;
import me.leoko.advancedgui.utils.TextProcessor;
import org.bukkit.entity.Player;

import java.util.Map;

public class LookupTextProcessor implements TextProcessor {
    private final Map<Interaction, PunishmentState> currentLookup;

    public LookupTextProcessor(Map<Interaction, PunishmentState> currentLookup) {
        this.currentLookup = currentLookup;
    }

    @Override
    public String processText(Player player, Interaction interaction, String text) {
        final PunishmentState punishmentState = currentLookup.get(interaction);
        if (punishmentState != null) {
            text = text.replaceAll("%ADV_UUID%", punishmentState.getTargetUUID().replaceAll("-", ""))
                    .replaceAll("%ADV_Name%", punishmentState.getTargetName())
                    .replaceAll("%ADV_Warns%", punishmentState.getWarns().size() + "")
                    .replaceAll("%ADV_Notes%", punishmentState.getNotes().size() + "");
        }
        return text;
    }
}
