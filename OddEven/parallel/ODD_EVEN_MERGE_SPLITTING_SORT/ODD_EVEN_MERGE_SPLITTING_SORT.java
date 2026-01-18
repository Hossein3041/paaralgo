import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Map;

public class ODD_EVEN_MERGE_SPLITTING_SORT{
    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);
        //System.out.println("N (Gesamtzahl), P (#Prozessoren): ");
        //int N = sc.nextInt(), P = sc.nextInt();
        int N = 8; int P = 4;
        long startTime, endTime, duration;
        if (N % P != 0) throw new IllegalArgumentException("N muss durch P teilbar sein");

        int[][] m_Array = new int[P][N / P];
        CyclicBarrier barrier = new CyclicBarrier(P);
        ODD_EVEN_SWAP[] processes = new ODD_EVEN_SWAP[P];
        ODD_EVEN_DATA[] data = new ODD_EVEN_DATA[P];
        Map<Integer, int[]> map = new HashMap<>();

        for(int i = 0; i < m_Array.length; ++i)
            data[i] = new ODD_EVEN_DATA();

        for(int i = 0; i < m_Array.length; ++i)
            for(int j = 0; j < m_Array[i].length; ++j)
                m_Array[i][j] = ThreadLocalRandom.current().nextInt(0, 99);

        // Unsortiertes Array ausgeben.
        System.out.println("Unsortiertes Array ausgeben:");
        for(int i = 0; i < m_Array.length; ++i){
            //System.out.print("Prozessor " + i + ": ");
            for(int j = 0; j < m_Array[i].length; ++j){
                System.out.print(m_Array[i][j] + "\t");
            }
            //System.out.println();
        }

        for(int i = 0; i < m_Array.length; ++i){
            int[] subArray = new int[m_Array[i].length];
            for(int j = 0; j < m_Array[i].length; ++j)
                subArray[j] = m_Array[i][j];
            map.put(i, subArray);
        }

        for(int i = 0; i < processes.length; ++i){
            int[] subArray = map.get(i);
            processes[i] = new ODD_EVEN_SWAP(P, i, subArray, data, barrier);
        }

        startTime = System.currentTimeMillis();        

        for(int i = 0; i < processes.length; ++i)
            processes[i].start();
        
        for(int i = 0; i < processes.length; ++i){
            try {
                processes[i].join();    
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
                System.err.println("Warten auf Thread " + i + " unterbrochen");
                break;
            }
        }

        endTime = System.currentTimeMillis();
        duration = endTime - startTime;
        
        for(int i = 0; i < m_Array.length; ++i){
            int[] sortedSubArray = processes[i].getSortedSubArray();
            for(int j = 0; j < m_Array[i].length; ++j)
                m_Array[i][j] = sortedSubArray[j];
        }
        
        // Sortiertes Array ausgeben
        System.out.println();
        System.out.println("Sortiertes Array ausgeben: ");
        for(int i = 0; i < m_Array.length; ++i){
            //System.out.print("Prozessor " + i + ": ");
            for(int j = 0; j < m_Array[i].length; ++j){
                System.out.print(m_Array[i][j] + "\t");
            }
            //System.out.println();
        }
        System.out.println();
        // In Thread -> Run - preSort implementieren (Array.sort() + BubbleSort)
        // Die Zeit messen
    }
}