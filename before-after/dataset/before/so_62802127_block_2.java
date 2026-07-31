class Solution {
    public static int largestRectangleArea(int[] height) {
        if (height == null || height.length == 0) {
            return 0;
        }
        int[] leftReduce = new int[height.length];
        int[] rightReduce = new int[height.length];
        rightReduce[height.length - 1] = height.length;
        leftReduce[0] = -1;
        for (int i = 1; i < height.length; i++) {
            int p = i - 1;
            while (p >= 0 && height[p] >= height[i]) {
                p = leftReduce[p];
            }
            leftReduce[i] = p;
        }
        for (int i = height.length - 2; i >= 0; i--) {
            int p = i + 1;
            while (p < height.length && height[p] >= height[i]) {
                p = rightReduce[p];
            }
            rightReduce[i] = p;
        }
        int maxArea = 0;
        for (int i = 0; i < height.length; i++) {
            maxArea = Math.max(maxArea, height[i] * (rightReduce[i] - leftReduce[i] - 1));
        }
        return maxArea;
    }
}
-------------
## References
- For additional details, you can see the [Discussion Board](https://leetcode.com/problems/largest-rectangle-in-histogram/discuss/). There are plenty of accepted solutions with a variety of [languages](https://support.leetcode.com/hc/en-us/articles/360011833974-What-are-the-environments-for-the-programming-languages-) and explanations, efficient algorithms, as well as asymptotic [time](https://en.wikipedia.org/wiki/Time_complexity)/[space](https://en.wikipedia.org/wiki/Space_complexity) complexity analysis<sup>[1](https://en.wikipedia.org/wiki/Big_O_notation), [2](https://en.wikipedia.org/wiki/Analysis_of_algorithms)</sup> in there.