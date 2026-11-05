package com.mrh0.createaddition.compat.jei.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/** A rolling mill (with its two animated shafts) drawn centred on the PiP box, see {@link RollingMillRenderer}. */
public record RollingMillRenderState(Matrix3x2f pose, int x0, int y0,
                                     ScreenRectangle bounds) implements PictureInPictureRenderState {
    public static final int SIZE = 40;

    public RollingMillRenderState(Matrix3x2f pose, int x, int y) {
        this(pose, x, y, new ScreenRectangle(x, y, SIZE, SIZE).transformMaxBounds(pose));
    }

    @Override
    public int x1() {
        return x0 + SIZE;
    }

    @Override
    public int y1() {
        return y0 + SIZE;
    }

    @Override
    public float scale() {
        return 22;
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return null;
    }
}
