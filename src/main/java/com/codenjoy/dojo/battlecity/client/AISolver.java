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

    private DeikstraFindWay way;

    public AISolver(Dice dice) {
        this.way = new DeikstraFindWay();
    }

    public DeikstraFindWay.Possible possible(final Board board) {
        return new DeikstraFindWay.Possible() {
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
        List<Direction> result = getDirections(board);
        if (result.isEmpty()) return act(Direction.RIGHT.toString());

        for (int i = 0; i < 50; i++) {
            result.addAll(sim(board));
        }

        Direction action = getAction(result);

        action = patchByBarriers(action, board);

        action = patchByTank(action, board);

        action = patchByBullet(action, board);

        return act(patchByBarriers(action, board).toString());
    }

    /**
     * Накладываем на выбранное направление патч, объезжающий препятствия
     * @param direction направление
     * @param board доска
     * @return направление
     */
    public Direction patchByBarriers(Direction direction, Board board) {
        int x = board.getMe().getX();
        int y = board.getMe().getY();

        // если на нашем пути есть препядствме - объедем его
        switch (direction) {
            case RIGHT:
                if (board.isBarrierAt(x + 1, y )) {
                    return rnd.nextInt(1) == 0 ? direction.clockwise() : direction.clockwise().inverted();
                }
                break;
            case LEFT:
                if (board.isBarrierAt(x - 1, y)) {
                    return rnd.nextInt(1) == 0 ? direction.clockwise() : direction.clockwise().inverted();
                }
                break;
            case DOWN:
                if (board.isBarrierAt(x, y + 1)) {
                    return rnd.nextInt(1) == 0 ? direction.clockwise() : direction.clockwise().inverted();
                }
                break;
            case UP:
                if (board.isBarrierAt(x, y - 1)) {
                    return rnd.nextInt(1) == 0 ? direction.clockwise() : direction.clockwise().inverted();
                }
                break;
        }
        return direction;
    }

    /**
     * Накладываем на выбранное направление патч, связанный с танком
     * @param direction направление
     * @param board доска
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
            return Direction.UP;
        } else if (board.isOtherTankAt(x, y + 1)) {
            return Direction.DOWN;
        }
        return direction;
    }

    /**
     * Накладываем на выбранное направление патч, связанный с пулями
     * @param direction направление
     * @param board доска
     * @return направление
     * @// TODO: 29.10.2018 не всегда отрабатывает
     */
    public Direction patchByBullet(Direction direction, Board board) {
        int x = board.getMe().getX();
        int y = board.getMe().getY();
        if (board.isAt(x + 1, y, Elements.BULLET)) {
            return Direction.DOWN;
        } else if (board.isAt(x - 1, y, Elements.BULLET)) {
            return Direction.UP;
        } else if (board.isAt(x, y - 1, Elements.BULLET)) {
            return Direction.LEFT;
        } else if (board.isAt(x, y + 1, Elements.BULLET)) {
            return Direction.RIGHT;
        }
        return direction;
    }

    /**
     * Получить наиболее часто встречающееся направление из списка направлений.
     * @param directions напраления
     * @return наиболее частое направление
     */
    public Direction getAction(List<Direction> directions) {
        long[] probs = new long[4];
        for (Direction d : directions) {
            switch (d) {
                case UP:
                    probs[0]++;
                    break;
                case DOWN:
                    probs[1]++;
                    break;
                case LEFT:
                    probs[2]++;
                    break;
                case RIGHT:
                    probs[3]++;
                    break;
            }
        }

        if (probs[0] > probs[1] && probs[0] > probs[2] && probs[0] > probs[3]) {
            return Direction.UP;
        } else if (probs[1] > probs[2] && probs[1] > probs[3]) {
            return Direction.DOWN;
        } else if (probs[2] > probs[3]) {
            return Direction.LEFT;
        }
        return Direction.RIGHT;
    }

    private String act(String command) {
        return ((command.equals("") ? "" : command + ", ") + "ACT");
    }

    /**
     * Получить направления по кратчайшим путям до вражеских танков.
     * @param board доска
     * @return направления
     */
    public List<Direction> getDirections(Board board) {
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
        DeikstraFindWay.Possible map = possible(board);
        return way.getShortestWay(size, from, to, map);
    }

    /**
     * Для данного танка получить направления кратчайших путей до врагов
     * @param board доска
     * @param tank танк
     * @return кратчайщие пути
     */
    public List<Direction> getDirections(Board board, Tank tank) {
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
        DeikstraFindWay.Possible map = possible(board);
        return way.getShortestWay(size, from, to, map);
    }

    /**
     * Просимулировать игровой ход
     * @param board доска
     * @return кратчайщие направления для нашего танка на данном ходу
     */
    public List<Direction> sim(final Board board) {
        for (Tank tank : board.tanks) {
            // танки выбирают лучший из путей по Дейкстре
            List<Direction> directions = getDirections(board, tank);
            Direction action = getAction(directions);
            tank.act(action, board);
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
                "http://localhost:8080/codenjoy-contest/board/player/kek@lol.com?code=15764545471366816317",
                new AISolver(new RandomDice()),
                new Board());
    }

}