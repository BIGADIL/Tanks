package com.codenjoy.dojo.battlecity.model;

import com.codenjoy.dojo.battlecity.client.AISolver;
import com.codenjoy.dojo.battlecity.client.Board;
import com.codenjoy.dojo.battlecity.client.Deikstra;
import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.PointImpl;

import java.util.ArrayList;
import java.util.Objects;

public class Tank {

    /**
     * Enum для типов танков. Содержит элементы, соотв. действиям танков.
     */
    public enum TankType {
        OTHER(Elements.OTHER_TANK_LEFT, Elements.OTHER_TANK_RIGHT, Elements.OTHER_TANK_UP, Elements.OTHER_TANK_DOWN),
        AI(Elements.AI_TANK_LEFT, Elements.AI_TANK_RIGHT, Elements.AI_TANK_UP, Elements.AI_TANK_DOWN),
        OUR(Elements.TANK_LEFT, Elements.TANK_RIGHT, Elements.TANK_UP, Elements.TANK_DOWN);

        public final Elements left;
        public final Elements right;
        public final Elements up;
        public final Elements down;

        TankType(Elements left, Elements right, Elements up, Elements down) {
            this.left = left;
            this.right = right;
            this.down = down;
            this.up = up;
        }

        public Elements byDirection(final Direction direction) {
            switch (direction) {
                case LEFT:
                    return left;
                case DOWN:
                    return down;
                case UP:
                    return up;
                case RIGHT:
                    return right;
            }
            throw new IllegalStateException();
        }
    }

    /**
     * Точка поля, в которой находится танк.
     */
    private final PointImpl point;

    /**
     * Тип танка.
     */
    public final TankType tankType;


    public Tank(final PointImpl point, final TankType tankType) {
        this.point = point;
        this.tankType = tankType;
    }

    private Tank(final Tank tank) {
        point = new PointImpl(tank.point.getX(), tank.point.getY());
        tankType = tank.tankType;
    }

    public Tank getCopy() {
        return new Tank(this);
    }


    /**
     * Действуем согласно заданному направлению.
     *
     * @param direction направление
     * @param board     доска
     */
    public void act(final Direction direction, final Board board) {
        PointImpl tmpPoint = point.copy();

        tmpPoint.change(direction);

        if (board.isAnyTankAt(tmpPoint.getX(), tmpPoint.getY())) {
            return;
        }
        int x = point.getX();
        int y = point.getY();
        board.set(x, y, Elements.NONE.ch);
        board.set(tmpPoint.getX(), tmpPoint.getY(), tankType.byDirection(direction).ch);
    }

    /**
     * Получить положение танка
     *
     * @return положение танка
     */
    public PointImpl getPoint() {
        return point;
    }
}
