import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.Arrays;
class ODD_EVEN_SWAP extends Thread{
    int m_Cycles, process_ID;
    int[] my_Array;
    ODD_EVEN_DATA[] m_Data;
    CyclicBarrier m_Barrier;
    public ODD_EVEN_SWAP(int cycles, int id, int[] array, ODD_EVEN_DATA[] data, CyclicBarrier barrier){
       this.m_Cycles = cycles;
       this.process_ID = id;
       this.my_Array = array;
       this.m_Data = data;
       this.m_Barrier = barrier;
    }

    public void sendToRight(int[] value){
        if(process_ID < m_Cycles - 1)
            m_Data[process_ID + 1].setFromLeft(value);
    }

    public int[] receiveFromRight(){
        return m_Data[process_ID].getFromRight();
    }

    public void sendToLeft(int[] value){
        if(process_ID > 0)
            m_Data[process_ID - 1].setFromRight(value);
    }

    public int[] receiveFromLeft(){
        return m_Data[process_ID].getFromLeft();
    }

    public synchronized void initializePreSorting(boolean sortAsBubble){
        if(!sortAsBubble)
            Arrays.sort(my_Array);
        else if(sortAsBubble){
            for (int i1 = 1; i1 < my_Array.length; ++i1) {
                boolean oneSwap = false;
                for (int i2 = 0; i2 < my_Array.length - i1; ++i2) {
                    if (my_Array[i2] > my_Array[i2 + 1]) {
                        int temp = my_Array[i2];
                        my_Array[i2] = my_Array[i2 + 1];
                        my_Array[i2 + 1] = temp;
                        oneSwap = true;
                    }
                }
                if (!oneSwap) break;
            }
        }

    }

    @Override
    public void run(){

        initializePreSorting(false);

        try{
            m_Barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread " + process_ID + " Synchronisationsfehler.");
            return;
        }

        for(int i = 0; i < m_Cycles; ++i){
            boolean amLeft = ((i + process_ID) % 2 == 0);

            if(amLeft && process_ID < m_Cycles - 1)
                sendToRight(my_Array);
            else if(!amLeft && process_ID > 0)
                sendToLeft(my_Array);
            
            try{
                m_Barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread " + process_ID + " Synchronisationsfehler.");
                return;
            }

            if(amLeft && process_ID < m_Cycles - 1){
                int[] other_Array = receiveFromRight();
                my_Array = ODD_EVEN_DATA.mergeAndSplit(my_Array, other_Array, true);
            } else if(!amLeft && process_ID > 0) {
                int[] other_Array = receiveFromLeft();
                my_Array = ODD_EVEN_DATA.mergeAndSplit(my_Array, other_Array, false);
            }

            try{
                m_Barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread " + process_ID + " Synchronisationsfehler.");
                return;
            }
        }
    }

    public int[] getSortedSubArray(){
        return my_Array;
    }
}