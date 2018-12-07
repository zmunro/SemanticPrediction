import java.util.*;
import java.lang.*;
import java.io.*;

class ProbTuple{
    double staticProb;
    double dynamicProb;

    Double weight() {
        if(dynamicProb == 0) {
            return staticProb;
        }
        return (this.staticProb + this.dynamicProb) / 2.0;
    }

    ProbTuple(double staticProb, double dynamicProb) {
        this.staticProb = staticProb;
        this.dynamicProb = dynamicProb;
    }
}

class ActionObjectTable {
    
    //<actionName, map of aoPairs to probabilities>
    HashMap<String, HashMap<String, ProbTuple>> aoTable;
    double getProb (String actionName, String objectName) {
        HashMap<String, ProbTuple> map = this.aoTable.get(actionName);
        ProbTuple probs = map.get(objectName);
        return probs.weight();
    }

    ObjectLevel[] getTopObjects(int numGetting, String actionName) {
        PriorityQueue<ObjectLevel> maxes = new 
            PriorityQueue<ObjectLevel>(new SortByProbAO());
        Iterator it = aoTable.get(actionName).entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ObjectLevel objlev = new ObjectLevel(
                pair.getKey().toString(),
                pair.getValue()
            );
            
            if (maxes.size() < numGetting) {
                maxes.add(objlev);
            } else if (pair.getValue() > maxes.get(0).probability) {
                maxes.poll();
                maxes.add(objlev);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return maxes.toArray(new ObjectLevel[numGetting]);
    }
}

class ExtraItemTable {
    //<aoPair, map of ExtraItem strings to probabilities>
    HashMap<String, HashMap> eiTable;

    double getProb (String object, String extraItem) {
        HashMap map = this.eiTable.get(aoPair);
        ProbTuple probs = map.get(extraItem);
        return probs.weight();
    }

    ExtraItemLevel[] getTopExtraItems(int numGetting, String object) {
        PriorityQueue<ExtraItemLevel> maxes = new 
            PriorityQueue<ExtraItemLevel>(new SortByProbEI());
        Iterator it = eiTable.get(object).entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ExtraItemLevel eil = new ExtraItemLevel(
                pair.getKey(),
                pair.getValue()
            );
            if (maxes.size() < numGetting) {
                maxes.add(eil);
            } else if (pair.getValue() > maxes.get(0).probability) {
                maxes.poll();
                maxes.add(eil);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return maxes.toArray(new ExtraItemLevel[numGetting]);
    }
}

class SortByProbEI implements Comparator<ExtraItemLevel> { 
    public int compare(ExtraItemLevel a, ExtraItemLevel b) { 
        return a.probability.weight() - b.probability.weight();
    } 
}

class SortByProbAO implements Comparator<ObjectLevel> { 
    public int compare(ObjectLevel a, ObjectLevel b) { 
        return a.probability.weight() - b.probability.weight();
    }
}

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

class ObjectLevel {
    ExtraItemLevel[] extraItems; // always sorted
    String objectName;

    ProbTuple probability;

    ExtraItemLevel topExtraItem() {
        return this.extraItems[0];
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

class ActionLevel {

    ObjectLevel[] objects; // always sorted
    String actionName;
    
    ObjectLevel topObject() {
        return objects[0];
    }

    void generatePredictions( 
        ActionObjectTable aoTable, ExtraItemTable eiTable, int numGetting) {  
        this.objects = aoTable.getTopObjects(numGetting, actionName);
        for(int i = 0; i < ObjectLevel.size(); i++) {
            objects[i].generatePredictions(eiTable, numGetting);
        }
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
    	File file = new File("C:\\Users\\user\\Documents\\HRI\\prediction\\input.txt"); 
  
		BufferedReader br = new BufferedReader(new FileReader(file)); 

		HashMap<String, HashMap> aoTable = new HashMap<>();
		HashMap<String, ProbTuple> objectProbMap = new HashMap<>();
		HashMap<String, HashMap> eiTable = new HashMap<>();
		HashMap<String, ProbTuple> extraItemProbMap = new HashMap<>();

		String st; 
		String currentAction = null;
		String currentObject = null;

		while ((st = br.readLine()) != null) {
			char lastChar = st.charAt(st.length() - 1);
			if(lastChar == ':') {
                // is an action
                String action = st.split(":")[0];
				if (currentAction != null) {
					aoTable.put(currentAction, objectProbMap);
					objectProbMap = new HashMap<>();
                }
				currentAction = action;
			} else if  (lastChar == ';') {
				// is an object
                String object = st.split(",")[0];
                if (currentObject != null) {
                    eiTable.put(currentObject, extraItemProbMap);
                    extraItemProbMap = new HashMap<>();
                }
				currentObject = object;
                Double prob = Double.valueOf(st.substring(st.length() - 1).split(",")[1]);
                ProbTuple probability = new ProbTuple(prob, 0);
				objectProbMap.put(object, probability);
			} else {
				// is an extra item
				String extraItem = st.split(",")[0];
                Double prob = Double.valueOf(st.split(",")[1]);
                ProbTuple probability = new ProbTuple(prob, 0);
				extraItemProbMap.put(extraItem, probability);
			}

		} 
        
	}
}
