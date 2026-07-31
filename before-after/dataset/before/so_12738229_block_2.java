    import java.util.*;
    
    public class Test {
    
        public static Map<String, Integer> sortByValue(Map<String, Integer> map) {
            List list = new LinkedList(map.entrySet());
            Collections.sort(list, new Comparator() {
    
                @Override
                public int compare(Object o1, Object o2) {
                    return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
                }
            });
    
            Map result = new LinkedHashMap();
            for (Iterator it = list.iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                result.put(entry.getKey(), entry.getValue());
            }
            return result;
        }
    
        public static void main(String[] args) {
    
            HashMap<String, Integer> map = new HashMap<String, Integer>();
    
            map.put("item1", 1);
            map.put("item2", 2);
            map.put("item3", 1);
            map.put("item4", 7);
    
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                System.out.println("Item is:" + entry.getKey() + " with value:"
                        + entry.getValue());
            }
            
            System.out.println("*******");
    
            Map<String,Integer> sortedMap = Test.sortByValue(map);
    
            for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
                System.out.println("Item is:" + entry.getKey() + " with value:"
                        + entry.getValue());
            }
    
        }
    }