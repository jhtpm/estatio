package org.estatio.app.services.budget;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;

import org.joda.time.LocalDate;

import org.apache.isis.applib.ApplicationException;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.Nature;
import org.apache.isis.applib.annotation.Programmatic;

import org.estatio.dom.Importable;
import org.estatio.dom.asset.Property;
import org.estatio.dom.asset.PropertyRepository;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budget.BudgetRepository;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationType;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.dom.budgeting.budgetitem.BudgetItemRepository;
import org.estatio.dom.budgeting.keytable.FoundationValueType;
import org.estatio.dom.budgeting.keytable.KeyTable;
import org.estatio.dom.budgeting.keytable.KeyTableRepository;
import org.estatio.dom.budgeting.keytable.KeyValueMethod;
import org.estatio.dom.budgeting.partioning.PartitionItem;
import org.estatio.dom.budgeting.partioning.PartitionItemRepository;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.charge.ChargeRepository;

import lombok.Getter;
import lombok.Setter;

@DomainObject(
        nature = Nature.VIEW_MODEL,
        objectType = "org.estatio.app.services.budget.BudgetImportExport"
)
public class BudgetImportExport implements Importable {

    public String title() {
        return "Budget Import / Export";
    }

    public BudgetImportExport(){
    }

    public BudgetImportExport(
            final String propertyReference,
            final LocalDate budgetStartDate,
            final LocalDate budgetEndDate,
            final String incomingChargeReference,
            final BigDecimal budgetedValue,
            final BigDecimal auditedValue,
            final String keyTableName,
            final String foundationValueType,
            final String keyValueMethod,
            final String outgoingChargeReference,
            final BigDecimal percentage
            ){
        this.propertyReference = propertyReference;
        this.budgetStartDate = budgetStartDate;
        this.budgetEndDate = budgetEndDate;
        this.incomingChargeReference = incomingChargeReference;
        this.budgetedValue = budgetedValue;
        this.auditedValue = auditedValue;
        this.keyTableName = keyTableName;
        this.foundationValueType = foundationValueType;
        this.keyValueMethod = keyValueMethod;
        this.outgoingChargeReference = outgoingChargeReference;
        this.percentage = percentage;
    }

    @Getter @Setter
    private String propertyReference;
    @Getter @Setter
    private LocalDate budgetStartDate;
    @Getter @Setter
    private LocalDate budgetEndDate;
    @Getter @Setter
    private String incomingChargeReference;
    @Getter @Setter
    private BigDecimal budgetedValue;
    @Getter @Setter
    private BigDecimal auditedValue;
    @Getter @Setter
    private String keyTableName;
    @Getter @Setter
    private String foundationValueType;
    @Getter @Setter
    private String keyValueMethod;
    @Getter @Setter
    private String outgoingChargeReference;
    @Getter @Setter
    private BigDecimal percentage;


    @Override
    @Programmatic
    public List<Object> importData(final Object previousRow) {
        if (previousRow==null){
            removeExistingBudgetItems();
        }
        Charge incomingCharge = fetchCharge(getIncomingChargeReference());
        BudgetItem budgetItem = findOrCreateBudgetAndBudgetItem(incomingCharge);
        if (getOutgoingChargeReference()!=null && getKeyTableName()!=null && getFoundationValueType()!=null && getKeyValueMethod()!=null && percentage!=null) {
           findOrCreatePartitionItem(budgetItem);
        }
        return Lists.newArrayList(budgetItem.getBudget());
    }

    private void removeExistingBudgetItems(){
        Property property = propertyRepository.findPropertyByReference(getPropertyReference());
        Budget budget = budgetRepository.findOrCreateBudget(property, getBudgetStartDate(), getBudgetEndDate());
        budget.removeAllBudgetItems();
    }

    private BudgetItem findOrCreateBudgetAndBudgetItem(final Charge incomingCharge){
        Property property = propertyRepository.findPropertyByReference(getPropertyReference());
        if (property == null) throw  new ApplicationException(String.format("Property with reference [%s] not found.", getPropertyReference()));
        Budget budget = budgetRepository.findOrCreateBudget(property, getBudgetStartDate(), getBudgetEndDate());
        BudgetItem budgetItem = budget
                .findOrCreateBudgetItem(incomingCharge)
                .updateOrCreateBudgetItemValue(getBudgetedValue(), getBudgetStartDate(), BudgetCalculationType.BUDGETED)
                .updateOrCreateBudgetItemValue(getAuditedValue(), getBudgetEndDate(), BudgetCalculationType.ACTUAL);
        return budgetItem;
    }

    private PartitionItem findOrCreatePartitionItem(final BudgetItem budgetItem){
        Charge targetCharge = fetchCharge(getOutgoingChargeReference());
        KeyTable keyTable = findOrCreateKeyTable(budgetItem.getBudget(), getKeyTableName(), getFoundationValueType(), getKeyValueMethod());
        return budgetItem.updateOrCreatePartitionItem(targetCharge, keyTable, getPercentage());
    }

    private Charge fetchCharge(final String chargeReference) {
        final Charge charge = chargeRepository
                .findByReference(chargeReference);
        if (charge == null) {
            throw new ApplicationException(String.format("Charge with reference %s not found.", chargeReference));
        }
        return charge;
    }

    private KeyTable findOrCreateKeyTable(final Budget budget, final String keyTableName, final String foundationValueType, final String keyValueMethod){
       return keyTableRepository.findOrCreateBudgetKeyTable(budget, keyTableName, FoundationValueType.valueOf(foundationValueType), KeyValueMethod.valueOf(keyValueMethod), 6);
    }

    @Inject
    private ChargeRepository chargeRepository;

    @Inject
    private BudgetRepository budgetRepository;

    @Inject
    private BudgetItemRepository budgetItemRepository;

    @Inject
    private PartitionItemRepository partitionItemRepository;

    @Inject
    private PropertyRepository propertyRepository;

    @Inject
    private KeyTableRepository keyTableRepository;

}
