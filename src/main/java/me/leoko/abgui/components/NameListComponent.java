package me.leoko.abgui.components;

import me.leoko.advancedgui.utils.Interaction;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.CustomComponent;
import me.leoko.advancedgui.utils.components.GroupComponent;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NameListComponent extends CustomComponent {
    public NameListComponent(int x, int y, Interaction interaction, List<String> names, BiConsumer<Interaction, String> nameCallback) {
        super(interaction);
        final List<Component> textComponents = IntStream.range(0, names.size())
                .mapToObj(i -> new PlayerNameComponent(
                        names.get(i),
                        x, y + i * 18,
                        (context, player, primaryTrigger) -> nameCallback.accept(context, names.get(i)),
                        interaction
                ))
                .collect(Collectors.toList());

        this.component = new GroupComponent("", null, hidden, interaction, textComponents);
    }
}
