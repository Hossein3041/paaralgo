class OddEvenTS{
    int[] m_Array;
    int n;
    boolean sortedFlag = false;
    public OddEvenTS(int[] array, int n){
        this.m_Array = array;
        this.n = n;
        initSorting();
    }

    public void initSorting(){
        while(!sortedFlag){
            sortedFlag = true;

            // Odd-Phase
            for(int i = 1; i < n-1; i += 2){
                if(m_Array[i] > m_Array[i + 1]){
                    swap(m_Array, i, i+1);
                    sortedFlag = false;
                }
            }

            // Even-Phase
            for(int i = 0; i < n-1; i += 2){
                if(m_Array[i] > m_Array[i + 1]){
                    swap(m_Array, i, i+1);
                    sortedFlag = false;
                }
            }
        }
    }

    public void swap(int[] m_Array, int iPos1, int iPos2){
        int temp = m_Array[iPos1];
        m_Array[iPos1] = m_Array[iPos2];
        m_Array[iPos2] = temp;
    }

    public int[] getArray(){
        return m_Array;
    }
}