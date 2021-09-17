package me.leoko.abgui.components;

import me.leoko.abgui.utils.PunishmentStatus;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedgui.manager.ResourceManager;
import me.leoko.advancedgui.utils.Interaction;
import me.leoko.advancedgui.utils.actions.CommandAction;
import me.leoko.advancedgui.utils.components.TextComponent;
import me.leoko.advancedgui.utils.components.*;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static me.leoko.abgui.Components.*;

public class PunishmentEntryComponent extends CustomComponent {
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("dd.MM.yyyy   HH:mm");

    public PunishmentEntryComponent(PunishmentStatus punishmentInfo, int x, int y, Interaction interaction) {
        super(interaction);

        final Font font = ResourceManager.getInstance().getFont("VT323", 17);
        final Font smallFont = font.deriveFont(12f);

        final Punishment punishment = punishmentInfo.getPunishment();

        final String timeStr = TIME_FORMATTER.format(new Date(punishment.getStart()));
        final GroupComponent normal = new GroupComponent("", null, false, interaction, Arrays.asList(
                new RectComponent("", null, false, interaction, x, y - 9, 95, 18, TRANSPARENT),
                new TextComponent("", null, false, interaction, x + 60, y, smallFont, punishmentInfo.isActive() ? "active" : "", GREEN),
                new TextComponent("", null, false, interaction, x, y, font, punishment.getType().getName(), WHITE),
                new TextComponent("", null, false, interaction, x, y + 8, smallFont, timeStr, LIGHT_GREY)
        ));
        final GroupComponent hovered = new GroupComponent("", null, false, interaction, Arrays.asList(
                punishmentInfo.isActive() ? new HoverComponent("",
                        new CommandAction("unpunish " + punishment.getId(), false, false), false, interaction,
                        new TextComponent("", null, false, interaction, x + 60, y, smallFont, "revoke", ORANGE),
                        new TextComponent("", null, false, interaction, x + 60, y, smallFont, "revoke", RED)
                ) : GroupComponent.createEmpty(interaction),
                new TextComponent("", null, false, interaction, x, y, font, punishment.getType().getName(), GREY),
                new TextComponent("", null, false, interaction, x, y + 8, smallFont, timeStr, GREY)
        ));

        this.component = new HoverComponent("", null, false, interaction, normal, hovered);
    }
}
