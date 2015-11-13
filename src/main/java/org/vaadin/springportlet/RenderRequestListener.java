package org.vaadin.springportlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.vaadin.server.VaadinPortletSession.PortletListener;
import com.vaadin.ui.UI;

public abstract class RenderRequestListener implements PortletListener {

    @Override
    public void handleResourceRequest(ResourceRequest request,
            ResourceResponse response, UI uI) {
        // do nothing
    }

    @Override
    public void handleEventRequest(EventRequest request, EventResponse response,
            UI uI) {
        // do nothing
    }

    @Override
    public void handleActionRequest(ActionRequest request,
            ActionResponse response, UI uI) {
        // do nothing
    }

    public abstract void handleRenderRequest(RenderRequest request,
            RenderResponse response, UI uI);
}