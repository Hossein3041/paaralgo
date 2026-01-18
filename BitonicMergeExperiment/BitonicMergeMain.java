import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.Arrays;
import java.util.Random;

public class BitonicMergeMain {
    /** Array mit allen Prozessoren (Zugriff über Index = Prozess-ID) */
    private static BitonicMergeThread[] processes;

    public static void main(String... args) {
        int N = 30;
        int P = 10;

        if (N % P != 0) {
            System.err.println("Fehler: N muss durch P teilbar sein");
            System.exit(2);
        }

        int[][] m_Array = new int[P][N / P];
        Random rand = new Random(0L);
        for (int i = 0; i < m_Array.length; ++i) {
            for (int j = 0; j < m_Array[i].length; ++j) {
                m_Array[i][j] = rand.nextInt();
            }
        }

        int newP = nextPowerOfTwo(P); // P durch N auf nächste 2^k bringen

        /*
        int[][] padded_Array = new int[newP][N / P];
        for (int i = 0; i < m_Array.length; ++i) {
            padded_Array[i] = m_Array[i];
        }
        for (int i = m_Array.length; i < padded_Array.length; ++i) {
            int[] pad = new int[N / P];
            Arrays.fill(pad, Integer.MAX_VALUE);
            padded_Array[i] = pad;
        }
        */

        CyclicBarrier barrier = new CyclicBarrier(newP); // Barrier für Synchronisation
        processes = new BitonicMergeThread[newP]; // Thread-Array anlegen

        print2DArray(m_Array, "Array unsortiert");

        for (int i = 0; i < processes.length; ++i) { // Thread-Objekte erzeugen: process_id, zu sortierender Wert,
                                                     // Stages als Anzahl der Schritte, sowie barriere
            int[] slice = (i < P) ? m_Array[i] : null;
            processes[i] = new BitonicMergeThread(i, slice, P, N/P, newP, barrier);
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
        System.out.println(title);
        int count = 0;
        for(int i = 0; i < array.length; ++i){
            for(int j = 0; j < array[i].length; ++j){
                System.out.print(array[i][j]);
                ++count;
                if(count % 20 == 0){
                    System.out.print("\n");
                } else {
                    System.out.print("\t");
                }
            }
        }
        if(count % 20 != 0) System.out.print("\n");
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
        private final int originalP;
        private final int subLength;
        private int[] my_Array; // Enthält aktuellen Wert des Prozessors. Am Ende beinhaltet sie den bereits
                                // sortierten Wert.
        private int[] next_Array;
        private final int m_ProcessCount; // Enthält Prozessor-Anzahl, ist eine Zweierpotenz, und entscheidet über
                                          // Rekursionstiefe.
        private final CyclicBarrier m_Barrier; // Barriere, ob alle Threads innerhalb compare-and-swap zwischen den
                                               // Schritten zu synchronisieren.
        private volatile int[] partner_Array; // Enthält Wert des Partners beim Datenaustausch

        /** === Konstruktor === */
        public BitonicMergeThread(int processID, int[] myArray, int originalP, int subLength, int processCount, CyclicBarrier barrier) {
            this.process_ID = processID;
            this.my_Array = myArray;
            this.originalP = originalP;
            this.subLength = subLength;
            this.next_Array = new int[subLength];
            this.m_ProcessCount = processCount;
            this.m_Barrier = barrier;
        }

        @Override
        public void run() {
            try {
                //initializePreSorting();
                if (my_Array != null) Arrays.sort(my_Array);
                // Sortierungsanstoß für Hypercube [0 .      . processCount) aufsteigend
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
    for (int distance = count >>> 1; distance > 0; distance >>>= 1) {
        int partnerID = process_ID ^ distance;
        // Prüfen, ob beide echte Threads sind UND im Subblock:
        boolean inBlock = process_ID < originalP
                       && partnerID  < originalP
                       && process_ID >= low
                       && process_ID <  low + count
                       && partnerID  >= low
                       && partnerID  <  low + count;
        if (!inBlock) {
            // völlig überspringen – keine send/recv, nur Barriers
            awaitAtBarrier();
            awaitAtBarrier();
            awaitAtBarrier();
            continue;
        }

        // Jetzt sind es zwei echte Threads: Datentausch
        sendMyArray(partnerID, my_Array);
        awaitAtBarrier();
        int[] received = receivePartnerArray();
        awaitAtBarrier();

        // erst hier mergen & swap
        boolean inLower = ((process_ID - low) % (distance<<1)) < distance;
        boolean takeMin = ascending ? inLower : !inLower;
        getRightArray(my_Array, received, takeMin, next_Array);
        int[] tmp = my_Array;  my_Array = next_Array;  next_Array = tmp;
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

        /** Eigenen Empangspuffer auslesen */
        private synchronized int[] receivePartnerArray() {
            return partner_Array;
        }

        /** Min oder Max nehmen, abhängig vom boolean */
        public void getRightArray(int[] a, int[] b, boolean takeMin, int[] dest) {
            int n = subLength, i = 0, j = 0, k = 0;
            int[] scratch = new int[n * 2];
            while (i < n && j < n) {
                int va = (a != null ? a[i] : Integer.MAX_VALUE);
                int vb = (b != null ? b[j] : Integer.MAX_VALUE);
                scratch[k++] = (va < vb) ? va : vb;
                if (va < vb) i++;
                else           j++;
            }
            while (i < n)     scratch[k++] = (a != null ? a[i++] : Integer.MAX_VALUE);
            while (j < n)     scratch[k++] = (b != null ? b[j++] : Integer.MAX_VALUE);

            if (takeMin) {
                // untere Hälfte übernehmen
                for (int x = 0; x < n; ++x) dest[x] = scratch[x];
            } else {
                // ** hier die Korrektur: **
                System.arraycopy(scratch, n, dest, 0, n);
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
            // Für Phantom‐Threads my_Array==null → liefert nur MAX-Werte,
            // aber wir rufen nur für i<originalP auf.
            return my_Array != null ? my_Array : new int[subLength];
        }
    }
}