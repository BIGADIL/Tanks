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

import java.util.*;

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

        for (Bullet bullet : bullets) {
            bullet.Acted(board);
        }
        bullets.removeIf(x -> x.isDead);

        rememberBullets(board);

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



    public void rememberBullets(Board board) {

        for (int x = 0; x < board.size(); x++) {
            for (int y = 0; y < board.size(); y++) {

                PointImpl me = new PointImpl(x, y);

                if (board.isBulletAt(x, y)) {
                    boolean isExist = false;
                    for (Bullet bullet : bullets) {
                        if (bullet.point.getX() == x && bullet.point.getY() == y) {
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist) {

                        PointImpl right = me.copy();
                        right.change(Direction.RIGHT);

                        PointImpl left = me.copy();
                        left.change(Direction.LEFT);

                        PointImpl up = me.copy();
                        up.change(Direction.UP);

                        PointImpl down = me.copy();
                        down.change(Direction.DOWN);

                        if (board.isAnyTankAt(left)) {
                            bullets.add(new Bullet(x, y, Direction.RIGHT));
                        } else if (board.isAnyTankAt(right)) {
                            bullets.add(new Bullet(x, y, Direction.LEFT));
                        } else if (board.isAnyTankAt(up)) {
                            bullets.add(new Bullet(x, y, Direction.DOWN));
                        } else if (board.isAnyTankAt(down)) {
                            bullets.add(new Bullet(x, y, Direction.UP));
                        } else {
                            System.err.println("AAAAAA");
                            bullets.add(new Bullet(x, y, Direction.DOWN));
                        }
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
        int x = me.getX();
        int y = me.getY();

        int size = board.size();

        Deikstra.Possible possible = possible(board);

        int delta1 = 1;
        int delta2 = 2;
        int delta3 = 3;

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

        if ((tmpUp != null && direction.inverted() == tmpUp.direction) || (tmpDown != null && direction.inverted() == tmpDown.direction)) {
            Direction d;
            if (possibleLeft && possibleRight) {
                d = rnd.nextInt(1) == 0 ? Direction.LEFT : Direction.RIGHT;
            } else if (possibleLeft) {
                d = Direction.LEFT;
            } else if (possibleRight) {
                d = Direction.RIGHT;
            } else {
                return "ACT";
            }
            return d.toString() + ", ACT";
        }

        if ((tmpLeft != null && direction.inverted() == tmpLeft.direction) || (tmpRight != null && direction.inverted() == tmpRight.direction)) {
            Direction d;
            if (possibleUp && possibleDown) {
                d = rnd.nextInt(1) == 0 ? Direction.DOWN : Direction.UP;
            } else if (possibleDown) {
                d = Direction.DOWN;
            } else if (possibleUp) {
                d = Direction.UP;
            } else {
                return "ACT";
            }
            return d.toString() + ", ACT";
        }


        switch (direction) {

            case UP:
                PointImpl up2 = me.copy();
                up2.change(Direction.UP);
                up2.change(Direction.UP);

                PointImpl up3 = me.copy();
                up3.change(Direction.UP);
                up3.change(Direction.UP);
                up3.change(Direction.UP);

                PointImpl left1 = me.copy();
                left1.change(Direction.LEFT);
                left1.change(Direction.UP);

                PointImpl left2 = me.copy();
                left2.change(Direction.LEFT);
                left2.change(Direction.LEFT);
                left2.change(Direction.UP);


                PointImpl right1 = me.copy();
                right1.change(Direction.RIGHT);
                right1.change(Direction.UP);

                PointImpl right2 = me.copy();
                right2.change(Direction.RIGHT);
                right2.change(Direction.RIGHT);
                right2.change(Direction.UP);


                Bullet tmpUp2 = null;
                Bullet tmpUp3 = null;

                if (!up2.isOutOf(size) || !up3.isOutOf(size)) {
                    for (Bullet bullet : bullets) {
                        if (bullet.point.equals(up2)) {
                            tmpUp2 = bullet;
                        } else if (bullet.point.equals(up3)) {
                            tmpUp3 = bullet;
                        }
                    }
                    if ((tmpUp2 != null && tmpUp2.direction == Direction.DOWN) || (tmpUp3 != null && tmpUp3.direction == Direction.DOWN)) {
                        Direction d;
                        if (possibleLeft && possibleRight) {
                            d =  rnd.nextInt(1) == 0 ? Direction.LEFT : Direction.RIGHT;
                        } else if (possibleLeft) {
                            d =  Direction.LEFT;
                        } else if (possibleRight) {
                            d =  Direction.RIGHT;
                        } else {
                            return "ACT";
                        }
                        return "ACT, " + d.toString();
                    }
                }


                Bullet tmpLeft1 = null;
                Bullet tmpLeft2 = null;
                Bullet tmpRight1 = null;
                Bullet tmpRight2 = null;

                if (!left1.isOutOf(size) || !left1.isOutOf(size) || !right1.isOutOf(size) || !right2.isOutOf(size)) {
                    for (Bullet bullet : bullets) {
                        if (bullet.point.equals(left1)) {
                            tmpLeft1 = bullet;
                        } else if (bullet.point.equals(left2)) {
                            tmpLeft2 = bullet;
                        } else if (bullet.point.equals(right1)) {
                            tmpRight1 = bullet;
                        } else if (bullet.point.equals(right2)) {
                            tmpRight2 = bullet;
                        }
                    }
                    if ((tmpLeft1 != null && tmpLeft1.direction == Direction.RIGHT) || (tmpLeft2 != null && tmpLeft2.direction == Direction.RIGHT) ||
                            (tmpRight1 != null && tmpRight1.direction == Direction.LEFT) || (tmpRight2 != null && tmpRight2.direction == Direction.LEFT) ) {
                        Direction d;
                        if (possibleLeft && possibleRight) {
                            d =  rnd.nextInt(1) == 0 ? Direction.LEFT : Direction.RIGHT;
                        } else if (possibleLeft) {
                            d =  Direction.LEFT;
                        } else if (possibleRight) {
                            d =  Direction.RIGHT;
                        } else {
                            return "ACT";
                        }
                        return "ACT, " + d.toString();
                    }
                }
                break;
            case DOWN:
                PointImpl down2 = me.copy();
                down2.change(Direction.DOWN);
                down2.change(Direction.DOWN);

                PointImpl down3 = me.copy();
                down3.change(Direction.DOWN);
                down3.change(Direction.DOWN);
                down3.change(Direction.DOWN);

                left1 = me.copy();
                left1.change(Direction.LEFT);
                left1.change(Direction.DOWN);

                left2 = me.copy();
                left2.change(Direction.LEFT);
                left2.change(Direction.LEFT);
                left2.change(Direction.DOWN);

                right1 = me.copy();
                right1.change(Direction.RIGHT);
                right1.change(Direction.DOWN);

                right2 = me.copy();
                right2.change(Direction.RIGHT);
                right2.change(Direction.RIGHT);
                right2.change(Direction.DOWN);

                Bullet tmpDown2 = null;
                Bullet tmpDown3 = null;

                if (!down2.isOutOf(size) || !down3.isOutOf(size)) {
                    for (Bullet bullet : bullets) {
                        if (bullet.point.equals(down2)) {
                            tmpDown2 = bullet;
                        } else if (bullet.point.equals(down3)) {
                            tmpDown3 = bullet;
                        }
                    }
                    if ((tmpDown2 != null && tmpDown2.direction == Direction.UP) || (tmpDown3 != null && tmpDown3.direction == Direction.UP)) {
                        Direction d;
                        if (possibleLeft && possibleRight) {
                            d =  rnd.nextInt(1) == 0 ? Direction.LEFT : Direction.RIGHT;
                        } else if (possibleLeft) {
                            d =  Direction.LEFT;
                        } else if (possibleRight) {
                            d =  Direction.RIGHT;
                        } else {
                            return "ACT";
                        }
                        return "ACT, " + d.toString();
                    }
                }


                tmpLeft1 = null;
                tmpLeft2 = null;
                tmpRight1 = null;
                tmpRight2 = null;

                if (!left1.isOutOf(size) || !left1.isOutOf(size) || !right1.isOutOf(size) || !right2.isOutOf(size)) {
                    for (Bullet bullet : bullets) {
                        if (bullet.point.equals(left1)) {
                            tmpLeft1 = bullet;
                        } else if (bullet.point.equals(left2)) {
                            tmpLeft2 = bullet;
                        } else if (bullet.point.equals(right1)) {
                            tmpRight1 = bullet;
                        } else if (bullet.point.equals(right2)) {
                            tmpRight2 = bullet;
                        }
                    }
                    if ((tmpLeft1 != null && tmpLeft1.direction == Direction.RIGHT) || (tmpLeft2 != null && tmpLeft2.direction == Direction.RIGHT) ||
                            (tmpRight1 != null && tmpRight1.direction == Direction.LEFT) || (tmpRight2 != null && tmpRight2.direction == Direction.LEFT) ) {
                        Direction d;
                        if (possibleLeft && possibleRight) {
                            d =  rnd.nextInt(1) == 0 ? Direction.LEFT : Direction.RIGHT;
                        } else if (possibleRight) {
                            d =  Direction.RIGHT;
                        } else if (possibleLeft) {
                            d =  Direction.LEFT;
                        } else {
                            return "ACT";
                        }
                        return "ACT, " + d.toString();
                    }
                }
                break;

            case LEFT:
                left2 = me.copy();
                left2.change(Direction.LEFT);
                left2.change(Direction.LEFT);
                left2.change(Direction.LEFT);

                PointImpl left3 = me.copy();
                left3.change(Direction.LEFT);
                left3.change(Direction.LEFT);
                left3.change(Direction.LEFT);


                PointImpl up1 = me.copy();
                up1.change(Direction.UP);
                up1.change(Direction.LEFT);


                up2 = me.copy();
                up2.change(Direction.UP);
                up2.change(Direction.UP);
                up2.change(Direction.LEFT);

                PointImpl down1 = me.copy();
                down1.change(Direction.DOWN);
                down1.change(Direction.LEFT);


                down2 = me.copy();
                down2.change(Direction.DOWN);
                down2.change(Direction.DOWN);
                down2.change(Direction.LEFT);

                tmpLeft2 = null;
                Bullet tmpLeft3 = null;

                if (!left2.isOutOf(size) || !left3.isOutOf(size)) {
                    for (Bullet bullet : bullets) {
                        if (bullet.point.equals(left2)) {
                            tmpLeft2 = bullet;
                        } else if (bullet.point.equals(left3)) {
                            tmpLeft3 = bullet;
                        }
                    }
                    if ((tmpLeft2 != null && tmpLeft2.direction == Direction.RIGHT) || (tmpLeft3 != null && tmpLeft3.direction == Direction.RIGHT)) {
                        Direction d;
                        if (possibleUp && possibleDown) {
                            d =  rnd.nextInt(1) == 0 ? Direction.UP : Direction.DOWN;
                        } else if (possibleUp) {
                            d =  Direction.UP;
                        } else if (possibleDown) {
                            d =  Direction.DOWN;
                        } else {
                            return "ACT";
                        }
                        return "ACT, " + d.toString();
                    }
                }


                Bullet tmpUp1 = null;
                tmpUp2 = null;
                Bullet tmpDown1 = null;
                tmpDown2 = null;

                if (!up1.isOutOf(size) || !up2.isOutOf(size) || !down1.isOutOf(size) || !down2.isOutOf(size)) {
                    for (Bullet bullet : bullets) {
                        if (bullet.point.equals(up1)) {
                            tmpUp1 = bullet;
                        } else if (bullet.point.equals(up2)) {
                            tmpUp2 = bullet;
                        } else if (bullet.point.equals(down1)) {
                            tmpDown1 = bullet;
                        } else if (bullet.point.equals(down2)) {
                            tmpDown2 = bullet;
                        }
                    }
                    if ((tmpUp1 != null && tmpUp1.direction == Direction.DOWN) || (tmpUp2 != null && tmpUp2.direction == Direction.DOWN) ||
                            (tmpDown1 != null && tmpDown1.direction == Direction.UP) || (tmpDown2 != null && tmpDown2.direction == Direction.UP) ) {
                        Direction d;
                        if (possibleUp && possibleDown) {
                            d =  rnd.nextInt(1) == 0 ? Direction.DOWN : Direction.UP;
                        } else if (possibleDown) {
                            d =  Direction.DOWN;
                        } else if (possibleRight) {
                            d =  Direction.RIGHT;
                        } else {
                            return "ACT";
                        }
                        return "ACT, " + d.toString();
                    }
                }
                break;

            case RIGHT:

                right2 = me.copy();
                right2.change(Direction.RIGHT);
                right2.change(Direction.RIGHT);

                PointImpl right3 = me.copy();
                right3.change(Direction.RIGHT);
                right3.change(Direction.RIGHT);
                right3.change(Direction.RIGHT);


                up1 = me.copy();
                up1.change(Direction.UP);
                up1.change(Direction.RIGHT);

                up2 = me.copy();
                up2.change(Direction.UP);
                up2.change(Direction.UP);
                up2.change(Direction.RIGHT);

                down1 = me.copy();
                down1.change(Direction.DOWN);
                down1.change(Direction.RIGHT);

                down2 = me.copy();
                down2.change(Direction.DOWN);
                down2.change(Direction.DOWN);
                down2.change(Direction.RIGHT);

                tmpRight2 = null;
                Bullet tmpRight3 = null;

                if (!right2.isOutOf(size) || !right3.isOutOf(size)) {
                    for (Bullet bullet : bullets) {
                        if (bullet.point.equals(right2)) {
                            tmpRight2 = bullet;
                        } else if (bullet.point.equals(right3)) {
                            tmpRight3 = bullet;
                        }
                    }
                    if ((tmpRight2 != null && tmpRight2.direction == Direction.LEFT) || (tmpRight3 != null && tmpRight3.direction == Direction.LEFT)) {
                        Direction d;
                        if (possibleUp && possibleDown) {
                            d =  rnd.nextInt(1) == 0 ? Direction.UP : Direction.DOWN;
                        } else if (possibleUp) {
                            d =  Direction.UP;
                        } else if (possibleDown) {
                            d =  Direction.DOWN;
                        } else {
                            return "ACT";
                        }
                        return "ACT, " + d.toString();
                    }
                }


                tmpUp1 = null;
                tmpUp2 = null;
                tmpDown1 = null;
                tmpDown2 = null;

                if (!up1.isOutOf(size) || !up2.isOutOf(size) || !down1.isOutOf(size) || !down2.isOutOf(size)) {
                    for (Bullet bullet : bullets) {
                        if (bullet.point.equals(up1)) {
                            tmpUp1 = bullet;
                        } else if (bullet.point.equals(up2)) {
                            tmpUp2 = bullet;
                        } else if (bullet.point.equals(down1)) {
                            tmpDown1 = bullet;
                        } else if (bullet.point.equals(down2)) {
                            tmpDown2 = bullet;
                        }
                    }
                    if ((tmpUp1 != null && tmpUp1.direction == Direction.DOWN) || (tmpUp2 != null && tmpUp2.direction == Direction.DOWN) ||
                            (tmpDown1 != null && tmpDown1.direction == Direction.UP) || (tmpDown2 != null && tmpDown2.direction == Direction.UP) ) {
                        Direction d;
                        if (possibleUp && possibleDown) {
                            d =  rnd.nextInt(1) == 0 ? Direction.DOWN : Direction.UP;
                        } else if (possibleDown) {
                            d =  Direction.DOWN;
                        } else if (possibleRight) {
                            d =  Direction.RIGHT;
                        } else {
                            return "ACT";
                        }
                        return "ACT, " + d.toString();
                    }
                }
                break;
        }

        return "ACT, " + direction.toString() ;
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
                "http://localhost:8080/codenjoy-contest/board/player/mater_1234@mail.ru?code=2087931698601200491",
                new AISolver(new RandomDice()),
                new Board());
    }

}