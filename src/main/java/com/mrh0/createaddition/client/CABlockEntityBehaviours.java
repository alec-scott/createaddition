package com.mrh0.createaddition.client;

import com.mrh0.createaddition.blocks.alternator.AlternatorBlockEntity;
import com.mrh0.createaddition.blocks.electric_motor.ElectricMotorBlock;
import com.mrh0.createaddition.blocks.electric_motor.ElectricMotorBlockEntity;
import com.mrh0.createaddition.blocks.electric_pump.ElectricPumpBlock;
import com.mrh0.createaddition.blocks.electric_pump.ElectricPumpBlockEntity;
import com.mrh0.createaddition.blocks.modular_accumulator.ModularAccumulatorBlockEntity;
import com.mrh0.createaddition.blocks.rolling_mill.RollingMillBlockEntity;
import com.mrh0.createaddition.blocks.servo_motor.ServoMotorBlock;
import com.mrh0.createaddition.blocks.servo_motor.ServoMotorBlockEntity;
import com.mrh0.createaddition.blocks.tesla_coil.TeslaCoilBlockEntity;
import com.mrh0.createaddition.index.CABlockEntities;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.KineticScrollValueBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.client.foundation.sound.SoundScapes;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Function;

/**
 * PORT-NOTE(26.1): Create Fly keeps a block entity's client-only parts (goggle tooltips, ambient audio, value boxes) in
 * client behaviours registered per type, like its own {@code AllBlockEntityBehaviours}.
 */
public final class CABlockEntityBehaviours {
    private CABlockEntityBehaviours() {
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private static <T extends SmartBlockEntity> void add(BlockEntityType<T> type, Function<T, BlockEntityBehaviour<?>>... factories) {
        for (Function<T, BlockEntityBehaviour<?>> factory : factories)
            BlockEntityBehaviour.CLIENT_REGISTRY.add(type, (Function<SmartBlockEntity, BlockEntityBehaviour<?>>) factory);
    }

    public static void register() {
        add(CABlockEntities.ELECTRIC_MOTOR.get(),
                CAKineticTooltipBehaviour.Generating::new,
                be -> new CAKineticAudioBehaviour<>(be, ElectricMotorBlockEntity::tickAudio),
                be -> new KineticScrollValueBehaviour(CreateLang.translateDirect("generic.speed"), be,
                        new CenteredSideValueBoxTransform((state, side) -> state.getValue(ElectricMotorBlock.FACING) == side.getOpposite())));
        // Servo value boxes: the switcher swaps in the box for the looked-at side (Create Fly handles one per block entity)
        add(CABlockEntities.SERVO_MOTOR.get(),
                CAKineticTooltipBehaviour.Generating::new,
                be -> new CAKineticAudioBehaviour<>(be, ServoMotorBlockEntity::tickAudio),
                ServoValueBoxes::speed,
                ServoValueBoxes.Switcher::new);
        add(CABlockEntities.ALTERNATOR.get(),
                CAKineticTooltipBehaviour.Consuming::new,
                be -> new CAKineticAudioBehaviour<>(be, AlternatorBlockEntity::tickAudio));
        add(CABlockEntities.ELECTRIC_PUMP.get(),
                CAKineticTooltipBehaviour.Consuming::new,
                be -> new CAKineticAudioBehaviour<>(be, ElectricPumpBlockEntity::tickAudio),
                be -> new ScrollValueBehaviour<>(CreateLang.translateDirect("generic.speed"), be,
                        new CenteredSideValueBoxTransform((state, side) -> side.getAxis() != state.getValue(ElectricPumpBlock.FACING).getAxis())));
        add(CABlockEntities.ROLLING_MILL.get(),
                CAKineticTooltipBehaviour.Consuming::new,
                be -> new CAKineticAudioBehaviour<>(be, CABlockEntityBehaviours::rollingMillAudio));

        add(CABlockEntities.SMALL_CONNECTOR.get(), CATooltipBehaviour::new);
        add(CABlockEntities.SMALL_LIGHT_CONNECTOR.get(), CATooltipBehaviour::new);
        add(CABlockEntities.LARGE_CONNECTOR.get(), CATooltipBehaviour::new);
        add(CABlockEntities.REDSTONE_RELAY.get(), CATooltipBehaviour::new);
        add(CABlockEntities.TESLA_COIL.get(), CATooltipBehaviour::new,
                be -> new CAClientTickBehaviour<>(be, TeslaCoilBlockEntity::tickAudio));
        add(CABlockEntities.LIQUID_BLAZE_BURNER.get(), CATooltipBehaviour::new);
        add(CABlockEntities.MODULAR_ACCUMULATOR.get(), CATooltipBehaviour::new,
                be -> new CAClientTickBehaviour<>(be, ModularAccumulatorBlockEntity::tickAudio));
    }

    // Was RollingMillBlockEntity.tickAudio (as Create Fly's MillstoneAudioBehaviour)
    private static void rollingMillAudio(RollingMillBlockEntity be) {
        if (be.getSpeed() == 0) return;
        if (be.getInput().isEmpty()) return;
        float pitch = Mth.clamp((Math.abs(be.getSpeed()) / 256f) + .45f, .85f, 1f);
        SoundScapes.play(SoundScapes.AmbienceGroup.MILLING, be.getBlockPos(), pitch);
    }
}
