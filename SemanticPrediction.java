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
    HashMap<String, HashMap<String, ProbTuple>> aoTable;
    
    double getProb(String actionName, String objectName) {
        HashMap<String, ProbTuple> map = this.aoTable.get(actionName);
        ProbTuple probs = map.get(objectName);
        return probs.weight();
    }

    void printProbs() {
        Iterator it =  aoTable.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey());
            HashMap<String, ProbTuple> map = aoTable.get(pair.getKey());
            Iterator objIt = map.entrySet().iterator();
            while(objIt.hasNext()) {
                Map.Entry objPair = (Map.Entry)objIt.next();
                double prob = Math.round(map.get((String)objPair.getKey()).weight() * 1000) / 1000.0;
                System.out.println("   " + objPair.getKey() + ": " + Double.toString(prob));
            }
        }
    }

    ArrayList<ObjectLevel> getTopObjects(int numGetting, String actionName) {
        PriorityQueue<ObjectLevel> maxes = new 
            PriorityQueue<ObjectLevel>(new SortByProbAO());
        Iterator it = aoTable.get(actionName).entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ProbTuple val = aoTable.get(actionName).get(pair.getKey());
            ObjectLevel objlev = new ObjectLevel(
                pair.getKey().toString(),
                val
            );

            
            if (maxes.size() < numGetting) {
                maxes.add(objlev);
            } else if (val.weight() > maxes.peek().probability.weight()) {
                maxes.poll();
                maxes.add(objlev);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return new ArrayList(Arrays.asList(maxes.toArray(new ObjectLevel[numGetting])));
    }

    ActionObjectTable(HashMap<String, HashMap<String, ProbTuple>> aoTable) {
        this.aoTable = aoTable;
    }

    // some obj has been chopped
    // bias all unchopped states down
    void chopped(String object) {
        Iterator it = aoTable.entrySet().iterator();
        // Go through each action and see if it is connected to the object
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            HashMap<String, ProbTuple> map = aoTable.get(pair.getKey());
            // bias unchopped object down
            if (map.containsKey(object)) {
                ProbTuple oldTuple = map.get(object);
                ProbTuple newTuple = new ProbTuple(oldTuple.staticProb, 0);
                map.put(object, newTuple);
                aoTable.put((String)pair.getKey(), map);
            }
        }
    }

    void potCooking() {
        Iterator it = aoTable.entrySet().iterator();
        // Go through each action and see if it is connected to the object
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            HashMap<String, ProbTuple> map = aoTable.get(pair.getKey());
            // bias unchopped object down
            if (map.containsKey("pot")) {
                ProbTuple oldTuple = map.get("pot");
                ProbTuple newTuple = new ProbTuple(oldTuple.staticProb, 0);
                map.put("pot", newTuple);
                aoTable.put((String)pair.getKey(), map);
            }
        }
    }
}

class ExtraItemTable {
    //<aoPair, map of ExtraItem strings to probabilities>
    HashMap<String, HashMap<String, ProbTuple>> eiTable;

    double getProb (String object, String extraItem) {
        HashMap<String, ProbTuple> map = this.eiTable.get(object);
        ProbTuple probs = map.get(extraItem);
        return probs.weight();
    }

    ArrayList<ExtraItemLevel> getTopExtraItems(int numGetting, String object) {
        PriorityQueue<ExtraItemLevel> maxes = new 
            PriorityQueue<ExtraItemLevel>(new SortByProbEI());
        Iterator it = eiTable.get(object).entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ProbTuple val = eiTable.get(object).get(pair.getKey());
            ExtraItemLevel eil = new ExtraItemLevel(
                (String)pair.getKey(),
                val
            );
            if (maxes.size() < numGetting) {
                maxes.add(eil);
            } else if (val.weight() > maxes.peek().probability.weight()) {
                maxes.poll();
                maxes.add(eil);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return new ArrayList(Arrays.asList(maxes.toArray(new ExtraItemLevel[numGetting])));
    }

    ExtraItemTable(HashMap<String, HashMap<String, ProbTuple>> eiTable) {
        this.eiTable = eiTable;
    }
}

class SortByProbEI implements Comparator<ExtraItemLevel> { 
    public int compare(ExtraItemLevel a, ExtraItemLevel b) { 
        return (int)((b.probability.weight() - a.probability.weight()) * 100);
    } 
}

class SortByProbAO implements Comparator<ObjectLevel> { 
    public int compare(ObjectLevel a, ObjectLevel b) { 
        return (int)((b.probability.weight() - a.probability.weight()) * 100);
    }
}

// Only to be used in the Prediction Tree
class ExtraItemLevel {
    ProbTuple probability;
    String extraItem;

