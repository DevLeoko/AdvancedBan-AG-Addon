package me.leoko.abgui;

import me.leoko.abgui.components.NameListComponent;
import me.leoko.abgui.components.PunishmentEntryComponent;
import me.leoko.abgui.textprocessors.CreationTextProcessor;
import me.leoko.abgui.textprocessors.LookupTextProcessor;
import me.leoko.abgui.textprocessors.PunishmentTextProcessor;
import me.leoko.abgui.utils.PunishmentData;
import me.leoko.abgui.utils.PunishmentState;
import me.leoko.abgui.utils.PunishmentStatus;
import me.leoko.advancedban.bukkit.event.PunishmentEvent;
import me.leoko.advancedban.bukkit.event.RevokePunishmentEvent;
import me.leoko.advancedban.manager.UUIDManager;
import me.leoko.advancedban.utils.PunishmentType;
import me.leoko.advancedgui.AdvancedGUI;
import me.leoko.advancedgui.manager.ResourceManager;
import me.leoko.advancedgui.utils.*;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.events.GuiInteractionExitEvent;
import me.leoko.advancedgui.utils.events.LayoutLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static me.leoko.abgui.Components.*;

public class GUIExtension implements LayoutExtension {
    private final Map<Interaction, PunishmentState> currentLookup = new HashMap<>();
    private final Map<Interaction, PunishmentStatus> currentLookupPunishment = new HashMap<>();
    private final Map<Interaction, PunishmentData> currentPunishmentSetup = new HashMap<>();
    private Layout layout;
    private GroupComponent componentTree;

    private ListComponent<PunishmentStatus> punishmentListComponent;

    @EventHandler
    public void onLayoutLoad(LayoutLoadEvent event) {
        final Layout layout = event.getLayout();
        if (layout.getName().equals(LAYOUT_NAME)) {
            this.layout = layout;
            this.componentTree = layout.getDefaultInteraction().getComponentTree();;
            setupSearch();
            setupLookup();
            setupPunishmentCreation();
        }
    }

    @EventHandler
    public void onInteractionEnd(GuiInteractionExitEvent event) {
        currentPunishmentSetup.remove(event.getInteraction());
        currentLookup.remove(event.getInteraction());
        currentLookupPunishment.remove(event.getInteraction());
    }

    @EventHandler
    public void onRevokePunishment(RevokePunishmentEvent event) {
        updatePunishmentData(event.getPunishment().getUuid());
    }

    @EventHandler
    public void onPunishment(PunishmentEvent event) {
        updatePunishmentData(event.getPunishment().getUuid());
    }

    private void updatePunishmentData(String uuid) {
        currentLookup.forEach((interaction, punishmentState) -> {
            if (punishmentState.getTargetUUID().equals(uuid)) {
                punishmentState.refresh();
                punishmentListComponent.locateOn(interaction).refreshItems();
            }
        });
    }

    private void setupSearch() {
        // Setup search for online & offline players
        final Font font = ResourceManager.getInstance().getFont(FONT_NAME, 18);

        final TextInputComponent inputComponent = new TextInputComponent(UUID.randomUUID().toString(), null, false, layout.getDefaultInteraction(), 0, 0, 128, 25, 7,
                GREY_BLUE_DARKER, TRANSPARENT, font, WHITE, LIGHT_GREY, "Search player...", "", null, false);

        final Component startHintComponent = layout.getDefaultInteraction().getComponentTree().locate(START_HINT_ID);

        // The search root will display:
        // A search hint - when input empty
        // List of players - if online players match the input
        // Offline player search button - if not
        final AdaptiveComponent searchRoot = new AdaptiveComponent(UUID.randomUUID().toString(), null, false, layout.getDefaultInteraction(), startHintComponent);

        componentTree.locate(LIST_ID, DummyComponent.class).setComponent(searchRoot);
        inputComponent.setInputHandler(InputHandler.limitHandler(
                (interaction, text) -> {
                    final AdaptiveComponent localSearchRoot = searchRoot.locateOn(interaction);
                    if (text.length() == 0) {
                        localSearchRoot.adapt(interaction.getComponentTree().locate(START_HINT_ID));
                    } else {
                        final String prefix = text.toLowerCase();
                        final List<String> players = Bukkit.getOnlinePlayers().stream()
                                .map(HumanEntity::getName)
                                .filter(name -> name.toLowerCase().startsWith(prefix))
                                .collect(Collectors.toList());

                        if (players.size() == 0) {
                            localSearchRoot.adapt(interaction.getComponentTree().locate(OFFLINE_HINT_ID));
                        } else {
                            localSearchRoot.adapt(new NameListComponent(5, 40, interaction, players, this::triggerLookup));
                        }
                    }
                    return text;
                },
                16
        ));

        componentTree.locate(INPUT_ID, DummyComponent.class).setComponent(inputComponent);

        componentTree.locate(OFFLINE_BTN_ID).setClickAction(
                (context, player, primaryTrigger) -> triggerLookup(context, inputComponent.locateOn(context).getInput())
        );
    }

