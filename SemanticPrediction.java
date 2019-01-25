import java.util.*;
import java.lang.*;
import java.awt.Desktop.Action;
import java.io.*;
import java.math.*;

// Stores the weight of a vertex between object/action or aoPair/extra item
class ProbTuple{
    double staticProb;
    double dynamicProb;

    // This can be modified to weight context higher or lower
    Double weight() {
        return (this.staticProb + 5 * this.dynamicProb) / 6.0;
    }

    ProbTuple(double staticProb) {
        this.staticProb = staticProb;
        this.dynamicProb = 0.5;
    }

    ProbTuple(double staticProb, double dynamicProb) {
        this.staticProb = staticProb;
        this.dynamicProb = dynamicProb;
    }
}

class ActionObjectTable {
    
    //<actionName, map of aoPairs to probabilities>
    HashMap<String, HashMap<String, Object>> aoTable;
    
    double getProb(String actionName, String objectName) {
        HashMap<String, Object> map = this.aoTable.get(actionName);
        ProbTuple probs = map.get(objectName).prob;
        return probs.weight();
    }

    void printProbs() {
        Iterator it =  aoTable.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey());
            HashMap<String, Object> map = aoTable.get(pair.getKey());
            Iterator objIt = map.entrySet().iterator();
            while(objIt.hasNext()) {
                Map.Entry objPair = (Map.Entry)objIt.next();
                double prob = Math.round(map.get((String)objPair.getKey()).prob.weight() * 1000) / 1000.0;
                System.out.println("   " + objPair.getKey() + ": " + Double.toString(prob));
            }
        }
    }

    ArrayList<Object> getTopObjects(int numGetting, String actionName) {
        PriorityQueue<Object> maxes = new 
            PriorityQueue<Object>(new SortByProbAO());
        Iterator it = aoTable.get(actionName).entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Object obj = aoTable.get(actionName).get(pair.getKey());
            
            if (maxes.size() < numGetting) {
                maxes.add(obj);
            } else if (obj.prob.weight() > maxes.peek().prob.weight()) {
                maxes.poll();
                maxes.add(obj);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        ArrayList<Object> finalArray = new ArrayList(Arrays.asList(maxes.toArray(new Object[numGetting])));
        return finalArray;
    }

    // biases the weight of objects corresponding to any action
    void anyActionBias(String objectName, double strength){
        Iterator<Map.Entry<String, HashMap<String,Object>>> it = aoTable.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Object> pair = (Map.Entry)it.next();
            HashMap<String, Object> map = aoTable.get(pair.getKey());
            if (map.containsKey(objectName)) {
                Object obj = map.get(objectName);
                ProbTuple newTuple;
                newTuple = new ProbTuple(obj.prob.staticProb, strength);
                obj.prob = newTuple;
                map.put(objectName, obj);
                aoTable.put((String)pair.getKey(), map);
            }
        }
    }

    // for setting bias on a specific action AND object
    void bias(String actionName, String objectName, double strength) {
        if (actionName.equals("any")) {
            anyActionBias(objectName, strength);
            return;
        }
        HashMap<String, Object> objMap = aoTable.get(actionName);
        Object temp = objMap.get(objectName);
        temp.prob.dynamicProb = strength;
        objMap.put(objectName, temp);
        aoTable.put(actionName, objMap);
    }

    ActionObjectTable(HashMap<String, HashMap<String, Object>> aoTable) {
        this.aoTable = aoTable;
    }
}

class ExtraItemTable {

    HashMap<String, HashMap<String, ExtraItem>> eiTable;

