import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class ODD_EVEN_SWAP extends Thread{
    int N; int process_ID; int my_Value;
    ODD_EVEN_DATA[] m_Data;
    CyclicBarrier m_Barrier;
    public ODD_EVEN_SWAP(int N, int id, int myvalue, ODD_EVEN_DATA[] data, CyclicBarrier barrier){
        this.N = N;
        this.process_ID = id;
        this.my_Value = myvalue;
        this.m_Data = data;
        this.m_Barrier = barrier;
    }

    public void sendToRight(int value){
        if(process_ID < N - 1)
            m_Data[process_ID + 1].setFromLeft(value);
    }

    public int receiveFromRight(){
        return m_Data[process_ID].getFromRight();
    }

    public void sendToLeft(int value){
        if(process_ID > 0)
            m_Data[process_ID - 1].setFromRight(value);
    }

    public int receiveFromLeft(){
        return m_Data[process_ID].getFromLeft();
    }

    @Override
    public void run(){
        for(int i = 0; i < N; ++i){
            boolean amLeft = ((i + process_ID) % 2 == 0);

            if(amLeft && process_ID < N - 1)
                sendToRight(my_Value);
            else if(!amLeft && process_ID > 0)
                sendToLeft(my_Value);
            
            try{
                m_Barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread " + process_ID + " Synchronisationsfehler.");
                return;
            }

            if(amLeft && process_ID < N - 1){
                int other_Value = receiveFromRight();
                my_Value = ODD_EVEN_DATA.min(my_Value, other_Value);
            } else if(!amLeft && process_ID > 0) {
                int other_Value = receiveFromLeft();
                my_Value = ODD_EVEN_DATA.max(my_Value, other_Value);
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

    public int getSortedValue(){
        return my_Value;
    }
}