public class ODD_EVEN_DATA{
    private int fromLeft;
    private int fromRight;

    public synchronized  void setFromLeft(int val){
        this.fromLeft = val;
    }

    public synchronized  int getFromLeft(){
        return fromLeft;
    }

    public synchronized void setFromRight(int val){
        this.fromRight = val;
    }

    public synchronized int getFromRight(){
        return fromRight;
    }

    public synchronized static int min(int a, int b){
        return (a < b) ? a : b;
    }

    public synchronized static int max(int a, int b){
        return (a > b) ? a : b;
    }
}