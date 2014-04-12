import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Launcher_extended {

    private static String input = "paris_54000.txt";
    private static String output = "output_cars.txt";
    private static int BASE_DEPTH = 13;
    private static int SAME_STREET_IN_PATH_LIMIT = 4;

    private static int N;
    private static int M;
    private static int T;
    private static int C;
    private static int S;
    private static int randomChoices;
    private static long globalTimeLeft;
    private static long globalPoints;
    private static long startTimeInMillis;

    private static ArrayList<Intersection> intersections = new ArrayList<Intersection>();
    private static ArrayList<Street> streets = new ArrayList<Street>();
    private static List<Car> cars;


    public static void main(String[] args) {
        startTimeInMillis = System.currentTimeMillis();

        // init
        init();

        // move the cars
        ArrayList<ArrayList<Intersection>> roads = moveCars(cars);

        // write the roads
        writeRoads(roads);

        // display the score
        System.out.println("randomChoices = " + randomChoices);
        System.out.println("Global points = " + globalPoints);

        //display the elapsed time
        long elapsedTimeInSeconds = (System.currentTimeMillis() - startTimeInMillis) / 1000;
        System.out.println("Computed in " + elapsedTimeInSeconds + "sec");
    }

    public static void writeRoads(ArrayList<ArrayList<Intersection>> roads) {
        System.out.println("writeroads -- start");
        try {
            FileWriter f0 = new FileWriter(output);
            f0.append(C + "\n");

            for (ArrayList<Intersection> road : roads) {
                f0.append(road.size() + "\n");
                for (Intersection intersection : road) {
                    f0.append(intersection.index + "\n");
                }
            }
            f0.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("writeroads -- end");
    }

    public static ArrayList<ArrayList<Intersection>> moveCars(List<Car> cars) {
        System.out.println("moveCars -- start");
        ArrayList<ArrayList<Intersection>> roads = new ArrayList<ArrayList<Intersection>>();
        boolean oneCarMoved;
        do {
            oneCarMoved = false;
            for (Car car : cars) {
                if (car.isMoving && car.move()) {
                    oneCarMoved = true;
                }
            }
            System.out.println("\t Global time left -- " + globalTimeLeft);
        } while (oneCarMoved);

        for (Car car : cars) {
            roads.add(car.intersectionsVisited);
        }
        System.out.println("moveCars -- end");
        return roads;
    }

    public static double visitStreets(ArrayList<Street> streets) {
        double totalTime = 0;
        for (Street street : streets) {
            street.visited++;
            totalTime += street.cost;
        }
        return totalTime;
    }

    public static ArrayList<Path> getPaths(int depth, Street lastStreet, Intersection currentIntersection, double timeLeft) {
        ArrayList<Path> paths = new ArrayList<Path>();

        if (depth == 1) {
            for (Street nextStreet : currentIntersection.streetFrom) {
                if (nextStreet == lastStreet) continue;
                Path path = new Path();
                path.streetsToVisit.add(nextStreet);
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
                paths.add(path);
            } else {
                for (Path nextIntersectionPath : nextIntersectionPaths) {
                    if (contains(nextIntersectionPath.streetsToVisit, nextStreet) > SAME_STREET_IN_PATH_LIMIT) continue;
                    Path path = new Path();
                    path.streetsToVisit.add(nextStreet);
                    path.streetsToVisit.addAll(nextIntersectionPath.streetsToVisit);
                    paths.add(path);
                }
            }

        }

        return paths;
    }

    public static int contains(List<Street> streets, Street street) {
        int contains = 0;
        for (Street candidate : streets) {
            if (street == candidate) {
                contains++;
            }
        }
        return contains;
    }

    // this function assume that curPos is either the start or the end of the street
    public static Intersection getNextPos(Street street, Intersection curPos) {
        return street.start == curPos ? street.end : street.start;
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
            //System.out.println("Getting paths for intersection " + current.index);
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

        if (maxScoreResults.size() > 1) {
            randomChoices++;
        }

        return maxScoreResults.get(new Random().nextInt(maxScoreResults.size()));
    }


    private static void setPaths() {
        for (Intersection intersection : intersections) {
            System.out.println("set paths for intersection " + intersection.index);
            intersection.paths = getPaths(BASE_DEPTH, null, intersection, T);
        }
    }

    private static void init() {
        System.out.println("init -- start");
        randomChoices = 0;
        globalPoints = 0;
        globalTimeLeft = 0;
        System.out.println("\t loadDataFromFile -- start");
        loadDataFromFile();
        System.out.println("\t loadDataFromFile -- end");
        System.out.println("\t initCars -- start");
        initCars();
        System.out.println("\t initCars -- start");
        System.out.println("init -- done");
    }

    private static void loadDataFromFile() {
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
    }


    private static void initCars() {
        cars = new ArrayList<Car>();
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
    }

    public static class Car {
        public int id;
        public int timeLeft;
        public ArrayList<Intersection> intersectionsVisited;
        public Intersection currentIntersection;
        public boolean isMoving;

        public boolean move() {
            ScoreResult bestScoreResult = bestScoreUsingPath(currentIntersection, timeLeft);

            if (bestScoreResult != null && bestScoreResult.visitedIntersections.size() > 0) {
                double totalTime = visitStreets(bestScoreResult.visitedStreets);
                globalTimeLeft -= totalTime;
                timeLeft -= totalTime;
                globalPoints += bestScoreResult.length;
                intersectionsVisited.addAll(bestScoreResult.visitedIntersections);
                currentIntersection = bestScoreResult.visitedIntersections.get(bestScoreResult.visitedIntersections.size() - 1);
            } else {
                isMoving = false;
                System.out.println("Car " + id + " has stopped with " + timeLeft + " seconds left.");
            }

            return isMoving;
        }
    }

    public static class ScoreResult {
        ArrayList<Street> visitedStreets;
        ArrayList<Intersection> visitedIntersections;
        double length;
        double cost;

        public ScoreResult() {
            visitedStreets = new ArrayList<Street>();
            visitedIntersections = new ArrayList<Intersection>();
            length = 0;
            cost = 0;
        }

        public double getRatio() {
            if (cost == 0d) {
                return 0d;
            } else {
                return length / cost;
            }
        }
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
        public long index;
        public Intersection start;
        public Intersection end;
        public int oneWay;
        public double cost;
        public double length;
        public int visited = 0;

        public Street(long index, Intersection start, Intersection end, int oneWay, double cost, double length) {
            this.index = index;
            this.start = start;
            this.end = end;
            this.oneWay = oneWay;
            this.cost = cost;
            this.length = length;
        }
    }

    public static class Path {
        ArrayList<Street> streetsToVisit;

        public Path() {
            streetsToVisit = new ArrayList<Street>();
        }
    }
}
