import NodeManager from './node-manager.js';
import EdgeManager from './edge-manager.js';
import UIManager from './ui-manager.js';
import ExecutionManager from './execution-manager.js';
import RenderManager from './render-manager.js';
import EventManager from './event-manager.js';
export class PipelineDesigner  {
  constructor(container, opts = {}) {
    this.container = (typeof container === 'string') ? document.getElementById(container) : container;
    if (!this.container) throw new Error('PipelineDesigner: container not found');
    this._autoFollow = true;
    this._animFrameId = null;
    this._animating = false;
    this._pendingFollow = null;
    this.pipelineId = opts.pipelineId || null;
    this.versionId = opts.versionId || null;
    this.nodeTypes = {};
    this.onFlowChanged = opts.onFlowChanged || (() => {});
    this._initialFlow = opts.flow || null;
    this._initialNodeTypes = opts.nodeTypes || null;
    this.readOnly = opts.readOnly || false;
    this.inputSchema = this._schemaToFields(opts.inputSchema);
    this.nodesById = Object.create(null);
    this.edges = [];
    this._edgeSeq = 0;
    this._selected = null;
    this.renderManager = new RenderManager(this);
    this.nodeManager = new NodeManager(this);
    this.edgeManager = new EdgeManager(this);
    this.uiManager = new UIManager(this);
    this.executionManager = new ExecutionManager(this);
    this.eventManager = new EventManager(this);
    window.pipelineDesigner = this;
    this._initDesignerState();
  }
  /**
   * Converts the canonical JSON Schema (server format) into the UI field list.
   * @param {Object} schema JSON Schema object {type:'object', properties, required}
   * @returns {Array} UI fields [{name, required, ...}]
   */
  _schemaToFields(schema) {
    if (!schema || typeof schema !== 'object' || typeof schema.properties !== 'object') return [];
    const required = Array.isArray(schema.required) ? schema.required : [];
    return Object.entries(schema.properties).map(([name, def]) => ({ name, required: required.includes(name), ...(def || {}) }));
  }
  /**
   * Converts the UI field list back into the canonical JSON Schema (server format).
   * @param {Array} fields UI fields [{name, required, ...}]
   * @returns {Object} JSON Schema object {type:'object', properties, required}
   */
  _fieldsToSchema(fields) {
    const properties = {};
    const required = [];
    for (const field of Array.isArray(fields) ? fields : []) {
      if (!field || !field.name) continue;
      const { name, required: isRequired, ...rest } = field;
      properties[name] = rest;
      if (isRequired) required.push(name);
    }
    return { type: 'object', properties, required };
  }
  async _initDesignerState() {
    try {
      if (this._initialNodeTypes) {
        this.nodeTypes = this._initialNodeTypes;
      } else {
        await this._fetchNodeTypes();
      }
      let flow;
      if (this._initialFlow) {
        flow = this._initialFlow;
      } else {
        const version = await this._fetchVersion();
        flow = version?.flow || { nodes: {}, edges: [] };
        if (version?.inputSchema) {
          try {
            this.inputSchema = this._schemaToFields(typeof version.inputSchema === 'string' ? JSON.parse(version.inputSchema) : version.inputSchema);
          } catch (e) {
            this.inputSchema = [];
          }
        }
      }
      if (typeof flow === 'string') {
        try {
          flow = JSON.parse(flow);
        } catch (e) {
          console.error('Failed to parse pipeline flow:', e);
          flow = { nodes: {}, edges: [] };
        }
      }
      this._skipSave = true;
      try {
        this._importFlow(flow);
      } finally {
        this._skipSave = false;
      }
      setTimeout(() => {
        this.edgeManager.redrawAllEdges();
        this.renderManager.centerView();
        this.uiManager.initTribute();
      }, 0);
    } catch (error) {
      console.error("Failed to fetch node types:", error);
      this.container.innerHTML = `
        <div class="alert alert-danger m-4">
          <div class="d-flex align-items-center">
        <i class="ti ti-alert-circle fs-3 me-3"></i>
        <div>
          <h5 class="alert-heading mb-2">Erreur lors du chargement des types de nœuds</h5>
          <p class="mb-0">Veuillez vérifier la console et réessayer.</p>
        </div>
          </div>
        </div>`;
    }
  }
  async _fetchPipeline() {
    try {
      const response = await fetch(`rest/platform/agent/admin/pipelines/${this.pipelineId}`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error('Error fetching pipeline:', error);
      throw error;
    }
  }
  async _fetchVersion() {
    try {
      const response = await fetch(`rest/platform/agent/admin/pipelines/versions/${this.versionId}`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error('Error fetching version:', error);
      throw error;
    }
  }
  async _fetchNodeTypes() {
    try {
      const response = await fetch('rest/platform/agent/admin/pipelines/nodes/types');
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      this.nodeTypes = await response.json();
    } catch (error) {
      console.error('Error fetching node types:', error);
      throw error;
    }
  }
  _getNodeTypeInfo(type) {
    const nodeTypeInfo = this.nodeTypes[type] || {};
    const name = nodeTypeInfo.name || type;
    const description = nodeTypeInfo.description || 'Pas de description disponible';
    return { name, description };
  }
  addNode(type, pos = { x: 60, y: 60 }, data = {}) {
    return this.nodeManager.addNode(type, pos, data);
  }
  removeNode(id) {
    this.nodeManager.removeNode(id);
  }
  addEdge({ source, sourcePort, target, targetPort }) {
    return this.edgeManager.addEdge({ source, sourcePort, target, targetPort });
  }
  removeEdge(id) {
    this.edgeManager.removeEdge(id);
  }
  getFlow() {
    const nodes = {};
    Object.values(this.nodesById).forEach(n => {
      nodes[n.id] = {
        id: n.id,
        type: n.type,
        data: n.data,
        x: n.x,
        y: n.y
      };
    });
    const edges = this.edges.map(e => ({
      source: e.source,
      sourcePort: e.sourcePort,
      target: e.target,
      targetPort: e.targetPort
    }));
    const root = Object.values(nodes).find(n => n.type === 'START')?.id || Object.keys(nodes)[0] || null;
    return { nodes, edges, rootNodeId: root };
  }
_updateFlowInput() {
  this.onFlowChanged(this.getFlow());
  
  if (this.uiManager) {
    this.uiManager._scanForCustomVariables();
  }
  if (!this._skipSave) {
    clearTimeout(this._saveTimeout);
    this._saveTimeout = setTimeout(() => {
      this._saveVersion();
    }, 1000);
  }
}
_importFlow(flow) {
    Object.values(flow.nodes || {}).forEach(n => {
        const pos = { x: n.x !== undefined ? n.x : 40, y: n.y !== undefined ? n.y : 40 };
        this.addNode(n.type, pos, { ...n.data, id: n.id });
    });
    (flow.edges || []).forEach(e => this.addEdge(e));
    this._updateFlowInput();
    if (this.uiManager) {
        setTimeout(() => {
            this.uiManager._performFullVariableScan();
            this.uiManager._refreshStartNodeSummary();
        }, 200);
    }
}
  findNodeByDataId(dataId) {
    if (!dataId) return null;
    for (const id in this.nodesById) {
      const node = this.nodesById[id];
      if (node.data && (node.data.nodeId === dataId || node.data.id === dataId || node.id === dataId)) {
        return node;
      }
    }
    return null;
  }
  resetAllNodeStatus() {
    Object.values(this.nodesById).forEach(node => {
      node.el.setAttribute('data-status', '');
      const badge = node.el.querySelector('.pf-status-badge');
      if (badge) {
        badge.textContent = '';
      }
    });
    this.edges.forEach(edge => {
      edge.pathEl.setAttribute('data-active', 'false');
    });
  }
getPrecedingNodes(nodeId) {
  if (!nodeId || !this.nodesById[nodeId]) return [];
  const reverseGraph = {};
  this.edges.forEach(edge => {
    if (!reverseGraph[edge.target]) {
      reverseGraph[edge.target] = [];
    }
    reverseGraph[edge.target].push(edge.source);
  });
  const visited = new Set();
  const queue = [nodeId];
  while (queue.length > 0) {
    const current = queue.shift();
    if (current !== nodeId) {
      visited.add(current);
    }
    const predecessors = reverseGraph[current] || [];
    for (const pred of predecessors) {
      if (!visited.has(pred)) {
        queue.push(pred);
      }
    }
  }
  return Array.from(visited);
}
async _saveVersion() {
  if (!this.versionId) {
    console.warn("Cannot save: No version ID");
    return;
  }
  try {
    this.uiManager._resolveFileAcceptTypes();
    const flow = JSON.stringify(this.getFlow());
    const response = await fetch(`rest/platform/agent/admin/pipelines/versions/${this.versionId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        flow: flow,
        inputSchema: this._fieldsToSchema(this.inputSchema)
      })
    });
    if (!response.ok) {
      throw new Error(`Failed to save: ${response.status}`);
    }
    this._showSaveNotification();
  } catch (error) {
    console.error("Error saving pipeline version:", error);
    this._showSaveNotification(false);
  }
}
_showSaveNotification(success = true) {
  const notification = document.createElement('div');
  notification.className = `pf-save-notification ${success ? 'success' : 'error'}`;
  notification.innerHTML = success ? 
    '<i class="ti ti-check"></i> Modifications enregistrées' : 
    '<i class="ti ti-alert-triangle"></i> Échec de l\'enregistrement';
  document.body.appendChild(notification);
  setTimeout(() => {
    notification.classList.add('visible');
  }, 10);
  setTimeout(() => {
    notification.classList.remove('visible');
    setTimeout(() => notification.remove(), 300);
  }, 2000);
}

}