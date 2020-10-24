package me.leoko.abgui.components;

import me.leoko.advancedgui.manager.ResourceManager;
import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.Interaction;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.TextComponent;
import org.bukkit.entity.Player;

import java.awt.*;

import static me.leoko.abgui.Components.PRIMARY;
import static me.leoko.abgui.Components.WHITE;

public class PlayerNameComponent extends Component {
    private final int x;
    private final int y;

    private TextComponent nameText;

    public PlayerNameComponent(String name, int x, int y, Action clickAction) {
        super("", clickAction);

        this.x = x;
        this.y = y;

        final Font font = ResourceManager.getInstance().getFont("VT323", 17);
        nameText = new TextComponent("-", null, x + 5, y, font, name, WHITE);
    }

    @Override
    public void apply(Graphics graphic, Interaction context, Player player, GuiPoint cursor) {
        nameText.apply(graphic, context, player, cursor);

        final boolean active = nameText.isInBounds(context, player, cursor);
        graphic.setColor(active ? PRIMARY : WHITE);
        graphic.fillRect(x, y - 11, active ? 2 : 1, 12);
    }

    @Override
    public boolean isInBounds(Interaction context, Player player, GuiPoint cursor) {
        return nameText.isInBounds(context, player, cursor);
    }

    @Override
    public String getState(Interaction context, Player player, GuiPoint cursor) {
        return nameText.getState(context, player, cursor) + isInBounds(context, player, cursor);
    }
}