    public double comparator() {
        return this.probability.weight();
    }

    ExtraItemLevel(String extraItem, ProbTuple prob) {
        this.extraItem = extraItem;
        this.probability = prob;
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
        this.extraItems = eiTable.getTopExtraItems(numGetting, objectName);
    }

    ObjectLevel(String object, ProbTuple prob) {
        this.objectName = object;
        this.probability = prob;
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
        this.objects = aoTable.getTopObjects(numGetting, actionName);
        for(int i = 0; i < this.objects.size(); i++) {
            objects.get(i).generatePredictions(eiTable, numGetting);
        }
    }
    
    ActionLevel(String action) {
        this.actionName = action;
    }
}

class FullSemantic {
    ActionLevel action;
    ObjectLevel object;
    ExtraItemLevel extraItem;

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

		HashMap<String, HashMap<String, ProbTuple>> aoTable = new HashMap<>();
		HashMap<String, ProbTuple> objectProbMap = new HashMap<>();
		HashMap<String, HashMap<String, ProbTuple>> eiTable = new HashMap<>();
		HashMap<String, ProbTuple> extraItemProbMap = new HashMap<>();

		String st; 
		String currentAction = null;
		String currentObject = null;

        // Kinda sloppy input processing algorithm
		while (sc.hasNextLine()) {
            st = sc.nextLine().replaceAll("\\s+","");
            // System.out.println(st);
            if( st.charAt(0) == '-') {
                st = sc.nextLine().replaceAll("\\s+","");
            } else if (st.charAt(0) == '!') {
                // System.out.println("found end");
                aoTable.put(currentAction, objectProbMap);
                objectProbMap = new HashMap<String, ProbTuple>();
                eiTable.put(currentObject, extraItemProbMap);
                extraItemProbMap = new HashMap<String, ProbTuple>();
                break;
            }

			char lastChar = st.charAt(st.length() - 1);
			if(lastChar == ':') {
                // is an action
                String action = st.split(":")[0];
                // System.out.println("action: " + action);
				if (currentAction != null) {
					aoTable.put(currentAction, objectProbMap);
					objectProbMap = new HashMap<String, ProbTuple>();
                }
				currentAction = action;
			} else if  (lastChar == ';') {
				// is an object
                String object = st.split(",")[0];
                // System.out.println("    object: " + object);
                if (currentObject != null) {
                    eiTable.put(currentObject, extraItemProbMap);
                    extraItemProbMap = new HashMap<String, ProbTuple>();
                }
                currentObject = object;
                
                Double prob = 0.0;
                try {
                    prob = Double.valueOf(st.substring(0,st.length() - 1).split(",")[1]);
                 }
                 catch (Exception e) {
                    System.out.println("Exception occurred");
                    System.out.println(st);
                    return;
                 }
                
                ProbTuple probability = new ProbTuple(prob);
				objectProbMap.put(object, probability);
			} else {
				// is an extra item
                String extraItem = st.split(",")[0];
                // System.out.println("        ei: " + extraItem);
                Double prob = Double.valueOf(st.split(",")[1]);
                ProbTuple probability = new ProbTuple(prob);
				extraItemProbMap.put(extraItem, probability);
			}
        } 
        
        ActionObjectTable actionObjTable = new ActionObjectTable(aoTable);
        ExtraItemTable extraItemTable = new ExtraItemTable(eiTable);
        PredictionTree pt = new PredictionTree();
        pt.aoTable = actionObjTable;
        pt.eiTable = extraItemTable;

        // pt.aoTable.chopped("pepper(raw)");
        // pt.aoTable.potCooking();

        // test(pt);

        // actionObjTable.printProbs();
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
                System.out.println("Not a valid action!!");
                System.out.println("------------------");
            }
        }
        scanner.close();
        FullSemantic fs = pt.getFullSemantic();
        System.out.print(fs.action.actionName + " ");
        System.out.print(fs.object.objectName + " ");
        System.out.println(fs.extraItem.extraItem);
    }
}
