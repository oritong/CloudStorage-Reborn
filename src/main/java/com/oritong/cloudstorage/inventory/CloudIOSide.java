package com.oritong.cloudstorage.inventory;

import net.minecraft.core.Direction;

/** A face relative to the front of the chest; the layout follows Mekanism's face diagram. */
public enum CloudIOSide {
    FRONT(67, 37), LEFT(44, 37), RIGHT(90, 37), BACK(44, 60), TOP(67, 14), BOTTOM(67, 60);

    public final int x, y;
    CloudIOSide(int x, int y) { this.x = x; this.y = y; }

    public Direction direction(Direction front) {
        return switch (this) {
            case FRONT -> front;
            case BACK -> front.getOpposite();
            case LEFT -> front.getClockWise();
            case RIGHT -> front.getCounterClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }
}
