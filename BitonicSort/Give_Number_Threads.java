public class Give_Number_Threads {
    public static void main(String... args) {
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("Verfügbare Prozessoren: " + threads);
    }
}