    private void setupLookup() {
        layout.getDefaultInteraction().getComponentTree().locate("hvrGCs4f").setHidden(false); // TODO fix in editor

        // Register custom placeholders
        layout.registerTextProcessor(new LookupTextProcessor(currentLookup));
        layout.registerTextProcessor(new PunishmentTextProcessor(currentLookupPunishment));

        // Is player banned?
        ((CheckComponent) layout.getDefaultInteraction().getComponentTree().locate(LOOKUP_BAN_CHECK_ID)).setCheck((interaction, player, primaryTrigger) -> {
            final PunishmentState punishmentState = currentLookup.get(interaction);
            return punishmentState == null || punishmentState.getBan() == null;
        });

        // Is player muted?
        ((CheckComponent) layout.getDefaultInteraction().getComponentTree().locate(LOOKUP_MUTE_CHECK_ID)).setCheck((interaction, player, primaryTrigger) -> {
            final PunishmentState punishmentState = currentLookup.get(interaction);
            return punishmentState == null || punishmentState.getMute() == null;
        });

        // Only show punishment view if a punishment is selected
        ((CheckComponent) layout.getDefaultInteraction().getComponentTree().locate(LOOKUP_PUNISHMENT_CHECK_ID))
                .setCheck((interaction, player, primaryTrigger) -> currentLookupPunishment.containsKey(interaction));

        //   Build history view list
        // Builder for history entries
        ListItemBuilder<PunishmentStatus> itemBuilder = (interaction, index, item) -> new HoverDetectorComponent(
                UUID.randomUUID().toString(), null, false, interaction,
                (context, player, primaryTrigger) -> currentLookupPunishment.put(interaction, item),
                (context, player, primaryTrigger) -> {
                    if (item.equals(currentLookupPunishment.get(interaction)))
                        currentLookupPunishment.remove(interaction);
                },
                new PunishmentEntryComponent(item, 4, 71 + 22 * index, interaction)
        );

        // Setup history-list
        punishmentListComponent = new ListComponent<>(UUID.randomUUID().toString(), null, false, layout.getDefaultInteraction(),
                interaction -> currentLookup.get(interaction) == null ? Collections.emptyList() : currentLookup.get(interaction).getHistoryInfo(), itemBuilder, 3, 1);

        ((DummyComponent) layout.getDefaultInteraction().getComponentTree().locate(LOOKUP_HISTORY_LIST_ID)).setComponent(punishmentListComponent);

        layout.getDefaultInteraction().getComponentTree().locate(LOOKUP_HISTORY_UP_ID).setClickAction(
                (context, player, primaryTrigger) -> punishmentListComponent.locateOn(context).previousPage()
        );
        layout.getDefaultInteraction().getComponentTree().locate(LOOKUP_HISTORY_DOWN_ID).setClickAction(
                (context, player, primaryTrigger) -> punishmentListComponent.locateOn(context).nextPage()
        );

        ((CheckComponent) layout.getDefaultInteraction().getComponentTree().locate(LOOKUP_HISTORY_UP_CHECK_ID)).setCheck(
                (context, player, primaryTrigger) -> punishmentListComponent.locateOn(context).getPage() != 0
        );
        ((CheckComponent) layout.getDefaultInteraction().getComponentTree().locate(LOOKUP_HISTORY_DOWN_CHECK_ID)).setCheck(
                (context, player, primaryTrigger) -> punishmentListComponent.locateOn(context).getPage() != punishmentListComponent.locateOn(context).getMaxPage()
        );


        // Setup punish buttons
        final String[] punIds = {
                LOOKUP_PUN_BAN_ID,
                LOOKUP_PUN_KICK_ID,
                LOOKUP_PUN_MUTE_ID,
                LOOKUP_PUN_WARN_ID,
                LOOKUP_PUN_NOTE_ID,
        };

        final PunishmentType[] punTypes = {
                PunishmentType.BAN,
                PunishmentType.KICK,
                PunishmentType.MUTE,
                PunishmentType.WARNING,
                PunishmentType.NOTE,
        };

        final Component punCreateGroup = layout.getDefaultInteraction().getComponentTree().locate(PUN_CREATE_VIEW_ID);
        final Component lookupView = layout.getDefaultInteraction().getComponentTree().locate(LOOKUP_VIEW_ID);

        for (int i = 0; i < punIds.length; i++) {
            int index = i;
            layout.getDefaultInteraction().getComponentTree().locate(punIds[i]).setClickAction(
                    (context, player, primaryTrigger) -> {
                        currentPunishmentSetup.put(context, new PunishmentData(punTypes[index]));
                        lookupView.locateOn(context).setHidden(true);
                        punCreateGroup.locateOn(context).setHidden(false);
                    }
            );
        }
    }

