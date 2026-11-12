package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;

/** Only the recipe catalog travels S2C; all actions keep the native Sophisticated channel. */
public record CraftingCatalogPayload(int menu, int container, long request, boolean first,
        List<Entry> entries) implements CustomPacketPayload {
    public static final int PAGE_SIZE = 64;
    public static final Type<CraftingCatalogPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID, "crafting_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingCatalogPayload> CODEC = StreamCodec.of((buffer, value) -> {
        buffer.writeVarInt(value.menu()); buffer.writeVarInt(value.container()); buffer.writeLong(value.request());
        buffer.writeBoolean(value.first()); buffer.writeVarInt(value.entries().size());
        for (Entry entry : value.entries()) {
            Identifier.STREAM_CODEC.encode(buffer, entry.id());
            ItemStack.STREAM_CODEC.encode(buffer, entry.result());
        }
    }, buffer -> {
        int menu = buffer.readVarInt(), container = buffer.readVarInt(); long request = buffer.readLong();
        boolean first = buffer.readBoolean(); int count = buffer.readVarInt();
        if (count < 0 || count > PAGE_SIZE) throw new IllegalArgumentException("Invalid recipe catalog page");
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(new Entry(Identifier.STREAM_CODEC.decode(buffer), ItemStack.STREAM_CODEC.decode(buffer)));
        return new CraftingCatalogPayload(menu, container, request, first, List.copyOf(entries));
    });
    @Override public Type<CraftingCatalogPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof StorageContainerMenuBase<?> menu
                    && menu.containerId == payload.menu()
                    && menu.getUpgradeContainers().get(payload.container()) instanceof RemoteCraftingUpgradeContainer container) {
                container.receiveCatalog(payload);
            }
        }));
    }
    public record Entry(Identifier id, ItemStack result) {}
}
