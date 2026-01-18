import java.util.concurrent.RecursiveAction;
import java.util.stream.IntStream;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.Random;
import java.util.Arrays;

public class ParMergeMain {
    private static int SEQ_TRESHOLD;
    private static int MERGE_TRESHOLD;
    private static int COPY_TRESHOLD;
    private static boolean SEQ_MERGE;
    private static boolean JAVA_PARALLEL_SORT;

    public static void main(String[] args) {
        long startTime, duration;
        long bestTime = Long.MAX_VALUE;
        boolean verbose = false;
        int N = 1_000_000;
        int P = 12;
        long seed = 0L;

        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "--size":
                case "-n":
                    N = Integer.parseInt(args[++i]);
                    break;
                case "--threads":
                case "-P":
                    P = Integer.parseInt(args[++i]);
                    break;
                case "--seed":
                    seed = Long.parseLong(args[++i]);
                    break;
                case "--verbose":
                    verbose = true;
                    break;
                case "--seqMerge":
                    SEQ_MERGE = true;
                    break;
                case "--java_parallel_sort":
                    JAVA_PARALLEL_SORT = true;
                    break;
                default:
                    System.err.println("Unbekanntes Argument: " + args[i]);
                    System.exit(1);
            }
        }

        initTresholds(N, P);
        int[] a = new int[N];
        int[] scratch = new int[N];

        for (int run = 1; run <= 3; ++run) {
            System.gc();
            fillWithRandomValues(a, seed);
            int[] original = deepCopy(a);
            if (verbose && run == 1)
                printArray(a, "array unsortiert");

            startTime = System.currentTimeMillis();
            sort(a, scratch, P);
            duration = System.currentTimeMillis() - startTime;

            if (verbose && run == 1)
                printArray(a, "array sortiert");
            confirmSortingValidation(a, original, verbose, run);

            if (duration < bestTime)
                bestTime = duration;
        }

        printResult(JAVA_PARALLEL_SORT, SEQ_MERGE, N, P, bestTime);
        // System.out.printf("%d\t%d\t%d%n", N, P, bestTime);
    }

    private static void initTresholds(int n, int p) {
        SEQ_TRESHOLD = Math.max(1, n / (p * 8));
        MERGE_TRESHOLD = SEQ_TRESHOLD / 2;
        // MERGE_TRESHOLD = n;
        COPY_TRESHOLD = SEQ_TRESHOLD / 2;
        // COPY_TRESHOLD = n;
    }

    private static void fillWithRandomValues(int[] array, long seed) {
        Random rand = new Random(seed);
        for (int i = 0; i < array.length; ++i) {
            array[i] = rand.nextInt();
        }
    }

    private static int[] deepCopy(int[] original) {
        return Arrays.copyOf(original, original.length);
    }

    public static void printArray(int[] array, String message) {
        System.out.println(message);
        for (int i = 0; i < array.length; ++i)
            System.out.print(array[i] + "\t");
        System.out.println();
    }

    private static void confirmSortingValidation(int[] a, int[] original, boolean verbose, int run) {
        if (!isSorted(a) || !hasSameElements(original, a)) {
            System.err.println("Fehler beim Sortieren!");
        } else {
            if (verbose && run == 1)
                System.out.println("Sortierung korrekt abgelaufen!");
        }
    }

    private static boolean isSorted(int[] array) {
        int prev = Integer.MIN_VALUE;
        for (int i = 0; i < array.length; ++i) {
            if (array[i] < prev) {
                return false;
            }
            prev = array[i];
        }
        return true;
    }

    private static boolean hasSameElements(int[] before, int[] after) {
        if (before.length != after.length)
            return false;
        Arrays.sort(before);
        for (int i = 0; i < before.length; ++i)
            if (before[i] != after[i])
                return false;
        return true;
    }

    private static void sort(int[] array, int[] scratch, int processes) {
        if (JAVA_PARALLEL_SORT) {
            ForkJoinPool custom = new ForkJoinPool(processes);
            try {
                custom.submit(() -> Arrays.parallelSort(array)).join();
            } finally {
                custom.shutdown();
            }
        } else {
            ForkJoinPool pool = new ForkJoinPool(processes);
            try {
                pool.submit(new ParMergeSort(array, scratch, 0, array.length - 1)).join();
            } finally {
                pool.shutdown();
            }
        }
    }

    private static void printResult(boolean javaSort, boolean seqMerge, int n, int p, long time) {
        String mode;
        if (javaSort)
            mode = "JAVA_PARALLEL_SORT";
        else if (seqMerge) {
            mode = "ParMergeSort(seqMerge)";
        } else
            mode = "ParMergeSort(parMerge)";
        System.out.printf("%s\t%d\t%d\t%d%n", mode, n, p, time);
    }

    private static class ParMergeSort extends RecursiveAction {
        private final int[] a, b;
        private final int p, r;

        public ParMergeSort(int[] a, int[] b, int p, int r) {
            this.a = a;
            this.b = b;
            this.p = p;
            this.r = r;
        }

        @Override
        protected void compute() {
            if (r - p + 1 <= SEQ_TRESHOLD) {
                Arrays.sort(a, p, r + 1); // Hier Array.sort();
            } else {
                final int q = (p + r) / 2;
                var leftTask = new ParMergeSort(a, b, p, q);
                var rightTask = new ParMergeSort(a, b, q + 1, r);
                leftTask.fork();
                rightTask.fork();
                rightTask.join();
                leftTask.join();

                if (SEQ_MERGE)
                    seqMerge(a, b, p, q, r);
                else
                    parMerge(a, b, p, q, r);

            }
        }

        protected void parMerge(int[] a, int[] b, int p, int q, int r) {
            // final int[] b = new int[a.length];
            new ParMergeAux(a, p, q, q + 1, r, b, p).invoke(); // Soll für p3 0 oder p rein? Im Buch steht p
            new ParCopy(b, a, p, r).invoke();
        }

        protected static void seqMerge(int[] a, int[] b, int p, int q, int r) {
            int i = p, j = q + 1, k = p;
            while (i <= q && j <= r)
                b[k++] = (a[i] <= a[j]) ? a[i++] : a[j++];
            if (i <= q) {
                int len = q - i + 1;
                System.arraycopy(a, i, b, k, len);
                k += len;
            }
            if (j <= r) {
                int len = r - j + 1;
                System.arraycopy(a, j, b, k, len);
            }
            System.arraycopy(b, p, a, p, r - p + 1);

        }
    }

    public static class ParMergeAux extends RecursiveAction {

        private final int[] a;
        private final int p1;
        private final int r1;
        private final int p2;
        private final int r2;
        private final int[] b;
        private final int p3;

        public ParMergeAux(int[] a, int p1, int r1, int p2, int r2, int[] b, int p3) {
            this.a = a;
            this.p1 = p1;
            this.r1 = r1;
            this.p2 = p2;
            this.r2 = r2;
            this.b = b;
            this.p3 = p3;
        }

        @Override
        protected void compute() {
            if (p1 > r1)
                copyRange(a, p2, r2, b, p3);
            else if (p2 > r2)
                copyRange(a, p1, r1, b, p3);
            else {
                int total = (r1 - p1 + 1) + (r2 - p2 + 1);
                if (total <= MERGE_TRESHOLD)
                    smallMergeToB(p1, r1, p2, r2, p3);
                else {

                    int len1 = r1 - p1 + 1;
                    int len2 = r2 - p2 + 1;
                    if (len1 < len2) {
                        new ParMergeAux(a, p2, r2, p1, r1, b, p3).compute();
                        return;
                    }

                    final int q1 = (p1 + r1) / 2;
                    final int x = a[q1];
                    final int q2 = findSplitPoint(a, p2, r2, x);
                    final int q3 = p3 + (q1 - p1) + (q2 - p2);
                    b[q3] = x;
                    var leftTask = new ParMergeAux(a, p1, q1 - 1, p2, q2 - 1, b, p3).fork();
                    var rightTask = new ParMergeAux(a, q1 + 1, r1, q2, r2, b, q3 + 1).fork();
                    rightTask.join();
                    leftTask.join();
                }
            }
        }

        private void copyRange(int[] src, int start, int end, int[] dest, int destPos) {
            // for (int i = start; i <= end; ++i)
            // dest[destPos++] = src[i];
            int len = end - start + 1;
            System.arraycopy(src, start, dest, destPos, len);
        }

        public void smallMergeToB(int p1, int r1, int p2, int r2, int p3) {
            int i = p1, j = p2, k = p3;
            while (i <= r1 && j <= r2)
                b[k++] = (a[i] <= a[j]) ? a[i++] : a[j++];
            if (i <= r1) {
                int len = r1 - i + 1;
                System.arraycopy(a, i, b, k, len);
                k += len;
            }
            if (j <= r2) {
                int len = r2 - j + 1;
                System.arraycopy(a, j, b, k, len);
            }
            /*
             * int total = (r1 - p1 + 1) + (r2 - p2 + 1);
             * int[] temp = new int[total];
             * int i = p1, j = p2, k = 0;
             * while (i <= r1 && j <= r2)
             * temp[k++] = (a[i] <= a[j]) ? a[i++] : a[j++];
             * while (i <= r1)
             * temp[k++] = a[i++];
             * while (j <= r2)
             * temp[k++] = a[j++];
             * 
             * for (int x = 0; x < total; ++x)
             * b[p3 + x] = temp[x];
             */
        }

        protected int findSplitPoint(int[] a, int p, int r, int x) {
            int low = p;
            int high = r + 1;
            while (low < high) {
                int mid = (low + high) / 2;
                if (x <= a[mid]) {
                    high = mid;
                } else {
                    low = mid + 1;
                }
            }
            return low;
        }
    }

    private static class ParCopy extends RecursiveAction {
        private final int[] b;
        private final int[] a;
        private final int p;
        private final int r;

        public ParCopy(int[] b, int[] a, int p, int r) {
            this.b = b;
            this.a = a;
            this.p = p;
            this.r = r;
        }

        @Override
        protected void compute() {
            int length = r - p + 1;
            if (length <= COPY_TRESHOLD) {
                // for (int i = p; i <= r; ++i)
                // a[i] = b[i];
                System.arraycopy(b, p, a, p, length);
            } else {
                int q = (p + r) / 2;
                var leftTask = new ParCopy(b, a, p, q);
                var rightTask = new ParCopy(b, a, q + 1, r);
                leftTask.fork();
                rightTask.compute();
                leftTask.join();
            }
        }
    }
}