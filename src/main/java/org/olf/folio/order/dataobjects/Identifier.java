package org.olf.folio.order.dataobjects;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.olf.folio.order.Constants;

import java.util.ArrayList;
import java.util.List;

public class Identifier
{

    private final String identifierTypeId;
    private final String identifierValue;

    private static final Logger logger = Logger.getLogger(Identifier.class);

    public Identifier (String identifierTypeId, String value) {
        this.identifierTypeId =  identifierTypeId;
        this.identifierValue = value;
    }

    /**
     * Creates a JSON array of identifiers (types and values) on the schema used in Instance records
     * @param fromMarcRecord The MARC record to extract identifiers from
     * @param includeQualifiers Indication whether to include additional subfields to certain identifier values
     * @param identifierTypeIds One or more identifier types to look up values for in the MARC record
     * @return A JSON array of identifiers
     */
    public static JSONArray createInstanceIdentifiersJson( Record fromMarcRecord, boolean includeQualifiers, String ...identifierTypeIds) {
        JSONArray identifiersJson = new JSONArray();
        List<Identifier> identifiers = createIdentifiers( fromMarcRecord, includeQualifiers, identifierTypeIds );
        for (Identifier identifier : identifiers) {
            identifiersJson.put( identifier.asInstanceIdentifierJson() );
        }
        return identifiersJson;
    }

    /**
     * Creates a JSON array of identifiers (types and values) on the schema used in Order Storage, product IDs
     * @param fromMarcRecord The MARC record to extract identifiers from
     * @param includeQualifiers Indication whether to include additional subfields to certain identifier values
     * @param identifierTypeIds One or more identifier types to look up values for in the MARC record
     * @return A JSON array of identifiers
     */
    public static JSONArray createProductIdentifiersJson( Record fromMarcRecord, boolean includeQualifiers, String ...identifierTypeIds) {
        JSONArray identifiersJson = new JSONArray();
        List<Identifier> identifiers = createIdentifiers( fromMarcRecord, includeQualifiers, identifierTypeIds );
        for (Identifier identifier : identifiers) {
            identifiersJson.put( identifier.asProductIdJson() );
        }
        return identifiersJson;
    }


    /**
     * Based on one ore more identifier types, creates list of Identifier objects with identifier type id and value
     * @param fromMarcRecord  The MARC record to find identifiers in
     * @param includeQualifiers indicates if the identifier value should be extended with additional sub fields
     * @param identifierTypeIds  The identifier type IDs (UUIDs) to create Identifier objects for
     * @return List of Identifier objects
     */
    public static List<Identifier> createIdentifiers ( Record fromMarcRecord, boolean includeQualifiers, String ...identifierTypeIds) {
        List<Identifier> identifiers = new ArrayList<>();
        for (String identifierType : identifierTypeIds) {
            List<DataField> identifierFields = getDataFieldsForIdentifierType(identifierType, fromMarcRecord);
            for (DataField identifierField : identifierFields) {
                String value = getIdentifierValue( identifierType, identifierField, includeQualifiers );
                if (value != null && ! value.isEmpty())
                {
                    identifiers.add(new Identifier(identifierType, value));
                }
            }
        }
        return identifiers;
    }

    /**
     * Finds identifier fields in the provided MARC records by tag, subfield tag(s) and possibly indicator2 -- all
     * dependent on the given identifier type
     * @param requestedIdentifierType The Identifier type to find data fields for
     * @param inMarcRecord The MARC record to look for identifiers in
     * @return List of identifier fields matching the applicable criteria for the given identifier type
     */
    public static List<DataField> getDataFieldsForIdentifierType( String requestedIdentifierType, Record inMarcRecord) {
        List<DataField> identifierFields = new ArrayList<>();
        switch(requestedIdentifierType)
        {
            case Constants.ISBN:
                return findIdentifierFieldsByTagAndSubFields( inMarcRecord, "020", 'a' );
            case Constants.INVALID_ISBN:
                return findIdentifierFieldsByTagAndSubFields( inMarcRecord, "020", 'z' );
            case Constants.ISSN:
                return findIdentifierFieldsByTagAndSubFields( inMarcRecord, "022", 'a' );
            case Constants.LINKING_ISSN:
                return findIdentifierFieldsByTagAndSubFields( inMarcRecord, "022", 'l' );
            case Constants.INVALID_ISSN:
                return findIdentifierFieldsByTagAndSubFields( inMarcRecord, "022", "zym".toCharArray() );
            case Constants.OTHER_STANDARD_IDENTIFIER:
                identifierFields.addAll( findIdentifierFieldsByTagAndSubFields( inMarcRecord, "024", 'a' ) );
                identifierFields.addAll( findIdentifierFieldsByTagAndSubFields( inMarcRecord, "025", 'a' ) );
                return identifierFields;
            case Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER:
                return findIdentifierFieldsByTagAndSubFields( inMarcRecord, "028", 'a' );
            case Constants.SYSTEM_CONTROL_NUMBER:
                List<DataField> fields035 = findIdentifierFieldsByTagAndSubFields( inMarcRecord, "035", 'a' );
                for ( DataField dataField : fields035 )
                {
                    if ( dataField.getIndicator2() == '9' )
                    {
                        identifierFields.add( dataField );
                    }
                }
                return identifierFields;
            default:
                logger.error("Requested identifier type not recognized when attempting to look up identifier field: " + requestedIdentifierType);
                return identifierFields;
        }
    }

