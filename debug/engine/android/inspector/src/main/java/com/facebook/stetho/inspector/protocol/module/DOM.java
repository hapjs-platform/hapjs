/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.graphics.Color;
import android.util.Pair;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.stetho.common.Accumulator;
import com.facebook.stetho.common.ArrayListAccumulator;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.UncheckedCallable;
import com.facebook.stetho.common.Util;
import com.facebook.stetho.inspector.elements.Document;
import com.facebook.stetho.inspector.elements.DocumentView;
import com.facebook.stetho.inspector.elements.ElementInfo;
import com.facebook.stetho.inspector.elements.NodeDescriptor;
import com.facebook.stetho.inspector.elements.NodeType;
import com.facebook.stetho.inspector.helper.ChromePeerManager;
import com.facebook.stetho.inspector.helper.PeersRegisteredListener;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcException;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcResult;
import com.facebook.stetho.inspector.jsonrpc.protocol.JsonRpcError;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.hapjs.inspector.DOMAccumulator;
import org.hapjs.inspector.Input;
import org.hapjs.inspector.InspectorVElement;
import org.hapjs.inspector.V8Inspector;
import org.hapjs.inspector.VDocumentProvider;
import org.hapjs.widgets.tab.Tabs;
import org.hapjs.widgets.view.list.FlexLayoutManager;
import org.json.JSONObject;

public class DOM implements ChromeDevtoolsDomain {
    private final ObjectMapper mObjectMapper;
    private final Document mDocument;
    private final Map<String, List<Integer>> mSearchResults;
    private final AtomicInteger mResultCounter;
    private final ChromePeerManager mPeerManager;
    private final DocumentUpdateListener mListener;

    private ChildNodeRemovedEvent mCachedChildNodeRemovedEvent;
    private ChildNodeInsertedEvent mCachedChildNodeInsertedEvent;

    public DOM(Document document) {
        mObjectMapper = new ObjectMapper();
        mDocument = Util.throwIfNull(document);
        mSearchResults = Collections.synchronizedMap(new HashMap<String, List<Integer>>());
        mResultCounter = new AtomicInteger(0);
        mPeerManager = new ChromePeerManager();
        mPeerManager.setListener(new PeerManagerListener());
        mListener = new DocumentUpdateListener();
    }

    @ChromeDevtoolsMethod
    public void enable(JsonRpcPeer peer, JSONObject params) {
        mPeerManager.addPeer(peer);
        // INSPECTOR ADD:
        V8Inspector.getInstance().domEnabled();
    }

    @ChromeDevtoolsMethod
    public void disable(JsonRpcPeer peer, JSONObject params) {
        mPeerManager.removePeer(peer);
    }

    @ChromeDevtoolsMethod
    public JsonRpcResult getDocument(JsonRpcPeer peer, JSONObject params) {
        final GetDocumentResponse result = new GetDocumentResponse();

        result.root =
                mDocument.postAndWait(
                        new UncheckedCallable<Node>() {
                            @Override
                            public Node call() {
                                Object element = mDocument.getRootElement();
                                return createNodeForElement(element, mDocument.getDocumentView(),
                                        null);
                            }
                        });

        return result;
    }

    @ChromeDevtoolsMethod
    public void highlightNode(JsonRpcPeer peer, JSONObject params) {
        final HighlightNodeRequest request =
                mObjectMapper.convertValue(params, HighlightNodeRequest.class);
        // INSPECTOR ADD:
        if (request != null) {
            if (request.nodeId == null) {
                LogUtil.w("DOM.highlightNode was not given a nodeId; JS objectId is not supported");
                return;
            }

            final RGBAColor contentColor = request.highlightConfig.contentColor;
            if (contentColor == null) {
                LogUtil.w("DOM.highlightNode was not given a color to highlight with");
                return;
            }

            mDocument.postAndWait(
                    new Runnable() {
                        @Override
                        public void run() {
                            Object element = mDocument.getElementForNodeId(request.nodeId);
                            if (element != null) {
                                mDocument.highlightElement(element, contentColor.getColor());
                            }
                        }
                    });
        } else {
            // INSPECTOR ADD:
            LogUtil.w("DOM.highlightNode request is null!");
        }
    }

