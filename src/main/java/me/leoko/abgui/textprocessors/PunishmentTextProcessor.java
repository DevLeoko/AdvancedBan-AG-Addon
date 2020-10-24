package me.leoko.abgui.textprocessors;

import me.leoko.abgui.utils.PunishmentStatus;
import me.leoko.advancedban.manager.TimeManager;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedgui.utils.Interaction;
import me.leoko.advancedgui.utils.TextProcessor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

public class PunishmentTextProcessor implements TextProcessor {
    private final Map<Interaction, PunishmentStatus> currentPunishment;

    public PunishmentTextProcessor(Map<Interaction, PunishmentStatus> currentPunishment) {
        this.currentPunishment = currentPunishment;
    }

    @Override
    public String processText(Player player, Interaction interaction, String text) {
        final PunishmentStatus punishmentInfo = currentPunishment.get(interaction);
        if (punishmentInfo != null) {
            final Punishment punishment = punishmentInfo.getPunishment();
            String reasonL1 = punishment.getReason();
            String reasonL2 = "";

            final int len = 18;
            if (reasonL1.length() > len) {
                reasonL2 = reasonL1.substring(len);
                reasonL1 = reasonL1.substring(0, len);

                if (reasonL2.length() > len) {
                    reasonL2 = reasonL2.substring(0, len - 3) + "...";
                }
            }

            String remaining = "perma";
            if (punishment.isExpired()) {
                remaining = "expired";
            } else if (punishment.getEnd() != -1) {
                final Duration duration = Duration.ofMillis(punishment.getEnd() - TimeManager.getTime());
                final long hours = duration.toHours();
                if (hours >= 24) {
                    final long days = duration.toDays();
                    remaining = String.format("%dd %dh", days, duration.minusDays(days).toDays());
                } else if (hours >= 1) {
                    remaining = String.format("%dh %dm", hours, duration.minusHours(hours).toMinutes());
                } else {
                    final long minutes = duration.toMinutes();
                    remaining = String.format("%dm %ds", minutes, duration.minusMinutes(minutes).getSeconds());
                }
            }

            text = text.replaceAll("%ADV_P_Operator%", punishment.getOperator())
                    .replaceAll("%ADV_P_Remaining%", remaining)
                    .replaceAll("%ADV_P_ReasonL1%", reasonL1)
                    .replaceAll("%ADV_P_ReasonL2%", reasonL2);
        }
        return text;
    }
}
