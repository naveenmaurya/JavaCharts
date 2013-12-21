// This function puts all the data in compositeData variable to sessionScope variable
function putDataInSessionScope(compositeDataMap:java.util.Map) {
	var chartID = @Unique();
	
	putDataInSessionScopeRecursive(compositeDataMap, chartID);
	
	/*
	// Get all the keys
	var keySet:java.util.Set = compositeDataMap.keySet();
	var itrKeys:java.util.Iterator = keySet.iterator();
	// Put all the data in the sessionScope
	while (itrKeys.hasNext()) {
		var key = itrKeys.next();
		if (key == "advancedProperties") {
			
		} else {
			sessionScope.put(key.toString() + chartID, compositeDataMap.get(key));
		}
	}
	*/
	compositeDataMap.put("chartID", chartID); // Store the chart ID in the compositeData so that it is available to current custom control
}

function putDataInSessionScopeRecursive(compositeDataMap:java.util.Map, chartID) {
	var keySet:java.util.Set = compositeDataMap.keySet();
	var itrKeys:java.util.Iterator = keySet.iterator();
	// Put all the data in the sessionScope
	while (itrKeys.hasNext()) {
		var key = itrKeys.next();
		if (key == "advancedProperties") {
			putDataInSessionScopeRecursive(compositeDataMap.get(key), chartID);
		} else {
			sessionScope.put(key.toString() + chartID, compositeDataMap.get(key));
		}
	}
}