package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.menu.SpecialChestMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CuriousChestsMod.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> BOTTOMLESS = register("bottomless", ChestKind.BOTTOMLESS);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> INFERNAL = register("infernal", ChestKind.INFERNAL);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> ENDER_DISPATCH = register("ender_dispatch", ChestKind.ENDER_DISPATCH);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> BUILDERS = register("builders", ChestKind.BUILDERS);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> COLLECTORS = register("collectors", ChestKind.COLLECTORS);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> SCULK_SENTINEL = register("sculk_sentinel", ChestKind.SCULK_SENTINEL);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> RESONANT = register("resonant", ChestKind.RESONANT);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> ARCHIVIST = register("archivist", ChestKind.ARCHIVIST);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> WITCH = register("witch", ChestKind.WITCH);
    public static final DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> TRAPPER = register("trapper", ChestKind.TRAPPER);

    private static DeferredHolder<MenuType<?>, MenuType<SpecialChestMenu>> register(String id, ChestKind kind) {
        return MENUS.register(id, () -> new MenuType<>(
                (containerId, inv) -> SpecialChestMenu.client(forKindUnsafe(kind), containerId, inv, kind),
                FeatureFlags.DEFAULT_FLAGS
        ));
    }

    private static MenuType<SpecialChestMenu> forKindUnsafe(ChestKind kind) {
        return forKind(kind);
    }

    public static MenuType<SpecialChestMenu> forKind(ChestKind kind) {
        return switch (kind) {
            case BOTTOMLESS -> BOTTOMLESS.get();
            case INFERNAL -> INFERNAL.get();
            case ENDER_DISPATCH -> ENDER_DISPATCH.get();
            case BUILDERS -> BUILDERS.get();
            case COLLECTORS -> COLLECTORS.get();
            case SCULK_SENTINEL -> SCULK_SENTINEL.get();
            case RESONANT -> RESONANT.get();
            case ARCHIVIST -> ARCHIVIST.get();
            case WITCH -> WITCH.get();
            case TRAPPER -> TRAPPER.get();
        };
    }

    private ModMenus() {}
}