    public void triggerLookup(Interaction interaction, String name) {
        interaction.getComponentTree().locate(SEARCH_VIEW_ID).setHidden(true);

        String uuid = UUIDManager.get().getUUID(name);
        final PunishmentState punishmentState = new PunishmentState(name, uuid);

        currentLookup.put(interaction, punishmentState);
        interaction.getComponentTree().locate(LOOKUP_VIEW_ID).setHidden(false);
    }

    public void setupPunishmentCreation() {
        layout.registerTextProcessor(new CreationTextProcessor(currentPunishmentSetup));

        final Font font = ResourceManager.getInstance().getFont(FONT_NAME, 13f);

        // Bind reason input
        final TextInputComponent reasonInput = new TextInputComponent(
                UUID.randomUUID().toString(), null, false, layout.getDefaultInteraction(),
                67, 36, 50, 8, 0,
                TRANSPARENT, TRANSPARENT, font, LIGHT_GREY, GREY,
                "NONE", "", null, false
        );

        reasonInput.setInputHandler(InputHandler.limitHandler(
                (interaction, text) -> {
                    final PunishmentData punishmentData = currentPunishmentSetup.get(interaction);
                    if(punishmentData != null)
                        punishmentData.setReason(text);
                    return text;
                },
                10
        ));

        // Bind duration input
        final TextInputComponent durationInput = new TextInputComponent(
                UUID.randomUUID().toString(), null, false, layout.getDefaultInteraction(),
                67, 77, 50, 8, 0,
                TRANSPARENT, TRANSPARENT, font, LIGHT_GREY, GREY,
                "Duration", PunishmentData.DEFAULT_DURATION, null, false
        );

        durationInput.setInputHandler(InputHandler.limitHandler(
                (interaction, text) -> {
                    final PunishmentData punishmentData = currentPunishmentSetup.get(interaction);
                    if(punishmentData != null)
                        punishmentData.setDuration(text);
                    return text;
                },
                10
        ));

        componentTree.locate(PUN_CREATE_REASON_INPUT_ID, DummyComponent.class).setComponent(reasonInput);
        componentTree.locate(PUN_CREATE_DURATION_INPUT_ID, DummyComponent.class).setComponent(durationInput);

        // Validate duration format
        componentTree.locate(PUN_CREATE_DURATION_INPUT_VALIDATION_ID, CheckComponent.class).setCheck(
                (context, player, primaryTrigger) -> {
                    final PunishmentData punishmentData = currentPunishmentSetup.get(context);
                    return punishmentData != null && punishmentData.getDuration().matches("[1-9][0-9]*([wdhms]|mo)|#.+");
                }
        );

        // Bind permanent input
        final CheckComponent tempSwitch = componentTree.locate(PUN_CREATE_TEMP_SWITCH_ID, CheckComponent.class);
        tempSwitch.setClickAction(
                (context, player, primaryTrigger) -> {
                    final PunishmentData setup = currentPunishmentSetup.get(context);
                    setup.setPermanent(!setup.isPermanent());
                }
        );
        tempSwitch.setCheck(
                (context, player, primaryTrigger) -> {
                    final PunishmentData punishmentData = currentPunishmentSetup.get(context);
                    return punishmentData != null && punishmentData.isPermanent();
                }
        );

        // Bind silent input
        final CheckComponent silentSwitch = componentTree.locate(PUN_CREATE_SILENT_SWITCH_ID, CheckComponent.class);
        silentSwitch.setClickAction(
                (context, player, primaryTrigger) -> {
                    final PunishmentData setup = currentPunishmentSetup.get(context);
                    setup.setSilent(!setup.isSilent());
                }
        );
        silentSwitch.setCheck(
                (context, player, primaryTrigger) -> {
                    final PunishmentData punishmentData = currentPunishmentSetup.get(context);
                    return punishmentData != null && punishmentData.isSilent();
                }
        );

        final Component lookupView = componentTree.locate(LOOKUP_VIEW_ID);
        final Component createView = componentTree.locate(PUN_CREATE_VIEW_ID);

        // Cancel creation
        componentTree.locate(PUN_CREATE_CANCEL_BTN_ID).setClickAction(
                (context, player, primaryTrigger) -> {
                    reasonInput.locateOn(context).setInput("", false);
                    durationInput.locateOn(context).setInput(PunishmentData.DEFAULT_DURATION, false);

                    createView.locateOn(context).setHidden(true);
                    lookupView.locateOn(context).setHidden(false);
                    currentPunishmentSetup.remove(context);
                }
        );

        // Only show permanent switch for possible temp punishments
        ((CheckComponent) componentTree.locate(PUN_CREATE_TEMP_CHECK_ID)).setCheck(
                (context, player, primaryTrigger) -> {
                    final PunishmentData punishmentData = currentPunishmentSetup.get(context);
                    if (punishmentData == null)
                        return false;
                    final PunishmentType type = punishmentData.getBasicType();
                    return type != PunishmentType.KICK && type != PunishmentType.NOTE;
                }
        );

        final Component statusBtn = componentTree.locate(LOOKUP_STATUS_BTN_ID);

        // Perform punishment
        componentTree.locate(PUN_CREATE_CREATE_BTN_ID).setClickAction(
                (context, player, primaryTrigger) -> {
                    Bukkit.getScheduler().runTaskLater(AdvancedGUI.getInstance(), () -> {
                        reasonInput.locateOn(context).setInput("", false);
                        durationInput.locateOn(context).setInput(PunishmentData.DEFAULT_DURATION, false);

                        createView.locateOn(context).setHidden(true);
                        lookupView.locateOn(context).setHidden(false);

                        statusBtn.getClickAction().execute(context, player, true);

                        final PunishmentData punishmentSetup = currentPunishmentSetup.remove(context);
                        final String target = currentLookup.get(context).getTargetName();
                        String command = punishmentSetup.getType().getName() + " " +
                                (punishmentSetup.isSilent() ? "-s " : "") +
                                target + " " +
                                (punishmentSetup.isPermanent() ? "" : punishmentSetup.getDuration() + " ") +
                                punishmentSetup.getReason();

                        player.performCommand(command);
                    }, 3);
                }
        );
    }
}
