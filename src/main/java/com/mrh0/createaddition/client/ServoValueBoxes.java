package com.mrh0.createaddition.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrh0.createaddition.blocks.servo_motor.ServoMotorBlock;
import com.mrh0.createaddition.blocks.servo_motor.ServoMotorBlockEntity;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.lang.Lang;
import com.zurrtum.create.client.flywheel.lib.transform.TransformStack;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsBoard;
import com.zurrtum.create.client.foundation.blockEntity.ValueSettingsFormatter;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import com.zurrtum.create.content.contraptions.IControlContraption.RotationMode;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettings;
import com.zurrtum.create.foundation.blockEntity.behaviour.ValueSettingsHandleBehaviour;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollValueBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * PORT-NOTE(26.1): all four servo value boxes in one client behaviour. Create Fly's client (ValueSettingsInputHandler,
 * ScrollValueRenderer) only looks at the single ScrollValueBehaviour.TYPE behaviour of a block entity, so this one
 * picks the setting from the side being looked at: back = speed, top = max angle, bottom = min angle, the remaining
 * two sides = movement mode (the same sides as the 1.21.1 boxes). The server side routes the settings by netId
 * (IMultiValueSettings + AllHandleMixin).
 */
public class ServoValueBoxes extends ScrollValueBehaviour<SmartBlockEntity, ServerScrollValueBehaviour> {
    private static final int SPEED = 0, MODE = 1, MAX = 2, MIN = 3;

    private final ServoMotorBlockEntity servo;
    private final Slot slot;

    public ServoValueBoxes(ServoMotorBlockEntity servo) {
        this(servo, new Slot());
    }

    private ServoValueBoxes(ServoMotorBlockEntity servo, Slot slot) {
        super(CreateLang.translateDirect("generic.speed"), servo, slot);
        this.servo = servo;
        this.slot = slot;
        withFormatter(v -> String.valueOf(Math.abs(v)));
    }

    private int current() {
        return slot.index(servo.getBlockState());
    }

    private ServerScrollValueBehaviour target() {
        ValueSettingsHandleBehaviour handle = servo.getValueSettingsBehaviour(current());
        return handle instanceof ServerScrollValueBehaviour scroll ? scroll : behaviour;
    }

    private void updateLabel() {
        label = switch (current()) {
            case MODE -> CreateLang.translateDirect("contraptions.movement_mode");
            case MAX -> Component.literal("Max Angle");
            case MIN -> Component.literal("Min Angle");
            default -> CreateLang.translateDirect("generic.speed");
        };
    }

    @Override
    public boolean testHit(Vec3 hit) {
        updateLabel();
        return super.testHit(hit);
    }

    @Override
    public String formatValue() {
        int value = target().getValue();
        return switch (current()) {
            case MODE -> Component.translatable("create.contraptions.movement_mode." + Lang.asId(RotationMode.values()[Math.clamp(value, 0, 2)].name())).getString();
            case MAX -> value + "°";
            case MIN -> -value + "°";
            default -> String.valueOf(Math.abs(value));
        };
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        updateLabel();
        return switch (current()) {
            case MODE -> new ValueSettingsBoard(label, 2, 1, ImmutableList.of(Component.literal("Select")),
                    new ValueSettingsFormatter(vs -> Component.translatable("create.contraptions.movement_mode."
                            + Lang.asId(RotationMode.values()[Math.clamp(vs.value(), 0, 2)].name()))));
            case MAX, MIN -> {
                boolean negated = current() == MIN;
                yield new ValueSettingsBoard(label, 180, 5, ImmutableList.of(Component.literal("°")),
                        new ValueSettingsFormatter(vs -> Component.literal((negated ? -vs.value() : vs.value()) + "°")));
            }
            default -> new ValueSettingsBoard(label, 256, 32,
                    ImmutableList.of(Component.literal("⟳").withStyle(ChatFormatting.BOLD), Component.literal("⟲").withStyle(ChatFormatting.BOLD)),
                    new ValueSettingsFormatter(this::formatSpeed));
        };
    }

    private MutableComponent formatSpeed(ValueSettings settings) {
        return CreateLang.number(Math.max(1, Math.abs(settings.value())))
                .add(CreateLang.text(settings.row() == 0 ? "⟳" : "⟲").style(ChatFormatting.BOLD)).component();
    }

    @Override
    public ValueSettings getValueSettings() {
        return target().getValueSettings();
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
        target().setValueSettings(player, valueSetting, ctrlDown);
    }

    @Override
    public boolean mayInteract(Player player) {
        return target().mayInteract(player);
    }

    @Override
    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        target().onShortInteract(player, hand, side, hitResult);
    }

    @Override
    public int netId() {
        return target().netId();
    }

    /** One sided slot for all four boxes; the side picks the setting (and the extender offset for the side boxes). */
    static class Slot extends CenteredSideValueBoxTransform {
        Slot() {
            super((state, side) -> index(state, side) >= 0);
        }

        int index(BlockState state) {
            return Math.max(0, index(state, getSide()));
        }

        static int index(BlockState state, Direction side) {
            if (!state.hasProperty(ServoMotorBlock.FACING)) return -1;
            if (state.getValue(ServoMotorBlock.FACING) == side.getOpposite()) return SPEED;
            if (ServoMotorBlockEntity.isMaxAngleSide(state, side)) return MAX;
            if (ServoMotorBlockEntity.isMinAngleSide(state, side)) return MIN;
            if (ServoMotorBlockEntity.isModeSide(state, side)) return MODE;
            return -1;
        }

        private boolean extended(BlockState state) {
            return index(state, getSide()) > SPEED;
        }

        @Override
        public Vec3 getLocalOffset(BlockState state) {
            Vec3 offset = super.getLocalOffset(state);
            if (!extended(state)) return offset;
            // As DirectionalExtenderScrollOptionSlot (the 1.21.1 side boxes)
            return offset.add(Vec3.atLowerCornerOf(state.getValue(ServoMotorBlock.FACING).getUnitVec3i()).scale(-2 / 16f));
        }

        @Override
        public void rotate(BlockState state, PoseStack ms) {
            if (extended(state) && !getSide().getAxis().isHorizontal())
                TransformStack.of(ms).rotateYDegrees(AngleHelper.horizontalAngle(state.getValue(ServoMotorBlock.FACING)) + 180);
            super.rotate(state, ms);
        }
    }
}
