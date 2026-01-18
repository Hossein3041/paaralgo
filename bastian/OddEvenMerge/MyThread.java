package OddEvenMerge;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.Arrays;

class MyThread extends Thread {

    private int index;
    CyclicBarrier cyclicBarrier;
    private int[] array1;
    private int parity;

    public MyThread(int index, int[] subarray, CyclicBarrier cyclicBarrier) {
        this.index = index;
        this.cyclicBarrier = cyclicBarrier;
        array1 = subarray;
        //array1 = Main.getNumbers(index);
        parity = (index+1) % 2;
    }

    private synchronized void preSort() {
        if(!Main.bubbleSort) {
            //Quick-Sort
            Arrays.sort(array1);

        }else {
            //Bubble Sort
            int tmp;
            for(int i=1; i<array1.length; i++) {
                for(int j=0; j<array1.length-i; j++) {
                    if(array1[j]>array1[j+1]) {
                        tmp=array1[j];
                        array1[j]=array1[j+1];
                        array1[j+1]=tmp;
                    }
                    
                }
            }
        }
    }

    public synchronized int[] getArray() {
        return array1;
    } 

    private void merge_split() {
        //Ausgabe welcher Thread in welchem Cycle diese Methode verwendet
        //System.out.println("Thread "+(index+1)+" in Cycle: "+Main.cycle);

        if(index != Main.p-1) {
            int[] array2 = Main.threadlist[index+1].getArray();
            int[] tmp = new int[array1.length * 2];
            int head1 = 0, head2 = 0;

            //sortiertes merges der beiden arrays in tmp
            for(int i=0; i<tmp.length; ++i) {
                //überprüfen ob ein Lesekopf bereits sein Ende erreicht hat
                if(head1 == array1.length) {
                    tmp[i] = array2[head2];
                    ++head2;
                } else {
                    if(head2 == array2.length) {
                        tmp[i] = array1[head1];
                        ++head1;
                        //ansonsten je nach kleinerem Wert
                        } else {
                            if(array1[head1] <= array2[head2]) {
                                    tmp[i] = array1[head1];
                                    ++head1;
                                } else {
                                    tmp[i] = array2[head2];
                                    ++head2;
                                }
                            }
                         } 
            }

            //vom gemergten Array zurück in die Teilarrays schreiben
            for(int i=0; i<tmp.length; ++i) {
                if(i<array1.length) array1[i] = tmp[i];
                else array2[i-array1.length] = tmp[i];
            }
        }
    }

    //Implementation der auszuführenden run-Methode
    @Override
    public void run() {

        preSort();

        for(int i=1; i<=Main.p; ++i) {
            if(i % 2 == parity) merge_split();

            try {
                    cyclicBarrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
            }
        }
        //Teil-Array an Main zurückliefern
        //Main.returnNumbers(index, array1);
    }
}
