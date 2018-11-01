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

    /**
     * Список пуль, выпущенных танком.
     */
    private final ArrayList<Bullet> bullets;

    /**
     * Пуля, выпущенная на данном ходе. Добавляется в список всех пуль по окончании хода, чтобы не походить ею лишний раз.
     */
    private Bullet tmpBullet = null;

    public Tank(final PointImpl point, final TankType tankType) {
        this.point = point;
        this.tankType = tankType;
        bullets = new ArrayList<>();
    }

    private Tank(final Tank tank) {
        point = new PointImpl(tank.point.getX(), tank.point.getY());
        tankType = tank.tankType;
        bullets = new ArrayList<>(tank.bullets);
        tmpBullet = tank.tmpBullet == null ? null : tank.tmpBullet.getCopy();
    }

    public Tank getCopy() {
        return new Tank(this);
    }

    /**
     * Симуляция полета пуль.
     *
     * @param board игровое поле.
     */
    public void actBullets(final Board board) {
        for (Bullet bullet : bullets) {
            if (bullet == null) continue;
            bullet.act(board);
            // Пуля отжила - установим ее в null
            if (bullet.isDead) bullet = null;
        }
        // и удалим
        bullets.removeIf(Objects::isNull);
        // добавляем пулю, созданную на данном ходу
        bullets.add(tmpBullet);
    }

    /**
     * Танчик умер - респим его на случайной позиции поля.
     *
     * @param board игровое поле.
     * @// TODO: 29.10.2018 Здесь респ идет на произвольной точке поля, где могут быть другие танчики.
     */
    public void sendDead(final Board board) {

        board.set(point.getX(), point.getY(), Elements.NONE.ch);

        while (true) {

            PointImpl point = new PointImpl(new PointImpl(AISolver.rnd.nextInt(board.size()), AISolver.rnd.nextInt(board.size())));
            int y = point.getY();
            int x = point.getX();
            if (!board.isBarrierAt(x, y) && !board.isOtherTankAt(x, y) && !board.isBulletAt(x, y) && !board.isMeAt(x, y)) {
                this.point.move(point);
                board.set(point.getX(), point.getY(), tankType.left.ch);
                break;
            }
        }
    }

    /**
     * Метод меня положение танка в зависимости от направления.
     *
     * @param direction направление
     * @param board     доска
     */
    private Direction changePoint(final Direction direction, final Board board) {

        int x = point.getX();
        int y = point.getY();

        PointImpl tmpPoint = new PointImpl(point.getX(), point.getY());
        tmpPoint.change(direction);

        Deikstra.Possible possible = AISolver.possible(board);

        if (possible.possible(point, direction)) {
            board.set(x, y, Elements.NONE.ch);
            point.change(direction);
            board.set(point.getX(), point.getY(), tankType.byDirection(direction).ch);
            return direction;
        } else {
            System.out.println("TANK OUT: " + tankType.right);
            board.set(x, y, Elements.NONE.ch);
            point.change(direction.inverted());
            board.set(point.getX(), point.getY(), tankType.byDirection(direction.inverted()).ch);
            return direction.inverted();
        }

    }

    /**
     * Действуем согласно заданному направлению.
     *
     * @param direction направление
     * @param board     доска
     */
    public void act(final Direction direction, final Board board) {

        tmpBullet = new Bullet(point, changePoint(direction, board));

        System.out.println(board.toString());
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
