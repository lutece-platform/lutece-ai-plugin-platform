export default class NodeManager {
  constructor(pipelineDesigner) {
    this.designer = pipelineDesigner;
  }

  _generateNodeId(type) {
    const existingNodes = Object.values(this.designer.nodesById);
    const prefix = type.toLowerCase();
    const regex = new RegExp(`^${prefix}_(\\d+)$`);

    let maxNum = 0;
    for (const node of existingNodes) {
      if (node.type === type) {
        const match = node.id.match(regex);
        if (match) {
          maxNum = Math.max(maxNum, parseInt(match[1], 10));
        } else if (node.id.startsWith(prefix)) {
          maxNum = Math.max(maxNum, 1);
        }
      }
    }

    return `${prefix}_${maxNum + 1}`;
  }

  addNode(type, pos = { x: 60, y: 60 }, data = {}) {
    const id = data.id || this._generateNodeId(type);

    const nodeData = { ...data };
    
    if (!nodeData.nodeId) {
      nodeData.nodeId = id;
    }
    
    const node = {
      id,
      type,
      data: nodeData,
      x: pos.x,
      y: pos.y
    };
    
    this.designer.nodesById[id] = node;
    this._createNodeEl(node);
    this.designer._updateFlowInput();
    
    setTimeout(() => this.designer.uiManager._updateAvailableOutputKeys(), 50);
    
    return id;
  }
  
removeNode(id) {
    const n = this.designer.nodesById[id];
    if (!n) return;
    
    this.designer.edges
        .filter(e => e.source === id || e.target === id)
        .forEach(e => this.designer.removeEdge(e.id));
    
    n.el.remove();
    delete this.designer.nodesById[id];
    
    if (this.designer._selected === id) {
        this._selectNode(null);
    }
    
    this.designer._updateFlowInput();
    
    if (this.designer.uiManager) {
        setTimeout(() => {
            this.designer.uiManager._scanForCustomVariables();
        }, 50);
    }
    
    setTimeout(() => this.designer.uiManager._updateAvailableOutputKeys(), 50);
}
  
  _createNodeEl(node) {
    const info = this.designer.nodeTypes[node.type] || {};

    const el = document.createElement('div');
    el.className = `pf-node`;
    el.classList.add(`pf-${node.type.toLowerCase().replace(/[^\w-]/g, '-')}`);
    el.dataset.id = node.id;
    el.dataset.type = node.type;
    el.dataset.name = info.name;
    el.dataset.description = info.description || 'No description available';
    
    const header = document.createElement('div');
    header.className = 'pf-node-header';
    
    const icon = document.createElement('span');
    icon.className = 'pf-node-icon';
    icon.innerHTML = info.icon || '<i class="ti ti-box"></i>';
    
    const title = document.createElement('div');
    title.className = 'pf-node-title';
    title.innerHTML = `${info.name}`;
    const idRibbon = document.createElement('code');
    idRibbon.className = 'pf-node-id-ribbon';
    idRibbon.textContent = node.id;
    title.appendChild(idRibbon);

    header.appendChild(icon);
    header.appendChild(title);
    el.appendChild(header);

    const configSummary = document.createElement('div');
    configSummary.className = 'pf-config-summary';
    this._updateConfigSummary(node, configSummary);
    el.appendChild(configSummary);
    
    const ghost = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    ghost.setAttribute('class', 'pf-ghost');
    
    let srcNode = null;
    let srcPort = null;
    
    const onGhostMove = (e) => {
      const p1 = this.designer.edgeManager._portCenter(srcNode, srcPort, true);
      const rect = this.designer.container.getBoundingClientRect();
      
      const x2_screen = e.clientX - rect.left + this.designer.container.scrollLeft;
      const y2_screen = e.clientY - rect.top + this.designer.container.scrollTop;
      
      const c1 = this.designer.renderManager._getCoordsInCanvas(p1.x, p1.y);
      const c2 = this.designer.renderManager._getCoordsInCanvas(x2_screen, y2_screen);
      
      ghost.setAttribute('d', this.designer.edgeManager.bezierPath(c1.x, c1.y, c2.x, c2.y));
    };
    
    const onGhostEnd = (e) => {
      ghost.remove();
      document.removeEventListener('mousemove', onGhostMove);
      
      const tgt = e.target.closest('.pf-port.pf-in');
      
      if (tgt) {
        this.designer.addEdge({
          source: srcNode,
          sourcePort: srcPort,
          target: tgt.closest('.pf-node').dataset.id,
          targetPort: tgt.dataset.port
        });
      } else {
        const rect = this.designer.container.getBoundingClientRect();
        const x = e.clientX - rect.left + this.designer.container.scrollLeft;
        const y = e.clientY - rect.top + this.designer.container.scrollTop;
        
        this.designer.uiManager._showNodeTypeMenu(x, y, e.clientX, e.clientY, srcNode, srcPort);
      }
    };
    
    const startConn = (btn, e) => {
      srcNode = node.id;
      srcPort = btn.dataset.port;
      
      this.designer.svg.appendChild(ghost);
      document.addEventListener('mousemove', onGhostMove);
      document.addEventListener('mouseup', onGhostEnd, { once: true });
      
      e.preventDefault();
    };
    
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'pf-delete-btn';
    deleteBtn.innerHTML = '<i class="ti ti-x"></i>';
    deleteBtn.title = 'Delete node';
    
    deleteBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.designer.removeNode(node.id);
    });
    
    el.appendChild(deleteBtn);
    
    const statusBadge = document.createElement('div');
    statusBadge.className = 'pf-status-badge';
    el.appendChild(statusBadge);
    
    const inWrap = document.createElement('div');
    const outWrap = document.createElement('div');
    
    this._renderPorts(node, el, inWrap, outWrap, startConn);
    
    let dragX = 0, dragY = 0, dragging = false;
    
    const onMove = e => {
      if (!dragging) return;
      
      node.x = e.clientX - dragX;
      node.y = e.clientY - dragY;
      
      this._updateNodePos(node);
      this.designer.edgeManager._redrawEdgesFor(node.id);
      this.designer._updateFlowInput();
    };
    
    el.addEventListener('mousedown', e => {
      if (e.button !== 0 || e.target.closest('.pf-delete-btn') || e.target.closest('.pf-port')) return;
      
      dragging = true;
      dragX = e.clientX - node.x;
      dragY = e.clientY - node.y;
      
      el.classList.add('pf-dragging');
      
      document.addEventListener('mousemove', onMove);
      
      document.addEventListener('mouseup', () => {
        dragging = false;
        el.classList.remove('pf-dragging');
        document.removeEventListener('mousemove', onMove);
      }, { once: true });
    });
    
    el.addEventListener('click', e => {
      if (e.target.closest('.pf-port') || e.target.closest('.pf-delete-btn')) return;
      
      this._selectNode(node.id);
    });
    
    el.addEventListener('dblclick', e => {
      if (e.target.closest('.pf-port') || e.target.closest('.pf-delete-btn')) return;
      
      this.designer.uiManager._renderConfig(node);
    });

    el.addEventListener('mouseenter', () => {
        this._highlightNodeConnections(node.id, true);
    });
    
    el.addEventListener('mouseleave', () => {
        this._highlightNodeConnections(node.id, false);
    });
    
    el.addEventListener('click', e => {
        if (e.target.closest('.pf-port') || e.target.closest('.pf-delete-btn')) return;
        this._selectNode(node.id);
        this._highlightNodeConnections(node.id, true);
    });
    
    node.el = el;
    this.designer.nodeLayer.appendChild(el);
    this._updateNodePos(node);
  }
  
  _highlightNodeConnections(nodeId, highlight = true) {
    this.designer.edges.forEach(edge => {
        if (edge.source === nodeId || edge.target === nodeId) {
            edge.pathEl.classList.toggle('pf-edge-highlighted', highlight);
        }
    });
  }
  
  _updateNodePos(node) {
    node.el.style.transform = `translate(${node.x}px, ${node.y}px)`;
  }
  
  _selectNode(id) {
    if (this.designer._selected === id) return;
    
    if (this.designer._selected && this.designer.nodesById[this.designer._selected]) {
      this.designer.nodesById[this.designer._selected].el.classList.remove('pf-selected');
    }
    
    this.designer._selected = id;
    
    if (id && this.designer.nodesById[id]) {
      this.designer.nodesById[id].el.classList.add('pf-selected');
    }
  }
  
  _renderPorts(node, el, inWrap, outWrap, startConn) {
    const info = this.designer.nodeTypes[node.type] || {};
    
    inWrap.innerHTML = '';
    outWrap.innerHTML = '';
    
    if (inWrap.parentNode) inWrap.parentNode.removeChild(inWrap);
    if (outWrap.parentNode) outWrap.parentNode.removeChild(outWrap);
    
    inWrap.className = 'pf-ports pf-inputs';
    outWrap.className = 'pf-ports pf-outputs';
    
    const mkPortBtn = (name, isOut, description) => {
      const wrapper = document.createElement('div');
      wrapper.className = `pf-port-wrapper ${isOut ? 'pf-out-wrapper' : 'pf-in-wrapper'}`;
      
      const b = document.createElement('button');
      b.className = `pf-port ${isOut ? 'pf-out' : 'pf-in'}`;
      b.title = description || name;
      b.dataset.port = name;
      b.dataset.io = isOut ? 'out' : 'in';
      
      const label = document.createElement('span');
      label.className = 'pf-port-label';
      label.textContent = name;
      wrapper.appendChild(label);

      wrapper.appendChild(b);
      
      return wrapper;
    };
    
    Object.entries(info.inputPorts || { input: 'Default input port' })
      .forEach(([p, desc]) => inWrap.appendChild(mkPortBtn(p, false, desc)));
    
    Object.entries(info.outputPorts || { output: 'Default output port' })
      .forEach(([p, desc]) => outWrap.appendChild(mkPortBtn(p, true, desc)));
    
    el.appendChild(inWrap);
    el.appendChild(outWrap);
    
    el.querySelectorAll('.pf-port.pf-out').forEach(btn => {
      btn.addEventListener('mousedown', e => {
        if (e.button !== 0) return;
        e.stopPropagation();
        
        if (typeof startConn === 'function') startConn(btn, e);
      });
      
      btn.addEventListener('contextmenu', e => {
        e.preventDefault();
        e.stopPropagation();
        
        const rect = this.designer.container.getBoundingClientRect();
        const p1 = this.designer.edgeManager._portCenter(node.id, btn.dataset.port, true);
        
        const x = p1.x + 40;
        const y = p1.y;
        
        this.designer.uiManager._showNodeTypeMenu(x, y, e.clientX, e.clientY, node.id, btn.dataset.port);
      });
    });
  }
  
  _updateConfigSummary(node, summaryEl) {
    if (node.type.toUpperCase() === 'START') {
      this._renderStartNodeSummary(summaryEl);
      return;
    }

    if (node.type.toUpperCase() === 'END') {
      this._renderEndNodeSummary(node, summaryEl);
      return;
    }

    if (
      !node.data ||
      Object.keys(node.data).filter(
      k =>
        k !== 'nodeId' &&
        k.toUpperCase() !== 'PORTORIENTATION' &&
        k.toUpperCase() !== 'ID'
      ).length === 0
    ) {
      summaryEl.innerHTML = '<div class="summary-empty">Pas de configuration</div>';
      return;
    }

    const summary = [];
    const info = this.designer.nodeTypes[node.type] || {};
    const vars = info.variables || {};

    const keys = Object.keys(node.data).filter(
      k =>
      k !== 'nodeId' &&
      k.toUpperCase() !== 'PORTORIENTATION' &&
      k.toUpperCase() !== 'ID'
    );

    const displayKeys = keys.slice(0, 4);
    
    displayKeys.forEach(key => {
      let value = node.data[key];

      const label = this.designer.nodeTypes[node.type]?.variables?.[key]?.title || key;
      
      if (typeof value === 'boolean') {
        value = value ? 'true' : 'false';
      } else if (typeof value === 'object') {
        value = '{ ... }';
      } else if (typeof value === 'string' && value.length > 50) {
        value = value.substring(0, 47) + '...';
      }
      
      summary.push(`
        <div class="summary-item">
          <code class="summary-label">${label}</code>
          <code class="summary-value">${value}</code>
        </div>`);
    });
    
    if (keys.length > displayKeys.length) {
      summary.push(`<div class="summary-more">+${keys.length - displayKeys.length} autre(s)...</div>`);
    }
    
    summaryEl.innerHTML = summary.join('');
  }

  /**
   * Renders the end node summary showing configured output keys and their source variables.
   * @param {Object} node - The end node.
   * @param {HTMLElement} summaryEl - The summary container element.
   */
  _renderEndNodeSummary(node, summaryEl) {
    const outputs = node.data?.outputs;
    if (!Array.isArray(outputs) || outputs.length === 0) {
      summaryEl.innerHTML = '<div class="summary-empty">Aucune sortie définie</div>';
      return;
    }
    const items = outputs.map(o => {
      const key = o.outputKey || '?';
      let val = (o.outputValue || '').trim();
      if (val.length > 40) val = val.substring(0, 37) + '...';
      return `<div class="summary-item">
        <code class="summary-label">${key}</code>
        <code class="summary-value">${val}</code>
      </div>`;
    });
    summaryEl.innerHTML = items.join('');
  }

  /**
   * Renders the start node summary with detected system variables.
   * @param {HTMLElement} summaryEl - The summary container element.
   */
  _renderStartNodeSummary(summaryEl) {
    const schema = this.designer.inputSchema || [];
    if (schema.length === 0) {
      summaryEl.innerHTML = '<div class="summary-empty">Aucun champ d\'entrée défini</div>';
      return;
    }
    const typeLabels = { STRING: 'Texte', NUMBER: 'Nombre', BOOLEAN: 'Oui/Non', ENUM: 'Liste', FILE: 'Fichier' };
    const items = schema.map(f => {
      const label = f.title || f.name;
      const type = typeLabels[f.type] || f.type;
      return `<div class="summary-item">
        <code class="summary-label">${label}${f.required ? ' *' : ''}</code>
        <code class="summary-value">${type}${f.multiple ? ' (multiple)' : ''}</code>
      </div>`;
    });
    summaryEl.innerHTML = items.join('');
  }

  updateNodeStatus(nodeId, status, message = '') {
    let node = this.designer.nodesById[nodeId];
    
    if (!node) {
      node = this.designer.findNodeByDataId(nodeId);
      
      if (!node) {
        console.warn(`Node with ID ${nodeId} not found`);
        return;
      }
    }
    
    node.el.setAttribute('data-status', status);
    
    if (status === 'success' || status === 'error') {
      node.el.setAttribute('data-streaming', 'false');
      
      setTimeout(() => {
        node.el.setAttribute('data-status', 'completed');
      }, 3000);
    }
    
    let badge = node.el.querySelector('.pf-status-badge');
    
    if (!badge) {
      badge = document.createElement('div');
      badge.className = 'pf-status-badge';
      node.el.appendChild(badge);
    }
    
    switch (status) {
      case 'running':
      badge.innerHTML = '<i class="ti ti-loader ti-loader-2 me-1"></i> En cours';
      break;
      case 'success':
      badge.innerHTML = '<i class="ti ti-check me-1"></i> Succès';
      break;
      case 'error':
      badge.innerHTML = '<i class="ti ti-alert-triangle me-1"></i> Erreur';
      break;
      default:
      badge.textContent = '';
    }
    
    if (message) {
      node.el.title = message;
    }
  }
}