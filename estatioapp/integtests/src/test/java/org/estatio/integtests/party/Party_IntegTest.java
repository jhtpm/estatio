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
package org.estatio.integtests.party;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.estatio.dom.party.Party;
import org.estatio.dom.party.PartyRepository;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.party.OrganisationForAcmeNl;
import org.estatio.fixture.party.OrganisationForTopModelGb;
import org.estatio.fixture.party.PersonForGinoVannelliGb;
import org.estatio.fixture.party.PersonForJohnDoeNl;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class Party_IntegTest extends EstatioIntegrationTest {

    @Inject
    PartyRepository partyRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public static class Remove extends Party_IntegTest {

        @Before
        public void setupData() {
            runFixtureScript(new FixtureScript() {
                @Override
                protected void execute(ExecutionContext executionContext) {
                    executionContext.executeChild(this, new EstatioBaseLineFixture());
                    // linked together:
                    executionContext.executeChild(this, new OrganisationForTopModelGb());
                    executionContext.executeChild(this, new PersonForGinoVannelliGb());
                    // only relationship
                    executionContext.executeChild(this, new PersonForJohnDoeNl());
                    // only comm channels
                    executionContext.executeChild(this, new OrganisationForAcmeNl());
                }
            });
        }

        @Test
        public void happyCase() {
            Party party = partyRepository.findPartyByReference(PersonForJohnDoeNl.REF);
            wrap(party).remove();
            assertNull(partyRepository.findPartyByReferenceOrNull(PersonForJohnDoeNl.REF));
        }

    }

}