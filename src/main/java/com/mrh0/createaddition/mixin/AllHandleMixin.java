package com.mrh0.createaddition.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mrh0.createaddition.util.IMultiValueSettings;
import com.zurrtum.create.AllHandle;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettingsHandleBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import com.zurrtum.create.infrastructure.packet.c2s.ValueSettingsPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// PORT-NOTE(26.1): routes value settings to the right behaviour on block entities with several (see IMultiValueSettings)
@Mixin(AllHandle.class)
public abstract class AllHandleMixin {
    @ModifyVariable(method = "handleValueSettings", at = @At("HEAD"), argsOnly = true)
    private static @Nullable ValueSettingsHandleBehaviour createaddition$routeValueSettings(
            @Nullable ValueSettingsHandleBehaviour handle, @Local(argsOnly = true) ValueSettingsPacket packet) {
        if (handle instanceof ServerScrollValueBehaviour scroll && scroll.blockEntity instanceof IMultiValueSettings multi) {
            ValueSettingsHandleBehaviour routed = multi.getValueSettingsBehaviour(packet.behaviourIndex());
            if (routed != null) return routed;
        }
        return handle;
    }
}
