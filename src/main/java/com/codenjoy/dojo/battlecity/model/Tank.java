package com.codenjoy.dojo.battlecity.model;

import com.codenjoy.dojo.battlecity.client.AISolver;
import com.codenjoy.dojo.battlecity.client.Board;
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
        tmpBullet = tank.tmpBullet.getCopy();
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
        point.move(new PointImpl(AISolver.rnd.nextInt(board.size()), AISolver.rnd.nextInt(board.size())));
    }

    /**
     * Метод меня положение танка в зависимости от направления.
     *
     * @param direction направление
     * @param board     доска
     * @param point     точка, в которую едет танк
     */
    private void changePoint(final Direction direction, final Board board, final PointImpl point) {
        int x = this.point.getX();
        int y = this.point.getY();
        // едем в точку только при отсутствии барьеров и если она не выходит за границы поля
        if (!board.isBarrierAt(point) && !point.isOutOf(board.size())) {
            this.point.move(point);
            board.set(point.getX(), point.getY(), tankType.byDirection(direction).ch);
        } else {
            // иначе тупо едем перпендикулярно заданному направлению в произвольную сторону
            final Direction newDirection = AISolver.rnd.nextInt(1) == 0 ? direction.clockwise() : direction.clockwise().inverted();
            this.point.change(newDirection);
            board.set(point.getX(), point.getY(), tankType.byDirection(newDirection).ch);
        }
        // обнуляем поле - теперь там нет танка.
        board.set(x, y, Elements.NONE.ch);
    }

    /**
     * Действуем согласно заданному направлению.
     *
     * @param direction направление
     * @param board     доска
     */
    public void act(final Direction direction, final Board board) {
        switch (direction) {
            case RIGHT:
                PointImpl right = new PointImpl(this.point.getX() + 1, this.point.getY());
                changePoint(direction, board, right);
                break;
            case LEFT:
                PointImpl left = new PointImpl(this.point.getX() - 1, this.point.getY());
                changePoint(direction, board, left);
                break;
            case DOWN:
                PointImpl down = new PointImpl(this.point.getX(), this.point.getY() + 1);
                changePoint(direction, board, down);
                break;
            case UP:
                PointImpl up = new PointImpl(this.point.getX(), this.point.getY() - 1);
                changePoint(direction, board, up);
                break;
        }
        // создаем пульку в направлении движения
        tmpBullet = new Bullet(point, direction);
    }

    /**
     * Получить положение танка
     * @return положение танка
     */
    public PointImpl getPoint() {
        return point;
    }
}
