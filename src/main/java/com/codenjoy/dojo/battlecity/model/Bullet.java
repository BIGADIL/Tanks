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
    public final PointImpl point;

    /**
     * Ее направление
     */
    public final Direction direction;

    public boolean isDead = false;


    public Bullet(final PointImpl point, Direction direction) {
        this.point = new PointImpl(point.getX(), point.getY());
        this.direction = direction;
    }

    public Bullet(final int x, final int y, Direction direction) {
        point = new PointImpl(x, y);
        this.direction = direction;
    }

    public void Acted(final Board board) {
        PointImpl tmpPoint = new PointImpl(point.getX(), point.getY());

        tmpPoint.change(direction);
        tmpPoint.change(direction);

        if (tmpPoint.isOutOf(board.size())) {
            isDead = true;
        } else if (board.isBulletAt(tmpPoint)) {
            point.move(tmpPoint);
        } else {
            isDead = true;
        }
    }
}