    /**
     * Finds data fields in the MARC record by their tag and filtered by the presence of given subfields
     * @param inMarcRecord The MARC record to look up fields by tag in
     * @param tagToFind  The tag (field) to look for
     * @param withAnyOfTheseSubFields One or more subfield codes, of which at least one must be present for the field to be included
     * @return A list of Identifier fields matching the given tag and subfield code criteria
     */
    static List<DataField> findIdentifierFieldsByTagAndSubFields( Record inMarcRecord, String tagToFind, char ...withAnyOfTheseSubFields) {
        List<DataField> fieldsFound = new ArrayList<>();
        List<VariableField> fieldsFoundForTag = inMarcRecord.getVariableFields(tagToFind);
        for (VariableField field : fieldsFoundForTag) {
            DataField dataField = (DataField) field;
            for (char subTag : withAnyOfTheseSubFields) {
                if (dataField.getSubfield( subTag ) != null) {
                    fieldsFound.add(dataField);
                    break;
                }
            }
        }
        return fieldsFound;
    }

    /**
     * Looks up the value of the identifier fields, optionally adding additional subfields to the value for given Identifier types
     * Will strip colons and spaces from ISBN value when not including qualifiers.
     * @param identifierType The type of identifier to find the identifier value for
     * @param identifierField  The identifier field to look for the value in
     * @param includeQualifiers Indication whether to add additional subfield(s) to the identifier value
     * @return The resulting identifier value
     */
    public static String getIdentifierValue ( String identifierType, DataField identifierField, boolean includeQualifiers) {
        String identifierValue;
        switch ( identifierType ) {
            case Constants.ISBN:                           // 020 using $a, extend with c,q
            case Constants.ISSN:                           // 022 using $a, extend with c,q
                identifierValue = identifierField.getSubfieldsAsString( "a" );
                if ( includeQualifiers ) {
                    if ( identifierField.getSubfield( 'c' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "c" );
                    if ( identifierField.getSubfield( 'q' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "q" );
                } else {
                    identifierValue = identifierValue.replaceAll("[: ]", "");
                }
                break;
            case Constants.INVALID_ISBN:                   // 020 using $z, extend with c,q
                identifierValue = identifierField.getSubfieldsAsString( "z" );
                if ( includeQualifiers ) {
                    if ( identifierField.getSubfield( 'c' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "c" );
                    if ( identifierField.getSubfield( 'q' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "q" );
                }
                break;
            case Constants.LINKING_ISSN:                   // 022 using $l, extend with c,q
                identifierValue = identifierField.getSubfieldsAsString( "l" );
                if ( includeQualifiers ) {
                    if ( identifierField.getSubfield( 'c' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "c" );
                    if ( identifierField.getSubfield( 'q' ) != null ) identifierValue += " " + identifierField.getSubfieldsAsString( "q" );
                }
                break;
            case Constants.INVALID_ISSN:                   // 022 using $z,y,m
                identifierValue = "";
                if ( identifierField.getSubfield('z') != null) identifierValue += identifierField.getSubfieldsAsString("z");
                if ( identifierField.getSubfield('y') != null) identifierValue += " " +  identifierField.getSubfieldsAsString("y");
                if ( identifierField.getSubfield('m') != null) identifierValue += " " + identifierField.getSubfieldsAsString("m");
                break;
            case Constants.OTHER_STANDARD_IDENTIFIER:       // 024, 025 using $a
            case Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER: // 028 using $a
            case Constants.SYSTEM_CONTROL_NUMBER:           // 035 using $a
                identifierValue = identifierField.getSubfieldsAsString("a");
                break;
            default:
                logger.error("Requested identifier type not recognized when attempting to retrieve identifier value: " + identifierType);
                identifierValue = null;
                break;
        }
        return identifierValue;
    }

    /**
     * Create identifier object on the schema in Inventory Storage
     * @return The identifier as JSON
     */
    public JSONObject asInstanceIdentifierJson () {
        JSONObject instanceIdentifier = new JSONObject();
        instanceIdentifier.put("identifierTypeId", identifierTypeId);
        instanceIdentifier.put("value", identifierValue);
        return instanceIdentifier;
    }

    /**
     * Create identifier object on the schema in Orders Storage
     * @return The identifier as JSON
     */
    public JSONObject asProductIdJson () {
        JSONObject productId = new JSONObject();
        productId.put("productIdType", identifierTypeId);
        productId.put("productId", identifierValue);
        return productId;
    }

    @Override
    public boolean equals (Object o) {
        if (o instanceof Identifier) {
            return ( (Identifier) o ).identifierTypeId.equals(this.identifierTypeId) && ((Identifier) o).identifierValue.equals( this.identifierValue );
        } else {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return (identifierTypeId + identifierValue).hashCode();
    }

}
