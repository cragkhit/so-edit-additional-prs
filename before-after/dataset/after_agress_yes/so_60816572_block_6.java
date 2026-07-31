java
public class test {
	public static void main(String[] args) {
		int[] v=first();
		second(v);
	}
	public static int[][] first () {
		int N= (int)(Math.random()*5)+1;
		int M= (int)(Math.random()*5)+1;
		int v [][] = new int [N][M];
    	for(int i=0; i < v.length; i++) {
	    	for(int j=0; j < v[0].length; j++) {
		    	v [i][j]= (int)(Math.random()*5);
            }
        }
        return v;
	}
	public static void second (int[][] v) {
		for(int i=0; i < v.length; i++) { 
			for(int j=0; j < v[0].length; j++)
				System.out.print(v [i][j] + " ");
			System.out.println("");
		}
	}
}
[**Notes**]
This does not only work with integer arrays but also any other array.
In fact, this works with any type.
By convention, class names should be written PascalCase and variable (and method) names should be written camelCase.