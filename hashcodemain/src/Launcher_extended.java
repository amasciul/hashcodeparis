
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Launcher_extended {

    private static String input = "paris_54000.txt";
    private static String output = "output_cars.txt";

    private static int N;
    private static int M;
    private static int T;
    private static int C;
    private static int S;

    private static ArrayList<Intersection> intersections = new ArrayList<Intersection>();
    private static ArrayList<Street> streets = new ArrayList<Street>();

    private static int BASE_DEPTH = 16;
    private static int MAX_DEPTH = 16;
    private static double minRatio = 0d;
    private static int maxVisit = 0;

    private static int endCauseByDepth = 0;
    private static int randomChoices = 0;
    private static long globalTimeLeft = 0;
    private static long globalPoints = 0;

    public static class Car {
        public int id;
        public int timeLeft;
        public ArrayList<Intersection> intersectionsVisited;
        public Intersection currentIntersection;
        public boolean isMoving;
    }


    public static class Intersection {

        public double lat;
        public double lng;
        public int index;
        public List<Street> streetFrom;
        public List<Path> paths;

        public Intersection(double lat, double lng, int index) {
            this.lat = lat;
            this.lng = lng;
            this.index = index;
            this.streetFrom = new ArrayList<Street>();
            this.paths = null;
        }

        public void initStreetFrom(List<Street> streets) {
            for (Street street : streets) {
                if (street.start == this) {
                    this.streetFrom.add(street);
                } else if (street.oneWay == 2 && street.end == this) {
                    this.streetFrom.add(street);
                }
            }
        }

    }

    public static class Street {
        public Street(long id, Intersection start, Intersection end, int oneWay, double cost, double length) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.oneWay = oneWay;
            this.cost = cost;
            this.length = length;
        }


        public long id;

        public Intersection start;
        public Intersection end;

        public int oneWay;
        public double cost;

        public double length;

        public int visited = 0;

    }


    public static void main(String[] args) {
        System.out.println("init");
        init();
        System.out.println("done");
        List<Car> cars = getCars();

        // setPaths();

        System.out.println("moveCars");
        ArrayList<ArrayList<Intersection>> roads = moveCars(cars);
        System.out.println("done");

        try {
            System.out.println("writeroads");
            writeRoads(roads);
            System.out.println("done");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("endCausedByDepth = " + endCauseByDepth);
        System.out.println("randomChoices = " + randomChoices);
        System.out.println("Global points -- " + globalPoints);

        return;
    }

    public static void writeRoads(ArrayList<ArrayList<Intersection>> roads) throws IOException {
        FileWriter f0 = new FileWriter(output);
        f0.append(C + "\n");

        for (ArrayList<Intersection> road : roads) {
            f0.append(road.size() + "\n");
            for (Intersection intersection : road) {
                f0.append(intersection.index + "\n");
            }
        }
        f0.close();
    }

    public static ArrayList<ArrayList<Intersection>> moveCars(List<Car> cars) {
        ArrayList<ArrayList<Intersection>> roads = new ArrayList<ArrayList<Intersection>>();
        boolean oneCarMoved;
        do {
            oneCarMoved = false;
            for (Car car : cars) {
                if (car.isMoving) {
                    if (moveCarUsingPath(car)) {
                        oneCarMoved = true;
                    }
                }
            }
            System.out.println("Global time left -- " + globalTimeLeft);
        } while (oneCarMoved);

        for (Car car : cars) {
            roads.add(car.intersectionsVisited);
        }
        return roads;
    }


    public static boolean moveCarMinimizingVisit(Car car) {
        VisitResult bestScoreResult = bestVisitResult(car.currentIntersection, car.timeLeft);

        if (bestScoreResult != null) {
            double totalTime = visitStreets(bestScoreResult.visitedStreets);
            globalTimeLeft -= totalTime;
            car.timeLeft -= totalTime;
            globalPoints += bestScoreResult.length;
            car.intersectionsVisited.addAll(bestScoreResult.visitedIntersections);
            car.currentIntersection = bestScoreResult.visitedIntersections.get(bestScoreResult.visitedIntersections.size() - 1);
        } else {
            car.isMoving = false;
            System.out.println("Car " + car.id + " has stopped with " + car.timeLeft + " seconds left.");
        }

        return car.isMoving;
    }

    public static boolean moveCar(Car car) {
        ScoreResult bestScoreResult = bestScore(car.currentIntersection, car.timeLeft);

        if (bestScoreResult != null) {
            double totalTime = visitStreets(bestScoreResult.visitedStreets);
            globalTimeLeft -= totalTime;
            car.timeLeft -= totalTime;
            globalPoints += bestScoreResult.length;
            car.intersectionsVisited.addAll(bestScoreResult.visitedIntersections);
            car.currentIntersection = bestScoreResult.visitedIntersections.get(bestScoreResult.visitedIntersections.size() - 1);
        } else {
            car.isMoving = false;
            System.out.println("Car " + car.id + " has stopped with " + car.timeLeft + " seconds left.");
        }

        return car.isMoving;
    }

    public static boolean moveCarUsingPath(Car car) {
        ScoreResult bestScoreResult = bestScoreUsingPath(car.currentIntersection, car.timeLeft);

        if (bestScoreResult != null && bestScoreResult.visitedIntersections.size() > 0) {
            double totalTime = visitStreets(bestScoreResult.visitedStreets);
            globalTimeLeft -= totalTime;
            car.timeLeft -= totalTime;
            globalPoints += bestScoreResult.length;
            car.intersectionsVisited.addAll(bestScoreResult.visitedIntersections);
            car.currentIntersection = bestScoreResult.visitedIntersections.get(bestScoreResult.visitedIntersections.size() - 1);
        } else {
            car.isMoving = false;
            System.out.println("Car " + car.id + " has stopped with " + car.timeLeft + " seconds left.");
        }

        return car.isMoving;
    }


    public static ArrayList<ArrayList<Intersection>> findRoads() {
        ArrayList<ArrayList<Intersection>> roads = new ArrayList<ArrayList<Intersection>>();
        for (int i = 0; i < C; i++) {
            System.out.println("Car number " + i);
            roads.add(findRoad());
        }
        return roads;
    }

    public static int getRunAwayTime(int carNumber) {
        switch (carNumber) {
            case 0:
                return 0;
            case 1:
            case 2:
                return Math.round(0.5f * 3600f);
            case 4:
            case 5:
                return 1 * 3600;

            default:
                return 2 * 3600;
        }
    }

    public static int getRunAwayDepth(int carNumber) {
        switch (carNumber) {
            case 0:
                return 0;
            case 1:
            case 2:
                return 2;
            case 4:
            case 5:
                return 4;

            default:
                return 5;
        }
    }

    public static ArrayList<Intersection> findSmartRoad(int runAwayTime, int runAwayDepth) {
        int timeLeft = T;
        ArrayList<Intersection> road = new ArrayList<Intersection>();

        Intersection currPos = intersections.get(S);
        road.add(currPos);

        while (timeLeft > 0) {
            boolean runAway = false;
            if (timeLeft > T - runAwayTime) {
                Street s = runAway(currPos, timeLeft, runAwayDepth);
                if (s != null) {
                    runAway = true;
                    s.visited++;
                    timeLeft -= s.cost;
                    if (currPos == s.start) {
                        road.add(s.end);
                        currPos = s.end;
                    } else {
                        road.add(s.start);
                        currPos = s.start;
                    }
                } else {
                    break;
                }
            }

            if (!runAway) {
                ScoreResult bestScoreResult = bestScore(currPos, timeLeft);
                if (bestScoreResult == null) break;
                double totalTime = visitStreets(bestScoreResult.visitedStreets);
                timeLeft -= totalTime;
                road.addAll(bestScoreResult.visitedIntersections);
                currPos = bestScoreResult.visitedIntersections.get(bestScoreResult.visitedIntersections.size() - 1);
            }
        }
        return road;
    }

    public static List<Intersection> getIntersectionFromPath(Path path, Intersection start) {
        List<Intersection> intersections = new ArrayList<Intersection>();
        Intersection current = start;
        for (Street street : path.streetsToVisit) {

        }
        return intersections;
    }


    public static ArrayList<Intersection> findRoad() {
        int timeLeft = T;
        ArrayList<Intersection> road = new ArrayList<Intersection>();

        Intersection currPos = intersections.get(S);
        road.add(currPos);

        while (timeLeft > 0) {
            ScoreResult bestScoreResult = bestScore(currPos, timeLeft);
            if (bestScoreResult == null) break;
            double totalTime = visitStreets(bestScoreResult.visitedStreets);
            timeLeft -= totalTime;
            road.addAll(bestScoreResult.visitedIntersections);
            currPos = bestScoreResult.visitedIntersections.get(bestScoreResult.visitedIntersections.size() - 1);
        }

        System.out.println("end with " + timeLeft + "sec");

        return road;
    }


    public static ArrayList<Intersection> findLongestRoad() {
        int timeLeft = T;
        ArrayList<Intersection> road = new ArrayList<Intersection>();

        Intersection currPos = intersections.get(S);
        road.add(currPos);

        while (timeLeft > 0) {
            Street s = longestRoad(currPos, timeLeft);
            if (s == null) break;
            s.visited++;
            timeLeft -= s.cost;
            if (currPos == s.start) {
                road.add(s.end);
                currPos = s.end;
            } else {
                road.add(s.start);
                currPos = s.start;
            }
        }

        return road;
    }

    public static ArrayList<Intersection> findFastestRoad() {
        int timeLeft = T;
        ArrayList<Intersection> road = new ArrayList<Intersection>();

        Intersection currPos = intersections.get(S);
        road.add(currPos);

        while (timeLeft > 0) {
            Street s = fastestRoad(currPos, timeLeft);
            if (s == null) break;
            s.visited++;
            timeLeft -= s.cost;
            if (currPos == s.start) {
                road.add(s.end);
                currPos = s.end;
            } else {
                road.add(s.start);
                currPos = s.start;
            }
        }

        return road;
    }

    public static double visitStreets(ArrayList<Street> streets) {
        double totalTime = 0;
        for (Street street : streets) {
            street.visited++;
            totalTime += street.cost;
        }
        return totalTime;
    }

    public static int minVisitedStreetFromNext(Intersection intersection) {
        int minVisited = Integer.MAX_VALUE;

        for (Street street : intersection.streetFrom) {
            if (street.visited < minVisited) {
                minVisited = street.visited;
            }
        }

        return minVisited;
    }

    public static class VisitResult extends ScoreResult {
        public int visit;

        public VisitResult() {
            super();
            visit = 0;
        }
    }


    public static class Path {
        ArrayList<Street> streetsToVisit;

        public Path() {
            streetsToVisit = new ArrayList<Street>();
        }
    }

    public static ArrayList<Path> getPaths(int depth, Street lastStreet, Intersection currentIntersection, double timeLeft) {
        ArrayList<Path> paths = new ArrayList<Path>();


        if (depth == 1) {
            for (Street nextStreet : currentIntersection.streetFrom) {
                if (nextStreet == lastStreet) continue;
                Intersection nextIntersection = nextStreet.start == currentIntersection ? nextStreet.end : nextStreet.start;
                Path path = new Path();
                path.streetsToVisit.add(nextStreet);
                //path.visitedIntersections.add(nextIntersection);
                paths.add(path);
            }
            return paths;
        }

        for (Street nextStreet : currentIntersection.streetFrom) {
            if (lastStreet == nextStreet) continue;
            if (nextStreet.cost > timeLeft) continue;

            Intersection nextIntersection = nextStreet.start == currentIntersection ? nextStreet.end : nextStreet.start;
            ArrayList<Path> nextIntersectionPaths = getPaths(depth - 1, nextStreet, nextIntersection, timeLeft - nextStreet.cost);
            if (nextIntersectionPaths.isEmpty()) {
                Path path = new Path();
                path.streetsToVisit.add(nextStreet);
                //path.visitedIntersections.add(nextIntersection);
                paths.add(path);
            } else {
                for (Path nextIntersectionPath : nextIntersectionPaths) {
                    Path path = new Path();
                    path.streetsToVisit.add(nextStreet);
                    //path.visitedIntersections.add(nextIntersection);
                    path.streetsToVisit.addAll(nextIntersectionPath.streetsToVisit);
                    //path.visitedIntersections.addAll(nextIntersectionPath.visitedIntersections);
                    paths.add(path);
                }
            }

        }

        return paths;
    }

    public static class ScoreResult {

        public ScoreResult() {
            visitedStreets = new ArrayList<Street>();
            visitedIntersections = new ArrayList<Intersection>();
            length = 0;
            cost = 0;
        }

        ArrayList<Street> visitedStreets;
        ArrayList<Intersection> visitedIntersections;
        double length;
        double cost;

        public double getRatio() {
            if (cost == 0d) {
                return 0d;
            } else {
                return length / cost;
            }
        }
    }

    public static ScoreResult getScore(int depth, Intersection current, Street street, double timeLeft, ArrayList<Street> streetStack) {
        ScoreResult scoreResult = new ScoreResult();

        if (timeLeft < street.cost) {
            // We don't have time to take the street.
            // We do nothing
            return scoreResult;
        }


        // We go through the street.
        scoreResult.visitedStreets.add(street);
        // But if the street is in the current stack we score nothing
        if (streetStack.contains(street)) {
            scoreResult.length = 0;
        } else {
            // If the street is already visited we "score" nothing
            scoreResult.length = street.visited == 0 ? street.length : 0;
            streetStack.add(street);
        }

        // Even is the street is already visited, it costs the same thing to go through.
        scoreResult.cost = street.cost;
        Intersection nextIntersection = street.start == current ? street.end : street.start;
        scoreResult.visitedIntersections.add(nextIntersection);

        if (depth == 1) {
            // We don't look any further.
            return scoreResult;
        }

        // We look a little bit further and try to get the max score
        ScoreResult bestScoreResult = new ScoreResult();
        bestScoreResult.cost = Double.MAX_VALUE;
        for (Street next : nextIntersection.streetFrom) {
            ScoreResult candidateScoreResult = getScore(depth - 1, nextIntersection, next, timeLeft - street.cost, (ArrayList<Street>) streetStack.clone());
            if (candidateScoreResult.getRatio() > bestScoreResult.getRatio()) {
                bestScoreResult = candidateScoreResult;
            }/* else if (candidateScoreResult.getRatio() == bestScoreResult.getRatio() && candidateScoreResult.cost < bestScoreResult.cost) {
                bestScoreResult = candidateScoreResult;
            }*/
        }

        scoreResult.visitedStreets.addAll(bestScoreResult.visitedStreets);
        scoreResult.length += bestScoreResult.length;
        scoreResult.cost += bestScoreResult.cost;
        scoreResult.visitedIntersections.addAll(bestScoreResult.visitedIntersections);

        return scoreResult;
    }

    public static Street runAway(Intersection current, int timeLeft, int runAwayDepth) {
        double minCost = Double.MAX_VALUE;
        ArrayList<Street> candidateStreets;

        // get the less visited street;
        candidateStreets = getStreetsWithLessVistedIntersection(current, timeLeft, runAwayDepth);


        if (candidateStreets.isEmpty()) {
            // blocked ??
            return null;
        }

        // get the best option among the candidates
        double bestRatio = 0;
        Street bestStreet = null;
        for (Street street : candidateStreets) {
            double currentRatio = street.length / street.cost;
            if (currentRatio >= bestRatio) {
                bestRatio = currentRatio;
                bestStreet = street;
            }
        }

        return bestStreet;
    }

    public static Street longestRoad(Intersection current, int timeLeft) {
        double minCost = Double.MAX_VALUE;
        ArrayList<Street> candidateStreets;

        // get the fastest non visited road.
        candidateStreets = getLongestStreets(current, timeLeft, true);

        if (candidateStreets.isEmpty()) {
            // get the fastest street;
            candidateStreets = getFastestStreets(current, timeLeft, true);
        }

        if (candidateStreets.isEmpty()) {
            // get the less visited street;
            candidateStreets = getLessVisitedStreets(current, timeLeft);
        }

        if (candidateStreets.isEmpty()) {
            // blocked ??
            return null;
        }

        return candidateStreets.get(new Random().nextInt(candidateStreets.size()));
    }


    public static Street fastestRoad(Intersection current, int timeLeft) {
        double minCost = Double.MAX_VALUE;
        ArrayList<Street> candidateStreets;

        // get the fastest non visited road.
        candidateStreets = getFastestStreets(current, timeLeft, true);

        if (candidateStreets.isEmpty()) {
            // get the less visited street;
            candidateStreets = getLessVisitedStreets(current, timeLeft);
        }

        if (candidateStreets.isEmpty()) {
            // blocked ??
            return null;
        }

        return candidateStreets.get(new Random().nextInt(candidateStreets.size()));
    }

    public static ArrayList<Street> getStreetsWithLessVistedIntersection(Intersection current, int timeLeft, int runAwayDepth) {
        double minVisited = Double.MAX_VALUE;
        ArrayList<Street> candidateStreets = new ArrayList<Street>();
        for (Street street : streets) {
            if (street.oneWay == 1 && street.start != current) continue;
            if (street.oneWay == 2 && street.start != current && street.end != current) continue;
            if (street.cost > timeLeft) continue;
            Intersection nextIntersection = street.start == current ? street.end : street.start;
            double visited = getVisitedTime(runAwayDepth, nextIntersection);
            if (visited < minVisited) {
                minVisited = visited;
                candidateStreets.clear();
                candidateStreets.add(street);
            } else if (visited == minVisited) {
                candidateStreets.add(street);
            }
        }
        return candidateStreets;
    }

    public static ArrayList<Street> getLessVisitedStreets(Intersection current, int timeLeft) {
        double minVisited = Double.MAX_VALUE;
        ArrayList<Street> candidateStreets = new ArrayList<Street>();
        for (Street street : streets) {
            if (street.oneWay == 1 && street.start != current) continue;
            if (street.oneWay == 2 && street.start != current && street.end != current) continue;
            if (street.cost > timeLeft) continue;
            if (street.visited < minVisited) {
                minVisited = street.visited;
                candidateStreets.clear();
                candidateStreets.add(street);
            } else if (street.visited == minVisited) {
                candidateStreets.add(street);
            }
        }
        return candidateStreets;
    }

    public static ArrayList<Street> getLongestStreets(Intersection current, int timeLeft, boolean skipVisited) {
        double maxLength = -1;
        ArrayList<Street> candidateStreets = new ArrayList<Street>();
        for (Street street : streets) {
            if (skipVisited && street.visited > 0) continue;
            if (street.oneWay == 1 && street.start != current) continue;
            if (street.oneWay == 2 && street.start != current && street.end != current) continue;
            if (street.cost > timeLeft) continue;
            if (street.length > maxLength) {
                maxLength = street.length;
                candidateStreets.clear();
                candidateStreets.add(street);
            } else if (street.length == maxLength) {
                candidateStreets.add(street);
            }
        }
        return candidateStreets;
    }

    public static ArrayList<Street> getFastestStreets(Intersection current, int timeLeft, boolean skipVisited) {
        double minCost = Double.MAX_VALUE;
        ArrayList<Street> candidateStreets = new ArrayList<Street>();
        for (Street street : streets) {
            if (skipVisited && street.visited > 0) continue;
            if (street.oneWay == 1 && street.start != current) continue;
            if (street.oneWay == 2 && street.start != current && street.end != current) continue;
            if (street.cost > timeLeft) continue;
            if (street.cost < minCost) {
                minCost = street.cost;
                candidateStreets.clear();
                candidateStreets.add(street);
            } else if (street.cost == minCost) {
                candidateStreets.add(street);
            }
        }
        return candidateStreets;
    }

    public static double getVisitedTime(int depth, Intersection intersection) {
        double visited = 0;
        for (Street street : intersection.streetFrom) {
            visited += street.visited;
        }

        if (depth <= 1) return visited;

        for (Street street : streets) {
            visited += getVisitedTime(depth - 1, getNextPos(street, intersection));
        }

        return visited;
    }

    // this function assume that curPos is either the start or the end of the street
    public static Intersection getNextPos(Street street, Intersection curPos) {
        return street.start == curPos ? street.end : street.start;
    }

    public static VisitResult getVisitResult(int depth, Intersection current, Street street, double timeLeft, HashMap<Street, Integer> visitStack) {
        VisitResult visitResult = new VisitResult();

        if (timeLeft < street.cost) {
            // We don't have time to take the street.
            // We do nothing
            return visitResult;
        }


        // We go through the street.
        visitResult.visitedStreets.add(street);
        // But if the street is in the current stack we score nothing
        if (visitStack.containsKey(street)) {
            visitResult.length = 0;
            int currentVisit = visitStack.get(street);
            visitResult.visit += visitResult.visit + currentVisit;
            visitStack.put(street, currentVisit + 1);
        } else {
            // If the street is already visited we "score" nothing
            visitResult.length = street.visited == 0 ? street.length : 0;
            visitResult.visit += street.visited;
            visitStack.put(street, 1);
        }

        // Even is the street is already visited, it costs the same thing to go through.
        visitResult.cost = street.cost;
        Intersection nextIntersection = street.start == current ? street.end : street.start;
        visitResult.visitedIntersections.add(nextIntersection);

        if (depth == 1) {
            // We don't look any further.
            return visitResult;
        }

        // We look a little bit further and try to get the max score
        VisitResult bestVisitResult = new VisitResult();
        bestVisitResult.visit = Integer.MAX_VALUE;
        for (Street next : nextIntersection.streetFrom) {
            VisitResult candidateVisitResult = getVisitResult(depth - 1, nextIntersection, next, timeLeft - street.cost, (HashMap<Street, Integer>) visitStack.clone());
            if (bestVisitResult.visit > candidateVisitResult.visit) {
                bestVisitResult = candidateVisitResult;
            } else if (bestVisitResult.visit == candidateVisitResult.visit && bestVisitResult.getRatio() > bestVisitResult.getRatio()) {
                bestVisitResult = candidateVisitResult;
            }
        }

        visitResult.visitedStreets.addAll(bestVisitResult.visitedStreets);
        visitResult.length += bestVisitResult.length;
        visitResult.cost += bestVisitResult.cost;
        visitResult.visitedIntersections.addAll(bestVisitResult.visitedIntersections);
        visitResult.visit += bestVisitResult.visit;

        return visitResult;
    }

    public static VisitResult bestVisitResult(Intersection current, int timeLeft) {
        int minVisit = Integer.MAX_VALUE;
        ArrayList<VisitResult> minVisitResults = new ArrayList<VisitResult>();
        int depth = BASE_DEPTH;

        // While we don't have a suitable score and we can go a bit further
        while (minVisit > maxVisit && depth <= MAX_DEPTH) {
            // For all the streets
            for (Street street : streets) {
                // Check if we can take the given street
                if (street.oneWay == 1 && street.start != current) continue;
                if (street.oneWay == 2 && street.start != current && street.end != current) continue;
                if (street.cost > timeLeft) continue;
                // Compute the score of the current (depth, street)
                VisitResult candidateVisitResult = getVisitResult(depth, current, street, timeLeft, new HashMap<Street, Integer>());
                if (candidateVisitResult.visit < minVisit) {
                    minVisit = candidateVisitResult.visit;
                    minVisitResults.clear();
                    minVisitResults.add(candidateVisitResult);
                } else if (candidateVisitResult.visit == minVisit) {
                    minVisitResults.add(candidateVisitResult);
                }
            }
            // don't forget to increase the depth !
            depth++;
        }

        if (minVisit > maxVisit) {
            endCauseByDepth++;
        }

        // Check is at least one ScoreResult has been found;
        if (minVisitResults.size() == 0) return null;

        List<VisitResult> finalCandidates = new ArrayList<VisitResult>();
        if (minVisitResults.size() > 1) {
            // We have multiple results with the same visits
            // but not the same cost, let's take the maximum ratio
            double maxRatio = -1;
            for (VisitResult visitResult : minVisitResults) {
                double ratio = visitResult.getRatio();
                if (visitResult.getRatio() > maxRatio) {
                    finalCandidates.clear();
                    finalCandidates.add(visitResult);
                    maxRatio = visitResult.getRatio();
                } else if (visitResult.getRatio() == maxRatio) {
                    finalCandidates.add(visitResult);
                }
            }
        } else {
            finalCandidates = minVisitResults;
        }


        return finalCandidates.get(new Random().nextInt(finalCandidates.size()));
    }


    public static ScoreResult getScoreFromPath(Intersection start, Path path, int timeLeft) {
        ScoreResult scoreResult = new ScoreResult();
        Intersection current = start;

        for (Street street : path.streetsToVisit) {
            if (street.cost <= timeLeft) {
                Intersection next = getNextPos(street, current);
                timeLeft -= street.cost;
                scoreResult.cost += street.cost;
                if (!scoreResult.visitedStreets.contains(street)) {
                    scoreResult.length += street.visited > 0 ? 0 : street.length;
                }
                scoreResult.visitedStreets.add(street);
                scoreResult.visitedIntersections.add(next);
                current = next;
            } else {
                break;
            }
        }
/*
        if(scoreResult.visitedIntersections.size() > 0) {
            Intersection next = getNextPos(scoreResult.visitedStreets.get(scoreResult.visitedStreets.size() - 1),
                    current);
        }*/

        return scoreResult;
    }

    public static ScoreResult bestScoreUsingPath(Intersection current, int timeLeft) {
        double maxRatio = 0d;
        ArrayList<ScoreResult> maxScoreResults = new ArrayList<ScoreResult>();
        int depth = BASE_DEPTH;

        if (current.paths == null) {
            System.out.println("Getting paths for intersection " + current.index);
            current.paths = getPaths(BASE_DEPTH, null, current, timeLeft);
        }

        for (Path path : current.paths) {
            ScoreResult scoreResult = getScoreFromPath(current, path, timeLeft);
            if (scoreResult.getRatio() > maxRatio) {
                maxScoreResults.clear();
                maxScoreResults.add(scoreResult);
                maxRatio = scoreResult.getRatio();
            } else if (scoreResult.getRatio() == maxRatio) {
                maxScoreResults.add(scoreResult);
            }
        }

        current.paths = null;

        // Check is at least one ScoreResult has been found;
        if (maxScoreResults.size() == 0) return null;

        if (maxRatio == 0d) {
            endCauseByDepth++;
        }

        return maxScoreResults.get(new Random().nextInt(maxScoreResults.size()));
    }

    public static ScoreResult bestScore(Intersection current, int timeLeft) {
        double maxRatio = 0;
        ArrayList<ScoreResult> maxScoreResults = new ArrayList<ScoreResult>();
        int depth = BASE_DEPTH;
        // While we don't have a suitable score and we can go a bit further
        while (maxRatio <= minRatio && depth <= MAX_DEPTH) {
            // For all the streets
            for (Street street : current.streetFrom) {
                // Check if we can take the given street
                if (street.cost > timeLeft) continue;
                // Compute the score of the current (depth, street)
                ScoreResult candidateScoreResult = getScore(depth, current, street, timeLeft, new ArrayList<Street>());
                if (candidateScoreResult.getRatio() > maxRatio) {
                    maxRatio = candidateScoreResult.getRatio();
                    maxScoreResults.clear();
                    maxScoreResults.add(candidateScoreResult);
                } else if (candidateScoreResult.getRatio() == maxRatio) {
                    maxScoreResults.add(candidateScoreResult);
                }
            }
            // don't forget to increase the depth !
            depth++;
        }

        if (maxRatio <= minRatio) {
            System.out.println("End because of depth :( ");
            endCauseByDepth++;
        }

        // Check is at least one ScoreResult has been found;
        if (maxScoreResults.size() == 0) return null;

/*        List<ScoreResult> finalCandidates = new ArrayList<ScoreResult>();
        if (maxScoreResults.size() > 1) {
            // We have multiple results with the same ratio
            // but not the same cost, let's take the minimum cost
            double minCost = Double.MAX_VALUE;
            for (ScoreResult scoreResult : maxScoreResults) {
                if (scoreResult.cost < minCost) {
                    finalCandidates.clear();
                    finalCandidates.add(scoreResult);
                    minCost = scoreResult.cost;
                } else if (scoreResult.cost == minCost) {
                    finalCandidates.add(scoreResult);
                }
            }
        } else {
            finalCandidates = maxScoreResults;
        }*/

        if (maxScoreResults.size() > 1) {
            System.out.println("Choose randomly between " + maxScoreResults.size() + " score results of " + maxRatio);
            randomChoices++;
        }

        return maxScoreResults.get(new Random().nextInt(maxScoreResults.size()));
    }


    private static List<Car> getCars() {
        List<Car> cars = new ArrayList<Car>();
        for (int i = 0; i < C; i++) {
            Car car = new Car();
            car.id = i;
            car.timeLeft = T;
            globalTimeLeft += T;
            car.currentIntersection = intersections.get(S);
            car.intersectionsVisited = new ArrayList<Intersection>();
            car.intersectionsVisited.add(car.currentIntersection);
            car.isMoving = true;
            cars.add(car);
        }
        return cars;
    }

    private static void setPaths() {
        for (Intersection intersection : intersections) {
            System.out.println("set paths for intersection " + intersection.index);
            intersection.paths = getPaths(BASE_DEPTH, null, intersection, T);
        }
    }

    private static void init() {
        System.out.println("init -- start");
        File file = new File(input);

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            line = br.readLine();

            String[] data = line.split(" ");

            N = Integer.valueOf(data[0]);
            M = Integer.valueOf(data[1]);
            T = Integer.valueOf(data[2]);
            C = Integer.valueOf(data[3]);
            S = Integer.valueOf(data[4]);

            for (int i = 0; i < N; i++) {
                line = br.readLine();
                data = line.split(" ");
                Intersection intersection = new Intersection(Double.valueOf(data[0]), Double.valueOf(data[1]), i);
                intersections.add(intersection);
            }

            for (int i = 0; i < M; i++) {
                line = br.readLine();
                data = line.split(" ");
                Intersection start = intersections.get(Integer.valueOf(data[0]));
                Intersection end = intersections.get(Integer.valueOf(data[1]));
                int oneWay = Integer.valueOf(data[2]);
                double cost = Double.valueOf(data[3]);
                double length = Double.valueOf(data[4]);

                Street street = new Street(i, start, end, oneWay, cost, length);
                streets.add(street);
            }

            for (int i = 0; i < N; i++) {
                intersections.get(i).initStreetFrom(streets);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("init -- done");
    }
}
