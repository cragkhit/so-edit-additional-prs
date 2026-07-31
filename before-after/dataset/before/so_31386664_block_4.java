    public void topview(Node root)
     {
         if(root==null)
          return;
         traverse_left(root.left);
         System.out.print(root.data+" ");
         traverse_right(root.right);
     }
     public void traverse_left(Node x)
     {
         if(x==null)
          return;
         traverse_left(x.left);
          System.out.print(x.data+"  ");
     }
     public void traverse_right(Node x)
     {
         if(x==null)
          return;
         System.out.print(x.data+"  ");
         traverse_right(x.right);     
     } 