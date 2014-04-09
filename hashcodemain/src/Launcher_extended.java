
import java.io.*;
import java.util.ArrayList;
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
    private static int BASE_DEPTH = 11;
    private static int MAX_DEPTH = 12;
    private static double minRatio = 0d;

    private static int endCauseByDepth = 0;
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

        public Intersection(double lat, double lng, int index) {
            this.lat = lat;
            this.lng = lng;
            this.index = index;
            this.streetFrom = new ArrayList<Street>();
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
        public Street(Intersection start, Intersection end, int oneWay, double cost, double length) {
            this.start = start;
            this.end = end;
            this.oneWay = oneWay;
            this.cost = cost;
            this.length = length;
        }

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
                    if (moveCar(car)) {
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


    public static boolean moveCar(Car car) {
        ScoreResult bestScoreResult = bestScore(car.currentIntersection, car.timeLeft);

        if (bestScoreResult != null) {
            double totalTime = visitStreets(bestScoreResult.visitedStreets);
            globalTimeLeft -= totalTime;
            car.timeLeft -= totalTime;
            globalPoints += bestScoreResult.length;
            car.intersectionsVisited.addAll(bestScoreResult.visitedIntersections);
            car.currentIntersection = bestScoreResult.visitedIntersections.get(bestScoreResult.visitedIntersections.size() - 1);
            /*if (bestScoreResult.getRatio() <= minRatio) {
                System.out.println("Car move, score " + bestScoreResult.length + " (ratio of " + bestScoreResult.getRatio() + ")");
            }*/
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
        double maxLength = Double.MIN_VALUE;
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

    public static ScoreResult bestScore(Intersection current, int timeLeft) {
        double maxRatio = 0;
        ArrayList<ScoreResult> maxScoreResults = new ArrayList<ScoreResult>();
        int depth = BASE_DEPTH;
        // While we don't have a suitable score and we can go a bit further
        while (maxRatio <= minRatio && depth < MAX_DEPTH) {
            // For all the streets
            for (Street street : streets) {
                // Check if we can take the given street
                if (street.oneWay == 1 && street.start != current) continue;
                if (street.oneWay == 2 && street.start != current && street.end != current) continue;
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

        if (depth >= MAX_DEPTH) {
            endCauseByDepth++;
        }

        // Check is at least one ScoreResult has been found;
        if (maxScoreResults.size() == 0) return null;

        List<ScoreResult> finalCandidates = new ArrayList<ScoreResult>();
        if (maxScoreResults.size() > 1) {
            // We have multiple results with the same ratio
            // but not the same cost, let's take the minimum cost
            double minCost = Double.MAX_VALUE;
            for (ScoreResult scoreResult : maxScoreResults) {
                if (scoreResult.cost < minCost) {
                    finalCandidates.clear();
                    finalCandidates.add(scoreResult);
                    minCost = scoreResult.cost;
                } else {
                    finalCandidates.add(scoreResult);
                }
            }
        } else {
            finalCandidates = maxScoreResults;
        }

        return finalCandidates.get(new Random().nextInt(finalCandidates.size()));
    }


/*    public static Street bestStreet(Intersection current, int timeLeft) {
        double maxScore = 0;
        ArrayList<Street> bestStreets = new ArrayList<Street>();

        int depth = BASE_DEPTH;
        while (maxScore == 0 && depth < MAX_DEPTH) {

            for (Street street : streets) {
                if (street.oneWay == 1 && street.start != current) continue;
                if (street.oneWay == 2 && street.start != current && street.end != current) continue;
                if (street.cost > timeLeft) continue;


                double score = getScore(depth, current, street, timeLeft);
                if (score > maxScore) {
                    maxScore = score;
                    bestStreets.clear();
                    bestStreets.add(street);
                } else if (score == maxScore) {
                    bestStreets.add(street);
                }
            }
            depth++;
        }

        if (bestStreets.size() == 0) return null;

        return bestStreets.get(new Random().nextInt(bestStreets.size()));
    }*/

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

    private static void init() {
        System.out.print("init -- start");
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

                Street street = new Street(start, end, oneWay, cost, length);
                streets.add(street);
            }

            for (int i = 0; i < N; i++) {
                intersections.get(i).initStreetFrom(streets);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.print("init -- done");
    }
}
