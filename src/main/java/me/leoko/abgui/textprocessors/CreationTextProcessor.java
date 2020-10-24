package me.leoko.abgui.textprocessors;

import me.leoko.abgui.utils.PunishmentData;
import me.leoko.advancedgui.utils.Interaction;
import me.leoko.advancedgui.utils.TextProcessor;
import org.bukkit.entity.Player;

import java.util.Map;

public class CreationTextProcessor implements TextProcessor {
    private final Map<Interaction, PunishmentData> currentSetup;

    public CreationTextProcessor(Map<Interaction, PunishmentData> currentSetup) {
        this.currentSetup = currentSetup;
    }

    @Override
    public String processText(Player player, Interaction interaction, String text) {
        final PunishmentData punishmentSetup = currentSetup.get(interaction);
        if (punishmentSetup != null) {
            text = text.replaceAll("%ADV_Pun_Type%", punishmentSetup.getType().getName());
        }
        return text;
    }
}
