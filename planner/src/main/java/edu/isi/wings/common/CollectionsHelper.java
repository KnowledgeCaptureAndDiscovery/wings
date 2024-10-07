package edu.isi.wings.common;

import edu.isi.wings.catalog.data.classes.VariableBindings;
import edu.isi.wings.catalog.data.classes.VariableBindingsList;
import edu.isi.wings.catalog.data.classes.VariableBindingsListSet;
import java.util.ArrayList;

public class CollectionsHelper {

  /**
   * Helper function to combine variable object bindings from multiple lists
   *
   * @return the combined list of variable object bindings
   */
  public static ArrayList<
    VariableBindingsList
  > combineVariableDataObjectMappings(VariableBindingsListSet listset) {
    ArrayList<VariableBindingsList> combinedList = new ArrayList<
      VariableBindingsList
    >();

    for (ArrayList<VariableBindingsList> list : listset) {
      if (combinedList.isEmpty()) combinedList = list; else {
        ArrayList<VariableBindingsList> templist = new ArrayList<
          VariableBindingsList
        >();
        for (VariableBindingsList map1 : combinedList) {
          for (VariableBindingsList map2 : list) {
            VariableBindingsList cmap = new VariableBindingsList();
            for (VariableBindings vb1 : map1) cmap.add(vb1);
            for (VariableBindings vb2 : map2) cmap.add(vb2);
            templist.add(cmap);
          }
        }
        combinedList = templist;
      }
    }
    return combinedList;
  }
}
