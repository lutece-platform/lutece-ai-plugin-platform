export default class EdgeManager {
  constructor(pipelineDesigner) {
    this.designer = pipelineDesigner;
  }
  
addEdge({ source, sourcePort, target, targetPort }) {
    if (!this.designer.nodesById[source] || !this.designer.nodesById[target]) return null;
    
    if (this.designer.edges.find(e => 
        e.source === source && 
        e.sourcePort === sourcePort && 
        e.target === target && 
        e.targetPort === targetPort
    )) return null;
    
    const id = `e-${++this.designer._edgeSeq}`;
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('data-id', id);
    path.classList.add('pf-edge');
    
    const edgeControls = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    edgeControls.classList.add('pf-edge-controls');
    edgeControls.innerHTML = `
        <circle class="pf-edge-delete" cx="0" cy="0" r="8"></circle>
        <text class="pf-edge-delete-icon" x="0" y="0" text-anchor="middle" dominant-baseline="central">×</text>
    `;
    
    path.addEventListener('mouseover', () => {
        path.classList.add('pf-edge-hover');
        this._updateEdgeControlsPosition(edgeControls, path);
        this.designer.svg.appendChild(edgeControls);
    });
    
    path.addEventListener('mouseout', (e) => {
        if (!edgeControls.contains(e.relatedTarget)) {
            path.classList.remove('pf-edge-hover');
            edgeControls.remove();
        }
    });
    
    edgeControls.addEventListener('mouseover', () => {
        path.classList.add('pf-edge-hover');
    });
    
    edgeControls.addEventListener('mouseout', (e) => {
        if (!path.contains(e.relatedTarget)) {
            path.classList.remove('pf-edge-hover');
            edgeControls.remove();
        }
    });
    
    const deleteBtn = edgeControls.querySelector('.pf-edge-delete');
    deleteBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        this.removeEdge(id);
    });
    
    const deleteIcon = edgeControls.querySelector('.pf-edge-delete-icon');
    deleteIcon.addEventListener('click', (e) => {
        e.stopPropagation();
        this.removeEdge(id);
    });
    
    this.designer.svg.appendChild(path);
    
    const edge = { id, source, sourcePort, target, targetPort, pathEl: path };
    this.designer.edges.push(edge);
    this._updateEdgePath(edge);
    this.designer._updateFlowInput();
    
    return id;
}
  
  _updateEdgeControlsPosition(controlsEl, pathEl) {
    const pathLength = pathEl.getTotalLength();
    const midpoint = pathEl.getPointAtLength(pathLength / 2);
    
    controlsEl.setAttribute('transform', `translate(${midpoint.x}, ${midpoint.y})`);
  }
  
  removeEdge(id) {
    const i = this.designer.edges.findIndex(e => e.id === id);
    if (i < 0) return;
    
    this.designer.edges[i].pathEl.remove();
    this.designer.edges.splice(i, 1);
    this.designer._updateFlowInput();
  }
  
  _portCenter(nodeId, port, isOut) {
    const node = this.designer.nodesById[nodeId];
    if (!node) return { x: 0, y: 0 };
    
    const sel = `.pf-port.${isOut ? 'pf-out' : 'pf-in'}[data-port="${CSS.escape(port)}"]`;
    const el = node.el.querySelector(sel);
    if (!el) return { x: 0, y: 0 };
    
    const r = el.getBoundingClientRect();
    const rc = this.designer.container.getBoundingClientRect();
    
    return {
      x: r.left + r.width / 2 - rc.left + this.designer.container.scrollLeft,
      y: r.top + r.height / 2 - rc.top + this.designer.container.scrollTop
    };
  }
  
  _updateEdgePath(edge) {
    const p1_screen = this._portCenter(edge.source, edge.sourcePort, true);
    const p2_screen = this._portCenter(edge.target, edge.targetPort, false);
    
    const p1 = this.designer.renderManager._getCoordsInCanvas(p1_screen.x, p1_screen.y);
    const p2 = this.designer.renderManager._getCoordsInCanvas(p2_screen.x, p2_screen.y);
    
    edge.pathEl.setAttribute('d', this.bezierPath(p1.x, p1.y, p2.x, p2.y));
  }
  
  _redrawEdgesFor(id) {
    this.designer.edges
      .filter(e => e.source === id || e.target === id)
      .forEach(e => this._updateEdgePath(e));
  }
  
  redrawAllEdges() {
    this.designer.svg.setAttribute('width', '100%');
    this.designer.svg.setAttribute('height', '100%');
    this.designer.svg.style.overflow = 'visible';
    
    this.designer.edges.forEach(edge => this._updateEdgePath(edge));
  }
  
  updateEdgeStatus(sourceId, targetId, active = true) {
    let edge = this.designer.edges.find(e => e.source === sourceId && e.target === targetId);
    
    if (!edge) {
      const sourceNode = this.designer.findNodeByDataId(sourceId);
      const targetNode = this.designer.findNodeByDataId(targetId);
      
      if (sourceNode && targetNode) {
        edge = this.designer.edges.find(e => e.source === sourceNode.id && e.target === targetNode.id);
      }
    }
    
    if (edge) {
      edge.pathEl.setAttribute('data-active', active.toString());
      
      if (active) {
        setTimeout(() => {
          edge.pathEl.setAttribute('data-active', 'false');
        }, 3000);
      }
    }
  }
  
  bezierPath(x1, y1, x2, y2) {
    const dx = Math.abs(x2 - x1);
    const dy = Math.abs(y2 - y1);
    const curve = Math.min(dx * 0.5, 80);
    
    let path;
    
    if (x2 > x1) {
      path = `M${x1},${y1} C${x1 + curve},${y1} ${x2 - curve},${y2} ${x2},${y2}`;
    } else {
      const midX = (x1 + x2) / 2;
      path = `M${x1},${y1} C${midX},${y1} ${midX},${y2} ${x2},${y2}`;
    }
    
    return path;
  }
}