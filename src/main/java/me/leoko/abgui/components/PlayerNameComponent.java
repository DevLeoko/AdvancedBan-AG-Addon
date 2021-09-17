package me.leoko.abgui.components;

import me.leoko.advancedgui.manager.ResourceManager;
import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.Interaction;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.TextComponent;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.UUID;

import static me.leoko.abgui.Components.PRIMARY;
import static me.leoko.abgui.Components.WHITE;

public class PlayerNameComponent extends Component {
    private final int x;
    private final int y;

    private TextComponent nameText;

    public PlayerNameComponent(String name, int x, int y, Action clickAction, Interaction interaction) {
        super(UUID.randomUUID().toString(), clickAction, false, interaction);

        this.x = x;
        this.y = y;

        final Font font = ResourceManager.getInstance().getFont("VT323", 17);
        nameText = new TextComponent("-", null, false, interaction, x + 5, y, font, name, WHITE);
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {
        nameText.apply(graphic, player, cursor);

        final boolean active = nameText.isInBounds(player, cursor);
        graphic.setColor(active ? PRIMARY : WHITE);
        graphic.fillRect(x, y - 11, active ? 2 : 1, 12);
    }

    @Override
    public boolean isInBounds(Player player, GuiPoint cursor) {
        return nameText.isInBounds(player, cursor);
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {
        return nameText.getState(player, cursor) + isInBounds(player, cursor);
    }

    @Override
    public Component clone(Interaction interaction) {
        return new PlayerNameComponent(nameText.getText(), x, y, clickAction, interaction);
    }
}
