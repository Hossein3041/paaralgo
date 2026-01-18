import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.Arrays;

public class BitonicMergeMain {
    /** Array mit allen Prozessoren (Zugriff über Index = Prozess-ID) */
    private static BitonicMergeThread[] processes;

    public static void main(String... args) {
        int[] m_Array = new int[] { 2, 5, 6, 10, 8, 7, 4, 1 }; // Beispiel-Eingabe (beliebige
                                                                                        // Länge, nicht
                                                                                        // unbedingt
        // 2^k)
        int N = m_Array.length; // *
        int P = nextPowerOfTwo(N); // P durch N auf nächste 2^k bringen

        int[] padded = new int[P]; // *
        System.arraycopy(m_Array, 0, padded, 0, N); // *
        for (int i = N; i < P; ++i) // Padding mit Integer.MAX_VALUE
            padded[i] = Integer.MAX_VALUE;

        CyclicBarrier barrier = new CyclicBarrier(P); // Barrier für Synchronisation
        processes = new BitonicMergeThread[P]; // Thread-Array anlegen

        printArray(m_Array, "Array unsortiert");

        for (int i = 0; i < processes.length; ++i) { // Thread-Objekte erzeugen: process_id, zu sortierender Wert,
                                                     // Stages als Anzahl der Schritte, sowie barriere
            processes[i] = new BitonicMergeThread(i, padded[i], P, barrier);
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

    /** Nächstgrößere 2^k => x */
    public static int nextPowerOfTwo(int x) {
        int p = 1;
        while (p < x)
            p <<= 1;
        return p;
    }

    /** === Innere Thread-Klasse === */
    static class BitonicMergeThread extends Thread {
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
        public BitonicMergeThread(int processID, int myValue, int processCount, CyclicBarrier barrier) {
            this.process_ID = processID;
            this.my_Value = myValue;
            this.m_ProcessCount = processCount;
            this.m_Barrier = barrier;
        }

        @Override
        public void run() {
            try {
                // Sortierungsanstoß für Hypercube [0 .. processCount) aufsteigend
                sort(m_ProcessCount, true);
            } catch (BrokenBarrierException | InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread " + process_ID + " abegebrochen.");
            }
        }

        /** Wrapper, der die Folge erstmal bitonisch macht und anschließend mergt */
        private void sort(int count, boolean ascending)
                throws BrokenBarrierException, InterruptedException {
            makeBitonic(0, count, ascending);

        }

        /**
         * Rekursiv wird der Block/sub-Block [low .. low + count) bitonisch sortiert.
         * Block/sub-Block wird in zwei geteilt: Erste aufsteigend, zweite Hälfte absteigend.
         * Anschließend wird gemergt.
         */
        private void makeBitonic(int low, int count, boolean ascending)
                throws BrokenBarrierException, InterruptedException {
            if (count > 1) {
                int half = count >> 1;  // Die hälfte der Größe wird ber bit-shifting berechnet
                makeBitonic(low, half, true);   // Linke Hälfte wird bitonisch sortiert, aufsteigend
                makeBitonic(low + half, half, false);   // Rechte Hälfte wird bitonisch sortiert, absteigend
                bitonicMerge(low, count, ascending);    // Beide Hälften werden in eine bitonische Folge gemergt
            }
        }

        /**
         * Hybride bitonic merge: for-Schleife für aktuellen Block
         * Rekursion, um sub-Blöcke zu bearbeiten und merge zu Ende zu führen
         * low:         Start-Index des Bliocks
         * count:       Anzahl Elemente im Block
         * ascending:   Nötig, um Sortierrichtung zu bestimmen
         */
        private void bitonicMerge(int low, int count, boolean ascending)
                throws BrokenBarrierException, InterruptedException {
            if (count > 1) {
                int half = count >> 1;
                for (int i = low; i < low + half; ++i)  // compare-and-swap zwischen Elementen in der linken und rechten Hälfte
                    compareAndSwap(i, half, ascending);
                // Rekursiver Merge-Aufruf, für die jeweiligen Hälften
                bitonicMerge(low, half, ascending);
                bitonicMerge(low + half, half, ascending);
            }
        }

        /**
         * Führt einen barrier-gesicherten compare-and-swap zwischen zwei Partner-Threads durch.
         * i:           Index des linken Elements im aktuellen Merge-Block
         * half:        Abstandsgröße (count/2) für den rechten Partner
         * ascending:   True = globaler Merge aufsteigend, false = absteigend
         * 
         * Ablauf im Detail:
         * 1. Bestimme 'left' und 'right' basierend auf i und half.
         * 2. Errechner partner_ID: Wenn aktueller Thread gleich left, ist Partner-Thread gleich right, sonst umgekehrt.
         * 3. Nur Threads mit process_ID == left oder == right tauschen aktiv Werte aus:
         * 4. Alle anderen Threads (nicht beteiligt) rufen trotzdem zweimal await(), um Barrier-Synchronisation nicht zu stören. 
         */
        private void compareAndSwap(int i, int half, boolean ascending)
                throws BrokenBarrierException, InterruptedException {

            // 1. Bestimme linke und rechte Position
            int left = i;
            int right = i + half;
            /*
             * 2. Errechner partner_ID:
             * Wenn process_ID == left, ist partner_ID right, sonst umgekehrt.
             * Für alle anderen Threads ist dieser Wert nicht relevant.
             */
            int partner_ID = (process_ID == left ? right : left);
            
            // 3. Prüfe, ob aktueller Thread (process_ID), Teil dieses Paares ist, also innerhalb des Blocks ist.
            if (process_ID == left || process_ID == right) {
                /*
                 * 3a. Bestimme, ob dieser Thread den kleineren oder grö0eren Wert behält:
                 *      - ascending==true   und process_ID==left  => diesen Thread behält das Minimum.
                 *      - ascending==true   und process_ID==right => diesen Thread behält das Maximum.
                 *      - ascending==false  und process_ID==left  => diesen Thread behält das Maximum.
                 *      - ascending==false  und process_ID==right => diesen Thread behält das Minumum.
                 */
                boolean takeMin = ascending ? (process_ID == left) : (process_ID == right);
                // 3b. Sende aktuellen eigenen Wert an den Partner
                sendMyValue(partner_ID, my_Value);
                // Warten, bis auch der Partner gesendet hat.
                awaitAtBarrier();

                // 3c. Lese den Wert, den der Partner geschickt hat
                int received_Value = receivePartnerValue();
                // 3d. Wähle min oder max basierend auf takeMin
                my_Value = getRightValue(my_Value, received_Value, takeMin);
                // Warten, bis beide Threads ihre Werte aktualisiert haben
                awaitAtBarrier();
            } else {
                /*
                 * 4. Threads, die nicht am Paar beteiligt sind,
                 * müssen trotzdem zweimal warten, damit alle Barrier-Aufrufe synchron bleiben und kein Deadlock entsteht. 
                 */
                awaitAtBarrier();   // Dummy: auf das erste await der aktiven Partner warten
                awaitAtBarrier();   // Dummy: auf das zweite await der aktiven Partner warten
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