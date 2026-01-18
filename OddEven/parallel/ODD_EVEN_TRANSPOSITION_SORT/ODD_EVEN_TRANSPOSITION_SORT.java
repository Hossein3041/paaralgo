import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ODD_EVEN_TRANSPOSITION_SORT{
    public static void main(String[] args) throws InterruptedException {
        int N = 10; int P = N;
        int[] m_Array = new int[N];
        CyclicBarrier barrier = new CyclicBarrier(N);
        ODD_EVEN_SWAP[] processes = new ODD_EVEN_SWAP[P];
        ODD_EVEN_DATA[] data = new ODD_EVEN_DATA[N];

        for(int i = 0; i < m_Array.length; ++i){
            m_Array[i] = ThreadLocalRandom.current().nextInt(0, 99);
            data[i] = new ODD_EVEN_DATA();
        }

        System.out.println("Unsortiertes Array: ");
        for(int i = 0; i < m_Array.length; ++i)
            System.out.print(m_Array[i] + "\t");

        for(int i = 0; i < m_Array.length; ++i)
            processes[i] = new ODD_EVEN_SWAP(N, i, m_Array[i], data, barrier);
        
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
        
        for(int i = 0; i < m_Array.length; ++i)
            m_Array[i] = processes[i].getSortedValue();
        
        System.out.println();
        System.out.println("Sortiertes Array: ");
        for(int i = 0; i < m_Array.length; ++i)
            System.out.print(m_Array[i] + "\t");
        System.out.println();
    }
}