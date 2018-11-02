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

import com.codenjoy.dojo.battlecity.model.Bullet;
import com.codenjoy.dojo.battlecity.model.Elements;
import com.codenjoy.dojo.battlecity.model.Tank;
import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.codenjoy.dojo.services.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AISolver implements Solver<Board> {

    public static final Random rnd = new Random();

    private Deikstra way;

    private final ArrayList<Bullet> bullets = new ArrayList<>();

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

        board.associateTanks();

        if (board.isGameOver()) return act("");

        dumpBullets(board);


        List<Deikstra.ShortestWay> directions = getDirections(board);
        if (directions == null || directions.isEmpty()) {
            return act(Direction.LEFT.toString() + ", ACT");
        }

        if (directions.get(0).weight <= 7) {
            Direction action = directions.get(0).direction;
            String s = patchByBullet(action, board);
            s = patchByTank(s, board);
            return act(s);
        }

        Board copy = board.getCopy();
        List<Deikstra.ShortestWay> simWays = simNextStage(copy);
        directions.addAll(simWays);

        double totalWeight = 0;
        for (Deikstra.ShortestWay direction : directions) {
            direction.weight = 1D / direction.weight;
            totalWeight += direction.weight;
        }

        for (Deikstra.ShortestWay direction : directions) {
            direction.weight /= totalWeight;
        }

        Direction action = getAction(directions);

        String s = patchByBullet(action, board);

        s = patchByTank(s, board);

        return act(s);
    }


    String patchByTank(String action, Board board) {
        Point me = board.getMe();

        PointImpl right = me.copy();
        right.change(Direction.RIGHT);

        PointImpl left = me.copy();
        left.change(Direction.LEFT);

        PointImpl up = me.copy();
        up.change(Direction.UP);

        PointImpl down = me.copy();
        down.change(Direction.DOWN);

        if (board.isOtherTankAt(right)) {
            return "RIGHT, ACT";
        } else if (board.isOtherTankAt(left)) {
            return "LEFT, ACT";
        } else if (board.isOtherTankAt(down)) {
            return "DOWN, ACT";
        } else if (board.isOtherTankAt(up)) {
            return "UP, ACT";
        }
        return action;
    }

    private void dumpBullets(Board board) {
        for (final Bullet bullet : bullets) {
            bullet.Acted(board);
        }
        bullets.removeIf(x -> x.isDead);

        for (int x = 0; x < board.size(); x++) {
            for (int y = 0; y < board.size(); y++) {
                if (board.isBulletAt(x, y)) {
                    boolean isExist = false;
                    for (final Bullet bullet : bullets) {
                        if (bullet.point.getX() == x && bullet.point.getY() == y) {
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist) {
                        bullets.add(new Bullet(x, y));
                    }
                }
            }
        }
    }


    /**
     * Накладываем на выбранное направление патч, связанный с пулями
     *
     * @param direction направление
     * @param board     доска
     * @return направление
     * @// TODO: 29.10.2018 не всегда отрабатывает
     */
    public String patchByBullet(Direction direction, Board board) {
        Point me = board.getMe();

        Deikstra.Possible possible = possible(board);

        boolean possibleLeft = possible.possible(me, Direction.LEFT);
        boolean possibleRight = possible.possible(me, Direction.RIGHT);
        boolean possibleUp = possible.possible(me, Direction.UP);
        boolean possibleDown = possible.possible(me, Direction.DOWN);

        PointImpl right = me.copy();
        right.change(Direction.RIGHT);

        PointImpl left = me.copy();
        left.change(Direction.LEFT);

        PointImpl up = me.copy();
        up.change(Direction.UP);

        PointImpl down = me.copy();
        down.change(Direction.DOWN);

        Bullet tmpUp = null;
        Bullet tmpDown = null;
        Bullet tmpLeft = null;
        Bullet tmpRight = null;

        for (Bullet bullet : bullets) {
            if (bullet.point.equals(up)) {
                tmpUp = bullet;
            } else if (bullet.point.equals(down)) {
                tmpDown = bullet;
            } else if (bullet.point.equals(left)) {
                tmpLeft = bullet;
            } else if (bullet.point.equals(right)) {
                tmpRight = bullet;
            }
        }

        if ((tmpUp != null && tmpUp.direction != null && tmpUp.direction == Direction.DOWN) || (tmpDown != null && tmpDown.direction != null && tmpDown.direction == Direction.UP)
                && (direction == Direction.DOWN || direction == Direction.UP)
        ) {
            if (possibleLeft && possibleRight) {
                final Direction dir = rnd.nextInt(1) == 0 ? Direction.LEFT : Direction.RIGHT;
                return dir + ", ACT";
            } else if (possibleLeft) {
                return Direction.LEFT.toString() + ", ACT";
            } else if (possibleRight) {
                return Direction.RIGHT.toString() + ", ACT";
            } else {
                System.err.println("AAA");
                return "ACT";
            }
        }

        if ((tmpLeft != null && tmpLeft.direction != null && tmpLeft.direction == Direction.RIGHT) || (tmpRight != null && tmpRight.direction != null && tmpRight.direction == Direction.LEFT)
                && (direction == Direction.LEFT || direction == Direction.RIGHT)
        ) {
            if (possibleDown && possibleUp) {
                final Direction dir = rnd.nextInt(1) == 0 ? Direction.UP : Direction.DOWN;
                return dir + ", ACT";
            } else if (possibleDown) {
                return Direction.DOWN.toString() + ", ACT";
            } else if (possibleUp) {
                return Direction.UP.toString() + ", ACT";
            } else {
                System.err.println("AAAA");
                return "ACT";
            }
        }

        return doubleDir(direction, board);
    }

    String doubleDir(Direction direction, Board board) {
        final Point me = board.getMe();

        final PointImpl futurePoint = me.copy();

        me.change(futurePoint);

        final Deikstra.Possible possible = possible(board);

        final PointImpl byDirection1 = futurePoint.copy();
        byDirection1.change(direction);

        final PointImpl byDirection2 = futurePoint.copy();
        byDirection2.change(direction);
        byDirection2.change(direction);

        final PointImpl byClockWise1 = futurePoint.copy();
        byClockWise1.change(direction.clockwise());

        final PointImpl byClockWise2 = futurePoint.copy();
        byClockWise2.change(direction.clockwise());
        byClockWise2.change(direction.clockwise());

        final PointImpl againstClockWise1 = futurePoint.copy();
        againstClockWise1.change(direction.clockwise().clockwise().clockwise());

        final PointImpl againstClockWise2 = futurePoint.copy();
        againstClockWise2.change(direction.clockwise().clockwise().clockwise());
        againstClockWise2.change(direction.clockwise().clockwise().clockwise());

        Bullet byDir1 = null;
        Bullet byDir2 = null;

        Bullet byClock1 = null;
        Bullet byClock2 = null;

        Bullet agClock1 = null;
        Bullet agClock2 = null;


        for (Bullet bullet : bullets) {
            if (bullet.point.equals(byDirection1)) {
                byDir1 = bullet;
            } else if (bullet.point.equals(byDirection2)) {
                byDir2 = bullet;
            } else if (bullet.point.equals(byClockWise1)) {
                byClock1 = bullet;
            } else if (bullet.point.equals(byClockWise2)) {
                byClock2 = bullet;
            } else if (bullet.point.equals(againstClockWise1)) {
                agClock1 = bullet;
            } else if (bullet.point.equals(againstClockWise2)) {
                agClock2 = bullet;
            }
        }



        if (byDir1 != null && (byDir1.direction != null || byDir1.direction == direction.inverted()) ||
                byDir2 != null && (byDir2.direction != null || byDir2.direction == direction.inverted()) ||
                byClock1 != null && (byClock1.direction != null || byClock1.direction == direction.clockwise().clockwise().clockwise()) ||
                byClock2 != null && (byClock2.direction != null || byClock2.direction == direction.clockwise().clockwise().clockwise()) ||
                agClock1 != null && (agClock1.direction != null || agClock1.direction == direction.clockwise()) ||
                agClock2 != null && (agClock2.direction != null || agClock2.direction == direction.clockwise())
        ) {
            final boolean clock = possible.possible(me, direction.clockwise());
            final boolean agClock = possible.possible(me, direction.clockwise().clockwise().clockwise());

            if (clock && agClock) {
                final Direction futureDir = rnd.nextInt(1) == 0 ? direction.clockwise() : direction.clockwise().clockwise();
                System.err.println(futureDir);
                return futureDir.toString() + ", ACT";
            } else if (clock) {
                System.err.println(direction.clockwise());
                return direction.clockwise().toString() + ", ACT";
            } else if (agClock) {
                System.err.println(direction.clockwise().clockwise().clockwise());
                return direction.clockwise().clockwise().clockwise() + ", ACT";
            } else {
                System.err.println("AAAA");
                return "ACT";
            }
        }

        return "ACT, " + direction.toString();
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

        if (command.equals("")) return "ACT";
        return command;
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
            if (tank.tankType == Tank.TankType.OUR) {
                continue;
            }
            // танки выбирают лучший из путей по Дейкстре
            List<Deikstra.ShortestWay> directions = getDirections(board, tank);
            if (directions != null && !directions.isEmpty()) {
                tank.act(directions.get(0).direction, board);
            }
        }


        // мы получаем все возможные кратчайшие пути
        return getDirections(board);
    }


    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://localhost:8080/codenjoy-contest/board/player/lol@kek.com?code=2692204611366816317",
                new AISolver(new RandomDice()),
                new Board());
    }

}