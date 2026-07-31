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
Just like the comments under the question, I'm also a bit puzzled with this line:
Deque<Helper> myStack = new ArrayDeque<Helper>();
-------------
## References
- For additional details, you can see the [Discussion Board](https://leetcode.com/problems/largest-rectangle-in-histogram/discuss/). There are plenty of accepted solutions with a variety of [languages](https://support.leetcode.com/hc/en-us/articles/360011833974-What-are-the-environments-for-the-programming-languages-) and explanations, efficient algorithms, as well as asymptotic [time](https://en.wikipedia.org/wiki/Time_complexity)/[space](https://en.wikipedia.org/wiki/Space_complexity) complexity analysis<sup>[1](https://en.wikipedia.org/wiki/Big_O_notation), [2](https://en.wikipedia.org/wiki/Analysis_of_algorithms)</sup> in there.
### Since you are preparing for [interviews](https://leetcode.com/problemset/top-interview-questions/?listId=wpwgkgt):
- We would want to write [bug-free](https://www.codegrip.tech/productivity/tips-to-write-bug-free-code-during-interview/) and [clean](https://www.quora.com/How-can-I-write-cleaner-code-during-onsite-interviews) codes based on standards and conventions (e.g., [tag:c]<sup>[1](https://www.doc.ic.ac.uk/lab/cplus/cstyle.html), [2](https://users.ece.cmu.edu/~eno/coding/CCodingStandard.html)</sup>, [tag:c++]<sup>[1](https://google.github.io/styleguide/cppguide.html), [2](https://isocpp.org/wiki/faq/coding-standards)</sup>, [tag:java]<sup>[1](https://google.github.io/styleguide/javaguide.html), [2](https://www.oracle.com/technetwork/java/codeconventions-150003.pdf)</sup>, [tag:c#]<sup>[1](https://docs.microsoft.com/en-us/dotnet/csharp/programming-guide/inside-a-program/coding-conventions), [2](https://docs.microsoft.com/en-us/dotnet/standard/design-guidelines/?redirectedfrom=MSDN)</sup>, [tag:python]<sup>[1](https://www.python.org/dev/peps/pep-0008/)</sup>, [tag:javascript]<sup>[1](https://google.github.io/styleguide/jsguide.html)</sup>, [tag:go]<sup>[1](https://github.com/golang/go/wiki/CodeReviewComments)</sup>, [tag:rust]<sup>[1](https://doc.rust-lang.org/1.0.0/style/)</sup>).
- The time to implement a solution during the interview is pretty limited. Make sure to not run out of time by complicating the design of your codes.
## Good luck with your interviews! ?_?