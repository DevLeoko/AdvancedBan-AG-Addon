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
import me.leoko.advancedgui.utils.Interaction;
import me.leoko.advancedgui.utils.Layout;
import me.leoko.advancedgui.utils.LayoutExtension;
import me.leoko.advancedgui.utils.ListItemBuilder;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.events.GuiInteractionExitEvent;
import me.leoko.advancedgui.utils.events.LayoutLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.leoko.abgui.Components.*;

public class GUIExtension implements LayoutExtension {
    private final Map<Interaction, PunishmentState> currentLookup = new HashMap<>();
    private final Map<Interaction, PunishmentStatus> currentLookupPunishment = new HashMap<>();
    private final Map<Interaction, PunishmentData> currentPunishmentSetup = new HashMap<>();
    private Layout layout;

    private ListComponent<PunishmentStatus> punishmentListComponent;

    @EventHandler
    public void onLayoutLoad(LayoutLoadEvent event) {
        final Layout layout = event.getLayout();
        if (layout.getName().equals(LAYOUT_NAME)) {
            this.layout = layout;
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
                punishmentListComponent.refreshItems(interaction);
            }
        });
    }

    private void setupSearch() {
        // Setup search for online & offline players
        final Font font = ResourceManager.getInstance().getFont(FONT_NAME, 18);

        final TextInputComponent inputComponent = new TextInputComponent("", null, 0, 0, 128, 25, 7,
                GREY_BLUE_DARKER, TRANSPARENT, font, WHITE, LIGHT_GREY, "Search player...", "");

        final Component offlineHint = layout.getComponentTree().locate(OFFLINE_HINT_ID);
        final Component startHint = layout.getComponentTree().locate(START_HINT_ID);

        // The search root will display:
        // A search hint - when input empty
        // List of players - if online players match the input
        // Offline player search button - if not
        final AdaptiveComponent searchRoot = new AdaptiveComponent("", null, startHint);

        ((DummyComponent) layout.getComponentTree().locate(LIST_ID)).setComponent(searchRoot);
        inputComponent.setInputHandler(TextInputComponent.limitHandler(
                (interaction, text) -> {
                    if (text.length() == 0) {
                        searchRoot.adapt(interaction, startHint);
                    } else {
                        final String prefix = text.toLowerCase();
                        final List<String> players = Bukkit.getOnlinePlayers().stream()
                                .map(HumanEntity::getName)
                                .filter(name -> name.toLowerCase().startsWith(prefix))
                                .collect(Collectors.toList());

                        if (players.size() == 0) {
                            searchRoot.adapt(interaction, offlineHint);
                        } else {
                            searchRoot.adapt(interaction, new NameListComponent(5, 40, players, this::triggerLookup));
                        }
                    }
                    return text;
                },
                16
        ));

        ((DummyComponent) layout.getComponentTree().locate(INPUT_ID)).setComponent(inputComponent);

        layout.getComponentTree().locate(OFFLINE_BTN_ID).setClickAction(
                (context, player, primaryTrigger) -> triggerLookup(context, inputComponent.getInput(context))
        );
    }

    private void setupLookup() {
        layout.getDefaultHidden().remove(layout.getComponentTree().locate("hvrGCs4f")); // TODO fix in editor

        // Register custom placeholders
        layout.registerTextProcessor(new LookupTextProcessor(currentLookup));
        layout.registerTextProcessor(new PunishmentTextProcessor(currentLookupPunishment));

        // Is player banned?
        ((CheckComponent) layout.getComponentTree().locate(LOOKUP_BAN_CHECK_ID)).setCheck((interaction, player, primaryTrigger) -> {
            final PunishmentState punishmentState = currentLookup.get(interaction);
            return punishmentState == null || punishmentState.getBan() == null;
        });

        // Is player muted?
        ((CheckComponent) layout.getComponentTree().locate(LOOKUP_MUTE_CHECK_ID)).setCheck((interaction, player, primaryTrigger) -> {
            final PunishmentState punishmentState = currentLookup.get(interaction);
            return punishmentState == null || punishmentState.getMute() == null;
        });

        // Only show punishment view if a punishment is selected
        ((CheckComponent) layout.getComponentTree().locate(LOOKUP_PUNISHMENT_CHECK_ID))
                .setCheck((interaction, player, primaryTrigger) -> currentLookupPunishment.containsKey(interaction));

        //   Build history view list
        // Builder for history entries
        ListItemBuilder<PunishmentStatus> itemBuilder = (interaction, index, item) -> new HoverDetectorComponent(
                "", null,
                (context, player, primaryTrigger) -> currentLookupPunishment.put(interaction, item),
                (context, player, primaryTrigger) -> {
                    if (item.equals(currentLookupPunishment.get(interaction)))
                        currentLookupPunishment.remove(interaction);
                },
                new PunishmentEntryComponent(item, 4, 71 + 22 * index)
        );

        // Setup history-list
        punishmentListComponent = new ListComponent<>("", null,
                interaction -> currentLookup.get(interaction).getHistoryInfo(), itemBuilder, 3, 1);

        ((DummyComponent) layout.getComponentTree().locate(LOOKUP_HISTORY_LIST_ID)).setComponent(punishmentListComponent);

        layout.getComponentTree().locate(LOOKUP_HISTORY_UP_ID).setClickAction(
                (context, player, primaryTrigger) -> punishmentListComponent.previousPage(context)
        );
        layout.getComponentTree().locate(LOOKUP_HISTORY_DOWN_ID).setClickAction(
                (context, player, primaryTrigger) -> punishmentListComponent.nextPage(context)
        );

        ((CheckComponent) layout.getComponentTree().locate(LOOKUP_HISTORY_UP_CHECK_ID)).setCheck(
                (context, player, primaryTrigger) -> punishmentListComponent.getPage(context) != 0
        );
        ((CheckComponent) layout.getComponentTree().locate(LOOKUP_HISTORY_DOWN_CHECK_ID)).setCheck(
                (context, player, primaryTrigger) -> punishmentListComponent.getPage(context) != punishmentListComponent.getMaxPage(context)
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

        final Component punCreateGroup = layout.getComponentTree().locate(PUN_CREATE_VIEW_ID);
        final Component lookupView = layout.getComponentTree().locate(LOOKUP_VIEW_ID);

        for (int i = 0; i < punIds.length; i++) {
            int index = i;
            layout.getComponentTree().locate(punIds[i]).setClickAction(
                    (context, player, primaryTrigger) -> {
                        currentPunishmentSetup.put(context, new PunishmentData(punTypes[index]));
                        context.setHidden(lookupView, true);
                        context.setHidden(punCreateGroup, false);
                    }
            );
        }
    }

    public void triggerLookup(Interaction interaction, String name) {
        interaction.setHidden(layout.getComponentTree().locate(SEARCH_VIEW_ID), true);

        String uuid = UUIDManager.get().getUUID(name);
        final PunishmentState punishmentState = new PunishmentState(name, uuid);

        currentLookup.put(interaction, punishmentState);
        interaction.setHidden(layout.getComponentTree().locate(LOOKUP_VIEW_ID), false);
    }

    public void setupPunishmentCreation() {
        layout.registerTextProcessor(new CreationTextProcessor(currentPunishmentSetup));

        final Font font = ResourceManager.getInstance().getFont(FONT_NAME, 13f);

        // Bind reason input
        final TextInputComponent reasonInput = new TextInputComponent(
                "", null,
                67, 36, 50, 8, 0,
                TRANSPARENT, TRANSPARENT, font, LIGHT_GREY, GREY,
                "NONE", ""
        );

        reasonInput.setInputHandler(TextInputComponent.limitHandler(
                (interaction, text) -> {
                    currentPunishmentSetup.get(interaction).setReason(text);
                    return text;
                },
                10
        ));

        // Bind duration input
        final TextInputComponent durationInput = new TextInputComponent(
                "", null,
                67, 77, 50, 8, 0,
                TRANSPARENT, TRANSPARENT, font, LIGHT_GREY, GREY,
                "Duration", PunishmentData.DEFAULT_DURATION
        );

        durationInput.setInputHandler(TextInputComponent.limitHandler(
                (interaction, text) -> {
                    currentPunishmentSetup.get(interaction).setDuration(text);
                    return text;
                },
                10
        ));

        ((DummyComponent) layout.getComponentTree().locate(PUN_CREATE_REASON_INPUT_ID)).setComponent(reasonInput);
        ((DummyComponent) layout.getComponentTree().locate(PUN_CREATE_DURATION_INPUT_ID)).setComponent(durationInput);

        // Validate duration format
        ((CheckComponent) layout.getComponentTree().locate(PUN_CREATE_DURATION_INPUT_VALIDATION_ID)).setCheck(
                (context, player, primaryTrigger) -> {
                    final PunishmentData punishmentData = currentPunishmentSetup.get(context);
                    return punishmentData != null && punishmentData.getDuration().matches("[1-9][0-9]*([wdhms]|mo)|#.+");
                }
        );

        // Bind permanent input
        final CheckComponent tempSwitch = (CheckComponent) layout.getComponentTree().locate(PUN_CREATE_TEMP_SWITCH_ID);
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
        final CheckComponent silentSwitch = (CheckComponent) layout.getComponentTree().locate(PUN_CREATE_SILENT_SWITCH_ID);
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

        final Component lookupView = layout.getComponentTree().locate(LOOKUP_VIEW_ID);
        final Component createView = layout.getComponentTree().locate(PUN_CREATE_VIEW_ID);

        // Cancel creation
        layout.getComponentTree().locate(PUN_CREATE_CANCEL_BTN_ID).setClickAction(
                (context, player, primaryTrigger) -> {
                    reasonInput.setInput(context, "", false);
                    durationInput.setInput(context, PunishmentData.DEFAULT_DURATION, false);

                    context.setHidden(createView, true);
                    context.setHidden(lookupView, false);
                    currentPunishmentSetup.remove(context);
                }
        );

        // Only show permanent switch for possible temp punishments
        ((CheckComponent) layout.getComponentTree().locate(PUN_CREATE_TEMP_CHECK_ID)).setCheck(
                (context, player, primaryTrigger) -> {
                    final PunishmentData punishmentData = currentPunishmentSetup.get(context);
                    if (punishmentData == null)
                        return false;
                    final PunishmentType type = punishmentData.getBasicType();
                    return type != PunishmentType.KICK && type != PunishmentType.NOTE;
                }
        );

        final Component statusBtn = layout.getComponentTree().locate(LOOKUP_STATUS_BTN_ID);

        // Perform punishment
        layout.getComponentTree().locate(PUN_CREATE_CREATE_BTN_ID).setClickAction(
                (context, player, primaryTrigger) -> {

                    Bukkit.getScheduler().runTaskLater(AdvancedGUI.getInstance(), () -> {
                        reasonInput.setInput(context, "", false);
                        durationInput.setInput(context, PunishmentData.DEFAULT_DURATION, false);

                        context.setHidden(createView, true);
                        context.setHidden(lookupView, false);

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