    ArrayList<ExtraItem> getTopExtraItems(int numGetting, String objectName) {
        PriorityQueue<ExtraItem> maxes = new 
            PriorityQueue<ExtraItem>(new SortByProbEI());
        Iterator<Map.Entry<String, ExtraItem>> it = eiTable.get(objectName).entrySet().iterator();
        int numGotten = 0;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ExtraItem item = eiTable.get(objectName).get(pair.getKey());
 
            if (maxes.size() < numGetting) {
                maxes.add(item);
                numGotten++;
            } else if (item.prob.weight() > maxes.peek().prob.weight()) {
                maxes.poll();
                maxes.add(item);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return new ArrayList(Arrays.asList(maxes.toArray(new ExtraItem[numGotten])));
    }

    ExtraItemTable(HashMap<String, HashMap<String, ExtraItem>> eiTable) {
        this.eiTable = eiTable;
    }
}

class SortByProbEI implements Comparator<ExtraItem> { 
    public int compare(ExtraItem a, ExtraItem b) { 
        return (int)((b.prob.weight() - a.prob.weight()) * 100);
    } 
}

class SortByProbAO implements Comparator<Object> { 
    public int compare(Object a, Object b) { 
        return (int)((b.prob.weight() - a.prob.weight()) * 100);
    }
}

// Only to be used in the Prediction Tree
class ExtraItemLevel {
    ProbTuple probability;
    String extraItem;

    public double comparator() {
        return this.probability.weight();
    }

    ExtraItemLevel(ExtraItem ei) {
        this.extraItem = ei.name;
        this.probability = ei.prob;
    }
}

// Only to be used in the Prediction Tree
class ObjectLevel {
    ArrayList<ExtraItemLevel> extraItems; // always sorted
    String objectName;

    ProbTuple probability;

    ExtraItemLevel topExtraItem() {
        return this.extraItems.get(0);
    }

    void generatePredictions(ExtraItemTable eiTable, int numGetting) {
        ArrayList<ExtraItem> items = eiTable.getTopExtraItems(numGetting, objectName);
        for(int i = 0; i < items.size(); i++) {
            ExtraItemLevel eiLevel = new ExtraItemLevel(items.get(i));
            this.extraItems.add(eiLevel);
        }
    }

    ObjectLevel(Object obj) {
        this.objectName = obj.name;
        this.probability = obj.prob;
        this.extraItems = new ArrayList<ExtraItemLevel>();
    }
}

// Only to be used in the Prediction Tree
class ActionLevel {

    ArrayList<ObjectLevel> objects; // always sorted
    String actionName;
    
    ObjectLevel topObject() {
        return objects.get(0);
    }

    void generatePredictions( 
        ActionObjectTable aoTable, ExtraItemTable eiTable, int numGetting) {  
        ArrayList<Object> newObjects = aoTable.getTopObjects(numGetting, actionName);

        for(int i = 0; i < newObjects.size(); i++) {
            ObjectLevel objLevel = new ObjectLevel(newObjects.get(i));
            objLevel.generatePredictions(eiTable, numGetting);
            this.objects.add(objLevel);
        }
    }
    
    ActionLevel(String action) {
        this.actionName = action;
        this.objects = new ArrayList<ObjectLevel>();
    }
}

class FullSemantic {
    ActionLevel action;
    ObjectLevel object;
    ExtraItemLevel extraItem;
    double likelihood;

    // When you instantiate a FullSemantic, it generates the predicted semantic
    FullSemantic(ActionLevel action) {
        this.action = action;
        this.object = action.topObject();
        this.extraItem = this.object.topExtraItem();
    }

    FullSemantic(ActionLevel action, ObjectLevel object) {
        this.action = action;
        this.object = object;
        this.extraItem = object.topExtraItem();
    }
}


class PredictionTree {
    ActionObjectTable aoTable;
    ExtraItemTable eiTable;
    ObjectLevel correctObject;

    ActionLevel action;

    void generatePredictions(int numGetting) {
        this.action.generatePredictions(
            this.aoTable, this.eiTable, numGetting);
    }

    FullSemantic getFullSemantic() {
        if (correctObject == null) {
            FullSemantic fs = new FullSemantic(this.action);
            return fs;
        } else {
            FullSemantic fs = new 
                FullSemantic(this.action, this.correctObject);
            return fs;
        }
        
    }

    PredictionTree() {
        this.correctObject = null;
    }
}

class Object {
    String name;
    ProbTuple prob;

    Object(String name, ProbTuple prob) {
        this.name = name;
        this.prob = prob;
    }
}

class ExtraItem {
    String name;
    ProbTuple prob;

