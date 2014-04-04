

import java.io.*;
import java.util.ArrayList;

public class Launcher {

    public static char[][] picture;
    private static int height;
    private static int width;

    public static void main(String[] args) {
        populatePicture();

        Square best;

        FileWriter f0 = null;
        try {
            f0 = new FileWriter("output.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        do {
            best = bestSquare();
            if(best.score != 0){
                ArrayList<String> instructions = squareToInstructions(best);



                for(String s : instructions){

                    try {

                        f0.append(s + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }


        } while (best.score !=0);

        try {
            f0.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    public static ArrayList<String> squareToInstructions(Square square) {
        ArrayList<String> instructions = new ArrayList<String>();

        int r = square.top + (square.s - 1) / 2;
        int c = square.left + (square.s - 1) / 2;

        instructions.add("PAINTSQ " + r + " " + c + " " + square.s);

        for(int i = square.top; i<square.top+square.s; i++) {
            for(int j = square.left; j<square.left+square.s; j++) {
                if(picture[i][j] == '.') {
                    instructions.add("ERASECELL " + i + " " + j);
                } else if (picture[i][j] == '#')
                {
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

        Square bestSquare = new Square(0,0,0,0);

        for (int s=1; s<=Math.min(height, width); s+=2) {

            for(int i = 0; i<= height - s; i++) {
                for (int j = 0; j<= width - s; j++) {

                    double score = calculScore(i,j,s);

                    if (score>bestSquare.score) {
                        bestSquare = new Square(i,j,s,score);
                    }


                }
            }

        }
        return bestSquare;
    }

    private static double calculScore(int top, int left, int s) {

        int toPaint = 0;
        int toErase = 0;

        for(int i = top; i<top+s; i++) {
            for(int j = left; j<left+s; j++) {
                if(picture[i][j] == '.') {
                    toErase++;
                } else if (picture[i][j] == '#') {
                    toPaint++;
                }
            }
        }

        return (double)toPaint / ((double)toErase + 1);
    }

    private static void populatePicture() {
        File file = new File("doodle.txt");

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            line = br.readLine();

            String[] sizeStrings = line.split(" ");
            height = Integer.valueOf(sizeStrings[0]);
            width = Integer.valueOf(sizeStrings[1]);

            picture = new char[height][width];

            int j = 0;

            while ((line = br.readLine()) != null) {
                for (int i = 0; i< line.length(); i++) {
                    picture[j][i] = line.charAt(i);
                }
                j++;
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
