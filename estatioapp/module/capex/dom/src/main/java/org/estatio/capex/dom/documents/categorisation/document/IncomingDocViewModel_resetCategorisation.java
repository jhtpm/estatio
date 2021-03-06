package org.estatio.capex.dom.documents.categorisation.document;

import javax.annotation.Nullable;

import org.apache.isis.applib.annotation.Mixin;

import org.incode.module.document.dom.impl.docs.Document;

@Mixin(method = "act")
public class IncomingDocViewModel_resetCategorisation extends DocOrIncomingDoc_resetAbstract {

    protected final IncomingDocViewModel hasDocument;

    public IncomingDocViewModel_resetCategorisation(final IncomingDocViewModel viewModel) {
        this.hasDocument = viewModel;
    }

    @Override
    public Document getDomainObject() {
        return hasDocument.getDocument();
    }

    @Override
    public Document act(@Nullable final String comment) {
        return super.act(comment);
    }

    @Override
    public boolean hideAct() {
        return super.hideAct();
    }

    @Override
    public String disableAct() {
        return super.disableAct();
    }
}
