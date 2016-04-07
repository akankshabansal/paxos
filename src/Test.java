//import java.util.*;
//
//public class Test {
//
////    static public Set<Integer> findCommon(List<Integer> set1, List<Integer> set2){
////        Set<Integer> output = new HashSet<Integer>();
////        for (int i = 0, j = 0; i < set1.size() ; ){
////            if (set1.get(i) == set2.get(j)){
////                output.add(set1.get(i));
////                i++;
////                j=0;
////            } else if (set1.get(i) > set2.get(j)){
////                j++;
////            } else {
////                i++;
////
////            }
////            if (j == set2.size()){
////                i++;
////                j=0;
////            }
////        }
////        return output;
////    }
//
//    public class Node {
//        int data;
//        int parent_id;
//
////        public Node(int data, int parentId) {
////            this.data = data;
////            this.parent_id = parentId;
////        }
//    }
//
//    static Node root;
//
//    static public void traversal(List<Node> nodeList) {
//        Map<Integer, Integer[]> map = new HashMap<Integer, Integer[]>();
//        for (Node node : nodeList) {
//            if (map.get(node.parent_id) == null) {
//                Integer[] temp = new Integer[2];
//                temp[0] = node.data;
//                map.put(node.parent_id, temp);
//            } else {
//                Integer[] temp = map.get(node.parent_id);
//                temp[1] = node.data;
//            }
//        }
//        print(root.data, map, "", 0);
//    }
//
//    static public String print(int nodeValue, Map<Integer, Integer[]> map, String str, int level) {
//        if (map.get(nodeValue) != null) {
//            Integer[] nodes = map.get(nodeValue);
//            for (int i = 0; i < level; i++)
//                str = str + " ";
//            str = print(nodes[0], map, str + nodes[0] + "\n", level++);
//            str = print(nodes[1], map, str + nodes[1] + "\n", level++);
//        }
//
//        return str;
//    }
//
//
//    public static void main(String[] args) {
////One array is not sorted....
//        List<Node> set1 = new ArrayList<Node>();
//        Node node = new Node();
//        traversal(set1);
//    }
//}
