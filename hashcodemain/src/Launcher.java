
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Launcher {

    private static String input = "paris_54000.txt";
    private static String output = "output.txt";

    private static int N;
    private static int M;
    private static int T;
    private static int C;
    private static int S;

    private static ArrayList<Intersection> intersections = new ArrayList<Intersection>();
    private static ArrayList<Street> streets = new ArrayList<Street>();
    private static int BASE_DEPTH = 4;
    private static int MAX_DEPTH = 6;


    public static class Intersection {

        public double lat;
        public double lng;

        public Intersection(double lat, double lng, int index) {
            this.lat = lat;
            this.lng = lng;
            this.index = index;
        }

        public int index;
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
        init();
        ArrayList<ArrayList<Intersection>> roads = findRoads();

        try {
            writeRoads(roads);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return;
    }

    public static void writeRoads(ArrayList<ArrayList<Intersection>> roads) throws IOException {
        FileWriter f0 = new FileWriter(output);
        f0.append(C + "\n");

        for(ArrayList<Intersection> road : roads) {
            f0.append(road.size() + "\n");
            for (Intersection intersection : road) {
                f0.append(intersection.index + "\n");
            }
        }
        f0.close();
    }

    public static ArrayList<ArrayList<Intersection>> findRoads() {
        ArrayList<ArrayList<Intersection>> roads = new ArrayList<ArrayList<Intersection>>();
        for(int i = 0; i < C; i++) {
            System.out.println("Car number " + i);
            roads.add(findRoad());
        }
        return roads;
    }

    public static ArrayList<Intersection> findRoad() {
        int timeLeft = T;
        ArrayList<Intersection> road = new ArrayList<Intersection>();

        Intersection currPos = intersections.get(S);
        road.add(currPos);

        while(timeLeft > 0) {
            Street s = bestStreet(currPos, timeLeft);
            if(s == null) break;
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

    public static ArrayList<Street> streetsFrom(Intersection intersection) {
        ArrayList<Street> result = new ArrayList<Street>();
        for (Street street : streets) {
            if(street.start == intersection) {
                result.add(street);
            } else if (street.oneWay == 2 && street.end == intersection) {
                result.add(street);
            }
        }
        return result;
    }

    public static int minVisitedStreetFromNext(Intersection intersection) {
        int minVisited = Integer.MAX_VALUE;

        ArrayList<Street> streetsFrom = streetsFrom(intersection);
        for (Street street : streetsFrom) {
            if (street.visited < minVisited) {
                minVisited = street.visited;
            }
        }

        return minVisited;
    }

    public static double getScore(int depth, Intersection current, Street street, double timeLeft) {

        if(timeLeft < street.cost) return 0;

        double potentialGain = street.visited == 0 ? street.length : 0;
        potentialGain /= street.cost;

        if(depth == 1) {
            return potentialGain;
        }

        double max = 0;

        Intersection nextIntersection = street.start == current ? street.end : street.start;

        for (Street next : streetsFrom(nextIntersection)) {
            double score = getScore(depth-1, nextIntersection, next, timeLeft-street.cost);
            if(score > max) {
                max = score;
            }
        }

        return max + potentialGain;
    }

    public static Street bestStreet(Intersection current, int timeLeft) {
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

        if(bestStreets.size() == 0) return null;

        return bestStreets.get(new Random().nextInt(bestStreets.size()));
    }

    private static void init(){
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


            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
