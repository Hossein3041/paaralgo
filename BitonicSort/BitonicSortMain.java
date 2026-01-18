import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.Arrays;
import java.util.Random;

public class BitonicSortMain {
    /** Array mit allen Prozessoren (Zugriff über Index = Prozess-ID) */
    private static BitonicSortThread[] processes;

    public static void main(String... args) {
        // int[] m_Array = new int[] {30,29,28,27,24,32325,2,23,34,5,6,7,54}; //
        // Beispiel-Eingabe (beliebige
        // Länge, nicht
        // unbedingt
        // 2^k)
        int[] m_Array = generateRandomArray(1024);
        int N = m_Array.length; // *
        // System.out.println(N);
        int P = N; // P = N

        CyclicBarrier barrier = new CyclicBarrier(P); // Barrier für Synchronisation
        processes = new BitonicSortThread[P]; // Thread-Array anlegen

        printArray(m_Array, "Array unsortiert");

        for (int i = 0; i < processes.length; ++i) { // Thread-Objekte erzeugen: process_id, zu sortierender Wert,
                                                     // Stages als Anzahl der Schritte, sowie barriere
            processes[i] = new BitonicSortThread(i, m_Array[i], P, barrier);
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
            m_Array[i] = processes[i].getSortedValue();
        }

        printArray(m_Array, "Array sortiert");
    }

    /** Array ausgeben */
    public static void printArray(int[] array, String message) {
        System.out.println(message);
        for (int i = 0; i < array.length; ++i) {
            System.out.print(array[i] + "\t");
        }
        System.out.println();
    }

    public static int[] generateRandomArray(int size) {
        if (size > 2000) {
            throw new IllegalArgumentException(
                    "Größe darf 100 nicht überschreiten, da nur 100 eindeutige Zahlen von 0 bis 99 möglich sind.");
        }

        int[] array = new int[size];
        Random rand = new Random();
        java.util.HashSet<Integer> used = new java.util.HashSet<>();

        int i = 0;
        while (i < size) {
            int candidate = rand.nextInt(10000);
            if (!used.contains(candidate)) {
                used.add(candidate);
                array[i++] = candidate;
            }
        }

        return array;
    }

    /** === Innere Thread-Klasse === */
    static class BitonicSortThread extends Thread {
        /** === Attribute für Prozessor === */
        private final int process_ID; // Prozess-ID, um Prozessor im Hypercube(0..P-1) zu identifizieren. Wird zur
                                      // Kommunikation mit dem Partner per Bit-flip benutzt.
        private int my_Value; // Enthält aktuellen Wert des Prozessors. Am Ende beinhaltet sie den bereits
                              // sortierten Wert.
        private final int m_ProcessCount; // Enthält Prozessor-Anzahl, ist eine Zweierpotenz, und entscheidet über
                                          // Rekursionstiefe.
        private final CyclicBarrier m_Barrier; // Barriere, ob alle Threads innerhalb compare-and-swap zwischen den
                                               // Schritten zu synchronisieren.
        private volatile int partner_Value; // Enthält Wert des Partners beim Datenaustausch

        /** === Konstruktor === */
        public BitonicSortThread(int processID, int myValue, int processCount, CyclicBarrier barrier) {
            this.process_ID = processID;
            this.my_Value = myValue;
            this.m_ProcessCount = processCount;
            this.m_Barrier = barrier;
        }

        @Override
        public void run() {
            try {
                // Sortierungsanstoß für Hypercube [0 .. processCount) aufsteigend
                sort(0, m_ProcessCount, true);
            } catch (BrokenBarrierException | InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread " + process_ID + " abegebrochen.");
            }
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
            if(((process_ID - low) % count) < count >> 1){   // Prüfen, ob aktueller Knoten in der unteren Hälfte ist, dann aufsteigend sortieren
                sort(low, count >> 1, true);
            } else {    // Sonst, absteigend sortieren.
                sort(low + (count >> 1), count >> 1, false);
            }
            // Aber wir dürfen low hier nicht angeben als low + (count >> 1). Das muss durch den Parameter definiert sein: Für linke Hälfte 0, für rechte Hälfte count/2
            // Vielleicht den Parameter low tauschen durch Dimension? Und anhand von Dimension irgendwie low berechnen oder so.
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
                // 4. Tausche immer - Partner liegt garantiert im selben Block
                sendMyValue(partner_ID, my_Value);
                awaitAtBarrier(); // 5. Warten, bis Partner gesendet hat
                // 6. Empfange Wert und vergleiche
                int received_Value = receivePartnerValue();
                int countSize = distance << 1; // 7. Blockgröße für diese Merge-Stufe
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
                my_Value = getRightValue(my_Value, received_Value, takeMin);

                awaitAtBarrier();
            }
        }

        /** Wert an Partner schicken */
        private void sendMyValue(int partnerID, int myValue) {
            processes[partnerID].setOtherValue(myValue);
        }

        /** Vom Partner aufgerufene Methode zum Setzen des Werts */
        private synchronized void setOtherValue(int val) {
            this.partner_Value = val;
        }

        /** Eigenen Empfangspuffer auslesen */
        private int receivePartnerValue() {
            return partner_Value;
        }

        /** Min oder Max nehmen, abhängig vom boolean */
        public int getRightValue(int a, int b, boolean min) {
            return min ? (a < b ? a : b) : (a > b ? a : b);
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
        public int getSortedValue() {
            return my_Value;
        }
    }
}