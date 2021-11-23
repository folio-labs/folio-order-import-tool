package org.olf.folio.order;

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
		}).collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
	public static final Map<String,String> CONTRIBUTOR_NAME_TYPES_MAP = Stream.of(new String[][] {
			{"Corporate name", "2e48e713-17f3-4c13-a9f8-23845bb210aa"},
			{"Personal name", "2b94c631-fca9-4892-a730-03ee529ffe2a"},
			{"Meeting name", "e8b311a6-3b21-43f2-a269-dd9310cb2d0a"}
	}).collect( Collectors.toMap(entry -> entry[0], entry -> entry[1]));
    // Identifier types
	public static final String ISBN = "8261054f-be78-422d-bd51-4ed9f33c3422";
	public static final String INVALID_ISBN = "fcca2643-406a-482a-b760-7a7f8aec640e";
	public static final String ISSN = "913300b2-03ed-469a-8179-c1092c991227";
	public static final String INVALID_ISSN = "27fd35a6-b8f6-41f2-aa0e-9c663ceb250c";
	public static final String LINKING_ISSN = "5860f255-a27f-4916-a830-262aa900a6b9";
	public static final String OTHER_STANDARD_IDENTIFIER = "2e8b3b6c-0e7d-4e48-bca2-b0b23b376af5";
	public static final String PUBLISHER_OR_DISTRIBUTOR_NUMBER = "b5d8cdc4-9441-487c-90cf-0c7ec97728eb";
	public static final String SYSTEM_CONTROL_NUMBER = "7e591197-f335-4afb-bc6d-a6d76ca3bace";

	static final String ITEM_NOTE_TYPE_ID_ELECTRONIC_BOOKPLATE = "f3ae3823-d096-4c65-8734-0c1efd2ffea8";
	static final String HOLDINGS_NOTE_TYPE_ID_ELECTRONIC_BOOKPLATE =  "88914775-f677-4759-b57b-1a33b90b24e0";
}