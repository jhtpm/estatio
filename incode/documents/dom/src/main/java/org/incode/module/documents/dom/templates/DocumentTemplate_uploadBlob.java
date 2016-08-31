/*
 *  Copyright 2016 Dan Haywood
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
package org.incode.module.documents.dom.templates;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.value.Blob;

import org.incode.module.documents.dom.docs.DocumentSort;

// TODO: if there are any documents for this template already, we should probably disable and then create a new template object (with new applicable date range)
@Mixin
public class DocumentTemplate_uploadBlob {

    //region > constructor
    private final DocumentTemplate documentTemplate;

    public DocumentTemplate_uploadBlob(final DocumentTemplate documentTemplate) {
        this.documentTemplate = documentTemplate;
    }
    //endregion


    @Action(semantics = SemanticsOf.IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_ACTION)
    public DocumentTemplate $$(
            @ParameterLayout(named = "File")
            final Blob blob
    ) {
        documentTemplate.setMimeType(blob.getMimeType().toString());
        documentTemplate.setBlobBytes(blob.getBytes());
        return documentTemplate;
    }

    public boolean hide$$() {
        return documentTemplate.getSort() != DocumentSort.CLOB;
    }

}