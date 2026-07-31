java
public class MergeSort { 
    void merge(int arr[], int low, int mid, int high) { 
        int sizeA = mid - low; 
        int sizeB = high - mid; 
        int A[] = new int[sizeA]; 
        int B[] = new int[sizeB]; 
        for (int i = 0; i < sizeA; i++) 
            A[i] = arr[low + i]; 
        for (int j = 0; j < sizeB; j++) 
            B[j] = arr[mid + j]; 
        int i = 0, j = 0; 
        int k = low; 
        
        while (i < sizeA && j < sizeB) { 
            if (A[i] <= B[j]) { 
                arr[k++] = A[i++]; 
            } else { 
                arr[k++] = B[j++]; 
            } 
        } 
        while (i < sizeA) {
            arr[k++] = A[i++];
        } 
        while (j < sizeB) { 
            arr[k++] = B[j++];
        } 
    } 
    void sort(int arr[], int low, int high) { 
        if (high - low >= 2) {  
            int mid = low + (high - low) / 2; 
            sort(arr, low, mid); 
            sort(arr, mid, high); 
            merge(arr, low, mid, high); 
        } 
    } 
    static void print(int arr[]) { 
        int n = arr.length; 
        for (int i = 0; i < n; ++i) {
            System.out.print(arr[i] + " ");
        }
        System.out.println(); 
    } 
    public static void main(String args[]) { 
        int arr[] = { 15, 2, 6, 7, 55, 0, 28, 41, 12, 10, 59 }; 
        MergeSort test = new MergeSort(); 
        test.sort(arr, 0, arr.length); 
        print(arr); 
    } 
}
To convert this into a 3-way merge version, `sort3` must follow these steps:
* split the range into 3 slices instead of 2. The first slice runs from `low` to `mid1 = low + (high - low)/3` excluded, the second from `mid1` to `mid2 = low + (high - low)*2/3` excluded and the third from `mid2` to `high` excluded.
* sort each of the 3 subslices recursively
* call `merge3(arr, low, mid1, mid2, high)`
  * make copies of the 3 subslices
  * write a loop for 3 index values running the 3 slices until one of them is exhausted
  * write 3 loops for the 2 remaining slices (A and B) or (B and C) or (A and C),
  * write 3 loops to copy the remaining elements from the remaining slice, A, B or C
**EDIT:** the `merge` function in your `TriMergeSort` class is missing the 3 loops that merge 2 slices once one of the 3 initial slices is exhausted. This explains why the array does not get properly sorted. After the 3-way merge loop, you should have:
        while (i < sizeA && j < sizeB) {
            ...
        }
        while (i < sizeA && x < sizeC) {
            ...
        }
        while (j < sizeB && x < sizeC) {
            ...
        }
To avoid all these repeated loops, you could combine tests on the index values into a single loop body:
c
public class TriMergeSort {
    void merge(int arr[], int low, int mid1, int mid2, int high) { 
        int sizeA = mid1 - low; 
        int sizeB = mid2 - mid1;
        int sizeC = high - mid2;
        int A[] = new int[sizeA]; 
        int B[] = new int[sizeB]; 
        int C[] = new int[sizeC];
        for (int i = 0; i < sizeA; i++) 
            A[i] = arr[low + i]; 
        for (int j = 0; j < sizeB; j++) 
            B[j] = arr[mid1 + j]; 
        for (int k = 0; k < sizeC; k++) 
            C[k] = arr[mid2 + k];
        int i = 0, j = 0, k = 0;
        
        while (low < high) {
            if (i < sizeA && (j >= sizeB || A[i] <= B[j])) {
                if (k >= sizeC || A[i] <= C[k]) {
                    arr[low++] = A[i++];
                } else {
                    arr[low++] = C[k++];
                }
            } else {
                if (j < sizeB && (k >= sizeC || B[j] <= C[k])) {
                    arr[low++] = B[j++];
                } else {
                    arr[low++] = C[k++];
                }
            }
        } 
    } 
    void sort(int arr[], int low, int high) { 
        if (high - low >= 2) {  
            int mid1 = low + (high - low) / 3; 
            int mid2 = low + (high - low) * 2 / 3;
            sort(arr, low, mid1); 
            sort(arr, mid1, mid2); 
            sort(arr, mid2, high);
            merge(arr, low, mid1, mid2, high); 
        } 
    } 
    static void print(int arr[]) { 
        int n = arr.length; 
        for (int i = 0; i < n; ++i) {
            System.out.print(arr[i] + " ");
        }
        System.out.println(); 
    } 
    public static void main(String args[]) { 
        int arr[] = { 15, 2, 6, 7, 55, 0, 28, 41, 12 }; 
        TriMergeSort test = new TriMergeSort(); 
        test.sort(arr, 0, arr.length); 
        print(arr); 
    }
}
The `while` loop above can be further simplified but somewhat less readable as:
    while (low < high) {
        if (i < sizeA && (j >= sizeB || A[i] <= B[j])) {
            arr[low++] = (k >= sizeC || A[i] <= C[k]) ? A[i++] : C[k++];
        } else {
            arr[low++] = (j < sizeB && (k >= sizeC || B[j] <= C[k])) ? B[j++] : C[k++];
        }
    } 
And even one step further:
    while (low < high) {
        arr[low++] = (i < sizeA && (j >= sizeB || A[i] <= B[j])) ?
            ((k >= sizeC || A[i] <= C[k]) ? A[i++] : C[k++]) :
            (j < sizeB && (k >= sizeC || B[j] <= C[k])) ? B[j++] : C[k++];
    } 