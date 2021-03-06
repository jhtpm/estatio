package org.estatio.capex.dom.documents.categorisation.document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.Digits;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.wicketstuff.pdfjs.Scale;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.MinLength;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.PromptStyle;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.services.bookmark.BookmarkService2;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.hint.HintStore;
import org.apache.isis.applib.services.registry.ServiceRegistry2;
import org.apache.isis.applib.value.Blob;

import org.isisaddons.wicket.pdfjs.cpt.applib.PdfJsViewer;

import org.incode.module.base.dom.types.ReferenceType;
import org.incode.module.country.dom.impl.Country;
import org.incode.module.document.dom.impl.docs.Document;
import org.incode.module.document.dom.impl.paperclips.PaperclipRepository;
import org.incode.module.document.dom.impl.types.DocumentType;

import org.estatio.capex.dom.documents.categorisation.invoice.IncomingDocAsInvoiceViewModel;
import org.estatio.capex.dom.documents.categorisation.order.IncomingDocAsOrderViewModel;
import org.estatio.capex.dom.project.Project;
import org.estatio.capex.dom.project.ProjectRepository;
import org.estatio.capex.dom.task.Task;
import org.estatio.capex.dom.util.PeriodUtil;
import org.estatio.dom.asset.FixedAsset;
import org.estatio.dom.asset.role.FixedAssetRole;
import org.estatio.dom.asset.role.FixedAssetRoleRepository;
import org.estatio.dom.asset.role.FixedAssetRoleTypeEnum;
import org.estatio.dom.asset.OwnershipType;
import org.estatio.dom.asset.Property;
import org.estatio.dom.asset.PropertyRepository;
import org.estatio.dom.asset.ownership.FixedAssetOwnership;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budget.BudgetRepository;
import org.estatio.dom.budgeting.budgetitem.BudgetItem;
import org.estatio.dom.budgeting.budgetitem.BudgetItemRepository;
import org.estatio.dom.charge.Applicability;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.charge.ChargeRepository;
import org.estatio.dom.financial.bankaccount.BankAccountRepository;
import org.estatio.dom.financial.utils.IBANValidator;
import org.estatio.dom.invoice.DocumentTypeData;
import org.estatio.dom.party.Organisation;
import org.estatio.dom.party.OrganisationRepository;
import org.estatio.dom.party.Party;
import org.estatio.dom.tax.Tax;

import lombok.Getter;
import lombok.Setter;

