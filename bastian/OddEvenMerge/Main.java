
import java.util.Random;
import java.util.concurrent.CyclicBarrier;

//import java.util.Scanner;

public class Main {

    private static long time;
    public static int n;
    public static int p;
    private static int runs;
    private static int seed;
    public static int cycle;
    public static MyThread[] threadlist;
    private static int[][] numbers;
    public static boolean bubbleSort;

    @SuppressWarnings("unused")
    private static void printNumbers() {
        for(int x=0; x<numbers.length; ++x) {
            for(int num : numbers[x]) {
                System.out.println(num);
            }
        }      
    }

    public static void printArray(int[] array) {
        System.out.println();
        for(int i : array) {
            System.out.print(i+"\t");
        }
        System.out.println();
    }

    //Alternativ: Threads erhalten Kopie der Daten
    /* public static synchronized int[] getNumbers(int index) {
        int[] tmp = new int[n/p];
        for(int i=0; i<tmp.length; ++i) {
            tmp[i] = numbers[index][i];
        }
        return tmp;
        return numbers[index];
    }

    public static synchronized void returnNumbers(int index, int[] array) {
        numbers[index] = array;
    } */

    private static void checkSorted() {
        boolean sorted = true;
        int tmp = numbers[0][0];
        for(int x=0; x<numbers.length; ++x) {
            for(int num : numbers[x]) {
                if(num < tmp) { 
                    sorted = false;
                    System.out.println("Zahlen sortiert: "+sorted);
                    break;
                }
                tmp = num;
            }
        } 
        //System.out.println("Zahlen sortiert: "+sorted);
    }

    private static long oddEvenMerge() {

        cycle = 1;

        CyclicBarrier cyclicBarrier = new CyclicBarrier(p, new Runnable() {
            @Override
            public void run() {
                ++cycle;
            }
        });

        //initialize number array with random int and seed
        Random random = new Random(seed);
        numbers = new int[p][n/p];
        for(int x=0; x<p; ++x) {
            for(int y=0; y<(n/p); ++y) {
                numbers[x][y] = random.nextInt();
            }
        }

        //Threads anlegen
        threadlist = new MyThread[p];
        for(int i=0; i<p; ++i) {
            threadlist[i] = new MyThread(i, numbers[i], cyclicBarrier);
        }

        //Start der Zeitmessung
        time = System.currentTimeMillis();

        //Threads starten
        for(int i=0; i<p; ++i) {
            threadlist[i].start();
        }

        //auf Ende der Threads warten
        try {
            for(int i=0; i<p; ++i) {
                threadlist[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Ende der Zeitmessung
        time = System.currentTimeMillis()-time;
        //System.out.println("Time to finish: "+time+"ms");

        //Test ob Zahlen aufsteigend sortiert sind
        checkSorted();

        return time;
    }

    private static long getTime(int anzThreads) {
        p = anzThreads;
        long[] times = new long[runs];
        for(int i=0; i<times.length; ++i) {
            times[i] = oddEvenMerge();
        }
        long tmp = times[0];
        for(long i : times) {
            if(tmp>i) tmp = i;
        }
        System.out.println("Run-Time("+anzThreads+"):  \t"+tmp+"ms");
        return tmp;
    }

    private static void getEfficency(int startThreads, int incThreads, int maxThreads) {
        long t1 = getTime(1);
        double[][] data = new double[3][100];

        int i = 0;
        for(int threads=startThreads; threads<=maxThreads; ++i) {
            System.out.println();
            data[0][i] = threads;
            data[1][i] = (double)t1 / (double)getTime(threads);
            data[2][i] = data[1][i] / threads;
            System.out.println("Speed-Up("+threads+"):  \t"+String.format("%.3f", data[1][i]));
            System.out.println("Efficency("+threads+"): \t"+String.format("%.3f", data[2][i]));
            threads+=incThreads;
        }

    }
    

    public static void main(String... args) {
        n = 100000000;
        p = 6;

        bubbleSort = false;
        runs = 3;
        seed = 0;

        //die beste Zeit für 3-Durchläufe bei p-Threads
        getTime(p);

        //Ausgabe der Effizienz für: 1:Anzahl Start Threads 2:Increment 3:Maximum
        //getEfficency(2,2,6);

    }       
}
