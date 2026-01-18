import java.util.concurrent.ThreadLocalRandom;

public class OETSSequential{
    public static void main(String[] args){
        int n = 10;
        int[] array = new int[n];

        for(int i = 0; i < array.length; ++i)
            array[i] = ThreadLocalRandom.current().nextInt(0, 99);

        System.out.println("Array unsortiert: ");
        for(int i = 0; i < array.length; ++i){
            System.out.print(array[i] + " \t");
        }
        System.out.println();

        OddEvenTS sort = new OddEvenTS(array, n);
        sort.getArray();

        System.out.println("Array sortiert: ");
        for(int i = 0; i < array.length; ++i){
            System.out.print(array[i] + " \t");
        }
        System.out.println();
    }
}