    @ChromeDevtoolsMethod
    public void hideHighlight(JsonRpcPeer peer, JSONObject params) {
        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        mDocument.hideHighlight();
                    }
                });
    }

    @ChromeDevtoolsMethod
    public ResolveNodeResponse resolveNode(JsonRpcPeer peer, JSONObject params)
            throws JsonRpcException {
        final ResolveNodeRequest request =
                mObjectMapper.convertValue(params, ResolveNodeRequest.class);

        final Object element =
                mDocument.postAndWait(
                        new UncheckedCallable<Object>() {
                            @Override
                            public Object call() {
                                return mDocument.getElementForNodeId(request.nodeId);
                            }
                        });

        if (element == null) {
            throw new JsonRpcException(
                    new JsonRpcError(
                            JsonRpcError.ErrorCode.INVALID_PARAMS,
                            // INSPECTOR MOD:
                            // "No known nodeId=" + request.nodeId,
                            ((request == null) ? "request is null" :
                                    "No known nodeId=" + request.nodeId),
                            null /* data */));
        }

        int mappedObjectId = Runtime.mapObject(peer, element);

        Runtime.RemoteObject remoteObject = new Runtime.RemoteObject();
        remoteObject.type = Runtime.ObjectType.OBJECT;
        remoteObject.subtype = Runtime.ObjectSubType.NODE;
        remoteObject.className = element.getClass().getName();
        remoteObject.value = null; // not a primitive
        remoteObject.description = null; // not sure what this does...
        remoteObject.objectId = String.valueOf(mappedObjectId);
        ResolveNodeResponse response = new ResolveNodeResponse();
        response.object = remoteObject;

        return response;
    }

    @ChromeDevtoolsMethod
    public void setAttributesAsText(JsonRpcPeer peer, JSONObject params) {
        final SetAttributesAsTextRequest request =
                mObjectMapper.convertValue(params, SetAttributesAsTextRequest.class);

        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        Object element = mDocument.getElementForNodeId(request.nodeId);
                        if (element != null) {
                            // INSPECTOR MOD
                            // mDocument.setAttributesAsText(element, request.text);
                            mDocument.setAttributesAsText(element, request.name, request.text);
                        }
                    }
                });
    }

    @ChromeDevtoolsMethod
    public void setInspectModeEnabled(JsonRpcPeer peer, JSONObject params) {
        final SetInspectModeEnabledRequest request =
                mObjectMapper.convertValue(params, SetInspectModeEnabledRequest.class);

        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        mDocument.setInspectModeEnabled(request.enabled);
                    }
                });
    }

    @ChromeDevtoolsMethod
    public PerformSearchResponse performSearch(JsonRpcPeer peer, final JSONObject params) {
        final PerformSearchRequest request =
                mObjectMapper.convertValue(params, PerformSearchRequest.class);

        final ArrayListAccumulator<Integer> resultNodeIds = new ArrayListAccumulator<>();

        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        mDocument.findMatchingElements(request.query, resultNodeIds);
                    }
                });

        // Each search action has a unique ID so that
        // it can be queried later.
        final String searchId = String.valueOf(mResultCounter.getAndIncrement());

        mSearchResults.put(searchId, resultNodeIds);

        final PerformSearchResponse response = new PerformSearchResponse();
        response.searchId = searchId;
        response.resultCount = resultNodeIds.size();

        return response;
    }

    @ChromeDevtoolsMethod
    public GetSearchResultsResponse getSearchResults(JsonRpcPeer peer, JSONObject params) {
        final GetSearchResultsRequest request =
                mObjectMapper.convertValue(params, GetSearchResultsRequest.class);
        // INSPECTOR ADD BEGIN:
        if (request == null) {
            LogUtil.w("request is null!");
            return null;
        }
        // END
        if (request.searchId == null) {
            LogUtil.w("searchId may not be null");
            return null;
        }

        final List<Integer> results = mSearchResults.get(request.searchId);

        if (results == null) {
            LogUtil.w("\"" + request.searchId + "\" is not a valid reference to a search result");
            return null;
        }

        final List<Integer> resultsRange = results.subList(request.fromIndex, request.toIndex);

        final GetSearchResultsResponse response = new GetSearchResultsResponse();
        response.nodeIds = resultsRange;

        return response;
    }

    @ChromeDevtoolsMethod
    public void discardSearchResults(JsonRpcPeer peer, JSONObject params) {
        final DiscardSearchResultsRequest request =
                mObjectMapper.convertValue(params, DiscardSearchResultsRequest.class);
        // INSPECTOR ADD BEGIN:
        if (request == null) {
            LogUtil.w("request is null!");
            return;
        }
        // END
        if (request.searchId != null) {
            mSearchResults.remove(request.searchId);
        }
    }

    // INSPECTOR ADD BEGIN
    @ChromeDevtoolsMethod
    public void setOuterHTML(JsonRpcPeer peer, JSONObject params) {
        final SetOuterHTMLRequest request =
                mObjectMapper.convertValue(params, SetOuterHTMLRequest.class);
        final Object elementForNodeId = mDocument.getElementForNodeId(request.nodeId);
        if (elementForNodeId == null) {
            LogUtil.w(
                    "Failed to get style of an element that does not exist, nodeid = "
                            + request.nodeId);
            return;
        }
        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        mDocument.setElementWithHTML(elementForNodeId, request.outerHTML);
                    }
                });
    }

    @ChromeDevtoolsMethod
    public void setInspectedNode(JsonRpcPeer peer, JSONObject params) {
        // Nothing to do here, just need to make sure Chrome doesn't get an error that this method
        // isn't implemented
    }

    @ChromeDevtoolsMethod
    public JsonRpcResult getBoxModel(JsonRpcPeer peer, JSONObject params) {
        final GetBoxModelRequest request =
                mObjectMapper.convertValue(params, GetBoxModelRequest.class);

        final GetBoxModelResult result = new GetBoxModelResult();

        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        final Object elementForNodeId =
                                mDocument.getElementForNodeId(request.nodeId);

                        if (elementForNodeId == null) {
                            LogUtil.w(
                                    "Failed to get style of an element that does not exist, nodeid = "
                                            + request.nodeId);
                            return;
                        }

                        final BoxModel boxModel = new BoxModel();
                        result.model = boxModel;

                        mDocument.getElementBoxModel(
                                elementForNodeId,
                                new DOMAccumulator() {
                                    @Override
                                    public void store(String name, int[] value) {
                                        List<Integer> listValue = new ArrayList<>();
                                        for (int v : value) {
                                            listValue.add((int) (v * Input.sScale));
                                        }
                                        switch (name) {
                                            case "content":
                                                boxModel.content = listValue;
                                                break;
                                            case "padding":
                                                boxModel.padding = listValue;
                                                break;
                                            case "border":
                                                boxModel.border = listValue;
                                                break;
                                            case "margin":
                                                boxModel.margin = listValue;
                                                break;
                                            case "width":
                                                boxModel.width = value[0];
                                                break;
                                            case "height":
                                                boxModel.height = value[0];
                                                break;
                                            default:
                                                LogUtil.e("get element BoxModel error , name :"
                                                        + name);
                                        }
                                    }
                                });
                    }
                });

        return result;
    }

    @ChromeDevtoolsMethod
    public JsonRpcResult getNodeForLocation(JsonRpcPeer peer, JSONObject params) {
        final GetNodeForLocationResult result = new GetNodeForLocationResult();
        final GetNodeForLocationRequest request =
                mObjectMapper.convertValue(params, GetNodeForLocationRequest.class);
        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Object rootElement = mDocument.getRootElement();
                            if (rootElement == null) {
                                return;
                            }
                            DocumentView docView = mDocument.getDocumentView();
                            ElementInfo rootInfo = docView.getElementInfo(rootElement);
                            if (rootInfo.children.size() != 1) {
                                return;
                            }
                            InspectorVElement docElement =
                                    (InspectorVElement) rootInfo.children.get(0);
                            Pair<Integer, InspectorVElement> pair =
                                    findElementByPosition(
                                            docElement,
                                            docView,
                                            (int) (request.x / Input.sScale),
                                            (int) (request.y / Input.sScale),
                                            0);
                            if (pair == null) {
                                return;
                            }
                            Object findElement = pair.second;

                            Integer nodeId = mDocument.getNodeIdForElement(findElement);
                            if (nodeId == null) {
                                return;
                            }
                            result.nodeId = nodeId;

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        return result;
    }

    private Pair<Integer, InspectorVElement> findElementByPosition(
            InspectorVElement e, DocumentView docView, int x, int y, int depth) {
        if (e.getComponent() == null) {
            LogUtil.e("component is null, " + e.getTagName());
            return null;
        }
        View hostView = e.getComponent().getHostView();
        if (!isTouchPointInView(hostView, x, y)) {
            return null;
        }
        InspectorVElement result = e;
        ElementInfo elementInfo = docView.getElementInfo(e);

        while (elementInfo.children.size() > 0) {
            boolean isTouchPointInChildView = false;
            int start = 0;
            int end = elementInfo.children.size();
            if (result.getComponent() == null) {
                LogUtil.e("component is null, " + result.getTagName());
                break;
            }
            if (result.getComponent().getHostView() instanceof RecyclerView) {
                RecyclerView v = (RecyclerView) (result.getComponent().getHostView());
                // INSPECTOR MOD
                RecyclerView.LayoutManager layoutManager = v.getLayoutManager();
                if (layoutManager instanceof FlexLayoutManager) {
                    int firstVisiblePosition =
                            ((FlexLayoutManager) layoutManager).findFlexFirstVisibleItemPosition();
                    int lastVisiblePosition =
                            ((FlexLayoutManager) layoutManager).findFlexLastVisibleItemPosition();
                    start = firstVisiblePosition;
                    end = lastVisiblePosition + 1;
                }
                // END
            }
            for (int i = end - 1, n = start; i >= n; --i) {
                final InspectorVElement childElement =
                        (InspectorVElement) elementInfo.children.get(i);
                if (isStack(childElement)) {
                    Pair<Integer, InspectorVElement> pairInStack =
                            findElementInStack(childElement, docView, x, y, depth + 1);
                    if (pairInStack != null) {
                        return pairInStack;
                    } else {
                        continue;
                    }
                }
                if (isTabContent(childElement)) {
                    Pair<Integer, InspectorVElement> pairInTabContent =
                            findElementInTabContent(childElement, docView, x, y, depth + 1);
                    if (pairInTabContent != null) {
                        return pairInTabContent;
                    } else {
                        continue;
                    }
                }

                if (childElement.getComponent() == null) {
                    LogUtil.e("component is null, " + childElement.getTagName());
                    break;
                }
                hostView = childElement.getComponent().getHostView();
                if (isTouchPointInView(hostView, x, y)) {
                    isTouchPointInChildView = true;
                    result = childElement;
                    elementInfo = docView.getElementInfo(childElement);
                    break;
                }
            }
            if (!isTouchPointInChildView) {
                break;
            }
            depth++;
        }
        return new Pair<>(depth, result);
    }

    private boolean isTabContent(InspectorVElement e) {
        String tagName = e.getTagName();
        return tagName != null && tagName.contains("tab-content");
    }

    private Pair<Integer, InspectorVElement> findElementInTabContent(InspectorVElement e, DocumentView docView,
                                                                     int x, int y, int depth) {
        InspectorVElement parent = e.getParent();  // the tabs element;
        int curIndex = ((Tabs) (parent.getComponent())).getCurrentIndex();
        if (curIndex == -1) {
            curIndex = 0;
        }
        ElementInfo tabContentInfo = docView.getElementInfo(e);
        InspectorVElement tabContentChild =
                (InspectorVElement) tabContentInfo.children.get(curIndex);
        Pair<Integer, InspectorVElement> result =
                findElementByPosition(tabContentChild, docView, x, y, depth + 1);
        return result;
    }

    private boolean isStack(InspectorVElement e) {
        int pageId = VDocumentProvider.getPageId();
        String tagName = e.getTagName();
        return tagName != null && tagName.contains("stack");
    }

    private Pair<Integer, InspectorVElement> findElementInStack(
            InspectorVElement e, DocumentView docView, int x, int y, int depth) {
        List<Pair<Integer, InspectorVElement>> resultList = new ArrayList<>();
        ElementInfo elementInfo = docView.getElementInfo(e);
        int end = elementInfo.children.size();
        for (int i = end - 1; i >= 0; --i) {
            final InspectorVElement childElement = (InspectorVElement) elementInfo.children.get(i);
            Pair<Integer, InspectorVElement> tempResult =
                    findElementByPosition(childElement, docView, x, y, depth + 1);
            if (tempResult != null) {
                resultList.add(tempResult);
            }
        }
        if (resultList.isEmpty()) {
            return null;
        }
        int maxDepth = 0;
        int maxSeq = 0;
        for (int i = 0; i < resultList.size(); i++) {
            if (maxDepth < resultList.get(i).first) {
                maxDepth = resultList.get(i).first;
                maxSeq = i;
            }
        }
        return resultList.get(maxSeq);
    }

    private boolean isTouchPointInView(final View view, final int x, final int y) {
        if (view == null) {
            return false;
        }
        if (view.getVisibility() == View.GONE) {
            return false;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();
        if (y >= top && y <= bottom && x >= left && x <= right) {
            return true;
        }
        return false;
    }

    @ChromeDevtoolsMethod
    public GetOuterHTMLResponse getOuterHTML(JsonRpcPeer peer, JSONObject params) {
        final GetOuterHTMLRequest request =
                mObjectMapper.convertValue(params, GetOuterHTMLRequest.class);
        final GetOuterHTMLResponse result = new GetOuterHTMLResponse();
        final Object elementForNodeId = mDocument.getElementForNodeId(request.nodeId);
        if (elementForNodeId == null) {
            LogUtil.w(
                    "Failed to get style of an element that does not exist, nodeid = "
                            + request.nodeId);
            return result;
        }
        mDocument.postAndWait(
                new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder outerHTML = new StringBuilder("");
                        getNodeHTML(elementForNodeId, mDocument.getDocumentView(), outerHTML, "");
                        result.outerHTML = outerHTML.toString();
                    }
                });
        return result;
    }

    private void getNodeHTML(
            Object element, DocumentView view, StringBuilder nodeHTML, String whiteSpaceStr) {
        NodeDescriptor descriptor = mDocument.getNodeDescriptor(element);

        // NodeName
        String localName = descriptor.getLocalName(element);

        // Attributes
        Document.AttributeListAccumulator attributes = new Document.AttributeListAccumulator();
        descriptor.getAttributes(element, attributes);
        nodeHTML.append(whiteSpaceStr).append('<').append(localName);
        getAttributeValue(attributes, nodeHTML);
        nodeHTML.append(">");

        // Children
        ElementInfo elementInfo = view.getElementInfo(element);
        int childrenSize = elementInfo.children.size();
        for (int i = 0; i < childrenSize; ++i) {
            final Object childElement = elementInfo.children.get(i);
            nodeHTML.append("\n");
            getNodeHTML(childElement, view, nodeHTML, whiteSpaceStr + "  ");
        }
        if (childrenSize > 0) {
            nodeHTML.append("\n").append(whiteSpaceStr);
        }

        // End Tag
        nodeHTML.append("</").append(localName).append(">");
    }

    private void getAttributeValue(List<String> attrs, StringBuilder nodeHTML) {
        try {
            for (int i = 0; i < attrs.size(); i += 2) {
                String name = attrs.get(i);
                String value = attrs.get(i + 1);
                if (value.contains("\"")) {
                    value = value.replaceAll("\"", "&quot;");
                }
                nodeHTML.append(" ").append(name).append("=\"").append(value).append("\"");
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }
    // INSPECTOR END

    private Node createNodeForElement(
            Object element, DocumentView view, @Nullable Accumulator<Object> processedElements) {
        if (processedElements != null) {
            processedElements.store(element);
        }

        NodeDescriptor descriptor = mDocument.getNodeDescriptor(element);

        Node node = new DOM.Node();
        node.nodeId = mDocument.getNodeIdForElement(element);
        node.nodeType = descriptor.getNodeType(element);
        node.nodeName = descriptor.getNodeName(element);
        node.localName = descriptor.getLocalName(element);
        node.nodeValue = descriptor.getNodeValue(element);

        Document.AttributeListAccumulator accumulator = new Document.AttributeListAccumulator();
        descriptor.getAttributes(element, accumulator);

        // Attributes
        node.attributes = accumulator;

        // Children
        ElementInfo elementInfo = view.getElementInfo(element);
        List<Node> childrenNodes =
                (elementInfo.children.size() == 0)
                        ? Collections.<Node>emptyList()
                        : new ArrayList<Node>(elementInfo.children.size());

        for (int i = 0, n = elementInfo.children.size(); i < n; ++i) {
            final Object childElement = elementInfo.children.get(i);
            Node childNode = createNodeForElement(childElement, view, processedElements);
            childrenNodes.add(childNode);
        }

        node.children = childrenNodes;
        node.childNodeCount = childrenNodes.size();

        return node;
    }

    private ChildNodeInsertedEvent acquireChildNodeInsertedEvent() {
        ChildNodeInsertedEvent childNodeInsertedEvent = mCachedChildNodeInsertedEvent;
        if (childNodeInsertedEvent == null) {
            childNodeInsertedEvent = new ChildNodeInsertedEvent();
        }
        mCachedChildNodeInsertedEvent = null;
        return childNodeInsertedEvent;
    }

    private void releaseChildNodeInsertedEvent(ChildNodeInsertedEvent childNodeInsertedEvent) {
        childNodeInsertedEvent.parentNodeId = -1;
        childNodeInsertedEvent.previousNodeId = -1;
        childNodeInsertedEvent.node = null;
        if (mCachedChildNodeInsertedEvent == null) {
            mCachedChildNodeInsertedEvent = childNodeInsertedEvent;
        }
    }

    private ChildNodeRemovedEvent acquireChildNodeRemovedEvent() {
        ChildNodeRemovedEvent childNodeRemovedEvent = mCachedChildNodeRemovedEvent;
        if (childNodeRemovedEvent == null) {
            childNodeRemovedEvent = new ChildNodeRemovedEvent();
        }
        mCachedChildNodeRemovedEvent = null;
        return childNodeRemovedEvent;
    }

    private void releaseChildNodeRemovedEvent(ChildNodeRemovedEvent childNodeRemovedEvent) {
        childNodeRemovedEvent.parentNodeId = -1;
        childNodeRemovedEvent.nodeId = -1;
        if (mCachedChildNodeRemovedEvent == null) {
            mCachedChildNodeRemovedEvent = childNodeRemovedEvent;
        }
    }

    private static class GetDocumentResponse implements JsonRpcResult {
        @JsonProperty(required = true)
        public Node root;
    }

    private static class Node implements JsonRpcResult {
        @JsonProperty(required = true)
        public int nodeId;

        @JsonProperty(required = true)
        public NodeType nodeType;

        @JsonProperty(required = true)
        public String nodeName;

        @JsonProperty(required = true)
        public String localName;

        @JsonProperty(required = true)
        public String nodeValue;

        @JsonProperty
        public Integer childNodeCount;

        @JsonProperty
        public List<Node> children;

        @JsonProperty
        public List<String> attributes;
    }

    private static class AttributeModifiedEvent {
        @JsonProperty(required = true)
        public int nodeId;

        @JsonProperty(required = true)
        public String name;

        @JsonProperty(required = true)
        public String value;
    }

    private static class AttributeRemovedEvent {
        @JsonProperty(required = true)
        public int nodeId;

        @JsonProperty(required = true)
        public String name;
    }

    private static class ChildNodeInsertedEvent {
        @JsonProperty(required = true)
        public int parentNodeId;

        @JsonProperty(required = true)
        public int previousNodeId;

        @JsonProperty(required = true)
        public Node node;
    }

    private static class ChildNodeRemovedEvent {
        @JsonProperty(required = true)
        public int parentNodeId;

        @JsonProperty(required = true)
        public int nodeId;
    }

    private static class HighlightNodeRequest {
        @JsonProperty(required = true)
        public HighlightConfig highlightConfig;

        @JsonProperty
        public Integer nodeId;

        @JsonProperty
        public String objectId;
    }

    private static class HighlightConfig {
        @JsonProperty
        public RGBAColor contentColor;
    }

    private static class InspectNodeRequestedEvent {
        @JsonProperty
        public int nodeId;
    }

    private static class SetInspectModeEnabledRequest {
        @JsonProperty(required = true)
        public boolean enabled;

        @JsonProperty
        public Boolean inspectShadowDOM;

        @JsonProperty
        public HighlightConfig highlightConfig;
    }

    private static class RGBAColor {
        @JsonProperty(required = true)
        public int r;

        @JsonProperty(required = true)
        public int g;

        @JsonProperty(required = true)
        public int b;

        @JsonProperty
        public Double a;

        public int getColor() {
            byte alpha;
            if (this.a == null) {
                alpha = (byte) 255;
            } else {
                long aLong = Math.round(this.a * 255.0);
                alpha = (aLong < 0) ? (byte) 0 : (aLong >= 255) ? (byte) 255 : (byte) aLong;
            }

            return Color.argb(alpha, this.r, this.g, this.b);
        }
    }

    private static class ResolveNodeRequest {
        @JsonProperty(required = true)
        public int nodeId;

        @JsonProperty
        public String objectGroup;
    }

    private static class SetAttributesAsTextRequest {
        @JsonProperty(required = true)
        public int nodeId;

        @JsonProperty(required = true)
        public String text;

        // INSPECTOR ADD
        @JsonProperty(required = true)
        public String name;
        // INSPECTOR END
    }

    // INSPECTOR ADD BEGIN
    private static class SetOuterHTMLRequest implements JsonRpcResult {
        @JsonProperty(required = true)
        public int nodeId;

        @JsonProperty(required = true)
        public String outerHTML;
    }

    private static class GetNodeForLocationRequest implements JsonRpcResult {
        @JsonProperty(required = true)
        public int x;

        @JsonProperty(required = true)
        public int y;

    /* @JsonProperty
    public boolean includeUserAgentShadowDOM;*/
    }

    private static class GetNodeForLocationResult implements JsonRpcResult {
        @JsonProperty(required = true)
        public int nodeId;
    }

    private static class GetBoxModelResult implements JsonRpcResult {
        @JsonProperty
        public BoxModel model;
    }

    private static class GetBoxModelRequest implements JsonRpcResult {
        @JsonProperty(required = true)
        public int nodeId;
    }

    private static class BoxModel {
        @JsonProperty(required = true)
        public List<Integer> content;

        @JsonProperty(required = true)
        public List<Integer> padding;

        @JsonProperty(required = true)
        public List<Integer> border;

        @JsonProperty(required = true)
        public List<Integer> margin;

        /* @JsonProperty
        ShapeOutsideInfo shapeOutside; */

        @JsonProperty(required = true)
        public int width;

        @JsonProperty(required = true)
        public int height;
    }

    private static class GetOuterHTMLRequest implements JsonRpcResult {
        @JsonProperty(required = true)
        public int nodeId;
    }

    private static class GetOuterHTMLResponse implements JsonRpcResult {
        @JsonProperty(required = true)
        public String outerHTML;
    }

    private static class ResolveNodeResponse implements JsonRpcResult {
        @JsonProperty(required = true)
        public Runtime.RemoteObject object;
    }

    private static class PerformSearchRequest {
        @JsonProperty(required = true)
        public String query;

        @JsonProperty
        public Boolean includeUserAgentShadowDOM;
    }
    // INSPECTOR END

    private static class PerformSearchResponse implements JsonRpcResult {
        @JsonProperty(required = true)
        public String searchId;

        @JsonProperty(required = true)
        public int resultCount;
    }

    private static class GetSearchResultsRequest {
        @JsonProperty(required = true)
        public String searchId;

        @JsonProperty(required = true)
        public int fromIndex;

        @JsonProperty(required = true)
        public int toIndex;
    }

    private static class GetSearchResultsResponse implements JsonRpcResult {
        @JsonProperty(required = true)
        public List<Integer> nodeIds;
    }

    private static class DiscardSearchResultsRequest {
        @JsonProperty(required = true)
        public String searchId;
    }

    private final class DocumentUpdateListener implements Document.UpdateListener {
        public void onAttributeModified(Object element, String name, String value) {
            AttributeModifiedEvent message = new AttributeModifiedEvent();
            message.nodeId = mDocument.getNodeIdForElement(element);
            message.name = name;
            message.value = value;
            mPeerManager.sendNotificationToPeers("DOM.attributeModified", message);
        }

        public void onAttributeRemoved(Object element, String name) {
            AttributeRemovedEvent message = new AttributeRemovedEvent();
            message.nodeId = mDocument.getNodeIdForElement(element);
            message.name = name;
            mPeerManager.sendNotificationToPeers("DOM.attributeRemoved", message);
        }

        public void onInspectRequested(Object element) {
            Integer nodeId = mDocument.getNodeIdForElement(element);
            if (nodeId == null) {
                LogUtil.d(
                        "DocumentProvider.Listener.onInspectRequested() "
                                + "called for a non-mapped node: element=%s",
                        element);
            } else {
                InspectNodeRequestedEvent message = new InspectNodeRequestedEvent();
                message.nodeId = nodeId;
                mPeerManager.sendNotificationToPeers("DOM.inspectNodeRequested", message);
            }
        }

        public void onChildNodeRemoved(int parentNodeId, int nodeId) {
            ChildNodeRemovedEvent removedEvent = acquireChildNodeRemovedEvent();

            removedEvent.parentNodeId = parentNodeId;
            removedEvent.nodeId = nodeId;
            mPeerManager.sendNotificationToPeers("DOM.childNodeRemoved", removedEvent);

            releaseChildNodeRemovedEvent(removedEvent);
        }

        public void onChildNodeInserted(
                DocumentView view,
                Object element,
                int parentNodeId,
                int previousNodeId,
                Accumulator<Object> insertedElements) {
            ChildNodeInsertedEvent insertedEvent = acquireChildNodeInsertedEvent();

            insertedEvent.parentNodeId = parentNodeId;
            insertedEvent.previousNodeId = previousNodeId;
            insertedEvent.node = createNodeForElement(element, view, insertedElements);

            mPeerManager.sendNotificationToPeers("DOM.childNodeInserted", insertedEvent);

            releaseChildNodeInsertedEvent(insertedEvent);
        }
    }

    private final class PeerManagerListener extends PeersRegisteredListener {
        @Override
        protected synchronized void onFirstPeerRegistered() {
            mDocument.addRef();
            mDocument.addUpdateListener(mListener);
        }

        @Override
        protected synchronized void onLastPeerUnregistered() {
            mSearchResults.clear();
            mDocument.removeUpdateListener(mListener);
            mDocument.release();
        }
    }
}
