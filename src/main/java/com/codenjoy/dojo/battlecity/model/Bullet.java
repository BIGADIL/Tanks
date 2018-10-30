package com.codenjoy.dojo.battlecity.model;

import com.codenjoy.dojo.battlecity.client.Board;
import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.PointImpl;

/**
 * Класс описывает пулю
 */
public class Bullet {

    /**
     * Точка, в которой находится пуля
     */
    private final PointImpl point;

    /**
     * Ее направление
     */
    private final Direction direction;

    /**
     * Признак того, что пуля отжила свое
     */
    public boolean isDead = false;


    public Bullet(final PointImpl point, Direction direction) {
        this.point = new PointImpl(point.getX(), point.getY());
        this.direction = direction;
    }

    private Bullet(final Bullet bullet) {
        point = new PointImpl(bullet.point.getX(), bullet.point.getY());
        direction = bullet.direction;
    }

    public Bullet getCopy() {
        return new Bullet(this);
    }

    private void destroyBuild(Board board) {
        Elements element;
        switch (direction) {
            case UP:
                element = board.getAt(this.point);
                isDead = true;
                switch (element) {
                    case CONSTRUCTION:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_DOWN.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_DOWN:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_DOWN_TWICE.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_UP:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_UP_DOWN.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_LEFT:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_DOWN_LEFT.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_RIGHT:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_DOWN_RIGHT.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_DOWN_TWICE:
                    case CONSTRUCTION_DESTROYED_DOWN_LEFT:
                    case CONSTRUCTION_DESTROYED_DOWN_RIGHT:
                    case CONSTRUCTION_DESTROYED_UP_DOWN:
                        board.set(this.point.getX(), this.point.getY(), Elements.NONE.ch);
                        break;
                    default:
                        isDead = false;
                }
                break;
            case DOWN:
                element = board.getAt(this.point);
                isDead = true;
                switch (element) {
                    case CONSTRUCTION:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_UP.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_UP:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_UP_TWICE.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_DOWN:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_UP_DOWN.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_LEFT:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_UP_LEFT.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_RIGHT:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_RIGHT_UP.ch);
                        break;

                    case CONSTRUCTION_DESTROYED_UP_TWICE:
                    case CONSTRUCTION_DESTROYED_UP_LEFT:
                    case CONSTRUCTION_DESTROYED_RIGHT_UP:
                    case CONSTRUCTION_DESTROYED_UP_DOWN:
                        board.set(this.point.getX(), this.point.getY(), Elements.NONE.ch);
                        break;
                    default:
                        isDead = false;
                }
                break;
            case LEFT:
                element = board.getAt(this.point);
                isDead = true;
                switch (element) {
                    case CONSTRUCTION:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_RIGHT.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_RIGHT:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_RIGHT_TWICE.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_UP:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_RIGHT_UP.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_DOWN:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_DOWN_RIGHT.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_LEFT:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_LEFT_RIGHT.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_RIGHT_TWICE:
                    case CONSTRUCTION_DESTROYED_RIGHT_UP:
                    case CONSTRUCTION_DESTROYED_DOWN_RIGHT:
                    case CONSTRUCTION_DESTROYED_LEFT_RIGHT:
                        board.set(this.point.getX(), this.point.getY(), Elements.NONE.ch);
                        break;
                    default:
                        isDead = false;
                }
                break;
            case RIGHT:
                element = board.getAt(this.point);
                isDead = true;
                switch (element) {
                    case CONSTRUCTION:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_LEFT.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_LEFT:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_LEFT_TWICE.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_RIGHT:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_RIGHT_UP.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_DOWN:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_DOWN_LEFT.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_UP:
                        board.set(this.point.getX(), this.point.getY(), Elements.CONSTRUCTION_DESTROYED_UP_LEFT.ch);
                        break;
                    case CONSTRUCTION_DESTROYED_LEFT_TWICE:
                    case CONSTRUCTION_DESTROYED_DOWN_LEFT:
                    case CONSTRUCTION_DESTROYED_LEFT_RIGHT:
                    case CONSTRUCTION_DESTROYED_UP_LEFT:
                        board.set(this.point.getX(), this.point.getY(), Elements.NONE.ch);
                        break;
                    default:
                        isDead = false;
                }
                break;
        }
    }

    /**
     * Симулирем полет пули
     * @param board доска
     */
    public void act(final Board board) {

        for (int i = 0; i < 2; i++) {

            int x = point.getX();
            int y = point.getY();

            // здесь теперь нет пули
            board.set(x, y, Elements.NONE.ch);

            // пуля двинулась в направлении движения
            this.point.change(direction);

            // вылезли за борд - все
            if (this.point.isOutOf(board.size())) {
                isDead = true;
                return;
            }

            if (board.isBulletAt(point)) {
                board.set(point.getX(), point.getY(), Elements.NONE.ch);
                isDead = true;
                return;
            }

            // получим ссылку на танк, который есть в точке вместе с пулей
            Tank tank = board.tankAt(this.point);
            if (tank != null) {
                // танк действительно есть => он умер вместе с пулей
                tank.sendDead(board);
                isDead = true;
                board.set(point.getX(), point.getY(), Elements.NONE.ch);
                return;
            }

            // рушим барикады
            destroyBuild(board);
            if (isDead) {
                return;
            }
        }
    }
}
