package com.codenjoy.dojo.battlecity.client;

import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;

import java.util.*;

public class Deikstra {
    private static final List<Direction> DIRECTIONS;
    private Map<Point, List<Direction>> possibleWays;
    private int size;
    private Deikstra.Possible possible;

    public static class ShortestWay {

        final Direction direction;
        double weight;

        public ShortestWay(Direction direction, int weight) {
            this.direction = direction;
            this.weight = weight;
        }
    }

    public Deikstra() {
    }

    public List<ShortestWay> getShortestWays (int size, Point from, List<Point> goals, Deikstra.Possible possible) {
        this.size = size;
        this.possible = possible;
        this.setupPossibleWays();
        LinkedList<ShortestWay> paths = new LinkedList<>();
        Iterator var6 = goals.iterator();

        LinkedList<Direction> shortest;

        Map<Point, LinkedList<Direction>> pathToAllPoints = getPath(from);

        int totalWeight = 0;
        for (final Point goal : goals) {
            LinkedList<Direction> directions = pathToAllPoints.get(goal);
            if (directions != null && !directions.isEmpty()) {
                int weight = directions.size();
                totalWeight += weight;
                ShortestWay shortestWay = new ShortestWay(directions.get(0), weight);
                paths.add(shortestWay);
            }
        }

        for (ShortestWay path : paths) {
            path.weight = (double) totalWeight / path.weight;
        }

        return paths;
    }

    public List<Direction> getShortestWay(int size, Point from, List<Point> goals, Deikstra.Possible possible) {
        this.size = size;
        this.possible = possible;
        this.setupPossibleWays();
        LinkedList<LinkedList<Direction>> paths = new LinkedList<>();
        Iterator var6 = goals.iterator();

        LinkedList<Direction> shortest;
        while(var6.hasNext()) {
            Point to = (Point)var6.next();
            shortest = this.getPath(from).get(to);
            if (shortest != null && !shortest.isEmpty()) {
                paths.add(shortest);
            }
        }

        int minDistance = 2147483647;
        int indexMin = 0;

        for(int index = 0; index < paths.size(); ++index) {
            List<Direction> path = paths.get(index);
            if (minDistance > path.size()) {
                minDistance = path.size();
                indexMin = index;
            }
        }

        if (paths.isEmpty()) {
            return Collections.emptyList();
        } else {
            shortest = paths.get(indexMin);
            if (shortest.size() == 0) {
                return Collections.emptyList();
            } else {
                return shortest;
            }
        }
    }

    private Map<Point, LinkedList<Direction>> getPath(Point from) {
        HashMap<Point, LinkedList<Direction>> path = new HashMap<>();

        for (Point point : this.possibleWays.keySet()) {
            path.put(point, new LinkedList<>());
        }

        boolean[][] processed = new boolean[this.size][this.size];
        LinkedList<Point> toProcess = new LinkedList<>();
        Point current = from;

        label50:
        do {
            if (current == null) {
                current = (Point)toProcess.remove();
            }

            LinkedList<Direction> before = path.get(current);
            Iterator var7 = ((List)this.possibleWays.get(current)).iterator();

            while(true) {
                Direction direction;
                Point to;
                LinkedList<Direction> directions;
                do {
                    do {
                        do {
                            if (!var7.hasNext()) {
                                processed[current.getX()][current.getY()] = true;
                                current = null;
                                continue label50;
                            }

                            direction = (Direction)var7.next();
                            to = direction.change(current);
                        } while(this.possible != null && !this.possible.possible(to));
                    } while(processed[to.getX()][to.getY()]);

                    directions = path.get(to);
                } while(!directions.isEmpty() && directions.size() <= before.size() + 1);

                directions.addAll(before);
                directions.add(direction);
                if (!processed[to.getX()][to.getY()]) {
                    toProcess.add(to);
                }
            }
        } while(!toProcess.isEmpty());

        return path;
    }

    private void setupPossibleWays() {
        this.possibleWays = new TreeMap<>();

        for(int x = 0; x < this.size; ++x) {
            for(int y = 0; y < this.size; ++y) {
                Point from = new PointImpl(x, y);
                List<Direction> directions = new LinkedList<>();

                for (Direction direction : DIRECTIONS) {
                    if (this.possible.possible(from, direction)) {
                        directions.add(direction);
                    }
                }

                this.possibleWays.put(from, directions);
            }
        }

    }

    public Map<Point, List<Direction>> getPossibleWays() {
        return this.possibleWays;
    }

    static {
        DIRECTIONS = Arrays.asList(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT);
    }

    public interface Possible {
        boolean possible(Point var1, Direction var2);

        boolean possible(Point var1);
    }
}
