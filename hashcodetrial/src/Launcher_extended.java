import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Launcher_extended {

    private static final String OUTPUT = "output.txt";
    private static final String INPUT = "doodle.txt";
    private static final int MAX_SQUARE_SIZE = 2 * 24 + 1; // used for performance issues
    private static final int BASE_DEPTH = 4;
    private static int maxSquareSize = MAX_SQUARE_SIZE; // used for performance issues
    //private static int currentDepth = BASE_DEPTH;
    public static char[][] picture;
    private static int height;
    private static int width;
    private static List<StartingPosition> lastStartingPositions = new ArrayList<StartingPosition>();
    private static int pixelToPaint = 0;

    private static boolean[] skipSize;
    private static double[] minTheoreticalRatio;

    public static class StartingPosition {
        public int height;
        public int width;

        public StartingPosition() {
            height = 0;
            width = 0;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("start with max square size " + MAX_SQUARE_SIZE);
            populatePicture();
            initLastStartingPositions();
            ArrayList<String> instructions = getPrintingInstuctions();
            writeOutput(instructions);
            System.out.println("end !!!!!!!");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static void initLastStartingPositions() {
        int maxSquareSize = Math.min(MAX_SQUARE_SIZE, Math.min(height, width));
        for (int i = 0; i <= maxSquareSize; i++) {
            lastStartingPositions.add(new StartingPosition());
        }
    }

    public static void initSkipSize() {
        int maxSquareSize = Math.min(MAX_SQUARE_SIZE, Math.min(height, width));
        skipSize = new boolean[maxSquareSize + 1];
        for (int i = 0; i <= maxSquareSize; i++) {
            skipSize[i] = false;
        }
    }

    public static void initMinTheoreticalRatio() {
        int maxSquareSize = Math.min(MAX_SQUARE_SIZE, Math.min(height, width));
        minTheoreticalRatio = new double[maxSquareSize + 1];
        for (int s = maxSquareSize; s >= 2; s -= 2) {
            double minRatio = Double.MAX_VALUE;

            for (int i = 0; i <= height - s; i++) {
                for (int j = 0; j <= width - s; j++) {
                    Score score = calculScore(i, j, s);
                    if (score.ratio > 1 && score.ratio < minRatio) {
                        minRatio = score.ratio;
                    }
                }
            }

            minTheoreticalRatio[s] = minRatio;
            System.out.println("min ratio for size " + s + " -> " + minRatio);
        }
    }

    public static ArrayList<String> getPrintingInstuctions() {
        Square best;
        ArrayList<String> instructions = new ArrayList<String>();
        do {
            best = bestSquare();
            if (best.score != 0) {
                instructions.addAll(squareToInstructions(best));
            }
        } while (best.score != 0);
        instructions.addAll(finalizePicture());
        return instructions;
    }

    public static void writeOutput(ArrayList<String> instructions) throws IOException {
        FileWriter f0 = new FileWriter(OUTPUT);
        f0.append(instructions.size() + "\n");
        for (String instruction : instructions) {
            f0.append(instruction);
        }
        f0.close();
    }

    public static ArrayList<String> squareToInstructions(Square square) {
        ArrayList<String> instructions = new ArrayList<String>();

        int r = square.top + (square.s - 1) / 2;
        int c = square.left + (square.s - 1) / 2;

        instructions.add("PAINTSQ " + r + " " + c + " " + (square.s - 1) / 2 + "\n");

        for (int i = square.top; i < square.top + square.s; i++) {
            for (int j = square.left; j < square.left + square.s; j++) {
                if (picture[i][j] == '.') {
                    instructions.add("ERASECELL " + i + " " + j + "\n");
                } else if (picture[i][j] == '#') {
                    picture[i][j] = '*';
                }
            }
        }

        return instructions;
    }

    public static class Square {
        public Square(int top, int left, int s, double score) {
            this.top = top;
            this.left = left;
            this.s = s;
            this.score = score;
        }

        public int top;
        public int left;
        public int s;

        public double score;
    }

    private static Square bestSquare() {
        Square bestSquare = new Square(0, 0, 0, 0);
        Score bestScore = new Score();
        int localMaxSquareSize = Math.min(maxSquareSize, Math.min(height, width));

        // Start with the maxSquareSize .
        for (int s = localMaxSquareSize; s >= 2; s -= 2) {
            // Compute the maximum score.
            int maxTheoreticalRatioForCurrentSize = s * s;

            // If our best score has a higher ratio than the max ratio for the current size
            // We can return the best score we won't find any better score
            if (bestScore.ratio > maxTheoreticalRatioForCurrentSize) {
                pixelToPaint -= bestScore.toPaint;
                System.out.println("best square size " + bestSquare.s + " max score " + bestSquare.score + " still " + pixelToPaint);
                // If the best score is found with a size that is far from the max square size
                // we just reduce the max square size.
                if (localMaxSquareSize - bestSquare.s >= 12) {
                    System.out.println("we should probably stop looking at sizes " + maxSquareSize);
                    maxSquareSize -= 2;
                }
                return bestSquare;
            }

            // Get the last stating position
            StartingPosition startingPosition = lastStartingPositions.get(s);
            boolean findCandidateSquare = false;

            // try to get the maximum local score.
            for (int i = startingPosition.height; i <= height - s; i++) {
                for (int j = 0; j <= width - s; j++) {
                    Score score = calculScore(i, j, s);

                    // we find a potential candidate for the current size
                    if (score.ratio > 1) {
                        findCandidateSquare = true;
                    }

                    // Check if we reached the maximum score.
                    if (s == localMaxSquareSize && score.ratio == maxTheoreticalRatioForCurrentSize) {
                        pixelToPaint -= score.toPaint;
                        System.out.println("max square size " + s + " max score " + score.ratio + " still " + pixelToPaint);
                        return new Square(i, j, s, score.ratio);
                    } else if (score.ratio > 1 && score.ratio > bestSquare.score) {
                        bestSquare = new Square(i, j, s, score.ratio);
                        bestScore = score;
                    } else if (!findCandidateSquare) {
                        startingPosition.height = i;
                        startingPosition.width = j;
                    }
                }
            }

            // If we found no candidate square for the max size
            // We wont found such a square in the future
            // Stop starting at this size
            if (!findCandidateSquare && s == localMaxSquareSize) {
                System.out.println("stop looking at size " + maxSquareSize);
                maxSquareSize -= 2;
            }
        }

        // If the best score is found with a size that is far from the max square size
        // we just reduce the max square size.
        if (localMaxSquareSize - bestSquare.s >= 12) {
            System.out.println("we should probably stop looking at sizes " + maxSquareSize);
            maxSquareSize -= 2;
        }

        pixelToPaint -= bestScore.toPaint;
        System.out.println("best square size " + bestSquare.s + " best score " + bestSquare.score + " still " + pixelToPaint);

        return bestSquare;
    }


    private static ArrayList<String> finalizePicture() {
        ArrayList<String> instructions = new ArrayList<String>();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (picture[i][j] == '#') {
                    instructions.add("PAINTSQ " + i + " " + j + " 0\n");
                }
            }
        }
        return instructions;
    }

    public static class Score {
        public double ratio;
        public int toPaint;
        public int toErase;

        public Score() {
            ratio = 0;
            toPaint = 0;
            toErase = 0;
        }
    }

    private static Score calculScore(int top, int left, int s) {

        int toPaint = 0;
        int toErase = 0;

        for (int i = top; i < top + s; i++) {
            for (int j = left; j < left + s; j++) {
                if (picture[i][j] == '.') {
                    toErase++;
                } else if (picture[i][j] == '#') {
                    toPaint++;
                }
            }
        }

        Score result = new Score();
        result.ratio = (double) toPaint / ((double) toErase + 1);
        result.toPaint = toPaint;
        result.toErase = toErase;

        return result;
    }

    private static void populatePicture() {
        File file = new File(INPUT);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            // Read the parameters.
            line = br.readLine();
            String[] sizeStrings = line.split(" ");
            height = Integer.valueOf(sizeStrings[0]);
            width = Integer.valueOf(sizeStrings[1]);

            // Create a representation of the given picture.
            picture = new char[height][width];
            int j = 0;
            while ((line = br.readLine()) != null) {
                for (int i = 0; i < line.length(); i++) {
                    picture[j][i] = line.charAt(i);
                    if (line.charAt(i) == '#') {
                        pixelToPaint++;
                    }
                }
                j++;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
