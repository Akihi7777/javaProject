import java.util.LinkedList;
import java.util.List;

class Solution {
    List<Integer>[] graph;
    boolean flag=true;
    int[] color;
    public boolean possibleBipartition(int n, int[][] dislikes) {
        color=new int[n];
        for(int i=0;i<n;i++){
            color[i]=0;
        }
        graph=new LinkedList[n];
        for(int i=0;i<n;i++){
            graph[i]=new LinkedList<>();
        }
        for(int[] dislike:dislikes){
            int v=dislike[0];
            int u=dislike[1];
            graph[v-1].add(u);
            //graph[u-1].add(v);
        }
        for(int i=1;i<=n;i++){
            traverse(graph,i,1);
        }
        return flag;
    }
    void traverse(List<Integer>[] graph,int node,int paint){
        if(color[node-1]>0){
            if(color[node-1]!=paint){
                flag=false;
            }
            return;
        }
        color[node-1]=paint;
        for(int v:graph[node-1]){
            if(paint==1){
                traverse(graph,v,2);
            }
            else{
                traverse(graph,v,1);
            }
        }
    }
}
class Main {
    public static void main(String[] args) {
        Solution solution = new Solution();
        int n=3;
        int [][]dislikes=new int[2][];
        dislikes[0]=new int[]{1,2};
        dislikes[1]=new int[]{1,3};
        boolean b = solution.possibleBipartition(n, dislikes);
        System.out.println(b);
    }
}

