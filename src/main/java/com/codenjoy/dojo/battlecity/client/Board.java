package com.codenjoy.dojo.battlecity.client;

/*-
 * #%L
 * Codenjoy - it's a dojo-like platform from developers to developers.
 * %%
 * Copyright (C) 2018 Codenjoy
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.codenjoy.dojo.battlecity.model.*;
import com.codenjoy.dojo.client.AbstractBoard;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;
import com.codenjoy.dojo.services.algs.DeikstraFindWay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.codenjoy.dojo.services.PointImpl.pt;

public class Board extends AbstractBoard<Elements> {

    public ArrayList<Tank> tanks = new ArrayList<>();

    public Board() {
    }

    public void associateTanks() {
        tanks.clear();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                PointImpl point = new PointImpl(i, j);
                if (isAt(point, Elements.AI_TANK_DOWN, Elements.AI_TANK_LEFT, Elements.AI_TANK_RIGHT, Elements.AI_TANK_UP)) {
                    tanks.add(new Tank(point, Tank.TankType.AI));
                } else if (isAt(point, Elements.OTHER_TANK_LEFT, Elements.OTHER_TANK_DOWN, Elements.OTHER_TANK_RIGHT, Elements.OTHER_TANK_UP)) {
                    tanks.add(new Tank(point, Tank.TankType.OTHER));
                } else if (isAt(point, Elements.TANK_DOWN, Elements.TANK_LEFT, Elements.TANK_RIGHT, Elements.TANK_UP)) {
                    tanks.add(new Tank(point, Tank.TankType.OUR));
                }
            }
        }
        System.out.println("Tanks:" + tanks.size());

        System.err.println(field.length);
        System.err.println(field[0].length);
        System.err.println(field[0][0].length);
    }

    @Override
    public Elements valueOf(char ch) {
        return Elements.valueOf(ch);
    }

    @Override
    protected int inversionY(int y) { // TODO разобраться с этим чудом
        return size - 1 - y;
    }

    public boolean isBarrierAt(int x, int y) {
        return isAt(x, y, Elements.BATTLE_WALL) ||
                isAt(x, y, Elements.CONSTRUCTION) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_DOWN) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_UP) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_LEFT) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_RIGHT) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_DOWN_TWICE) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_UP_TWICE) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_LEFT_TWICE) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_RIGHT_TWICE) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_LEFT_RIGHT) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_UP_DOWN) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_UP_LEFT) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_RIGHT_UP) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_DOWN_LEFT) ||
                isAt(x, y, Elements.CONSTRUCTION_DESTROYED_DOWN_RIGHT);
    }

    public boolean isBarrierAt(final PointImpl point) {
        return isBarrierAt(point.getX(), point.getY());
    }

    public Point getMe() {

        List<Point> points = get(Elements.TANK_UP, Elements.TANK_DOWN, Elements.TANK_LEFT, Elements.TANK_RIGHT);

        return points.isEmpty() ? null : points.get(0);
    }

    public boolean isGameOver() {
        return get(Elements.TANK_UP,
                Elements.TANK_DOWN,
                Elements.TANK_LEFT,
                Elements.TANK_RIGHT).isEmpty();
    }

    public boolean isBulletAt(int x, int y) {
        return getAt(x, y).equals(Elements.BULLET);
    }

    public boolean isBulletAt(Point point) {
        return isBulletAt(point.getX(), point.getY());
    }

    /**
     * Есть ли в точке танк
     *
     * @param point точка
     * @return ссылка на танк в данной точке, либо null
     */
    public Tank tankAt(final PointImpl point) {
        for (final Tank tank : tanks) {
            if (tank.getPoint().equals(point)) return tank;
        }
        return null;
    }

    /**
     * Есть ли в данной точке чужой танк
     *
     * @param x x
     * @param y y
     * @return есть ли в точке танк
     */
    public boolean isOtherTankAt(int x, int y) {
        return isAt(x, y, Elements.AI_TANK_DOWN,
                Elements.AI_TANK_LEFT,
                Elements.AI_TANK_RIGHT,
                Elements.AI_TANK_UP,
                Elements.OTHER_TANK_DOWN,
                Elements.OTHER_TANK_LEFT,
                Elements.OTHER_TANK_RIGHT,
                Elements.OTHER_TANK_UP);
    }


    public boolean isAnyTankAt(int x, int y) {
        return isAt(x, y, Elements.AI_TANK_DOWN,
                Elements.AI_TANK_LEFT,
                Elements.AI_TANK_RIGHT,
                Elements.AI_TANK_UP,
                Elements.OTHER_TANK_DOWN,
                Elements.OTHER_TANK_LEFT,
                Elements.OTHER_TANK_RIGHT,
                Elements.OTHER_TANK_UP,
                Elements.TANK_DOWN,
                Elements.TANK_LEFT,
                Elements.TANK_RIGHT,
                Elements.TANK_UP);
    }

    public boolean isMeAt(int x, int y) {
        return isAt(x, y, Elements.TANK_DOWN,
                Elements.TANK_LEFT,
                Elements.TANK_RIGHT,
                Elements.TANK_UP);
    }

    public boolean isAITank(int x, int y) {
        return isAt(x, y, Elements.AI_TANK_DOWN,
                Elements.AI_TANK_LEFT,
                Elements.AI_TANK_RIGHT,
                Elements.AI_TANK_UP);
    }

    public boolean isOtherTank(int x, int y) {
        return isAt(x, y,
                Elements.OTHER_TANK_DOWN,
                Elements.OTHER_TANK_LEFT,
                Elements.OTHER_TANK_RIGHT,
                Elements.OTHER_TANK_UP);
    }

    public boolean isRightTank(int x, int y) {
        return isAt(x, y,
                Elements.AI_TANK_RIGHT,
                Elements.OTHER_TANK_RIGHT,
                Elements.TANK_RIGHT);
    }

    public boolean isLeftTank(int x, int y) {
        return isAt(x, y,
                Elements.AI_TANK_LEFT,
                Elements.OTHER_TANK_LEFT,
                Elements.TANK_LEFT);
    }

    public boolean isUpTank(int x, int y) {
        return isAt(x, y,
                Elements.AI_TANK_UP,
                Elements.OTHER_TANK_UP,
                Elements.TANK_UP);
    }

    public boolean isDownTank(int x, int y) {
        return isAt(x, y,
                Elements.AI_TANK_DOWN,
                Elements.OTHER_TANK_DOWN,
                Elements.TANK_DOWN);
    }

    /**
     * Копия борда
     *
     * @return копия борда
     */
    public Board getCopy() {
        Board board = new Board();
        board.size = size;
        board.field = new char[field.length][][];
        for (int i = 0; i < field.length; i++) {
            board.field[i] = new char[field[i].length][];
            for (int j = 0; j < field[i].length; j++) {
                board.field[i][j] = new char[field[i][j].length];
            }
        }

        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[i].length; j++) {
                if (field[i][j].length >= 0) System.arraycopy(field[i][j], 0, board.field[i][j], 0, field[i][j].length);
            }
        }
        board.source = source;
        board.layersString = layersString;
        for (Tank tank : tanks) {
            board.tanks.add(tank.getCopy());
        }
        return board;
    }
}
