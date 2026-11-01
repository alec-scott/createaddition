package com.mrh0.createaddition.util;

import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettingsHandleBehaviour;
import org.jetbrains.annotations.Nullable;

/**
 * PORT-NOTE(26.1): a block entity with several value settings. Create Fly's ValueSettingsPacket handler only looks up
 * the ServerScrollValueBehaviour.TYPE behaviour; {@code mixin.AllHandleMixin} swaps in the one matching the packet's
 * behaviour index (netId).
 */
public interface IMultiValueSettings {
    @Nullable
    ValueSettingsHandleBehaviour getValueSettingsBehaviour(int netId);
}
