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
    public Direction direction = null;

    public boolean isDead = false;


    public Bullet(PointImpl point) {
     this.point = point;
    }

    public Bullet (final int x, final int y) {
        this(new PointImpl(x, y));
    }

    public void Acted(final Board board) {

        final int size = board.size();

        if (direction != null) {
            final PointImpl futurePoint = point.copy();
            futurePoint.change(direction);
            futurePoint.change(direction);
            if (futurePoint.isOutOf(size)) {
                isDead = true;
                return;
            }
            if (board.isBarrierAt(futurePoint)) {
                point.move(futurePoint);
            } else {
                isDead = true;
            }
        } else {
            final PointImpl upPoint = point.copy();
            upPoint.change(Direction.UP);
            upPoint.change(Direction.UP);
            if (!upPoint.isOutOf(size) && board.isBulletAt(upPoint)) {
                direction = Direction.UP;
                point.move(upPoint);
                return;
            }

            final PointImpl downPoint = point.copy();
            downPoint.change(Direction.DOWN);
            downPoint.change(Direction.DOWN);
            if (!downPoint.isOutOf(size) && board.isBulletAt(downPoint)) {
                direction = Direction.DOWN;
                point.move(downPoint);
                return;
            }

            final PointImpl leftPoint = point.copy();
            leftPoint.change(Direction.LEFT);
            leftPoint.change(Direction.LEFT);
            if (!leftPoint.isOutOf(size) && board.isBulletAt(leftPoint)) {
                direction = Direction.LEFT;
                point.move(leftPoint);
                return;
            }

            final PointImpl rightPoint = point.copy();
            rightPoint.change(Direction.RIGHT);
            rightPoint.change(Direction.RIGHT);
            if (!rightPoint.isOutOf(size) && board.isBulletAt(rightPoint)) {
                direction = Direction.RIGHT;
                point.move(rightPoint);
                return;
            }
        }

        isDead = true;
    }
}