    ExtraItem(String name, ProbTuple prob) {
        this.name = name;
        this.prob = prob;
    }
}

class SemanticPrediction {

    public static void main(String[] args) {

        File file = new File("C:\\Users\\User\\Documents\\HRI\\prediction\\input.txt"); 
        Scanner sc = null;
        try {
            sc = new Scanner(file); 
        }
        catch (FileNotFoundException ex)  {
            // insert code to run when exception occurs
            System.out.println(ex);
        }

		HashMap<String, HashMap<String, Object>> aoTable = new HashMap<>();
		HashMap<String, Object> objectMap = new HashMap<>();
		HashMap<String, HashMap<String, ExtraItem>> eiTable = new HashMap<>();
		HashMap<String, ExtraItem> extraItemMap = new HashMap<>();

		String st; 
		String currentAction = null;
		String currentObject = null;

        // Kinda sloppy input processing algorithm
		while (sc.hasNextLine()) {
            st = sc.nextLine().replaceAll("\\s+","");
            char lastChar = st.charAt(st.length() - 1);

            if (st.charAt(0) == '!') { // ---------CHECK FOR END OF FILE----------
                aoTable.put(currentAction, objectMap);
                objectMap = new HashMap<String, Object>();
                eiTable.put(currentObject, extraItemMap);
                extraItemMap = new HashMap<String, ExtraItem>();
                break;
            }
            
			if(lastChar == ':') { // ------------ NEW ACTION ---------
                String actionName = st.split(":")[0];

                if (currentAction != null) {
					aoTable.put(currentAction, objectMap);
					objectMap = new HashMap<String, Object>();
                }
                currentAction = actionName;
			} else if  (lastChar == ';') { // ---------NEW OBJECT---------
				
                String objectName = st.split(",")[0];

                //Store the previous ExtraItemMap
                if (currentObject != null) {
                    eiTable.put(currentObject, extraItemMap);
                    extraItemMap = new HashMap<String, ExtraItem>();
                }
                
                Double prob = 0.0;
                try {
                    prob = Double.valueOf(st.substring(0,st.length() - 1).split(",")[1]);
                }
                catch (Exception e) {
                    System.out.println("Exception occurred");
                    System.out.println(st);
                    return;
                }
                currentObject = objectName;
				objectMap.put(objectName, new Object(objectName, new ProbTuple(prob)));
			} else { // ---------- NEW EXTRA ITEM -------------
                String eiName = st.split(",")[0];
                Double prob = Double.valueOf(st.split(",")[1]);
                ExtraItem item = new ExtraItem(eiName, new ProbTuple(prob));
				extraItemMap.put(eiName, item);
			}
        } 
        
        ActionObjectTable actionObjTable = new ActionObjectTable(aoTable);
        ExtraItemTable extraItemTable = new ExtraItemTable(eiTable);
        PredictionTree pt = new PredictionTree();
        pt.aoTable = actionObjTable;
        pt.eiTable = extraItemTable;

        // actionObjTable.printProbs();
        test(pt);
    }
    
    // Test out the prediction of a full semantic given an action
    static void test(PredictionTree pt) {
        Scanner scanner = new Scanner(System.in);
        ActionLevel newAction = null;
        boolean invalidAction = true;
        String userActionName = "";
        while(invalidAction) {
            try{
                System.out.println("Enter action: ");
                userActionName = scanner.nextLine();
                newAction = new ActionLevel(userActionName);
                pt.action = newAction;
                
                pt.generatePredictions(3);
                invalidAction = false;
            } catch(Exception e) {
                invalidAction = true;
                System.out.println(e);
                System.out.println("Not a valid action!!");
                System.out.println("------------------");
            }
        }
        scanner.close();
        FullSemantic fs = pt.getFullSemantic();
        System.out.print(fs.action.actionName);
        System.out.print(fs.object.objectName + " ");
        System.out.println(fs.extraItem.extraItem + " ");
        Double prob = fs.object.probability.weight() * fs.extraItem.probability.weight();
        System.out.println("prob: " + prob);
    }
}