@XmlTransient // abstract class so do not map
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class IncomingDocViewModel<T> implements HintStore.HintIdProvider {


    public IncomingDocViewModel() {}

    public IncomingDocViewModel(final Document document) {
        this.document = document;
    }

    public String title() {
        return getDocument().getName();
    }


    @Getter @Setter
    protected Document document;

    public DocumentType getType() {
        return getDocument().getType();
    }

    public DateTime getCreatedAt() {
        return getDocument().getCreatedAt();
    }



    @org.apache.isis.applib.annotation.Property(hidden = Where.ALL_TABLES)
    @PdfJsViewer(initialPageNum = 1, initialScale = Scale._2_00, initialHeight = 900)
    public Blob getBlob() {
        return getDocument() != null ? getDocument().getBlob() : null;
    }

    /**
     * For view models with inline property edits, allows the focus to stay on the same field after OK.
     */
    @Override
    public String hintId() {
        return  bookmarkService2.bookmarkFor(getDocument()).toString();
    }





    @Programmatic
    public abstract void setDomainObject(T t);

    /**
     * Optional, the (categorisation) task (ie, just completed) that was used to create the view model.
     *
     * <p>
     *     Used in order to advance to next task after this has been classified.
     * </p>
     */
    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(hidden = Where.EVERYWHERE)
    private Task originatingTask;

    @Programmatic
    protected abstract String minimalRequiredDataToComplete();

    @Programmatic
    public void inferFixedAssetFromPaperclips() {
        final FixedAsset fixedAsset = paperclipRepository.paperclipAttaches(document, FixedAsset.class);
        modifyFixedAsset(fixedAsset);
    }

    @Programmatic
    public void modifyFixedAsset(final FixedAsset fixedAsset) {
        setFixedAsset(fixedAsset);
        deriveBuyer();
    }

    private void deriveBuyer(){
        Party ownerCandidate = null;
        if (hasFixedAsset()){
            for (FixedAssetOwnership fos: getFixedAsset().getOwners()){
                if (fos.getOwnershipType()== OwnershipType.FULL){
                    ownerCandidate = fos.getOwner();
                    continue;
                }
            }
            // temporary extra search until fixed asset ownership is fully in use
            if (ownerCandidate == null && getFixedAsset().ownerCandidates().size() > 0) {
                ownerCandidate = getFixedAsset().ownerCandidates().get(0).getParty();
            }
        }
        setBuyer(ownerCandidate);
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private Party buyer;

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private Party seller;
    // use of modify so can be overridden on IncomingInvoiceViewmodel
    public void modifySeller(final Party seller){
        setSeller(seller);
    }

    @Action(
            semantics = SemanticsOf.IDEMPOTENT
    )
    public IncomingDocViewModel createSeller(
            final @Parameter(regexPattern = ReferenceType.Meta.REGEX, regexPatternReplacement = ReferenceType.Meta.REGEX_DESCRIPTION, optionality = Optionality.OPTIONAL) String reference,
            final boolean useNumeratorForReference,
            final String name,
            final Country country,
            @Parameter(optionality = Optionality.OPTIONAL)
            final String ibanNumber) {
        Organisation organisation = organisationRepository
                .newOrganisation(reference, useNumeratorForReference, name, country);
        setSeller(organisation);
        if (ibanNumber != null) {
            bankAccountRepository.newBankAccount(organisation, ibanNumber, null);
        }
        return this;
    }

    public String validateCreateSeller(
            final String reference,
            final boolean useNumeratorForReference,
            final String name,
            final Country country,
            final String ibanNumber){
        if (ibanNumber != null && !IBANValidator.valid(ibanNumber)){
            return String.format("%s is not a valid iban number", ibanNumber);
        }
        return null;
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private String description;

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.DISABLED)
    private Charge charge;

    @MemberOrder(name = "charge", sequence = "1")
    @ActionLayout(promptStyle = PromptStyle.INLINE_AS_IF_EDIT)
    public T editCharge(final Charge charge) {
        setCharge(charge);
        return (T) this;
    }
    public List<Charge> autoComplete0EditCharge(@MinLength(3) String search){
        return chargeRepository.findByApplicabilityAndMatchOnReferenceOrName(search, Applicability.INCOMING);
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private FixedAsset<?> fixedAsset;

    public List<Property> choicesFixedAsset(){
        List<Property> result = new ArrayList<>();
        if (hasBuyer()) {
            for (FixedAssetRole role : fixedAssetRoleRepository.findByPartyAndType(getBuyer(), FixedAssetRoleTypeEnum.PROPERTY_OWNER)){
                if (role.getAsset().getClass().isAssignableFrom(Property.class)) {
                    result.add((Property) role.getAsset());
                }
            }
        }
        return result.size()>0 ? result : propertyRepository.allProperties();
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private Project project;

    public List<Project> choicesProject(){
        return getFixedAsset()==null ? projectRepository.listAll() : projectRepository.findByFixedAsset(getFixedAsset());
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private BudgetItem budgetItem;
    public void modifyBudgetItem(final BudgetItem budgetItem) {
        setBudgetItem(budgetItem);
        derivePeriodFromBudgetItem();
        deriveChargeFromBudgetItem();
    }

    public List<BudgetItem> choicesBudgetItem(){
        List<BudgetItem> result = new ArrayList<>();
        if (hasFixedAsset()){
            for (Budget budget : budgetRepository.findByProperty((Property) getFixedAsset())){
                if (hasCharge()){
                    result.add(budgetItemRepository.findByBudgetAndCharge(budget, getCharge()));
                } else {
                    result.addAll(budget.getItems());
                }
            }
        } else {
            if (hasCharge()){
                result = budgetItemRepository.allBudgetItems().stream().filter(x->x.getCharge().equals(getCharge())).collect(Collectors.toList());
            } else {
                result = budgetItemRepository.allBudgetItems();
            }
        }
        return result;
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public IncomingDocViewModel createBudgetItem(final Budget budget, final Charge charge){
        budgetItemRepository.findOrCreateBudgetItem(budget, charge);
        deriveChargeFromBudgetItem();
        derivePeriodFromBudgetItem();
        return this;
    }

    public List<Budget> choices0CreateBudgetItem(final Budget budget, final Charge charge){
        if (hasFixedAsset()){
            return budgetRepository.findByProperty((Property) getFixedAsset());
        }
        return budgetRepository.allBudgets();
    }

    public List<Charge> choices1CreateBudgetItem(final Budget budget, final Charge charge){
        if (hasCharge()){
            return Arrays.asList(getCharge());
        }
        return chargeRepository.allIncoming();
    }

    @Action(semantics = SemanticsOf.IDEMPOTENT)
    public IncomingDocViewModel createNextBudget(final Budget previousBudget){
        previousBudget.createNextBudget();
        return this;
    }

    public List<Budget> choices0CreateNextBudget(final Budget budget){
        if (hasFixedAsset()){
            return budgetRepository.findByProperty((Property) getFixedAsset());
        }
        return budgetRepository.allBudgets();
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private String period;

    public String validatePeriod(final String period) {
        if (period==null) return null; // period is optional
        return !PeriodUtil.isValidPeriod(period)
                ? "Not a valid period; use four digits of the year with optional prefix F for a financial year (for example: F2017)"
                : null;
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private BigDecimal netAmount;
    @Digits(integer=13, fraction = 2)
    public BigDecimal getNetAmount() {
        return netAmount;
    }
    public void modifyNetAmount(BigDecimal netAmount) {
        setNetAmount(netAmount);
        calculateVat();
        determineAmounts();
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private BigDecimal vatAmount;
    @Digits(integer=13, fraction = 2)
    public BigDecimal getVatAmount() {
        return vatAmount;
    }
    public void modifyVatAmount(BigDecimal vatAmount) {
        setVatAmount(vatAmount);
        calculateVat();
        determineAmounts();
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    private Tax tax;
    public void modifyTax(Tax tax) {
        setTax(tax);
        calculateVat();
        determineAmounts();
    }

    @Setter @Getter
    @org.apache.isis.applib.annotation.Property(editing = Editing.ENABLED)
    @PropertyLayout(promptStyle = PromptStyle.INLINE)
    private BigDecimal grossAmount;
    @Digits(integer=13, fraction = 2)
    public BigDecimal getGrossAmount() {
        return grossAmount;
    }
    public void modifyGrossAmount(BigDecimal grossAmount) {
        setGrossAmount(grossAmount);
        if (!hasNetAmount()){
            final BigDecimal valueToUse = getVatAmount()!=null ? grossAmount.subtract(getVatAmount()):getGrossAmount();
            setNetAmount(valueToUse);
        }
        calculateVat();
        determineAmounts();
    }

    // ////////////////////////////////////

    public IncomingDocViewModel changeDimensions(
            @Parameter(optionality = Optionality.OPTIONAL)
            final Charge charge,
            @Parameter(optionality = Optionality.OPTIONAL)
            final Property property,
            @Parameter(optionality = Optionality.OPTIONAL)
            final Project project,
            @Parameter(optionality = Optionality.OPTIONAL)
            final BudgetItem budgetItem,
            @Parameter(optionality = Optionality.OPTIONAL)
            final String period
    ){
        setCharge(charge);
        setFixedAsset(property);
        setProject(project);
        setBudgetItem(budgetItem);
        setPeriod(period);
        derivePeriodFromBudgetItem();
        deriveChargeFromBudgetItem();
        return this;
    }

    public Charge default0ChangeDimensions(){
        return getCharge();
    }

    public Property default1ChangeDimensions(){
        return (Property) getFixedAsset();
    }

    public Project default2ChangeDimensions(){
        return getProject();
    }

    public BudgetItem default3ChangeDimensions(){
        return getBudgetItem();
    }

    public String default4ChangeDimensions(){
        return getPeriod();
    }

//    public List<Charge> autoComplete0ChangeDimensions(@MinLength(3) final String search) {
//        return autoComplete0EditCharge(search);
//    }

    public List<Property> choices1ChangeDimensions() {
        return choicesFixedAsset();
    }

    public List<Project> choices2ChangeDimensions() {
        return choicesProject();
    }

    public List<BudgetItem> choices3ChangeDimensions() {
        return choicesBudgetItem();
    }

    public String validateChangeDimensions(
            final Charge charge,
            final Property property,
            final Project project,
            final BudgetItem budgetItem,
            final String period
    ) {
        return validatePeriod(period);
    }

    // ////////////////////////////////////

    public IncomingDocViewModel changeItemDetails(
            final String description,
            final BigDecimal netAmount,
            @Nullable
            final BigDecimal vatAmount,
            @Nullable
            final Tax tax,
            @Nullable
            final BigDecimal grossAmount
    ){
        setDescription(description);
        setNetAmount(netAmount);
        setVatAmount(vatAmount);
        setTax(tax);
        setGrossAmount(grossAmount);
        calculateVat();
        determineAmounts();
        return this;
    }

    public String default0ChangeItemDetails(){
        return getDescription();
    }

    public BigDecimal default1ChangeItemDetails(){
        return getNetAmount();
    }

    public BigDecimal default2ChangeItemDetails(){
        return getVatAmount();
    }

    public Tax default3ChangeItemDetails(){
        return getTax();
    }

    public BigDecimal default4ChangeItemDetails(){
        return getGrossAmount();
    }

    // ////////////////////////////////////

    void determineAmounts(){
        if (hasVatAmount() && hasNetAmount() && !hasGrossAmount()){
            setGrossAmount(getNetAmount().add(getVatAmount()));
            return;
        }

        if (hasVatAmount() && hasGrossAmount() && !hasNetAmount()){
            setNetAmount(getGrossAmount().subtract(getVatAmount()));
            return;
        }

        if (hasNetAmount() && hasGrossAmount() && !hasVatAmount()){
            setVatAmount(getGrossAmount().subtract(getNetAmount()));
            return;
        }

    }

    void calculateVat(){
        if (hasTax() && hasNetAmount() && !hasVatAmount() && !hasGrossAmount()){
            BigDecimal grossAmount = getTax().grossFromNet(getNetAmount(), clockService.now());
            setVatAmount(grossAmount.subtract(getNetAmount()));
            return;
        }
    }

    void derivePeriodFromBudgetItem(){
        if (hasBudgetItem() && !hasPeriod()){
            setPeriod(String.valueOf(getBudgetItem().getBudget().getBudgetYear()));
        }
    }

    void deriveChargeFromBudgetItem(){
        if (hasBudgetItem() && !hasCharge()){
            setCharge(getBudgetItem().getCharge());
        }
    }

    protected boolean hasTax(){
        return getTax() != null;
    }
    protected boolean hasNetAmount(){
        return getNetAmount() != null;
    }
    protected boolean hasVatAmount(){
        return getVatAmount() != null;
    }
    protected boolean hasGrossAmount(){
        return getGrossAmount() != null;
    }
    protected boolean hasCharge(){
        return getCharge() != null;
    }
    protected boolean hasBuyer(){
        return getBuyer() != null;
    }
    protected boolean hasSeller(){
        return getSeller() != null;
    }
    protected boolean hasFixedAsset(){
        return getFixedAsset() != null;
    }
    protected boolean hasProject(){
        return getProject() != null;
    }
    protected boolean hasBudgetItem(){
        return getBudgetItem() != null;
    }
    protected boolean hasPeriod(){
        return getPeriod() != null;
    }
    protected boolean hasDescription(){
        return getDescription() != null;
    }


    /////////////////////////////////

    @DomainService(nature = NatureOfService.DOMAIN)
    public static class Factory {

        @Programmatic
        public Object createFor(final Document document) {
            if(DocumentTypeData.INCOMING_ORDER.isDocTypeFor(document)) {
                final IncomingDocAsOrderViewModel viewModel = new IncomingDocAsOrderViewModel(document);
                serviceRegistry2.injectServicesInto(viewModel);
                viewModel.inferFixedAssetFromPaperclips();
                return viewModel;
            }
            if(DocumentTypeData.INCOMING_INVOICE.isDocTypeFor(document)) {
                final IncomingDocAsInvoiceViewModel viewModel = new IncomingDocAsInvoiceViewModel(document);
                serviceRegistry2.injectServicesInto(viewModel);
                viewModel.inferFixedAssetFromPaperclips();
                return viewModel;
            }
            return document;
        }

        @Programmatic
        public List<Object> map(final List<Document> documents) {
            return Lists.newArrayList(
                    FluentIterable.from(documents)
                            .transform(this::createFor)
                            .filter(Objects::nonNull)
                            .toList());
        }

        @Inject
        protected PaperclipRepository paperclipRepository;

        @Inject
        ServiceRegistry2 serviceRegistry2;

    }


    /////////////////////////////////


    @XmlTransient
    @Inject
    protected BookmarkService2 bookmarkService2;

    @Inject
    @XmlTransient
    OrganisationRepository organisationRepository;

    @Inject
    @XmlTransient
    ChargeRepository chargeRepository;

    @Inject
    @XmlTransient
    ClockService clockService;

    @Inject
    @XmlTransient
    PropertyRepository propertyRepository;

    @Inject
    @XmlTransient
    FixedAssetRoleRepository fixedAssetRoleRepository;

    @Inject
    @XmlTransient
    ProjectRepository projectRepository;

    @Inject
    @XmlTransient
    BudgetRepository budgetRepository;

    @Inject
    @XmlTransient
    BudgetItemRepository budgetItemRepository;

    @Inject
    @XmlTransient
    protected BankAccountRepository bankAccountRepository;

    @Inject
    @XmlTransient
    protected PaperclipRepository paperclipRepository;



}
