package domini;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Class for working with arraylists that act like sets, in particular, the union and intersection of those sets
 * @author Jaume C.
 */
public class UnionIntersection {
    
    /**
     * Returns the union of two ArrayLists
     * @param list1 first ArrayList of Llibres
     * @param list2 second ArrayList of Llibres
     * @return ArrayList with all the elements of both lists without repetitions
     */
    public static ArrayList<Llibre> getUnion(ArrayList<Llibre> list1, ArrayList<Llibre> list2) {
        HashSet<Llibre> hs = new HashSet<>();
 
        for (int i = 0; i < list1.size(); i++) {
            hs.add(list1.get(i));
        }
        for (int i = 0; i < list2.size(); i++) {
            hs.add(list2.get(i));
        }

        return new ArrayList<Llibre>(hs);
    }
 
    /**
     * Returns the intersection of two ArrayLists
     * @param list1 first ArrayList of Llibres
     * @param list2 second ArrayList of Llibres
     * @return ArrayList with all the common elements in list1 and list2
     */
    public static ArrayList<Llibre> getIntersection(ArrayList<Llibre> list1, ArrayList<Llibre> list2)
    {
        HashSet<Llibre> hs = new HashSet<>();
 
        for (int i = 0; i < list1.size(); i++) {
            hs.add(list1.get(i));
        }

        ArrayList<Llibre> result = new ArrayList<Llibre>();

        for (int i = 0; i < list2.size(); i++) {
            if (hs.contains(list2.get(i))) {
                result.add(list2.get(i));
            }
        }

        return result;
    }

}
