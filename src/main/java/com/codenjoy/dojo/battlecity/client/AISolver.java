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

import com.codenjoy.dojo.battlecity.model.Elements;
import com.codenjoy.dojo.battlecity.model.Tank;
import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.services.algs.DeikstraFindWay;

import java.util.*;

public class AISolver implements Solver<Board> {

    public static final Random rnd = new Random();

    private Deikstra way;

    public AISolver(Dice dice) {
        this.way = new Deikstra();
    }

    public Deikstra.Possible possible(final Board board) {
        return new Deikstra.Possible() {
            @Override
            public boolean possible(Point from, Direction where) {
                int x = from.getX();
                int y = from.getY();
                if (board.isBarrierAt(x, y)) return false;

                Point newPt = where.change(from);
                int nx = newPt.getX();
                int ny = newPt.getY();

                if (board.isOutOfField(nx, ny)) return false;

                if (board.isBarrierAt(nx, ny)) return false;
                return !board.isBulletAt(nx, ny);
            }

            @Override
            public boolean possible(Point atWay) {
                return true;
            }
        };
    }

    @Override
    public String get(final Board board) {

        if (board.isGameOver()) return act("");
        List<Deikstra.ShortestWay> result = new LinkedList<>();
        Deikstra.ShortestWay directions = getDirections(board);
        if (directions == null) {
            return act(Direction.RIGHT.toString());
        }
        result.add(directions);

        Board copy = board.getCopy();
        for (int i = 0; i < 10; i++) {
            Deikstra.ShortestWay simWay = sim(copy);
            if (simWay == null) {
                result.add(new Deikstra.ShortestWay(Direction.RIGHT, (Math.pow(1.2, (i + 2)))));
            } else {
                simWay.weight /= (Math.pow(1.2, (i + 2)));
                result.add(simWay);
            }
        }

        Direction action = getAction(result);

        action = patchByBarriers(action, board);

        action = patchByTank(action, board);

        action = patchByBullet(action, board);

        return act(patchByBarriers(action, board).toString());
    }

    /**
     * Накладываем на выбранное направление патч, объезжающий препятствия
     *
     * @param direction направление
     * @param board     доска
     * @return направление
     */
    public Direction patchByBarriers(Direction direction, Board board) {
        Point me = board.getMe();

        Deikstra.Possible possible = possible(board);

        if (!possible.possible(me, direction)) {
            return rnd.nextInt(1) == 0 ? direction.clockwise() : direction.clockwise().inverted();
        }
        return direction;
    }

    /**
     * Накладываем на выбранное направление патч, связанный с танком
     *
     * @param direction направление
     * @param board     доска
     * @return направление
     * @// TODO: 29.10.2018 не всегда отрабатывает
     */
    public Direction patchByTank(Direction direction, Board board) {
        int x = board.getMe().getX();
        int y = board.getMe().getY();
        // если вокруг нас есть танк - шмаляем в него
        if (board.isOtherTankAt(x + 1, y)) {
            return Direction.RIGHT;
        } else if (board.isOtherTankAt(x - 1, y)) {
            return Direction.LEFT;
        } else if (board.isOtherTankAt(x, y - 1)) {
            return Direction.DOWN;
        } else if (board.isOtherTankAt(x, y + 1)) {
            return Direction.UP;
        }
        return direction;
    }

    /**
     * Накладываем на выбранное направление патч, связанный с пулями
     *
     * @param direction направление
     * @param board     доска
     * @return направление
     * @// TODO: 29.10.2018 не всегда отрабатывает
     */
    public Direction patchByBullet(Direction direction, Board board) {
        Point me = board.getMe();
        int x = me.getX();
        int y = me.getY();

        int size = board.size();

        Deikstra.Possible possible = possible(board);

        int delta1 = 1;
        int delta2 = 2;

        PointImpl pointRight = new PointImpl(x + delta1, y);
        PointImpl point2xRight = new PointImpl(x + delta2, y);

        PointImpl pointLeft = new PointImpl(x - delta1, y);
        PointImpl point2xLeft = new PointImpl(x - delta2, y);

        PointImpl pointUp = new PointImpl(x, y + delta1);
        PointImpl point2xUp = new PointImpl(x , y + delta2);

        PointImpl pointDown = new PointImpl(x, y - delta1);
        PointImpl point2xDown = new PointImpl(x, y - delta2);

        if (board.isBulletAt(pointRight) || (!point2xRight.isOutOf(size) && board.isBulletAt(point2xRight))) {
            return possible.possible(me, Direction.DOWN) ? Direction.DOWN : Direction.UP;
        } else if (board.isBulletAt(pointLeft) || (!point2xLeft.isOutOf(size) && board.isBulletAt(point2xLeft))) {
            return possible.possible(me, Direction.UP) ? Direction.UP : Direction.DOWN;
        } else if (board.isBulletAt(pointUp) || (!point2xUp.isOutOf(size) && board.isBulletAt(point2xUp))) {
            return possible.possible(me, Direction.LEFT) ? Direction.LEFT : Direction.RIGHT;
        } else if (board.isBulletAt(pointDown) || (!point2xDown.isOutOf(size) && board.isBulletAt(point2xDown))) {
            return possible.possible(me, Direction.RIGHT) ? Direction.RIGHT : Direction.LEFT;
        }
        return direction;
    }

