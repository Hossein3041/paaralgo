import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.Arrays;
import java.util.Random;

public class BitonicMergeMain {
    /** Array mit allen Prozessoren (Zugriff über Index = Prozess-ID) */
    private static BitonicMergeThread[] processes;

    public static void main(String... args) {
        int N = 70;
        int P = 35;
        System.out.println("N: " + N + " P: " + P);
        if (N % P != 0) {
            System.err.println("Fehler: N muss durch P teilbar sein");
            System.exit(2);
        }

        int[][] m_Array = new int[P][N / P];
        Random rand = new Random(0L);
        for (int i = 0; i < m_Array.length; ++i) {
            for (int j = 0; j < m_Array[i].length; ++j) {
                m_Array[i][j] = rand.nextInt(20);
            }
        }

        int newP = nextPowerOfTwo(P); // P durch N auf nächste 2^k bringen

        int[][] padded_Array = new int[newP][N / P];
        for (int i = 0; i < m_Array.length; ++i) {
            padded_Array[i] = m_Array[i];
        }
        for (int i = m_Array.length; i < padded_Array.length; ++i) {
            int[] pad = new int[N / P];
            Arrays.fill(pad, Integer.MAX_VALUE);
            padded_Array[i] = pad;
        }

        CyclicBarrier barrier = new CyclicBarrier(newP); // Barrier für Synchronisation
        processes = new BitonicMergeThread[newP]; // Thread-Array anlegen

        print2DArray(m_Array, "Array unsortiert");

        for (int i = 0; i < processes.length; ++i) { // Thread-Objekte erzeugen: process_id, zu sortierender Wert,
                                                     // Stages als Anzahl der Schritte, sowie barriere
            processes[i] = new BitonicMergeThread(i, padded_Array[i], newP, barrier);
        }

        for (int i = 0; i < processes.length; ++i) { // Thread-Objekte starten
            processes[i].start();
        }

        for (int i = 0; i < processes.length; ++i) { // Thread-Objekte joinen: Auf Beendigung aller Threads warten
            try {
                processes[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Warten auf Thread " + i + " unterbrochen");
                break;
            }
        }

        // Sortierten Wert zurücklegen
        for (int i = 0; i < m_Array.length; ++i) {
            m_Array[i] = processes[i].getSortedSubArray();
        }

        print2DArray(m_Array, "Array sortiert");
    }

    /** Array ausgeben */
    public static void print2DArray(int[][] array, String title) {
        System.out.println();
        System.out.println(title);
        for (int i = 0; i < array.length; ++i) {
            for (int j = 0; j < array[i].length; ++j) {
                System.out.print(array[i][j] + "\t");
            }
        }
        System.out.println();
    }

    /** Nächstgrößere 2^k => x */
    public static int nextPowerOfTwo(int x) {
        int p = 1;
        while (p < x)
            p <<= 1;
        return p;
    }

    public static boolean isSortedAndHasSameElements(int[][] before, int[][] after) {
        int prev = Integer.MIN_VALUE;
        boolean result = true;
        Arrays.sort(before);
        for (int i = 0; i < before.length; ++i) {
            for (int j = 0; j < before[i].length; ++j) {
                if ((prev > after[i][j]) || (before[i][j] != after[i][j])) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    /** === Innere Thread-Klasse === */
    static class BitonicMergeThread extends Thread {
        /** === Attribute für Prozessor === */
        private final int process_ID; // Prozess-ID, um Prozessor im Hypercube(0..P-1) zu identifizieren. Wird zur
                                      // Kommunikation mit dem Partner per Bit-flip benutzt.
        private int[] my_Array; // Enthält aktuellen Wert des Prozessors. Am Ende beinhaltet sie den bereits
                                // sortierten Wert.
        private int[] next_Array;
        private final int m_ProcessCount; // Enthält Prozessor-Anzahl, ist eine Zweierpotenz, und entscheidet über
                                          // Rekursionstiefe.
        private final CyclicBarrier m_Barrier; // Barriere, ob alle Threads innerhalb compare-and-swap zwischen den
                                               // Schritten zu synchronisieren.
        private volatile int[] partner_Array; // Enthält Wert des Partners beim Datenaustausch

        /** === Konstruktor === */
        public BitonicMergeThread(int processID, int[] myArray, int processCount, CyclicBarrier barrier) {
            this.process_ID = processID;
            this.my_Array = myArray;
            this.next_Array = new int[myArray.length];
            this.m_ProcessCount = processCount;
            this.m_Barrier = barrier;
        }

        @Override
        public void run() {
            try {
                initializePreSorting();
                // Sortierungsanstoß für Hypercube [0 .. processCount) aufsteigend
                sort(0, m_ProcessCount, true);
            } catch (BrokenBarrierException | InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread " + process_ID + " abegebrochen.");
            }
        }

        public synchronized void initializePreSorting(){
            Arrays.sort(my_Array);
        }

        /** Wrapper, der die Folge erstmal bitonisch macht und anschließend mergt */
        private void sort(int low, int count, boolean ascending)
                throws BrokenBarrierException, InterruptedException {
            // makeBitonic(0, count, ascending);
            if (count <= 1)
                return;
            makeBitonic(low, count, ascending);
            bitonicMerge(low, count, ascending);
        }

        /**
         * Rekursiv wird der Block/sub-Block [low .. low + count) bitonisch sortiert.
         * Block/sub-Block wird in zwei geteilt: Erste aufsteigend, zweite Hälfte
         * absteigend.
         * Anschließend wird gemergt.
         */
        private void makeBitonic(int low, int count, boolean ascending)
                throws BrokenBarrierException, InterruptedException {
            sort(low, count >> 1, true);
            sort(low + (count >> 1), count >> 1, false);
        }

        /*
         * Führe eine iterative Bitonic-Merge-Phase auf dem Teilblock [lwo .. low +
         * count) durch.
         * low: Startindex des aktuellen Subblocks.
         * count: Länge des aktuellen Subblocks (muss eine Zweierpotenz sein).
         * ascending: True= sortiere aufsteigend, False= sortiere absteigend
         * 
         * Ablauf:
         * 1. Distance nimmt nacheinander Werte: count/2, count/4, ..., 1 an.
         * 2. Partner-Findung wird per Bit-Flip (XOR) gefunden: prozess_ID ^ distance.
         * 3. inCountSize: Prüft, ob sowohl dieser Thread als auch sein Partner
         * innerhalb des Subblocks [low, low+count) liegen.
         * 4. Wenn nicht, führen beide Threads zwei Dummy-Barriers aus und überspringen
         * diese Phase.
         * 5. Wenn ja, tauschen sie ihre Werte aus (sendMyValue / receivePartnerValue),
         * synchronisiert über zwei awaitBarrier-Aufrufe.
         * 6. Danach entscheidet jeder, ob er den kleineren (takeMin) oder größeren Wert
         * behält, abhängig davon, ob er in der unteren oder oberen Hälfte des
         * distance-Blocks liegt.
         */
        private void bitonicMerge(int low, int count, boolean ascending)
                throws BrokenBarrierException, InterruptedException {
            // Nummerierung ist nicht unbedingt wie oben!
            // Lauf über alle Compare-Disdanzen: count/2, count/4, ..., 1
            for (int distance = count >> 1; distance > 0; distance >>= 1) {
                // 1. Partner-findung durch Bit-Flip an der distance-Stelle
                int partner_ID = process_ID ^ distance;
                // 2. Prüfen, ob beide Threads (Dieser und Partner) im Subblock sind
                int countSize = distance << 1; // 7. Blockgröße für diese Merge-Stufe
                boolean inCountSize = process_ID >= low
                        && process_ID < low + count
                        && partner_ID >= low
                        && partner_ID < low + count;
                if (!inCountSize) {
                    // 3. Threads außerhalb überspringen, aber rufen dennoch zwei Barirers auf
                    awaitAtBarrier();
                    awaitAtBarrier();
                    awaitAtBarrier();
                    continue;
                }
                // 4. Tausche immer - Partner liegt garantiert im selben Block
                sendMyArray(partner_ID, my_Array);
                awaitAtBarrier(); // 5. Warten, bis Partner gesendet hat
                // 6. Empfange Wert und vergleiche
                int[] received_Array = receivePartnerArray();
                awaitAtBarrier();
                boolean inLowerHalf = ((process_ID - low) % countSize) < distance; // 8. Prüfen, ob process_ID sich in
                                                                                   // der unteren oder oberen Hälfte
                                                                                   // befindet.
                boolean takeMin = ascending ? inLowerHalf : !inLowerHalf;
                /*
                 * ascending=true, Threads in der unteren Hälfte behalten das Mininum
                 * (takeMin=true)
                 * ascending=true, Threads in der oberen Hälfte behalten das Maximum
                 * (takeMin=false)
                 * ascending=false, Threads in der unteren Hälfte behalten das Maximum
                 * (takeMin=false)
                 * ascending=false, Threads in der oberen Hälfte behalten das Minimmum
                 * (takeMin=true)
                 */
                getRightArray(received_Array, takeMin, next_Array);
                int[] temp = my_Array;
                my_Array = next_Array;
                next_Array = temp;
                awaitAtBarrier();
            }
        }

        /** Wert an Partner schicken */
        private void sendMyArray(int partnerID, int[] myArray) {
            processes[partnerID].setOtherValue(myArray);
        }

        /** Vom Partner aufgerufene Methode zum Setzen des Werts */
        private synchronized void setOtherValue(int[] arr) {
            this.partner_Array = arr;
        }

        /** Eigenen Empfangspuffer auslesen */
        private synchronized int[] receivePartnerArray() {
            return partner_Array;
        }

        /** Min oder Max nehmen, abhängig vom boolean */
        public void getRightArray(int[] other_Array, boolean takeMin, int[] destination_Array) {
            int n = my_Array.length;
            int i1 = 0; int i2 = 0; int k = 0;
            int[] scratch = new int[n * 2];
            while (i1 < n && i2 < n)
                scratch[k++] = (my_Array[i1] < other_Array[i2]) ? my_Array[i1++] : other_Array[i2++];
            while (i1 < n)
                scratch[k++] = my_Array[i1++];
            while (i2 < n)
                scratch[k++] = other_Array[i2++];

            if (takeMin) {
                for (int j = 0; j < n; ++j)
                    destination_Array[j] = scratch[j]; // untere Hälfte
            } else {
                for (int j = 0; j < n; ++j)
                    destination_Array[j] = scratch[n + j]; // obere Hälfte
            }
        }

        /** Synchronisationspunkt */
        private void awaitAtBarrier() {
            try {
                m_Barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread " + process_ID + " Synchronisationsfehler.");
            }
        }

        /** Gibt den final sortierenden Wert zurück */
        public int[] getSortedSubArray() {
            return my_Array;
        }
    }
}