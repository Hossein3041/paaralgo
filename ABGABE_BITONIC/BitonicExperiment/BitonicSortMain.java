import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.Arrays;
import java.util.Random;

public class BitonicSortMain {
    /** Array mit allen Prozessoren (Zugriff über Index = Prozess-ID) */
    private static BitonicSortThread[] processes;

    public static void main(String... args) {
        long startTime, duration;
        long bestTime = Long.MAX_VALUE;
        boolean verbose = false;
        int N = 1000000;
        int P = 16;
        long seed = 0L;

        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "--size":
                case "-n":
                    N = Integer.parseInt(args[++i]);
                    break;
                case "--threads":
                case "-p":
                    P = Integer.parseInt(args[++i]);
                    break;
                case "--seed":
                    seed = Long.parseLong(args[++i]);
                    break;
                case "--verbose":
                    verbose = true;
                    break;
                default:
                    System.err.println("Unbekanntes Argument: " + args[i]);
                    System.exit(1);
            }
        }

        confirmInputValidation(N, P);
        int[][] m_Array = new int[P][N / P];
        int[][] original;

        CyclicBarrier barrier = new CyclicBarrier(P); // Barrier für Synchronisation
        processes = new BitonicSortThread[P]; // Thread-Array anlegen

        for (int run = 1; run <= 3; ++run) {
            System.gc();

            fillWithRandomValues(m_Array, seed);
            original = deepCopy2DArray(m_Array);

            if (verbose)
                printArray(m_Array, "Array unsortiert");

            for (int i = 0; i < processes.length; ++i) { // Thread-Objekte erzeugen: process_id, zu sortierender Wert,
                // Stages als Anzahl der Schritte, sowie barriere
                processes[i] = new BitonicSortThread(i, m_Array[i], P, barrier, verbose);
            }

            startTime = System.currentTimeMillis();
            handleProcesses(processes);
            duration = System.currentTimeMillis() - startTime;

            // Sortierten Wert zurücklegen
            for (int i = 0; i < m_Array.length; ++i) {
                m_Array[i] = processes[i].getSortedSubArray();
            }

            if (verbose)
                printArray(m_Array, "Array sortiert");

            confirmSortingValidation(m_Array, original);

            if (duration < bestTime) {
                bestTime = duration;
            }
        }

        System.out.printf("%d\t%d\t%d%n", N, P, bestTime);
    }

    private static void confirmInputValidation(int n, int p) {
        if (n % p != 0) {
            System.err.println("Fehler: N muss durch P teilbar sein!");
            System.exit(2);
        }
    }

    private static void fillWithRandomValues(int[][] array, long seed) {
        Random rand = new Random(seed);
        for (int i = 0; i < array.length; ++i) {
            for (int j = 0; j < array[i].length; ++j) {
                array[i][j] = rand.nextInt();
            }
        }
    }

    public static int[][] deepCopy2DArray(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; ++i)
            copy[i] = Arrays.copyOf(original[i], original[i].length);
        return copy;
    }

    public static void handleProcesses(BitonicSortThread[] processes) {
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
    }

    /** Array ausgeben */
    public static void printArray(int[][] array, String message) {
        System.out.println();
        System.out.println(message);
        for (int i = 0; i < array.length; ++i) {
            for (int j = 0; j < array[i].length; ++j) {
                System.out.print(array[i][j] + "\t");
            }
        }
        System.out.println();
    }

    private static void confirmSortingValidation(int[][] m_Array, int[][] original) {
        if (!isSorted(m_Array) || !hasSameElements(original, m_Array)) {
            System.err.printf("Fehler beim Sortieren!%n");
        }
    }

    private static boolean isSorted(int[][] array) {
        int prev = Integer.MIN_VALUE;
        for (int i = 0; i < array.length; ++i) {
            for (int j = 0; j < array[i].length; ++j) {
                if (array[i][j] < prev)
                    return false;
                prev = array[i][j];
            }
        }
        return true;
    }

    private static boolean hasSameElements(int[][] before, int[][] after) {
        int rows = before.length;
        int columns = before[0].length;
        int totalSize = rows * columns;

        int[] array_Before = new int[totalSize];
        int[] array_After = new int[totalSize];
        int index = 0;

        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < columns; ++j) {
                array_Before[index] = before[i][j];
                array_After[index] = after[i][j];
                ++index;
            }
        }

        Arrays.sort(array_Before);
        for (int i = 0; i < totalSize; ++i) {
            if (array_Before[i] != array_After[i])
                return false;
        }
        return true;
    }

    /** === Innere Thread-Klasse === */
    static class BitonicSortThread extends Thread {
        /** === Attribute für Prozessor === */
        private final int process_ID; // Prozess-ID, um Prozessor im Hypercube(0..P-1) zu identifizieren. Wird zur
                                      // Kommunikation mit dem Partner per Bit-flip benutzt.
        private int[] my_Array; // Enthält aktuellen Wert des Prozessors. Am Ende beinhaltet sie den bereits
                                // sortierten Wert.
        private final int m_ProcessCount; // Enthält Prozessor-Anzahl, ist eine Zweierpotenz, und entscheidet über
                                          // Rekursionstiefe.
        private final CyclicBarrier m_Barrier; // Barriere, ob alle Threads innerhalb compare-and-swap zwischen den
                                               // Schritten zu synchronisieren.
        private volatile int[] partner_Array; // Enthält Wert des Partners beim Datenaustausch
        private int[] nextArray;
        private static volatile int m_Step = 0;
        private boolean m_Verbose;

        /** === Konstruktor === */
        public BitonicSortThread(int processID, int[] myArray, int processCount, CyclicBarrier barrier,
                boolean mVerbose) {
            this.process_ID = processID;
            this.my_Array = myArray;
            this.nextArray = new int[myArray.length];
            this.m_ProcessCount = processCount;
            this.m_Barrier = barrier;
            this.m_Verbose = mVerbose;
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

        public synchronized void initializePreSorting() {
            Arrays.sort(my_Array);
        }

        /** Wrapper, der die Folge erstmal bitonisch macht und anschließend mergt */
        private void sort(int low, int count, boolean ascending)
                throws BrokenBarrierException, InterruptedException {
            // makeBitonic(0, count, ascending);
            if (count <= 1)
                return;
            makeBitonic(low, count, ascending);

            if (m_Verbose)
                printBitonicSequenz(low, count);

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
            if (((process_ID - low) % count) < (count >> 1)) { // Prüfen, ob aktueller Knoten in der unteren Hälfte ist,
                                                               // dann aufsteigend sortieren
                sort(low, count >> 1, true);
            } else { // Sonst, absteigend sortieren.
                sort(low + (count >> 1), count >> 1, false);
            }
            // Aber wir dürfen low hier nicht angeben als low + (count >> 1). Das muss durch
            // den Parameter definiert sein: Für linke Hälfte 0, für rechte Hälfte count/2
            // Vielleicht den Parameter low tauschen durch Dimension? Und anhand von
            // Dimension irgendwie low berechnen oder so.
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

                sendMyArray(partner_ID, my_Array);
                awaitAtBarrier(); // 5. Warten, bis Partner gesendet hat
                // 6. Empfange Wert und vergleiche
                int[] received_Array = receivePartnerArray();
                int countSize = distance << 1; // 7. Blockgröße für diese Merge-Stufe
                boolean inLowerHalf = ((process_ID - low) % countSize) < distance; // 8. Prüfen, ob process_ID sich
                                                                                   // in
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
                // mergeAndSplit(received_Array, my_Array, takeMin);
                merge(received_Array, takeMin);
                // mergeAndSwap(received_Array, takeMin);

                awaitAtBarrier();

                // swapArrays();

                if (m_Verbose) {
                    if (process_ID == 0 && low == 0 && count == m_ProcessCount) {
                        int currentStep = incrementAndGetStep();
                        printState(currentStep, low, count, distance);
                    }
                }

            }
        }

        /** Wert an Partner schicken */
        private void sendMyArray(int partnerID, int[] myArray) {
            // int[] copy = Arrays.copyOf(myArray, myArray.length);
            processes[partnerID].setOtherValue(myArray);
        }

        /** Vom Partner aufgerufene Methode zum Setzen des Werts */
        private synchronized void setOtherValue(int[] arr) {
            this.partner_Array = arr;
        }

        /** Eigenen Empfangspuffer auslesen */
        private int[] receivePartnerArray() {
            return partner_Array;
        }

        /** Min oder Max nehmen, abhängig vom boolean */
        private void merge(int[] otherArray, boolean takeMin) {
            // Optimierung zur Prüfung, ob man den gesuchten Array bereits hat, oder nur
            // swappen muss.
            int firstIndex = 0, lastIndex = my_Array.length - 1;
            if (takeMin) {
                if (my_Array[lastIndex] <= otherArray[firstIndex])
                    return;
                else if (my_Array[firstIndex] > otherArray[lastIndex]) {
                    int[] temp = my_Array;
                    my_Array = otherArray;
                    otherArray = temp;
                    return;
                }
            } else {
                if (otherArray[lastIndex] <= my_Array[firstIndex])
                    return;
                else if (otherArray[firstIndex] > my_Array[lastIndex]) {
                    int[] temp = my_Array;
                    my_Array = otherArray;
                    otherArray = temp;
                    return;
                }

            }
            // Die eigentliche Logik für das Mischen
            mergeAndSwap(otherArray, takeMin);
        }

        private void mergeAndSwap(int[] otherArray, boolean takeMin) {
            if (takeMin) { // Wir wollen die kleinsten Werte in nextArray
                int i = 0, j = 0;
                for (int k = 0; k < nextArray.length; ++k)
                    nextArray[k] = (j >= nextArray.length || (i < nextArray.length && my_Array[i] <= otherArray[j]))
                            ? my_Array[i++]
                            : otherArray[j++];
            } else { // Wir wollen die größten Werte in nextArray
                int i = nextArray.length - 1, j = nextArray.length - 1;
                for (int k = nextArray.length - 1; k >= 0; --k)
                    nextArray[k] = (j < 0 || (i >= 0 && my_Array[i] >= otherArray[j]))
                            ? my_Array[i--]
                            : otherArray[j--];
            }
            // Dreiecksswap
            int[] temp = my_Array;
            my_Array = nextArray;
            nextArray = temp;
        }

        private static synchronized int incrementAndGetStep() {
            return ++m_Step;
        }

        private void printBitonicSequenz(int low, int count) {
            awaitAtBarrier();
            if (process_ID == 0 && low == 0 && count == m_ProcessCount) {
                System.out.println();
                System.out.print("Bitonische Folge:\n   ");
                for (int i = 0; i < processes.length; ++i) {
                    int[] sub = processes[i].my_Array;
                    for (int j = 0; j < sub.length; ++j) {
                        System.out.print(String.format("%4d ", sub[j]));
                    }
                }
                System.out.println("\n");
            }
            awaitAtBarrier();
        }

        private void printState(int step, int low, int count, int distance) {
            int blockSize = distance << 1;
            int numberOfBlocks = count / blockSize;
            System.out.printf("%n--- Step %d (distance=%d) — %d Subblocks of size %d --- %n",
                    step, distance, numberOfBlocks, blockSize);

            int coldWidth = 6;
            System.out.print("ID:    ");
            for (int i = 0; i < processes.length; ++i) {
                System.out.print(String.format("%" + coldWidth + "d", i));
                if (((i - low + 1) % blockSize) == 0 && i != processes.length - 1) {
                    System.out.print(" | ");
                }
            }
            System.out.println();

            System.out.print("Value: ");
            for (int i = 0; i < processes.length; ++i) {
                int[] sub = processes[i].my_Array;
                for (int j = 0; j < sub.length; ++j) {
                    System.out.print(String.format("%4d ", sub[j]));
                }
                if (((i - low + 1) % blockSize) == 0 && i != processes.length - 1) {
                    System.out.print(" | ");
                }
            }
            System.out.println();
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