/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.integtests.invoice;

import java.util.SortedSet;
import org.estatio.dom.asset.Properties;
import org.estatio.dom.invoice.Invoices;
import org.estatio.dom.lease.Lease;
import org.estatio.dom.lease.Leases;
import org.estatio.dom.lease.invoicing.InvoiceCalculationService;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.asset.PropertiesAndUnitsFixture;
import org.estatio.fixture.invoice.InvoiceAndInvoiceItemFixture;
import org.estatio.fixture.lease.LeasesAndLeaseUnitsAndLeaseItemsAndLeaseTermsAndTagsAndBreakOptionsFixture;
import org.estatio.fixture.party.PersonsAndOrganisationsAndCommunicationChannelsFixture;
import org.estatio.fixturescripts.CreateRetroInvoices;
import org.estatio.integtests.EstatioIntegrationTest;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.apache.isis.applib.fixturescripts.CompositeFixtureScript;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateRetroInvoicesTest_findDueDatesForLease extends EstatioIntegrationTest {

    @Before
    public void setupData() {
        scenarioExecution().install(new CompositeFixtureScript() {
            @Override
            protected void execute(ExecutionContext executionContext) {
                execute(new EstatioBaseLineFixture(), executionContext);
                execute("parties", new PersonsAndOrganisationsAndCommunicationChannelsFixture(), executionContext);
                execute("properties", new PropertiesAndUnitsFixture(), executionContext);
                execute("leases", new LeasesAndLeaseUnitsAndLeaseItemsAndLeaseTermsAndTagsAndBreakOptionsFixture(), executionContext);
                execute("invoices", new InvoiceAndInvoiceItemFixture(), executionContext);
            }
        });
    }

    private Invoices invoices;
    private Properties properties;
    private Leases leases;
    private CreateRetroInvoices creator;
    private InvoiceCalculationService invoiceCalculationService;

    Lease lease;

    @Before
    public void setup() {
        invoices = service(Invoices.class);
        properties = service(Properties.class);
        leases = service(Leases.class);
        invoiceCalculationService = service(InvoiceCalculationService.class);

        creator = new CreateRetroInvoices();
        creator.leases = leases;
        creator.invoices = invoices;
        creator.properties = properties;
        creator.invoiceCalculationService = invoiceCalculationService;

        lease = leases.findLeaseByReference("OXF-TOPMODEL-001");
    }

    @Test
    public void happyCase() {
        SortedSet<LocalDate> dueDates = creator.findDueDatesForLease(dt(2012, 1, 1), dt(2014, 1, 1), lease);
        assertThat(dueDates.size(), is(10));
    }


}