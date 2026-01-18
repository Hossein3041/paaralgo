public class SeqMergeSort {
    public static void main(String[] args) {
        int[] a = { 5, 2, 9, 1, 3 };
        printArray(a, "array unsortiert");
        MergeSorter sorter = new MergeSorter();
        sorter.seqMergeSort(a, 0, a.length - 1);
        printArray(a, "array sortiert");
    }

    public static void printArray(int[] a, String message) {
        System.out.println(message);
        for (int i = 0; i < a.length; ++i)
            System.out.print(a[i] + "\t");
        System.out.println();
    }

    static class MergeSorter {
        public void seqMergeSort(int[] a, int p, int r) {
            if (p >= r)
                return;
            int q = (p + r) / 2;

            seqMergeSort(a, p, q);
            seqMergeSort(a, q + 1, r);

            seqMerge(a, p, q, r);
        }
    }

    public static void seqMerge(int[] a, int p, int q, int r) {
        int leftSize = q - p + 1;
        int rightSize = r - q;
        int[] L = new int[leftSize];
        int[] R = new int[rightSize];
        for (int i = 0; i < leftSize; ++i)
            L[i] = a[p + i];
        for (int i = 0; i < rightSize; ++i)
            R[i] = a[q + 1 + i];
        int iL = 0, iR = 0, iK = p;
        while (iL < leftSize && iR < rightSize) {
            a[iK++] = (L[iL] < R[iR]) ? L[iL++] : R[iR++];
        }
        while (iL < leftSize) {
            a[iK++] = L[iL++];
        }
        while (iR < rightSize) {
            a[iK++] = R[iR++];
        }
    }
}