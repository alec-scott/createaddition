package com.mrh0.createaddition.client;

import com.google.common.collect.ImmutableList;
import com.mrh0.createaddition.blocks.servo_motor.ServoMotorBlock;
import com.mrh0.createaddition.blocks.servo_motor.ServoMotorBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.KineticScrollValueBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.RotationModeScrollBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.content.contraptions.IControlContraption.RotationMode;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.BehaviourType;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollOptionBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * PORT-NOTE(26.1): the servo's four value boxes. Create Fly's client (ValueSettingsInputHandler, ScrollValueRenderer)
 * only looks at the one ScrollValueBehaviour.TYPE behaviour of a block entity, so {@link Switcher} swaps in the box
 * for the side being looked at: back = speed (Create's kinetic speed box), the two sides = movement mode (Create's
 * bearing rotation mode box, icons included), top = max angle, bottom = min angle (as in 1.21.1). Each box is bound to
 * its own server behaviour, so the settings go out with the right netId (IMultiValueSettings + AllHandleMixin).
 */
public final class ServoValueBoxes {
    private ServoValueBoxes() {
    }

    /** Back side: the generated speed (bound by type to ServerScrollValueBehaviour.TYPE, which is the speed). */
    public static KineticScrollValueBehaviour speed(ServoMotorBlockEntity be) {
        return new KineticScrollValueBehaviour(CreateLang.translateDirect("generic.speed"), be,
                new CenteredSideValueBoxTransform((state, side) -> state.getValue(ServoMotorBlock.FACING) == side.getOpposite()));
    }

    /** Side faces: Create Fly's bearing rotation mode box, bound to the servo's own movement mode behaviour. */
    public static class Mode extends RotationModeScrollBehaviour {
        @SuppressWarnings("unchecked")
        public Mode(ServoMotorBlockEntity be) {
            super(be);
            behaviour = (ServerScrollOptionBehaviour<RotationMode>) be.getValueSettingsBehaviour(1);
        }
    }

    /** Top (max) and bottom (min) angle limits, 0-180 in steps of 5, as in 1.21.1. */
    public static class Angle extends ScrollValueBehaviour<SmartBlockEntity, ServerScrollValueBehaviour> {
        private final boolean negated;

        public Angle(ServoMotorBlockEntity be, boolean max) {
            super(Component.literal(max ? "Max Angle" : "Min Angle"), be,
                    new DirectionalExtenderScrollOptionSlot(max ? ServoMotorBlockEntity::isMaxAngleSide : ServoMotorBlockEntity::isMinAngleSide));
            this.negated = !max;
            behaviour = (ServerScrollValueBehaviour) be.getValueSettingsBehaviour(max ? 2 : 3);
            withFormatter(v -> (negated ? -v : v) + "°");
        }

        @Override
        public void initialize() {
        }

        @Override
        public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
            ValueSettingsFormatter fmt = new ValueSettingsFormatter(vs -> Component.literal((negated ? -vs.value() : vs.value()) + "°"));
            return new ValueSettingsBoard(label, 180, 5, ImmutableList.of(Component.literal("°")), fmt);
        }
    }

    /** Puts the box for the looked-at side in the ScrollValueBehaviour.TYPE slot (client tick). */
    public static class Switcher extends BlockEntityBehaviour<ServoMotorBlockEntity> {
        public static final BehaviourType<Switcher> TYPE = new BehaviourType<>();
        private ScrollValueBehaviour<?, ?> speed;
        private Mode mode;
        private Angle max;
        private Angle min;

        public Switcher(ServoMotorBlockEntity be) {
            super(be);
        }

        @Override
        public BehaviourType<?> getType() {
            return TYPE;
        }

        @Override
        public void tick() {
            if (speed == null) {
                speed = blockEntity.getBehaviour(ScrollValueBehaviour.TYPE);
                if (speed == null) return;
                mode = new Mode(blockEntity);
                max = new Angle(blockEntity, true);
                min = new Angle(blockEntity, false);
            }
            if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(blockEntity.getBlockPos()))
                return;
            ScrollValueBehaviour<?, ?> wanted = forSide(blockEntity.getBlockState(), hit.getDirection());
            if (wanted != null && blockEntity.getBehaviour(ScrollValueBehaviour.TYPE) != wanted)
                blockEntity.attachBehaviourLate(wanted);
        }

        private ScrollValueBehaviour<?, ?> forSide(BlockState state, Direction side) {
            if (!state.hasProperty(ServoMotorBlock.FACING)) return null;
            if (state.getValue(ServoMotorBlock.FACING) == side.getOpposite()) return speed;
            if (ServoMotorBlockEntity.isMaxAngleSide(state, side)) return max;
            if (ServoMotorBlockEntity.isMinAngleSide(state, side)) return min;
            if (ServoMotorBlockEntity.isModeSide(state, side)) return mode;
            return null;
        }
    }
}
