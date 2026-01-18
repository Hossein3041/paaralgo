class ODD_EVEN_DATA{
    private int[] fromLeft, fromRight;

    public synchronized  void setFromLeft(int[] val){
        this.fromLeft = val.clone();
    }

    public synchronized  int[] getFromLeft(){
        return fromLeft.clone();
    }

    public synchronized void setFromRight(int[] val){
        this.fromRight = val.clone();
    }

    public synchronized int[] getFromRight(){
        return fromRight.clone();
    }

    public synchronized static int[] mergeAndSplit(int[] arr1, int[] arr2, boolean min){
        int[] arr = new int[arr1.length + arr2.length], min_Array = new int[arr1.length], max_Array = new int[arr2.length];
        int i = 0; int i1 = 0; int i2 = 0;

        while(i1 < arr1.length && i2 < arr2.length)
            arr[i++] = arr1[i1] < arr2[i2] ? arr1[i1++] : arr2[i2++];
        while(i1 < arr1.length) arr[i++] = arr1[i1++];
        while(i2 < arr2.length) arr[i++] = arr2[i2++];

        for(int j = 0; j < arr1.length; ++j) min_Array[j] = arr[j];
        for(int j = 0; j < arr2.length; ++j) max_Array[j] = arr[arr1.length + j];

        return min ? min_Array : max_Array;
    }
}