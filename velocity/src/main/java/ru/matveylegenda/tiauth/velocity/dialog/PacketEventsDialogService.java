package ru.matveylegenda.tiauth.velocity.dialog;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
import com.github.retrooper.packetevents.protocol.dialog.action.DynamicCustomAction;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.dialog.input.Input;
import com.github.retrooper.packetevents.protocol.dialog.input.TextInputControl;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerClearDialog;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerShowDialog;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import ru.matveylegenda.tiauth.config.MainConfig;
import ru.matveylegenda.tiauth.velocity.TiAuth;
import ru.matveylegenda.tiauth.velocity.manager.AuthManager;

import java.util.ArrayList;
import java.util.List;

public final class PacketEventsDialogService implements VelocityDialogService {
    private static final ResourceLocation LOGIN_ACTION = new ResourceLocation("minecraft:tiauth_login");
    private static final ResourceLocation REGISTER_ACTION = new ResourceLocation("minecraft:tiauth_register");

    private final PacketEventsAPI<?> api;
    private final AuthManager authManager;
    private final PacketListenerCommon responseListener;

    public PacketEventsDialogService(TiAuth plugin, AuthManager authManager) {
        this.api = PacketEvents.getAPI();
        if (!api.isLoaded() || api.isTerminated()) {
            throw new IllegalStateException("PacketEvents API is not available");
        }

        this.authManager = authManager;
        this.responseListener = api.getEventManager().registerListener(new DialogResponseListener());
        plugin.getLogger().info("Velocity dialog support enabled through PacketEvents");
    }

    @Override
    public boolean isAvailable() {
        return !api.isTerminated();
    }

    @Override
    public void show(
            Player player,
            Component title,
            List<TextInput> inputs,
            Component confirmButton,
            String actionId,
            Component notice
    ) {
        MainConfig.Auth.Dialog settings = MainConfig.IMP.auth.dialog;
        List<Input> packetInputs = new ArrayList<>(inputs.size());
        for (TextInput input : inputs) {
            packetInputs.add(new Input(
                    input.key(),
                    new TextInputControl(
                            dialogSize(settings.inputWidth),
                            input.label(),
                            settings.showInputLabels,
                            "",
                            MainConfig.IMP.auth.maxPasswordLength,
                            null
                    )
            ));
        }

        List<DialogBody> body = notice == null
                ? List.of()
                : List.of(new PlainMessageDialogBody(
                        new PlainMessage(notice, dialogSize(settings.notificationWidth))
                ));

        CommonDialogData common = new CommonDialogData(
                title,
                null,
                false,
                false,
                DialogAction.NONE,
                body,
                packetInputs
        );
        ActionButton confirmAction = new ActionButton(
                new CommonButtonData(
                        confirmButton,
                        null,
                        dialogSize(settings.confirmButtonWidth)
                ),
                new DynamicCustomAction(new ResourceLocation(actionId), null)
        );
        Dialog dialog = new MultiActionDialog(common, List.of(confirmAction), null, 1);

        api.getPlayerManager().sendPacket(player, new WrapperPlayServerShowDialog(dialog));
    }

    @Override
    public void clear(Player player) {
        api.getPlayerManager().sendPacket(player, new WrapperPlayServerClearDialog());
    }

    @Override
    public void close() {
        if (!api.isTerminated()) {
            api.getEventManager().unregisterListener(responseListener);
        }
    }

    private int dialogSize(int size) {
        return Math.clamp(size, 1, 1024);
    }

    private final class DialogResponseListener extends PacketListenerAbstract {
        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != PacketType.Play.Client.CUSTOM_CLICK_ACTION) {
                return;
            }

            WrapperPlayClientCustomClickAction response = new WrapperPlayClientCustomClickAction(event);
            ResourceLocation action = response.getId();
            if (!LOGIN_ACTION.equals(action) && !REGISTER_ACTION.equals(action)) {
                return;
            }

            event.setCancelled(true);
            Player player = event.getPlayer();
            if (player == null) {
                return;
            }

            NBT payload = response.getPayload();
            NBTCompound values = payload instanceof NBTCompound compound ? compound : new NBTCompound();
            String password = values.getStringTagValueOrDefault("password", "").trim();

            if (LOGIN_ACTION.equals(action)) {
                authManager.loginPlayer(player, password);
            } else {
                String repeatPassword = MainConfig.IMP.auth.repeatPasswordWhenRegister
                        ? values.getStringTagValueOrDefault("repeatPassword", "").trim()
                        : password;
                authManager.registerPlayer(player, password, repeatPassword);
            }
        }
    }
}
