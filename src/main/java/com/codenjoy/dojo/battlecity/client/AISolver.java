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

import java.util.*;

public class AISolver implements Solver<Board> {

    public static final Random rnd = new Random();

    private Deikstra way;

    public AISolver(Dice dice) {
        this.way = new Deikstra();
    }

    public static Deikstra.Possible possible(final Board board) {
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

        board.associateTanks();

        List<Deikstra.ShortestWay> directions = getDirections(board);
        if (directions == null || directions.isEmpty()) {
            return act(Direction.RIGHT.toString());
        }

        if (directions.get(0).weight <= 5) {
            Direction action = directions.get(0).direction;
            action = patchByBullet(action, board);
            return act(action.toString());
        }

        double totalWeight = 0;
        for (Deikstra.ShortestWay direction : directions) {
            direction.weight = 1D / direction.weight;
            totalWeight += direction.weight;
        }

        for (Deikstra.ShortestWay direction : directions) {
            direction.weight /= totalWeight;
        }


        Board copy = board.getCopy();
        for (int i = 0; i < 5; i++) {
            System.err.println(i);
            List<Deikstra.ShortestWay> simWays = simNextStage(copy);
            if (simWays == null) {
                continue;
            } else {
                for (Deikstra.ShortestWay direction : simWays) {
                    direction.weight = 1D / direction.weight;
                    totalWeight += direction.weight;
                }

                for (Deikstra.ShortestWay direction : simWays) {
                    direction.weight /= (totalWeight * (1.5 * (i + 1)));
                }
            }
        }

        Direction action = getAction(directions);

        action = patchByBullet(action, board);

        return act(action.toString());
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

        switch (direction) {
            case DOWN:
                if (board.isBulletAt(x + delta1, y) || (!new PointImpl(x + delta2, y).isOutOf(size) && board.isBulletAt(x + delta2, y))) {
                    boolean possibleLeft = possible.possible(me, Direction.LEFT);
                    boolean possibleRight = possible.possible(me, Direction.RIGHT);
                    if (possibleLeft && possibleRight) {
                        return rnd.nextInt(1) == 0 ? Direction.LEFT : Direction.RIGHT;
                    } else if (possibleLeft) {
                        return Direction.LEFT;
                    } else if (possibleRight) {
                        return Direction.RIGHT;
                    }
                    return Direction.UP;
                }
            case UP:
                if (board.isBulletAt(x - delta1, y) || (!new PointImpl(x - delta2, y).isOutOf(size) && board.isBulletAt(x - delta2, y))) {
                    boolean possibleLeft = possible.possible(me, Direction.LEFT);
                    boolean possibleRight = possible.possible(me, Direction.RIGHT);
                    if (possibleLeft && possibleRight) {
                        return rnd.nextInt(1) == 0 ? Direction.LEFT : Direction.RIGHT;
                    } else if (possibleLeft) {
                        return Direction.LEFT;
                    } else if (possibleRight) {
                        return Direction.RIGHT;
                    }
                    return Direction.DOWN;
                }
            case LEFT:
                if (board.isBulletAt(x, y - delta1) || (!new PointImpl(x, y - delta2).isOutOf(size) && board.isBulletAt(x, y - delta2))) {
                    boolean possibleUp = possible.possible(me, Direction.UP);
                    boolean possibleDown = possible.possible(me, Direction.DOWN);
                    if (possibleUp && possibleDown) {
                        return rnd.nextInt(1) == 0 ? Direction.DOWN : Direction.UP;
                    } else if (possibleUp) {
                        return Direction.UP;
                    } else if (possibleDown) {
                        return Direction.DOWN;
                    }
                    return Direction.RIGHT;
                }
            case RIGHT:
                if (board.isBulletAt(x, y + delta1) || (!new PointImpl(x, y + delta2).isOutOf(size) && board.isBulletAt(x, y + delta2))) {
                    boolean possibleUp = possible.possible(me, Direction.UP);
                    boolean possibleDown = possible.possible(me, Direction.DOWN);
                    if (possibleUp && possibleDown) {
                        return rnd.nextInt(1) == 0 ? Direction.DOWN : Direction.UP;
                    } else if (possibleUp) {
                        return Direction.UP;
                    } else if (possibleDown) {
                        return Direction.DOWN;
                    }
                    return Direction.LEFT;
                }
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

        Deikstra.ShortestWay[] totalDirection = new Deikstra.ShortestWay[4];
        totalDirection[0] = new Deikstra.ShortestWay(Direction.RIGHT, 0);
        totalDirection[1] = new Deikstra.ShortestWay(Direction.LEFT, 0);
        totalDirection[2] = new Deikstra.ShortestWay(Direction.UP, 0);
        totalDirection[3] = new Deikstra.ShortestWay(Direction.DOWN, 0);


        for (final Deikstra.ShortestWay direction : weightedDirections) {
            switch (direction.direction) {
                case RIGHT:
                    totalDirection[0].weight += direction.weight;
                    break;
                case LEFT:
                    totalDirection[1].weight += direction.weight;
                    break;
                case UP:
                    totalDirection[2].weight += direction.weight;
                    break;
                case DOWN:
                    totalDirection[3].weight += direction.weight;
                    break;
            }
        }

        System.out.println();
        for (int i = 0; i < totalDirection.length; i++) {
            System.out.println(i + " " + totalDirection[i].direction + ": " + totalDirection[i].weight);
        }


        if (totalDirection[0].weight > totalDirection[1].weight &&
                totalDirection[0].weight > totalDirection[2].weight &&
                totalDirection[0].weight > totalDirection[3].weight) {
            return totalDirection[0].direction;
        } else if (totalDirection[1].weight > totalDirection[2].weight &&
                totalDirection[1].weight > totalDirection[3].weight) {
            return totalDirection[1].direction;
        } else if (totalDirection[2].weight > totalDirection[3].weight) {
            return totalDirection[2].direction;
        }
        return totalDirection[3].direction;
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
    public List<Deikstra.ShortestWay> getDirections(Board board) {
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
        return way.getShortestWays(size, from, to, map);
    }

    /**
     * Для данного танка получить направления кратчайших путей до врагов
     *
     * @param board доска
     * @param tank  танк
     * @return кратчайщие пути
     */
    public List<Deikstra.ShortestWay> getDirections(Board board, Tank tank) {
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

        return way.getShortestWays(size, from, to, map);
    }

    /**
     * Просимулировать игровой ход
     *
     * @param board доска
     * @return кратчайщие направления для нашего танка на данном ходу
     */
    public List<Deikstra.ShortestWay> simNextStage(final Board board) {
        for (Tank tank : board.tanks) {
            // танки выбирают лучший из путей по Дейкстре
            List<Deikstra.ShortestWay> directions = getDirections(board, tank);
            if (directions != null && !directions.isEmpty()) {
                tank.act(directions.get(0).direction, board);
            } else {
                tank.act(Direction.RIGHT, board);
            }
        }
        for (Tank tank : board.tanks) {
            tank.actBullets(board);
        }

        if (board.getMe() == null) {
            return new ArrayList<>();
        }
        // мы получаем все возможные кратчайшие пути
        return getDirections(board);
    }


    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://localhost:8080/codenjoy-contest/board/player/mater_1234@mail.ru?code=2087931698601200491",
                new AISolver(new RandomDice()),
                new Board());
    }

}