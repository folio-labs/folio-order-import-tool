package org.olf.folio.order.dataobjects;

import org.olf.folio.order.MarcRecordMapping;

public class Fund extends JsonDataObject {
  public static final String P_DISTRIBUTION_TYPE = "distributionType";
  public static final String V_PERCENTAGE = "percentage";
  public static final String P_VALUE = "value";
  public static final String P_FUND_ID = "fundId";
  public static final String P_CODE = "code";
  public static final String P_EXPENSE_CLASS_ID = "expenseClassId";

  public static Fund fromMarcRecord(MarcRecordMapping mappedMarc) throws Exception{
    return new Fund()
            .putDistributionType(V_PERCENTAGE)
            .putValue(100)
            .putFundId(mappedMarc.fundUUID())
            .putCode(mappedMarc.fundCode())
            .putExpenseClassIdIfPresent(mappedMarc.getExpenseClassUUID());
  }

  public Fund putDistributionType (String distributionType) {
    return (Fund) putString(P_DISTRIBUTION_TYPE, distributionType);
  }
  public Fund putValue (int value) {
    return (Fund) putInteger(P_VALUE, value);
  }
  public Fund putFundId (String fundId) {
    return (Fund) putString(P_FUND_ID, fundId);
  }
  public Fund putCode (String code) {
    return (Fund) putString(P_CODE, code);
  }
  public Fund putExpenseClassId (String expenseClassId) {
    return (Fund) putString(P_EXPENSE_CLASS_ID, expenseClassId);
  }
  public Fund putExpenseClassIdIfPresent (String expenseClassId) {
    return present(expenseClassId) ? putExpenseClassId(expenseClassId) : this;
  }

}
