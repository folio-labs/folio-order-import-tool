package org.olf.folio.order;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Constants {
	public static final String LOOKUP_TABLE = "lookupTable";
	public static final Pattern UUID_PATTERN = Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");
	public static final Map<String,String> MATERIAL_TYPES_MAP = Stream.of(new String[][] {
			{"book","1a54b431-2e4f-452d-9cae-9cee66c9a892"},
			{"dvd","5ee11d91-f7e8-481d-b079-65d708582ccc"},
			{"electronic resource","615b8413-82d5-4203-aa6e-e37984cb5ac3"},
			{"microform","fd6c6515-d470-4561-9c32-3e3290d4ca98"},
			{"sound recording","dd0bf600-dbd9-44ab-9ff2-e2a61a6539f1"},
			{"text","d9acad2f-2aac-4b48-9097-e6ab85906b25"},
			{"unspecified","71fbd940-1027-40a6-8a48-49b44d795e46"},
			{"video recording","30b3e36a-d3b2-415e-98c2-47fbdf878862"}
		}).collect(Collectors.toMap(data -> data[0], data -> data[1]));
	//TODO
	//MORE STRINGS AS CONSTANTS

}