    /**
     * Получить наиболее часто встречающееся направление из списка направлений.
     *
     * @param weightedDirections напраления
     * @return наиболее частое направление
     */
    public Direction getAction(List<Deikstra.ShortestWay> weightedDirections) {
        double[] directionWeight = new double[4];
        for (final Deikstra.ShortestWay direction : weightedDirections) {
            switch (direction.direction) {
                case UP:
                    directionWeight[0]++;
                    break;
                case DOWN:
                    directionWeight[1]++;
                    break;
                case LEFT:
                    directionWeight[2]++;
                    break;
                case RIGHT:
                    directionWeight[3]++;
                    break;
            }
        }

        if (directionWeight[0] > directionWeight[1] && directionWeight[0] > directionWeight[2] && directionWeight[0] > directionWeight[3]) {
            return Direction.UP;
        } else if (directionWeight[1] > directionWeight[2] && directionWeight[1] > directionWeight[3]) {
            return Direction.DOWN;
        } else if (directionWeight[2] > directionWeight[3]) {
            return Direction.LEFT;
        }
        return Direction.RIGHT;
    }

    private String act(String command) {
        return ((command.equals("") ? "" : command + ", ") + "ACT");
    }

    /**
     * Получить направления по кратчайшим путям до вражеских танков.
     *
     * @param board доска
     * @return направления
     */
    public Deikstra.ShortestWay getDirections(Board board) {
        int size = board.size();
        Point from = board.getMe();
        List<Point> to = board.get(Elements.AI_TANK_DOWN,
                Elements.AI_TANK_LEFT,
                Elements.AI_TANK_RIGHT,
                Elements.AI_TANK_UP,
                Elements.OTHER_TANK_DOWN,
                Elements.OTHER_TANK_LEFT,
                Elements.OTHER_TANK_RIGHT,
                Elements.OTHER_TANK_UP);
        Deikstra.Possible map = possible(board);
        return way.getShortestWay(size, from, to, map);
    }

    /**
     * Для данного танка получить направления кратчайших путей до врагов
     *
     * @param board доска
     * @param tank  танк
     * @return кратчайщие пути
     */
    public Deikstra.ShortestWay getDirections(Board board, Tank tank) {
        int size = board.size();
        Point from = tank.getPoint();
        List<Point> to = board.get(Elements.AI_TANK_DOWN,
                Elements.AI_TANK_LEFT,
                Elements.AI_TANK_RIGHT,
                Elements.AI_TANK_UP,
                Elements.OTHER_TANK_DOWN,
                Elements.OTHER_TANK_LEFT,
                Elements.OTHER_TANK_RIGHT,
                Elements.OTHER_TANK_UP,
                Elements.TANK_DOWN,
                Elements.TANK_UP,
                Elements.TANK_LEFT,
                Elements.TANK_RIGHT);
        Deikstra.Possible map = possible(board);

        to.removeIf(point -> point.getX() == from.getX() && point.getY() == from.getY());

        return way.getShortestWay(size, from, to, map);
    }

    /**
     * Просимулировать игровой ход
     *
     * @param board доска
     * @return кратчайщие направления для нашего танка на данном ходу
     */
    public Deikstra.ShortestWay sim(final Board board) {
        for (Tank tank : board.tanks) {
            // танки выбирают лучший из путей по Дейкстре
            Deikstra.ShortestWay directions = getDirections(board, tank);
            if (directions != null) {
                tank.act(directions.direction, board);
            } else {
                tank.act(Direction.RIGHT, board);
            }
        }
        for (Tank tank : board.tanks) {
            tank.actBullets(board);
        }
        // мы получаем все возможные кратчайшие пути
        return getDirections(board);
    }

    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://localhost:8080/codenjoy-contest/board/player/kek@lol.com?code=1576454547601200491",
                new AISolver(new RandomDice()),
                new Board());
    }

}